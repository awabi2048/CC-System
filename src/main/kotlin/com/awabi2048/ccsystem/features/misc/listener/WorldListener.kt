package com.awabi2048.ccsystem.features.misc.listener

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import org.bukkit.GameRules
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldLoadEvent

/**
 * ワールドリスナー
 */
class WorldListener : Listener {

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        // 分割設定で機能が有効になっているかチェック
        if (!ConfigManager.isGlobalSoundEventsAutoDisable()) {
            return
        }

        // globalSoundEvents ゲームルールを false に設定
        val world = event.world
        world.setGameRule(GameRules.GLOBAL_SOUND_EVENTS, false)
        
        CCSystem.instance.logger.info("Set globalSoundEvents to false for world: ${world.name}")
    }
}
