package com.awabi2048.ccsystem.api

import com.awabi2048.ccsystem.api.config.ConfigSchemaService
import com.awabi2048.ccsystem.api.cosmetic.CosmeticPlatform
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectService
import com.awabi2048.ccsystem.api.gui.GuiElementService
import com.awabi2048.ccsystem.api.gui.GuiLayoutService
import com.awabi2048.ccsystem.api.gui.LoreService
import com.awabi2048.ccsystem.api.gui.MenuNavigationService
import com.awabi2048.ccsystem.api.gui.MenuCapabilityService
import com.awabi2048.ccsystem.api.gui.MenuSoundService
import com.awabi2048.ccsystem.api.gui.MenuRuntimeService
import com.awabi2048.ccsystem.api.gui.MenuDialogService
import com.awabi2048.ccsystem.api.gui.MenuConfirmationService
import com.awabi2048.ccsystem.api.gui.MenuFormService
import com.awabi2048.ccsystem.api.input.PlayerInteractionClaimService
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiService
import com.awabi2048.ccsystem.api.item.ItemGrantService
import com.awabi2048.ccsystem.api.sound.SoundResolutionService
import com.awabi2048.ccsystem.api.action.ContentActionDispatcher
import com.awabi2048.ccsystem.api.time.SharedClockService
import com.awabi2048.ccsystem.api.time.SeasonService
import com.awabi2048.ccsystem.api.resource.ResourceWorldLifecycleService
import com.awabi2048.ccsystem.api.world.WorldDirectoryService
import com.awabi2048.ccsystem.api.world.WorldIdentityService
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.ConfigSchemaServiceImpl
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.cosmetic.CosmeticPlatformImpl
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectServiceImpl
import com.awabi2048.ccsystem.core.gui.GuiElementServiceImpl
import com.awabi2048.ccsystem.core.gui.GuiLayoutServiceImpl
import com.awabi2048.ccsystem.core.gui.LoreServiceImpl
import com.awabi2048.ccsystem.core.gui.MenuNavigationServiceImpl
import com.awabi2048.ccsystem.core.gui.MenuCommandServiceImpl
import com.awabi2048.ccsystem.core.gui.MenuCapabilityServiceImpl
import com.awabi2048.ccsystem.core.gui.MenuSoundServiceImpl
import com.awabi2048.ccsystem.core.gui.MenuRuntimeServiceImpl
import com.awabi2048.ccsystem.core.gui.MenuReversibleStateProviderRegistryImpl
import com.awabi2048.ccsystem.core.gui.MenuDialogServiceImpl
import com.awabi2048.ccsystem.core.gui.MenuConfirmationServiceImpl
import com.awabi2048.ccsystem.core.gui.MenuFormServiceImpl
import com.awabi2048.ccsystem.core.input.PlayerInteractionClaimServiceImpl
import com.awabi2048.ccsystem.core.gesturegui.GestureGuiInputListener
import com.awabi2048.ccsystem.core.gesturegui.GestureGuiLifecycleListener
import com.awabi2048.ccsystem.core.gesturegui.GestureGuiServiceImpl
import com.awabi2048.ccsystem.core.item.ItemGrantServiceImpl
import com.awabi2048.ccsystem.core.sound.SoundResolutionServiceImpl
import com.awabi2048.ccsystem.core.action.ContentActionDispatcherImpl
import com.awabi2048.ccsystem.core.time.SharedClockServiceImpl
import com.awabi2048.ccsystem.core.time.SeasonServiceImpl
import com.awabi2048.ccsystem.core.resource.ResourceWorldLifecycleRuntime
import com.awabi2048.ccsystem.core.resource.NaturalOriginRuntime
import com.awabi2048.ccsystem.api.resource.NaturalOriginRegistry
import com.awabi2048.ccsystem.core.world.WorldDirectoryServiceImpl
import com.awabi2048.ccsystem.core.world.WorldIdentityServiceImpl
import com.awabi2048.ccsystem.core.queue.ChunkTaskQueueManager
import com.awabi2048.ccsystem.core.queue.model.ChunkTask
import com.awabi2048.ccsystem.core.queue.model.ContentType
import com.awabi2048.ccsystem.core.queue.model.TaskState
import java.io.File
import net.kyori.adventure.text.Component
import com.awabi2048.ccsystem.api.localization.LocalizationKey
import org.bukkit.World
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * CC-System APIの実装クラス
 * LanguageManagerおよびChunkTaskQueueManagerをラップして他のプラグインに機能を提供します
 */
internal class CCSystemAPIImpl(plugin: JavaPlugin, dataFolder: File) : CCSystemAPI {
    init {
        ResourceWorldLifecycleRuntime.initialize(dataFolder) { owner, failure ->
            Bukkit.getLogger().warning("[CC-System][ResourceWorld] 購読者 $owner の処理に失敗しました: ${failure.message}")
        }
        NaturalOriginRuntime.initialize(dataFolder)
    }
    private val menuNavigationService = MenuNavigationServiceImpl()
    private val menuCommandService = MenuCommandServiceImpl()
    private val guiElementService = GuiElementServiceImpl(::resolveLocalizedText)
    private val menuCapabilityService = MenuCapabilityServiceImpl()
    private val menuReversibleStateProviderRegistry = MenuReversibleStateProviderRegistryImpl()
    private val guiLayoutService = GuiLayoutServiceImpl(guiElementService)
    private val loreService = LoreServiceImpl(::resolveLocalizedText)
    private val menuSoundService = MenuSoundServiceImpl()
    private val menuPresentationTracker = com.awabi2048.ccsystem.core.gui.MenuPresentationTracker()
    private val menuRuntimeService = MenuRuntimeServiceImpl(
        plugin,
        menuNavigationService,
        menuSoundService,
        guiLayoutService,
        menuPresentationTracker,
        menuCapabilityService,
        menuReversibleStateProviderRegistry,
    ).also {
        plugin.server.pluginManager.registerEvents(it, plugin)
    }
    private val menuDialogService = MenuDialogServiceImpl(
        plugin,
        menuRuntimeService,
        menuPresentationTracker,
    )
    private val menuConfirmationService = MenuConfirmationServiceImpl(
        menuRuntimeService,
        guiLayoutService,
    )
    private val menuFormService = MenuFormServiceImpl(
        plugin,
        menuSoundService,
        menuRuntimeService,
        menuPresentationTracker,
    )
    private val playerInteractionClaimService = PlayerInteractionClaimServiceImpl()
    private val gestureGuiService = GestureGuiServiceImpl(plugin, playerInteractionClaimService).also {
        // 入力とライフサイクルを同じサービスへ接続し、Entity UUIDを外部へ公開しません。
        plugin.server.pluginManager.registerEvents(GestureGuiInputListener(it), plugin)
        plugin.server.pluginManager.registerEvents(GestureGuiLifecycleListener(it), plugin)
    }
    private val configSchemaService = ConfigSchemaServiceImpl()
    private val itemGrantService = ItemGrantServiceImpl()
    private val worldIdentityService = WorldIdentityServiceImpl()
    private val worldDirectoryService = WorldDirectoryServiceImpl(
        Bukkit.getWorldContainer().toPath(),
        Bukkit.getWorlds().firstOrNull { it.key == org.bukkit.NamespacedKey.minecraft("overworld") }?.name
            ?: "world"
    )
    private val soundResolutionService = SoundResolutionServiceImpl()
    private val seasonSettingsFile = File(dataFolder, "config/season.yml")
    private val sharedClockService = SharedClockServiceImpl(settingsFile = seasonSettingsFile)
    private val seasonService = SeasonServiceImpl(
        sharedClockService,
        File(dataFolder, "data/season/override.yml"),
        Bukkit.getLogger(),
        seasonSettingsFile
    )
    private val contentActionDispatcher = ContentActionDispatcherImpl { owner, failure ->
        Bukkit.getLogger().warning("[CC-System][ContentAction] 購読者 $owner の処理に失敗しました: ${failure.message}")
    }
    private val cosmeticPlatform = CosmeticPlatformImpl(plugin, dataFolder)
    private val displayEffectService = DisplayEffectServiceImpl(plugin)

    init {
        plugin.server.pluginManager.registerEvents(displayEffectService, plugin)
    }

    internal fun reloadTimeSettings() {
        sharedClockService.reload()
        seasonService.reload()
    }
    
    
    override fun getPlayerLanguage(player: Player): String {
        return LanguageManager.getPlayerLanguageCode(player)
    }
    
    override fun getSupportedLanguages(): Set<String> {
        return LanguageManager.getSupportedLanguages()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getLocalized(player: Player?, key: LocalizationKey<T>, placeholders: Map<String, Any>): T =
        when (key.valueType) {
            LocalizationKey.ValueType.TEXT -> LanguageManager.getUnified().getString(player, key.id, placeholders)
            LocalizationKey.ValueType.TEXT_LIST -> LanguageManager.getUnified().getStringList(player, key.id, placeholders)
        } as T

    @Suppress("UNCHECKED_CAST")
    override fun <T> getLocalized(locale: String, key: LocalizationKey<T>, placeholders: Map<String, Any>): T {
        val value: Any = when (key.valueType) {
            LocalizationKey.ValueType.TEXT -> resolveLocalizedText(
                locale,
                key as LocalizationKey<String>,
                placeholders,
            )
            LocalizationKey.ValueType.TEXT_LIST -> resolveLocalizedTextList(
                locale,
                key as LocalizationKey<List<String>>,
                placeholders,
            )
        }
        return value as T
    }

    /** GUI基盤内でも生成済みキーだけを受け取り、文字列キーの再流入を防ぎます。 */
    private fun resolveLocalizedText(
        player: Player?,
        key: LocalizationKey<String>,
        placeholders: Map<String, Any>,
    ): String = LanguageManager.getUnified().getString(player, key.id, placeholders)

    private fun resolveLocalizedText(
        locale: String,
        key: LocalizationKey<String>,
        placeholders: Map<String, Any>,
    ): String {
        val raw = LanguageManager.getUnified().getRawString(locale, key.id)
            ?: throw IllegalStateException("言語キーが見つかりません: locale=$locale key=${key.id}")
        return placeholders.entries.fold(raw) { acc, (placeholderKey, value) ->
            acc.replace("{$placeholderKey}", value.toString()).replace("%$placeholderKey%", value.toString())
        }
    }

    private fun resolveLocalizedTextList(
        locale: String,
        key: LocalizationKey<List<String>>,
        placeholders: Map<String, Any>,
    ): List<String> {
        val raw = LanguageManager.getUnified().getRawStringList(locale, key.id)
            ?: throw IllegalStateException("言語キーが見つからないか型が不正です: locale=$locale key=${key.id} expected=List")
        return raw.map { line ->
            placeholders.entries.fold(line) { acc, (placeholderKey, value) ->
                acc.replace("{$placeholderKey}", value.toString()).replace("%$placeholderKey%", value.toString())
            }
        }
    }

    override fun getI18nComponent(player: Player?, key: LocalizationKey<String>, placeholders: Map<String, Any>): Component =
        LanguageManager.getUnified().getComponent(player, key.id, placeholders)

    override fun getI18nComponentList(
        player: Player?,
        key: LocalizationKey<List<String>>,
        placeholders: Map<String, Any>,
    ): List<Component> = LanguageManager.getUnified().getComponentList(player, key.id, placeholders)

    override fun getGuiElementService(): GuiElementService {
        return guiElementService
    }

    override fun getGuiLayoutService(): GuiLayoutService {
        return guiLayoutService
    }

    override fun getLoreService(): LoreService {
        return loreService
    }

    override fun getMenuNavigationService(): MenuNavigationService {
        return menuNavigationService
    }

    override fun getMenuCommandService(): com.awabi2048.ccsystem.api.gui.MenuCommandService = menuCommandService

    override fun getMenuCapabilityService(): MenuCapabilityService = menuCapabilityService

    override fun getMenuReversibleStateProviderRegistry(): com.awabi2048.ccsystem.api.gui.MenuReversibleStateProviderRegistry =
        menuReversibleStateProviderRegistry

    override fun getMenuSoundService(): MenuSoundService {
        return menuSoundService
    }

    override fun getMenuRuntimeService(): MenuRuntimeService {
        return menuRuntimeService
    }

    override fun getMenuDialogService(): MenuDialogService {
        return menuDialogService
    }

    override fun getMenuConfirmationService(): MenuConfirmationService =
        menuConfirmationService

    override fun getMenuFormService(): MenuFormService = menuFormService

    override fun getPlayerInteractionClaimService(): PlayerInteractionClaimService {
        return playerInteractionClaimService
    }

    override fun getGestureGuiService(): GestureGuiService = gestureGuiService

    override fun getConfigSchemaService(): ConfigSchemaService = configSchemaService

    override fun getItemGrantService(): ItemGrantService = itemGrantService

    override fun getWorldIdentityService(): WorldIdentityService = worldIdentityService

    override fun getWorldDirectoryService(): WorldDirectoryService = worldDirectoryService

    override fun getSoundResolutionService(): SoundResolutionService = soundResolutionService

    override fun getSharedClockService(): SharedClockService = sharedClockService

    override fun getSeasonService(): SeasonService = seasonService

    override fun getContentActionDispatcher(): ContentActionDispatcher = contentActionDispatcher

    override fun getResourceWorldLifecycleService(): ResourceWorldLifecycleService = ResourceWorldLifecycleRuntime.service

    override fun getNaturalOriginRegistry(): NaturalOriginRegistry = NaturalOriginRuntime.registry

    override fun getCosmeticPlatform(): CosmeticPlatform = cosmeticPlatform

    override fun getDisplayEffectService(): DisplayEffectService = displayEffectService

    internal fun getDisplayParticleCount(): Int = displayEffectService.currentDisplayParticleCount()

    internal fun shutdown() {
        gestureGuiService.shutdown()
        displayEffectService.shutdown()
        cosmeticPlatform.shutdown()
    }

    override fun isResourceWorld(world: World): Boolean {
        return ConfigManager.isResourceWorldName(world.name)
    }

    // ─── チャンクタスクキューAPI ────────────────────────────────────────

    override fun addChunkTask(contentType: String, worldName: String): ChunkTask? {
        val type = ContentType.fromString(contentType) ?: return null
        return ChunkTaskQueueManager.addTask(type, worldName)
    }

    override fun getProcessingChunkTask(): ChunkTask? {
        return ChunkTaskQueueManager.getProcessingTask()
    }

    override fun getWaitingChunkTaskQueue(): List<ChunkTask> {
        return ChunkTaskQueueManager.getWaitingQueueSnapshot()
    }

    override fun updateChunkTaskStatus(taskId: String, status: String): Boolean {
        val state = TaskState.fromString(status) ?: return false
        return ChunkTaskQueueManager.updateTaskStatus(taskId, state)
    }

    override fun findChunkTask(taskId: String): ChunkTask? {
        return ChunkTaskQueueManager.findTask(taskId)
    }

    override fun getChunkTaskHistory(): List<ChunkTask> {
        return ChunkTaskQueueManager.getCompletedHistory()
    }

    override fun getChunkTaskQueueStatus(): Map<String, Any> {
        return ChunkTaskQueueManager.getQueueStatus()
    }
}
