package com.awabi2048.ccsystem

import org.bukkit.plugin.java.JavaPlugin
import com.awabi2048.ccsystem.api.CCSystemAPI
import com.awabi2048.ccsystem.api.CCSystemAPIImpl
import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicy
import com.awabi2048.ccsystem.api.config.ConfigClassification
import com.awabi2048.ccsystem.api.config.ConfigMigration
import com.awabi2048.ccsystem.api.config.ManagedConfigSpec
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.config.MessageManager
import com.awabi2048.ccsystem.core.data.PlacedBlockLedgerManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import com.awabi2048.ccsystem.features.announce.command.AnnounceCommand
import com.awabi2048.ccsystem.features.announce.listener.AnnounceListener
import com.awabi2048.ccsystem.features.announce.listener.AnnouncementNotificationListener
import com.awabi2048.ccsystem.features.announce.manager.AnnouncementManager
import com.awabi2048.ccsystem.features.clock.command.ClockCommand
import com.awabi2048.ccsystem.features.clock.manager.ClockManager
import com.awabi2048.ccsystem.features.misc.command.CCSystemCommand
import com.awabi2048.ccsystem.features.misc.command.DelayCommand
import com.awabi2048.ccsystem.features.misc.command.NpcMessageCommand
import com.awabi2048.ccsystem.features.misc.command.UnifiedManagementCommand
import com.awabi2048.ccsystem.features.misc.listener.MusicListener
import com.awabi2048.ccsystem.features.misc.listener.DynamicDistanceListener
import com.awabi2048.ccsystem.features.misc.listener.PlayerLeftClickBinderListener
import com.awabi2048.ccsystem.features.misc.listener.PlayerLeftClickTriggerListener
import com.awabi2048.ccsystem.features.misc.listener.ShiftFBinderListener
import com.awabi2048.ccsystem.features.misc.listener.PlayerDataListener
import com.awabi2048.ccsystem.features.misc.listener.PlayerDeathListener
import com.awabi2048.ccsystem.features.misc.listener.GuiProtectionListener
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
import org.bukkit.GameRules
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import java.util.jar.JarFile

class CCSystem : JavaPlugin() {
    
    companion object {
        lateinit var instance: CCSystem
            private set
        
        private lateinit var _api: CCSystemAPI
        
        /**
         * CC-System APIを取得します
         * 他のプラグインがこのメソッド経由でCC-Systemの機能を利用できます
         */
        @JvmStatic
        fun getAPI(): CCSystemAPI = _api
    }
    
    lateinit var musicListener: MusicListener
    lateinit var resourceListener: ResourceListener
    lateinit var dynamicDistanceListener: DynamicDistanceListener
    lateinit var announcementNotificationListener: AnnouncementNotificationListener

    private var musicListenerRegistered: Boolean = false
    private var resourceListenerRegistered: Boolean = false
    private var dynamicDistanceListenerRegistered: Boolean = false

    private var shiftFBinderListener: ShiftFBinderListener? = null
    private var shiftFBinderListenerRegistered: Boolean = false

    private var playerLeftClickBinderListener: PlayerLeftClickBinderListener? = null
    private var playerLeftClickBinderListenerRegistered: Boolean = false

    private var worldListener: WorldListener? = null
    private var worldListenerRegistered: Boolean = false

    private var publicSignListener: PublicSignListener? = null
    private var publicSignListenerRegistered: Boolean = false

    private var rentalAreaListener: RentalAreaListener? = null
    private var rentalAreaListenerRegistered: Boolean = false

    private var resourceRuntimeInitialized: Boolean = false

    fun hasMusicListener(): Boolean = ::musicListener.isInitialized
    fun hasResourceListener(): Boolean = ::resourceListener.isInitialized
    fun hasDynamicDistanceListener(): Boolean = ::dynamicDistanceListener.isInitialized
    fun hasAnnouncementNotificationListener(): Boolean = ::announcementNotificationListener.isInitialized

    private fun registerListenerIfNeeded(listener: Listener, isRegistered: Boolean): Boolean {
        if (isRegistered) {
            return true
        }
        server.pluginManager.registerEvents(listener, this)
        return true
    }

    private fun unregisterListenerIfNeeded(listener: Listener?, isRegistered: Boolean): Boolean {
        if (!isRegistered || listener == null) {
            return isRegistered
        }
        HandlerList.unregisterAll(listener)
        return false
    }

    fun syncFeatureRuntime() {
        syncMusicFeature()
        syncShiftFBinderFeature()
        syncPlayerLeftClickBinderFeature()
        syncGlobalSoundEventsFeature()
        syncDynamicDistanceFeature()
        syncPublicSignFeature()
        syncRentalAreaFeature()
        syncResourceWorldFeature()
    }

    private fun syncMusicFeature() {
        if (ConfigManager.isMusicEnabled()) {
            if (!hasMusicListener()) {
                musicListener = MusicListener()
            }
            musicListenerRegistered = registerListenerIfNeeded(musicListener, musicListenerRegistered)
            musicListener.stopAllPlayersMusic()
            musicListener.startAllPlayersMusic()
            return
        }

        if (hasMusicListener()) {
            musicListener.stopAllPlayersMusic()
            musicListenerRegistered = unregisterListenerIfNeeded(musicListener, musicListenerRegistered)
        }
    }

    private fun syncShiftFBinderFeature() {
        if (ConfigManager.isShiftFBinderEnabled()) {
            if (shiftFBinderListener == null) {
                shiftFBinderListener = ShiftFBinderListener()
            }
            shiftFBinderListenerRegistered =
                registerListenerIfNeeded(shiftFBinderListener ?: return, shiftFBinderListenerRegistered)
            return
        }

        shiftFBinderListenerRegistered = unregisterListenerIfNeeded(shiftFBinderListener, shiftFBinderListenerRegistered)
    }

    private fun syncPlayerLeftClickBinderFeature() {
        if (ConfigManager.isPlayerLeftClickBinderEnabled()) {
            if (playerLeftClickBinderListener == null) {
                playerLeftClickBinderListener = PlayerLeftClickBinderListener()
            }
            playerLeftClickBinderListenerRegistered =
                registerListenerIfNeeded(playerLeftClickBinderListener ?: return, playerLeftClickBinderListenerRegistered)
            return
        }

        playerLeftClickBinderListenerRegistered =
            unregisterListenerIfNeeded(playerLeftClickBinderListener, playerLeftClickBinderListenerRegistered)
    }

    private fun syncGlobalSoundEventsFeature() {
        if (ConfigManager.isGlobalSoundEventsAutoDisable()) {
            if (worldListener == null) {
                worldListener = WorldListener()
            }
            worldListenerRegistered = registerListenerIfNeeded(worldListener ?: return, worldListenerRegistered)
            applyGlobalSoundEventsRuleToLoadedWorlds()
            return
        }

        worldListenerRegistered = unregisterListenerIfNeeded(worldListener, worldListenerRegistered)
    }

    private fun syncDynamicDistanceFeature() {
        if (ConfigManager.isDynamicDistanceEnabled()) {
            if (!hasDynamicDistanceListener()) {
                dynamicDistanceListener = DynamicDistanceListener()
            } else {
                dynamicDistanceListener.reload()
            }
            dynamicDistanceListenerRegistered =
                registerListenerIfNeeded(dynamicDistanceListener, dynamicDistanceListenerRegistered)
            return
        }

        if (hasDynamicDistanceListener()) {
            dynamicDistanceListener.shutdown()
            dynamicDistanceListenerRegistered = unregisterListenerIfNeeded(dynamicDistanceListener, dynamicDistanceListenerRegistered)
        }
    }

    private fun syncPublicSignFeature() {
        if (ConfigManager.isPublicSignEnabled()) {
            PublicSignManager.load()
            if (publicSignListener == null) {
                publicSignListener = PublicSignListener()
            }
            publicSignListenerRegistered = registerListenerIfNeeded(publicSignListener ?: return, publicSignListenerRegistered)
            return
        }

        publicSignListenerRegistered = unregisterListenerIfNeeded(publicSignListener, publicSignListenerRegistered)
    }

    private fun syncRentalAreaFeature() {
        if (ConfigManager.isRentalAreaEnabled()) {
            RemainedItemManager.load()
            RentalAreaManager.load()
            if (rentalAreaListener == null) {
                rentalAreaListener = RentalAreaListener()
            }
            rentalAreaListenerRegistered = registerListenerIfNeeded(rentalAreaListener ?: return, rentalAreaListenerRegistered)
            return
        }

        rentalAreaListenerRegistered = unregisterListenerIfNeeded(rentalAreaListener, rentalAreaListenerRegistered)
    }

    private fun syncResourceWorldFeature() {
        if (ConfigManager.isResourceWorldEnabled()) {
            ensureResourceRuntimeInitialized()
            if (!resourceListenerRegistered) {
                resourceListener = ResourceListener()
                resourceListenerRegistered = registerListenerIfNeeded(resourceListener, resourceListenerRegistered)
            }
            return
        }

        if (hasResourceListener()) {
            resourceListener.cancelMonitorTask()
            resourceListenerRegistered = unregisterListenerIfNeeded(resourceListener, resourceListenerRegistered)
        }

        if (resourceRuntimeInitialized) {
            WorldManager.cancelAllPregenTasks()
            ScoreboardManager.disable()
            resourceRuntimeInitialized = false
        }
    }

    private fun ensureResourceRuntimeInitialized() {
        if (resourceRuntimeInitialized) {
            return
        }
        PregenerationStateManager.load()
        WorldManager.loadExistingWorlds()
        ScoreboardManager.init()
        WorldManager.resumePregeneration()
        resourceRuntimeInitialized = true
    }

    private fun applyGlobalSoundEventsRuleToLoadedWorlds() {
        for (world in server.worlds) {
            world.setGameRule(GameRules.GLOBAL_SOUND_EVENTS, false)
        }
    }

    private fun shutdownFeatureRuntime() {
        if (hasMusicListener()) {
            musicListener.stopAllPlayersMusic()
            musicListenerRegistered = unregisterListenerIfNeeded(musicListener, musicListenerRegistered)
        }

        if (hasDynamicDistanceListener()) {
            dynamicDistanceListener.shutdown()
            dynamicDistanceListenerRegistered = unregisterListenerIfNeeded(dynamicDistanceListener, dynamicDistanceListenerRegistered)
        }

        if (hasResourceListener()) {
            resourceListener.cancelMonitorTask()
            resourceListenerRegistered = unregisterListenerIfNeeded(resourceListener, resourceListenerRegistered)
        }

        shiftFBinderListenerRegistered = unregisterListenerIfNeeded(shiftFBinderListener, shiftFBinderListenerRegistered)
        playerLeftClickBinderListenerRegistered =
            unregisterListenerIfNeeded(playerLeftClickBinderListener, playerLeftClickBinderListenerRegistered)
        worldListenerRegistered = unregisterListenerIfNeeded(worldListener, worldListenerRegistered)
        publicSignListenerRegistered = unregisterListenerIfNeeded(publicSignListener, publicSignListenerRegistered)
        rentalAreaListenerRegistered = unregisterListenerIfNeeded(rentalAreaListener, rentalAreaListenerRegistered)

        if (resourceRuntimeInitialized) {
            WorldManager.cancelAllPregenTasks()
            ScoreboardManager.disable()
            resourceRuntimeInitialized = false
        }
    }

    fun ensureDefaultFiles() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        listOf(
            "config/core.yml",
            "config/misc.yml",
            "config/resource_world.yml",
            "config/rental_area.yml",
            "config/public_sign.yml",
            "config/announce.yml",
            "config/queue.yml",
            "data/rental_area/rental_area_data.yml",
            "data/ledger/placed_block_ledger.yml",
            "data/announce/announce_data.yml",
            "data/clock/clock_data.yml"
        ).forEach { resourcePath ->
            val file = File(dataFolder, resourcePath)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                saveResource(resourcePath, false)
            }
        }

        File(dataFolder, "data/public_sign").mkdirs()
        File(dataFolder, "data/resource_world").mkdirs()
        File(dataFolder, "data/queue").mkdirs()
        File(dataFolder, "data/rental_area/remained_item").mkdirs()
        File(dataFolder, "data/clock").mkdirs()
        File(dataFolder, "playerdata").mkdirs()
        saveSplitLanguageResources()
    }

    private fun registerManagedConfigs() {
        val paths = listOf(
            "config/core.yml",
            "config/misc.yml",
            "config/resource_world.yml",
            "config/rental_area.yml",
            "config/public_sign.yml",
            "config/announce.yml",
            "config/queue.yml"
        )
        val specs = paths.map { resourcePath ->
            val currentVersion = if (resourcePath == "config/misc.yml") 2 else 1
            ManagedConfigSpec(
                owner = "cc-system",
                sourcePlugin = this,
                resourcePath = resourcePath,
                targetPath = File(dataFolder, resourcePath).toPath(),
                currentVersion = currentVersion,
                classification = ConfigClassification.MANAGED_CONFIG,
                migrations = if (resourcePath == "config/misc.yml") {
                    mapOf(
                        1 to ConfigMigration { configuration ->
                            val worlds = configuration.getConfigurationSection("music.worlds")
                            worlds?.getKeys(false)
                                ?.filterNot { ':' in it }
                                ?.forEach { legacyName ->
                                    val currentKey = "minecraft:$legacyName"
                                    if (!worlds.contains(currentKey)) {
                                        worlds.set(currentKey, worlds.get(legacyName))
                                    }
                                    worlds.set(legacyName, null)
                                }
                        }
                    )
                } else {
                    emptyMap()
                },
                validator = com.awabi2048.ccsystem.api.config.ConfigValidator {},
                reloadAction = {
                    ConfigManager.reload()
                    syncFeatureRuntime()
                }
            )
        }
        _api.getConfigSchemaService().register("cc-system", specs)
        val prepared = _api.getConfigSchemaService().prepare("cc-system")
        check(prepared.successful) {
            "CC-System Config preparation failed: ${prepared.statuses.filter { it.message != null }}"
        }
    }

    private fun saveSplitLanguageResources() {
        val codeSource = runCatching {
            File(javaClass.protectionDomain.codeSource.location.toURI())
        }.getOrNull() ?: return
        if (!codeSource.isFile) {
            return
        }

        JarFile(codeSource).use { jar ->
            jar.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("lang/") && it.name.endsWith(".yml") }
                .forEach { entry ->
                    val target = File(dataFolder, entry.name)
                    target.parentFile?.mkdirs()
                    saveResource(entry.name, true)
                }
        }
    }
    
    override fun onEnable() {
        // インスタンス保存
        instance = this
        
        // API初期化
        _api = CCSystemAPIImpl(dataFolder)
        _api.getMenuNavigationService().registerMenuMatcher("cc-system") { inventory ->
            inventory.holder?.javaClass?.name?.startsWith("com.awabi2048.ccsystem") == true
        }
        _api.getMenuNavigationService().registerInventoryPolicy("cc-system", GuiInventoryPolicy())
        
        // 設定読み込み
        ensureDefaultFiles()
        registerManagedConfigs()
        
        // マネージャー初期化
        ConfigManager.load()
        LanguageManager.load()
        MessageManager.load()
        PlayerDataManager.load()
        PlacedBlockLedgerManager.load()
        AnnouncementManager.load()
        ClockManager.load()

        // チャンクタスクキューマネージャー初期化
        ChunkTaskQueueManager.load()

        announcementNotificationListener = AnnouncementNotificationListener()

        // リスナー登録
        server.pluginManager.registerEvents(PlayerDataListener(), this)
        server.pluginManager.registerEvents(PlayerDeathListener(), this)
        server.pluginManager.registerEvents(GuiProtectionListener(_api.getMenuNavigationService()), this)
        server.pluginManager.registerEvents(AnnounceListener(), this)
        server.pluginManager.registerEvents(announcementNotificationListener, this)
        server.pluginManager.registerEvents(PlayerLeftClickTriggerListener(), this)

        syncFeatureRuntime()
        
        // コマンド登録
        val resourceCommand = ResourceCommand()
        getCommand("resource")?.setExecutor(resourceCommand)
        getCommand("resource")?.tabCompleter = resourceCommand
        getCommand("delay")?.setExecutor(DelayCommand())
        getCommand("npc_message")?.setExecutor(NpcMessageCommand())
        getCommand("cc-system")?.setExecutor(CCSystemCommand())
        val unifiedManagementCommand = UnifiedManagementCommand()
        getCommand("cc")?.setExecutor(unifiedManagementCommand)
        getCommand("cc")?.tabCompleter = unifiedManagementCommand
        getCommand("rental-receive")?.setExecutor(RentalReceiveCommand())
        val clockCommand = ClockCommand()
        getCommand("clock")?.setExecutor(clockCommand)
        getCommand("clock")?.tabCompleter = clockCommand
        val announceCommand = AnnounceCommand()
        getCommand("announcement")?.setExecutor(announceCommand)
        getCommand("announcement")?.tabCompleter = announceCommand
        
        logger.info("CC-System v${pluginMeta.version} を有効化しました")
    }

    override fun onDisable() {
        // 再起動前に現在画面をUUID単位で保存し、次回ログインの一回だけ復元できる状態にする。
        _api.getMenuNavigationService().persistCurrentRoutes(server.onlinePlayers)
        _api.getMenuNavigationService().closeAllMenus(server.onlinePlayers)
        // 資源ワールド関連のクリーンアップ
        PlacedBlockLedgerManager.save()
        shutdownFeatureRuntime()
        if (hasAnnouncementNotificationListener()) {
            announcementNotificationListener.shutdown()
        }
        ClockManager.unload()
        AnnouncementManager.unload()

        // チャンクタスクキューのシャットダウン（状態保存）
        ChunkTaskQueueManager.unload()
        
        logger.info("CC-System v${pluginMeta.version} を無効化しました")
    }
}
