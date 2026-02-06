package com.awabi2048.ccsystem

import org.bukkit.plugin.java.JavaPlugin
import com.awabi2048.ccsystem.api.CCSystemAPI
import com.awabi2048.ccsystem.api.CCSystemAPIImpl
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.config.MessageManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import com.awabi2048.ccsystem.features.misc.command.CCSystemCommand
import com.awabi2048.ccsystem.features.misc.command.DelayCommand
import com.awabi2048.ccsystem.features.misc.command.NpcMessageCommand
import com.awabi2048.ccsystem.features.misc.listener.MusicListener
import com.awabi2048.ccsystem.features.misc.listener.ShiftFBinderListener
import com.awabi2048.ccsystem.features.misc.listener.PlayerDataListener
import com.awabi2048.ccsystem.features.misc.listener.PlayerDeathListener
import com.awabi2048.ccsystem.features.misc.listener.WorldListener
import com.awabi2048.ccsystem.features.resourceworld.command.ResourceCommand
import com.awabi2048.ccsystem.features.resourceworld.listener.ResourceListener

class CCSystem : JavaPlugin() {
    
    companion object {
        lateinit var instance: CCSystem
            private set
        
        private lateinit var _api: CCSystemAPI
        
        /**
         * CC-System APIを取得します
         * 他のプラグインがこのメソッド経由でCC-Systemの機能を利用できます
         */
        fun getAPI(): CCSystemAPI = _api
    }
    
    lateinit var musicListener: MusicListener
    
    override fun onEnable() {
        // インスタンス保存
        instance = this
        
        // API初期化
        _api = CCSystemAPIImpl()
        
        // 設定読み込み
        saveDefaultConfig()
        reloadConfig()
        
        // マネージャー初期化
        ConfigManager.load(config)
        LanguageManager.load()
        MessageManager.load()
        PlayerDataManager.load()
        
        // リスナー初期化
        musicListener = MusicListener()
        
        // リスナー登録
        server.pluginManager.registerEvents(musicListener, this)
        server.pluginManager.registerEvents(ShiftFBinderListener(), this)
        server.pluginManager.registerEvents(PlayerDataListener(), this)
        server.pluginManager.registerEvents(PlayerDeathListener(), this)
        server.pluginManager.registerEvents(WorldListener(), this)
        server.pluginManager.registerEvents(ResourceListener(), this)
        
        // コマンド登録
        getCommand("resource")?.setExecutor(ResourceCommand())
        getCommand("delay")?.setExecutor(DelayCommand())
        getCommand("npc_message")?.setExecutor(NpcMessageCommand())
        getCommand("ccsystem")?.setExecutor(CCSystemCommand())
        
        logger.info("CC-System v${description.version} を有効化しました")
    }

    override fun onDisable() {
        logger.info("CC-System v${description.version} を無効化しました")
    }
}
