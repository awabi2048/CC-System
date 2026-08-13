package com.awabi2048.ccsystem.core.gesturegui

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent

/** 座標の連続追従では安全に扱えないライフサイクル境界だけを即時終了へ接続します。 */
class GestureGuiLifecycleListener(private val service: GestureGuiServiceImpl) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) = service.leaveImmediately(event.player.uniqueId)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDeath(event: PlayerDeathEvent) = service.leaveImmediately(event.entity.uniqueId)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) = service.leaveImmediately(event.player.uniqueId)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onWorldChanged(event: PlayerChangedWorldEvent) = service.leaveImmediately(event.player.uniqueId)
}
