package com.awabi2048.ccsystem.core.skin

import com.awabi2048.ccsystem.api.skin.BedrockSkin
import com.awabi2048.ccsystem.api.skin.BedrockSkinLookupResult
import com.awabi2048.ccsystem.api.skin.BedrockSkinService
import com.google.gson.JsonParser
import org.bukkit.Bukkit
import org.geysermc.floodgate.api.FloodgateApi
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletableFuture

internal class BedrockSkinServiceImpl : BedrockSkinService {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    override fun fetch(playerName: String): CompletableFuture<BedrockSkinLookupResult> {
        if (!Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            return CompletableFuture.completedFuture(BedrockSkinLookupResult.FloodgateUnavailable)
        }
        val floodgate = FloodgateApi.getInstance()
        val prefix = floodgate.playerPrefix
        val requestedName = playerName.trim()
        val gamertag = if (prefix.isNotEmpty() && requestedName.startsWith(prefix)) requestedName.removePrefix(prefix) else requestedName
        if (gamertag.isBlank()) return CompletableFuture.completedFuture(BedrockSkinLookupResult.PlayerNotFound)

        // 認識名を使えない場合にも接頭辞を二重に付けず、入力名からヘッド名を確定できます。
        val normalizedRequestedName = if (prefix.isEmpty() || requestedName.startsWith(prefix)) {
            requestedName
        } else {
            prefix + requestedName
        }

        // 参加中はFloodgateの確定済みXUIDを優先し、オフラインの場合だけ名前解決APIを利用します。
        val online = floodgate.players.firstOrNull {
            it.correctUsername.equals(requestedName, true) || it.username.equals(gamertag, true)
        }
        val xuidFuture = online?.xuid?.toLongOrNull()?.let { CompletableFuture.completedFuture(it) }
            ?: floodgate.getXuidFor(gamertag)
        return xuidFuture.thenCompose { xuid ->
            if (xuid == null || xuid <= 0) {
                return@thenCompose CompletableFuture.completedFuture(BedrockSkinLookupResult.PlayerNotFound)
            }
            val request = HttpRequest.newBuilder(URI.create("https://api.geysermc.org/v2/skin/$xuid"))
                .timeout(Duration.ofSeconds(10)).GET().build()
            http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply { response ->
                when (response.statusCode()) {
                    404 -> BedrockSkinLookupResult.SkinNotFound
                    200 -> {
                        runCatching {
                            val json = JsonParser.parseString(response.body()).asJsonObject
                            val value = json.get("value")?.takeUnless { it.isJsonNull }?.asString
                            if (value.isNullOrBlank()) return@runCatching BedrockSkinLookupResult.InvalidResponse
                            val skin = BedrockSkin(
                                gamertag = gamertag,
                                serverName = online?.correctUsername?.takeIf(String::isNotBlank)
                                    ?: normalizedRequestedName,
                                xuid = xuid.toString(),
                                value = value,
                                signature = json.get("signature")?.takeUnless { it.isJsonNull }?.asString,
                            )
                            BedrockSkinLookupResult.Found(skin)
                        }.getOrElse { BedrockSkinLookupResult.InvalidResponse }
                    }
                    else -> BedrockSkinLookupResult.ServiceUnavailable
                }
            }
        }
            .exceptionally { BedrockSkinLookupResult.ServiceUnavailable }
    }
}
