package com.awabi2048.ccsystem

import org.bukkit.plugin.java.JavaPlugin
import com.awabi2048.ccsystem.api.CCSystemAPI
import com.awabi2048.ccsystem.api.CCSystemAPIImpl
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.config.MessageManager
import com.awabi2048.ccsystem.core.data.PlacedBlockLedgerManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import com.awabi2048.ccsystem.features.misc.command.CCSystemCommand
import com.awabi2048.ccsystem.features.misc.command.DelayCommand
import com.awabi2048.ccsystem.features.misc.command.NpcMessageCommand
import com.awabi2048.ccsystem.features.misc.listener.MusicListener
import com.awabi2048.ccsystem.features.misc.listener.DynamicDistanceListener
import com.awabi2048.ccsystem.features.misc.listener.ShiftFBinderListener
import com.awabi2048.ccsystem.features.misc.listener.PlayerDataListener
import com.awabi2048.ccsystem.features.misc.listener.PlayerDeathListener
import com.awabi2048.ccsystem.features.misc.listener.WorldListener
import com.awabi2048.ccsystem.features.publicsign.listener.PublicSignListener
import com.awabi2048.ccsystem.features.publicsign.manager.PublicSignManager
import com.awabi2048.ccsystem.features.rentalarea.listener.RentalAreaListener
import com.awabi2048.ccsystem.features.rentalarea.manager.RentalAreaManager
import com.awabi2048.ccsystem.features.rentalarea.command.RentalReceiveCommand
import com.awabi2048.ccsystem.features.rentalarea.storage.RemainedItemManager
import com.awabi2048.ccsystem.features.resourceworld.command.ResourceCommand
import com.awabi2048.ccsystem.features.resourceworld.listener.ResourceListener
import com.awabi2048.ccsystem.features.resourceworld.manager.ScoreboardManager
import com.awabi2048.ccsystem.features.resourceworld.manager.WorldManager
import com.awabi2048.ccsystem.features.resourceworld.manager.PregenerationStateManager
import com.awabi2048.ccsystem.core.queue.ChunkTaskQueueManager
import java.io.File

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
    lateinit var resourceListener: ResourceListener
    lateinit var dynamicDistanceListener: DynamicDistanceListener

    fun hasMusicListener(): Boolean = ::musicListener.isInitialized
    fun hasResourceListener(): Boolean = ::resourceListener.isInitialized
    fun hasDynamicDistanceListener(): Boolean = ::dynamicDistanceListener.isInitialized

    fun ensureDefaultFiles() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        saveDefaultConfig()

        val langDir = File(dataFolder, "lang")
        if (!langDir.exists()) {
            langDir.mkdirs()
        }

        listOf("ja_jp", "en_us").forEach { lang ->
            val file = File(langDir, "$lang.yml")
            if (!file.exists()) {
                saveResource("lang/$lang.yml", false)
            }
        }

        val rentalAreaFile = File(dataFolder, "rental_area_data.yml")
        if (!rentalAreaFile.exists()) {
            saveResource("rental_area_data.yml", false)
        }

        val placedBlockLedgerFile = File(dataFolder, "placed_block_ledger.yml")
        if (!placedBlockLedgerFile.exists()) {
            saveResource("placed_block_ledger.yml", false)
        }
    }
    
    override fun onEnable() {
        // インスタンス保存
        instance = this
        
        // API初期化
        _api = CCSystemAPIImpl()
        
        // 設定読み込み
        ensureDefaultFiles()
        reloadConfig()
        
        // マネージャー初期化
        ConfigManager.load(config)
        LanguageManager.load()
        MessageManager.load()
        PlayerDataManager.load()
        PlacedBlockLedgerManager.load()
        if (ConfigManager.isRentalAreaEnabled()) {
            RemainedItemManager.load()
            RentalAreaManager.load()
        }
        if (ConfigManager.isPublicSignEnabled()) {
            PublicSignManager.load()
        }
        
        // 資源ワールドマネージャー初期化
        if (ConfigManager.isResourceWorldEnabled()) {
            PregenerationStateManager.load()
            WorldManager.loadExistingWorlds()
            ScoreboardManager.init()
        }

        // チャンクタスクキューマネージャー初期化
        ChunkTaskQueueManager.load()
        
        // リスナー初期化
        if (ConfigManager.isMusicEnabled()) {
            musicListener = MusicListener()
        }
        if (ConfigManager.isResourceWorldEnabled()) {
            resourceListener = ResourceListener()
        }
        if (ConfigManager.getDynamicDistanceSettings().enabled) {
            dynamicDistanceListener = DynamicDistanceListener()
        }
        
        // リスナー登録
        if (hasMusicListener()) {
            server.pluginManager.registerEvents(musicListener, this)
        }
        if (ConfigManager.isShiftFBinderEnabled()) {
            server.pluginManager.registerEvents(ShiftFBinderListener(), this)
        }
        server.pluginManager.registerEvents(PlayerDataListener(), this)
        server.pluginManager.registerEvents(PlayerDeathListener(), this)
        if (ConfigManager.isGlobalSoundEventsAutoDisable()) {
            server.pluginManager.registerEvents(WorldListener(), this)
        }
        if (hasResourceListener()) {
            server.pluginManager.registerEvents(resourceListener, this)
        }
        if (hasDynamicDistanceListener()) {
            server.pluginManager.registerEvents(dynamicDistanceListener, this)
        }
        if (ConfigManager.isPublicSignEnabled()) {
            server.pluginManager.registerEvents(PublicSignListener(), this)
        }
        if (ConfigManager.isRentalAreaEnabled()) {
            server.pluginManager.registerEvents(RentalAreaListener(), this)
        }
        
        // 中断されていた事前生成を再開
        if (ConfigManager.isResourceWorldEnabled()) {
            WorldManager.resumePregeneration()
        }
        
        // コマンド登録
        if (ConfigManager.isResourceWorldEnabled()) {
            getCommand("resource")?.setExecutor(ResourceCommand())
        }
        if (ConfigManager.isDelayCommandEnabled()) {
            getCommand("delay")?.setExecutor(DelayCommand())
        }
        if (ConfigManager.isNpcMessageEnabled()) {
            getCommand("npc_message")?.setExecutor(NpcMessageCommand())
        }
        getCommand("cc-system")?.setExecutor(CCSystemCommand())
        if (ConfigManager.isRentalAreaEnabled()) {
            getCommand("rental-receive")?.setExecutor(RentalReceiveCommand())
        }
        
        logger.info("CC-System v${description.version} を有効化しました")
    }

    override fun onDisable() {
        // 資源ワールド関連のクリーンアップ
        PlacedBlockLedgerManager.save()
        if (ConfigManager.isResourceWorldEnabled()) {
            WorldManager.cancelAllPregenTasks()
            ScoreboardManager.disable()
        }
        if (hasResourceListener()) {
            resourceListener.cancelMonitorTask()
        }
        if (hasDynamicDistanceListener()) {
            dynamicDistanceListener.shutdown()
        }

        // チャンクタスクキューのシャットダウン（状態保存）
        ChunkTaskQueueManager.unload()
        
        logger.info("CC-System v${description.version} を無効化しました")
    }
}
