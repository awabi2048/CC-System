package com.awabi2048.ccsystem.features.misc.displayparticle

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectStartResult
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectServiceImpl
import com.awabi2048.ccsystem.core.displayeffect.DisplayParticleBookJsonParser
import com.awabi2048.ccsystem.core.displayeffect.ParsedDisplayParticleBook
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID

internal class DisplayParticleBookTestController(
    private val plugin: Plugin,
    private val displayEffectService: DisplayEffectServiceImpl
) : Listener {
    private val enabledPlayers = mutableSetOf<UUID>()
    private val lastHandledTick = mutableMapOf<UUID, Int>()
    private val cachedBooks = mutableMapOf<UUID, CachedBook>()
    private val cacheIdKey = NamespacedKey(plugin, "display-particle-book-cache")

    fun toggle(player: Player): Boolean {
        val enabled = if (enabledPlayers.remove(player.uniqueId)) false else {
            enabledPlayers += player.uniqueId
            true
        }
        if (!enabled) {
            cachedBooks.remove(player.uniqueId)
            lastHandledTick.remove(player.uniqueId)
        }
        player.sendMessage(message(player, if (enabled) "management.debug.particle_test_enabled" else "management.debug.particle_test_disabled"))
        return enabled
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBookClose(event: PlayerEditBookEvent) {
        val player = event.player
        if (player.uniqueId !in enabledPlayers) return

        val editedMeta = event.newBookMeta
        // 署名すると本と羽ペンではなくなるため、テスト用キャッシュとしては扱いません。
        if (event.isSigning) {
            cachedBooks.remove(player.uniqueId)
            editedMeta.persistentDataContainer.remove(cacheIdKey)
            event.newBookMeta = editedMeta
            return
        }

        val serializer = PlainTextComponentSerializer.plainText()
        val pages = editedMeta.pages().map(serializer::serialize)
        runCatching { DisplayParticleBookJsonParser.parsePages(pages) }
            .onSuccess { parsed ->
                // PDCの識別子とプレイヤー単位キャッシュを対応させ、別の本や編集前の本の誤使用を防ぎます。
                val cacheId = UUID.randomUUID().toString()
                editedMeta.persistentDataContainer.set(cacheIdKey, PersistentDataType.STRING, cacheId)
                event.newBookMeta = editedMeta
                cachedBooks[player.uniqueId] = CachedBook(cacheId, parsed)
                player.sendMessage(message(player, "management.debug.particle_test_cached"))
            }
            .onFailure { failure ->
                cachedBooks.remove(player.uniqueId)
                editedMeta.persistentDataContainer.remove(cacheIdKey)
                event.newBookMeta = editedMeta
                player.sendMessage(
                    message(player, "management.debug.particle_test_invalid_json", mapOf("detail" to failure.detail()))
                )
            }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onLeftClick(event: PlayerAnimationEvent) {
        if (event.animationType != PlayerAnimationType.ARM_SWING) return
        val player = event.player
        if (player.uniqueId !in enabledPlayers) return
        if (player.inventory.itemInMainHand.type != Material.WRITABLE_BOOK) return
        // テスト用の本を振った操作は、通常の左クリック連携へ重複して送りません。
        event.isCancelled = true
        val tick = Bukkit.getCurrentTick()
        if (lastHandledTick.put(player.uniqueId, tick) == tick) return

        val meta = player.inventory.itemInMainHand.itemMeta as? BookMeta ?: return
        val cacheId = meta.persistentDataContainer.get(cacheIdKey, PersistentDataType.STRING)
        val cached = cachedBooks[player.uniqueId]
        if (cacheId == null || cached == null || cached.cacheId != cacheId) {
            player.sendMessage(message(player, "management.debug.particle_test_not_cached"))
            return
        }

        runCatching {
            val parsed = cached.parsed
            val location = player.location.clone().add(parsed.offset.x, parsed.offset.y, parsed.offset.z)
            // キャッシュされた設定は再利用しつつ、発生ごとの個体差は固定されないようシードだけを更新します。
            val request = parsed.request.copy(randomSeed = System.nanoTime())
            displayEffectService.emitTransientDisplayParticles(plugin, location, parsed.preset, request)
        }.onSuccess { result ->
            when (result) {
                // 連続表示でチャットを埋めないよう、正常開始時は通知しません。
                is DisplayEffectStartResult.Started -> Unit
                is DisplayEffectStartResult.Rejected -> player.sendMessage(
                    message(player, "management.debug.particle_test_rejected", mapOf("detail" to result.message))
                )
            }
        }.onFailure { failure ->
            player.sendMessage(
                message(player, "management.debug.particle_test_invalid_json", mapOf("detail" to failure.detail()))
            )
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        enabledPlayers.remove(event.player.uniqueId)
        lastHandledTick.remove(event.player.uniqueId)
        cachedBooks.remove(event.player.uniqueId)
    }

    private fun message(player: Player, key: String, placeholders: Map<String, Any> = emptyMap()): String =
        LanguageManager.getUnified().getString(player, key, placeholders)

    private fun Throwable.detail(): String = message ?: this::class.simpleName.orEmpty()

    private data class CachedBook(
        val cacheId: String,
        val parsed: ParsedDisplayParticleBook
    )
}
