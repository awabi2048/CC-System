package com.awabi2048.ccsystem.features.misc.displayparticle

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectStartResult
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectServiceImpl
import com.awabi2048.ccsystem.core.displayeffect.DisplayParticleBookJsonParser
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.plugin.Plugin
import java.util.UUID

internal class DisplayParticleBookTestController(
    private val plugin: Plugin,
    private val displayEffectService: DisplayEffectServiceImpl
) : Listener {
    private val enabledPlayers = mutableSetOf<UUID>()
    private val lastHandledTick = mutableMapOf<UUID, Int>()

    fun toggle(player: Player): Boolean {
        val enabled = if (enabledPlayers.remove(player.uniqueId)) false else {
            enabledPlayers += player.uniqueId
            true
        }
        player.sendMessage(message(player, if (enabled) "management.debug.particle_test_enabled" else "management.debug.particle_test_disabled"))
        return enabled
    }

    @EventHandler(ignoreCancelled = true)
    fun onLeftClick(event: PlayerAnimationEvent) {
        if (event.animationType != PlayerAnimationType.ARM_SWING) return
        val player = event.player
        if (player.uniqueId !in enabledPlayers) return
        if (player.inventory.itemInMainHand.type != Material.WRITABLE_BOOK) return
        val tick = Bukkit.getCurrentTick()
        if (lastHandledTick.put(player.uniqueId, tick) == tick) return

        val meta = player.inventory.itemInMainHand.itemMeta as? BookMeta
        val json = meta?.pages()?.joinToString("\n") { PlainTextComponentSerializer.plainText().serialize(it) }.orEmpty()
        if (json.isBlank()) {
            player.sendMessage(message(player, "management.debug.particle_test_empty_book"))
            return
        }
        runCatching {
            val parsed = DisplayParticleBookJsonParser.parse(json)
            val location = player.location.clone().add(parsed.offset.x, parsed.offset.y, parsed.offset.z)
            displayEffectService.emitTransientDisplayParticles(plugin, location, parsed.preset, parsed.request)
        }.onSuccess { result ->
            when (result) {
                is DisplayEffectStartResult.Started -> player.sendMessage(message(player, "management.debug.particle_test_started"))
                is DisplayEffectStartResult.Rejected -> player.sendMessage(
                    message(player, "management.debug.particle_test_rejected", mapOf("detail" to result.message))
                )
            }
        }.onFailure { failure ->
            player.sendMessage(
                message(player, "management.debug.particle_test_invalid_json", mapOf("detail" to (failure.message ?: failure::class.simpleName.orEmpty())))
            )
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        enabledPlayers.remove(event.player.uniqueId)
        lastHandledTick.remove(event.player.uniqueId)
    }

    private fun message(player: Player, key: String, placeholders: Map<String, Any> = emptyMap()): String =
        LanguageManager.getUnified().getString(player, key, placeholders)
}
