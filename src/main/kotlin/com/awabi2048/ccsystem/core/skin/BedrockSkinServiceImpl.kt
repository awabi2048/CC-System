package com.awabi2048.ccsystem.core.skin

import com.awabi2048.ccsystem.api.skin.BedrockSkin
import com.awabi2048.ccsystem.api.skin.BedrockSkinService
import com.google.gson.JsonParser
import org.bukkit.Bukkit
import org.geysermc.floodgate.api.FloodgateApi
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletableFuture

internal class BedrockSkinServiceImpl : BedrockSkinService {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    override fun fetch(playerName: String): CompletableFuture<BedrockSkin?> {
        if (!Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            return CompletableFuture.failedFuture(IllegalStateException("Floodgate is unavailable"))
        }
        val floodgate = FloodgateApi.getInstance()
        val prefix = floodgate.playerPrefix
        val gamertag = if (prefix.isNotEmpty() && playerName.startsWith(prefix)) playerName.removePrefix(prefix) else playerName
        if (gamertag.isBlank()) return CompletableFuture.completedFuture(null)

        // 参加中はFloodgateの確定済みXUIDを優先し、オフラインの場合だけ名前解決APIを利用します。
        val online = floodgate.players.firstOrNull {
            it.correctUsername.equals(gamertag, true) || it.javaUsername.equals(playerName, true)
        }
        val xuidFuture = online?.xuid?.toLongOrNull()?.let { CompletableFuture.completedFuture(it) }
            ?: floodgate.getXuidFor(gamertag)
        return xuidFuture.thenCompose { xuid ->
            if (xuid == null || xuid <= 0) return@thenCompose CompletableFuture.completedFuture(null)
            val request = HttpRequest.newBuilder(URI.create("https://api.geysermc.org/v2/skin/$xuid"))
                .timeout(Duration.ofSeconds(10)).GET().build()
            http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply { response ->
                when (response.statusCode()) {
                    404 -> null
                    200 -> {
                        val json = JsonParser.parseString(response.body()).asJsonObject
                        val value = json.get("value")?.takeUnless { it.isJsonNull }?.asString
                        if (value.isNullOrBlank()) null else BedrockSkin(
                            gamertag, xuid.toString(), value,
                            json.get("signature")?.takeUnless { it.isJsonNull }?.asString,
                        )
                    }
                    else -> error("Geyser skin API returned HTTP ${response.statusCode()}")
                }
            }
        }
    }
}
