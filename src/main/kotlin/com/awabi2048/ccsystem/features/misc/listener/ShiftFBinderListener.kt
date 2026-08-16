package com.awabi2048.ccsystem.features.misc.listener

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.api.input.PlayerInteractionChannel
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
        if (!ConfigManager.isShiftFBinderEnabled()) {
            return
        }

        val player = event.player
        if (CCSystem.getAPI().getPlayerInteractionClaimService().isClaimed(player.uniqueId, PlayerInteractionChannel.SWAP_HAND)) {
            return
        }
        
        // スニーク中ならキャンセルしてコマンド実行
        if (player.isSneaking) {
            val commands = ConfigManager.getShiftFBinderCommands()
            
            if (commands.isEmpty()) return
            
            event.isCancelled = true
            
            BinderCommandExecutor.execute(player, commands)
        }
    }
}
