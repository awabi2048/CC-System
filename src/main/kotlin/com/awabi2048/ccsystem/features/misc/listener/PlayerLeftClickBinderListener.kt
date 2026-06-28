package com.awabi2048.ccsystem.features.misc.listener

import com.awabi2048.ccsystem.api.event.PlayerLeftClickPlayerEvent
import com.awabi2048.ccsystem.core.config.ConfigManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class PlayerLeftClickBinderListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerLeftClickPlayer(event: PlayerLeftClickPlayerEvent) {
        if (!ConfigManager.isPlayerLeftClickBinderEnabled()) {
            return
        }

        BinderCommandExecutor.execute(event.player, ConfigManager.getPlayerLeftClickBinderCommands(), event.target)
    }
}
