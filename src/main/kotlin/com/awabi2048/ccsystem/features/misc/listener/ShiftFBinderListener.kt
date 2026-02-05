package com.awabi2048.ccsystem.features.misc.listener

import com.awabi2048.ccsystem.core.config.ConfigManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerSwapHandItemsEvent

/**
 * Shift + F バインダーリスナー
 */
class ShiftFBinderListener : Listener {

    @EventHandler
    fun onShiftF(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        
        // スニーク中ならキャンセルしてコマンド実行
        if (player.isSneaking) {
            val commands = ConfigManager.getShiftFBinderCommands()
            
            if (commands.isEmpty()) return
            
            event.isCancelled = true
            
            for (command in commands) {
                if (command.isEmpty()) continue
                
                val processedCommand = command
                    .replace("%player_name%", player.name)
                    .replace("%player_uuid%", player.uniqueId.toString())
                
                player.performCommand(processedCommand)
            }
        }
    }
}