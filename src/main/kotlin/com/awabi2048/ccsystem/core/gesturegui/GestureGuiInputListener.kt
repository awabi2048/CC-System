package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.destroystokyo.paper.event.player.PlayerJumpEvent
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

/**
 * Bukkitの複数イベントをジェスチャーへ正規化します。
 * 同じパケット由来のInteract/InteractAt等はtick単位で一度だけActionへ渡します。
 */
class GestureGuiInputListener(private val service: GestureGuiServiceImpl) : Listener {
    private data class InputKey(val playerId: UUID, val gesture: GestureGuiGesture)
    private val handledTick = mutableMapOf<InputKey, Int>()

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onAnimation(event: PlayerAnimationEvent) {
        if (event.animationType != PlayerAnimationType.ARM_SWING) return
        if (dispatch(event.player, primary(event.player))) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onWorldInteract(event: PlayerInteractEvent) {
        val gesture = when {
            event.action.isLeftClick -> primary(event.player)
            event.action.isRightClick -> secondary(event.player)
            else -> return
        }
        if (dispatch(event.player, gesture)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onEntityInteract(event: PlayerInteractEntityEvent) {
        val catcher = service.ownsCatcher(event.rightClicked)
        // catcherは背後のブロックを遮る入口であり、正規の要素判定は同じ視線rayで行います。
        val handled = dispatch(event.player, secondary(event.player))
        if (catcher || handled) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onEntityInteractAt(event: PlayerInteractAtEntityEvent) {
        val catcher = service.ownsCatcher(event.rightClicked)
        val handled = dispatch(event.player, secondary(event.player))
        if (catcher || handled) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onEntityAttack(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val catcher = service.ownsCatcher(event.entity)
        val handled = dispatch(player, primary(player))
        if (catcher || handled) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onSwapHand(event: PlayerSwapHandItemsEvent) {
        // PUBLIC画面ではFが第三者の最初の操作にもなり得ます。Action解決中にclaimを取得するため、
        // 後続のFreecam listenerは同じイベントをジェスチャーGUI所有として認識できます。
        if (dispatch(event.player, GestureGuiGesture.SWAP_HAND)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onJump(event: PlayerJumpEvent) {
        if (!event.player.isSneaking || !service.leaveOrClose(event.player.uniqueId)) return
        event.isCancelled = true
    }

    private fun primary(player: Player) =
        if (player.isSneaking) GestureGuiGesture.SHIFT_PRIMARY else GestureGuiGesture.PRIMARY

    private fun secondary(player: Player) =
        if (player.isSneaking) GestureGuiGesture.SHIFT_SECONDARY else GestureGuiGesture.SECONDARY

    private fun dispatch(player: Player, gesture: GestureGuiGesture): Boolean {
        val key = InputKey(player.uniqueId, gesture)
        val tick = Bukkit.getCurrentTick()
        if (handledTick[key] == tick) return service.isParticipating(player.uniqueId)
        handledTick[key] = tick
        if (handledTick.size > 256) handledTick.entries.removeIf { it.value < tick - 1 }
        return service.handleGesture(player, gesture)
    }
}
