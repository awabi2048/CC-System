package com.awabi2048.ccsystem.features.misc.listener

import com.awabi2048.ccsystem.core.data.PlayerDataManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/**
 * プレイヤーデータリスナー
 */
class PlayerDataListener : Listener {

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        PlayerDataManager.unload(event.player.uniqueId)
    }
}