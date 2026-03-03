package com.awabi2048.ccsystem.features.misc.listener

import com.awabi2048.ccsystem.core.data.PlayerDataManager
import com.awabi2048.ccsystem.features.announce.manager.AnnouncementManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.time.Instant

/**
 * プレイヤーデータリスナー
 */
class PlayerDataListener : Listener {

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        PlayerDataManager.set(
            event.player.uniqueId,
            AnnouncementManager.PLAYER_DATA_LAST_LOGOUT_AT,
            Instant.now().toString()
        )
        PlayerDataManager.unload(event.player.uniqueId)
    }
}
