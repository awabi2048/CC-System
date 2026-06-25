package com.awabi2048.ccsystem.api

import com.awabi2048.ccsystem.api.gui.GuiElementService
import com.awabi2048.ccsystem.api.gui.GuiLayoutService
import com.awabi2048.ccsystem.api.gui.LoreService
import com.awabi2048.ccsystem.api.gui.MenuNavigationService
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.gui.GuiElementServiceImpl
import com.awabi2048.ccsystem.core.gui.GuiLayoutServiceImpl
import com.awabi2048.ccsystem.core.gui.LoreServiceImpl
import com.awabi2048.ccsystem.core.gui.MenuNavigationServiceImpl
import com.awabi2048.ccsystem.core.queue.ChunkTaskQueueManager
import com.awabi2048.ccsystem.core.queue.model.ChunkTask
import com.awabi2048.ccsystem.core.queue.model.ContentType
import com.awabi2048.ccsystem.core.queue.model.TaskState
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * CC-System APIの実装クラス
 * LanguageManagerおよびChunkTaskQueueManagerをラップして他のプラグインに機能を提供します
 */
internal class CCSystemAPIImpl : CCSystemAPI {
    private val menuNavigationService = MenuNavigationServiceImpl()
    private val guiElementService = GuiElementServiceImpl()
    private val guiLayoutService = GuiLayoutServiceImpl(guiElementService)
    private val loreService = LoreServiceImpl()
    
    
    override fun getPlayerLanguage(player: Player): String {
        return LanguageManager.getPlayerLanguageCode(player)
    }
    
    override fun getSupportedLanguages(): Set<String> {
        return LanguageManager.getSupportedLanguages()
    }

    override fun getI18nString(player: Player?, key: String, placeholders: Map<String, Any>): String {
        return LanguageManager.getUnified().getString(player, key, placeholders)
    }

    override fun getI18nString(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any>): String {
        return LanguageManager.getUnified().getString(sourceId, player, key, placeholders)
    }

    override fun getI18nString(locale: String, key: String, placeholders: Map<String, Any>): String {
        val raw = LanguageManager.getUnified().getRawString(locale, key)
            ?: throw IllegalStateException("言語キーが見つかりません: locale=$locale key=$key")
        return placeholders.entries.fold(raw) { acc, (placeholderKey, value) ->
            acc.replace("{$placeholderKey}", value.toString()).replace("%$placeholderKey%", value.toString())
        }
    }

    override fun getI18nString(sourceId: String, locale: String, key: String, placeholders: Map<String, Any>): String {
        val raw = LanguageManager.getUnified().getRawString(sourceId, locale, key)
            ?: throw IllegalStateException("言語キーが見つかりません: source=$sourceId locale=$locale key=$key")
        return placeholders.entries.fold(raw) { acc, (placeholderKey, value) ->
            acc.replace("{$placeholderKey}", value.toString()).replace("%$placeholderKey%", value.toString())
        }
    }

    override fun getI18nStringList(player: Player?, key: String, placeholders: Map<String, Any>): List<String> {
        return LanguageManager.getUnified().getStringList(player, key, placeholders)
    }

    override fun getI18nStringList(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any>): List<String> {
        return LanguageManager.getUnified().getStringList(sourceId, player, key, placeholders)
    }

    override fun getI18nStringList(locale: String, key: String, placeholders: Map<String, Any>): List<String> {
        val raw = LanguageManager.getUnified().getRawStringList(locale, key)
            ?: throw IllegalStateException("言語キーが見つからないか型が不正です: locale=$locale key=$key expected=List")
        return raw.map { line ->
            placeholders.entries.fold(line) { acc, (placeholderKey, value) ->
                acc.replace("{$placeholderKey}", value.toString()).replace("%$placeholderKey%", value.toString())
            }
        }
    }

    override fun getI18nStringList(sourceId: String, locale: String, key: String, placeholders: Map<String, Any>): List<String> {
        val raw = LanguageManager.getUnified().getRawStringList(sourceId, locale, key)
            ?: throw IllegalStateException("言語キーが見つからないか型が不正です: source=$sourceId locale=$locale key=$key expected=List")
        return raw.map { line ->
            placeholders.entries.fold(line) { acc, (placeholderKey, value) ->
                acc.replace("{$placeholderKey}", value.toString()).replace("%$placeholderKey%", value.toString())
            }
        }
    }

    override fun getI18nComponent(player: Player?, key: String, placeholders: Map<String, Any>): Component {
        return LanguageManager.getUnified().getComponent(player, key, placeholders)
    }

    override fun getI18nComponent(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any>): Component {
        return LanguageManager.getUnified().getComponent(sourceId, player, key, placeholders)
    }

    override fun getI18nComponentList(player: Player?, key: String, placeholders: Map<String, Any>): List<Component> {
        return LanguageManager.getUnified().getComponentList(player, key, placeholders)
    }

    override fun getI18nComponentList(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any>): List<Component> {
        return LanguageManager.getUnified().getComponentList(sourceId, player, key, placeholders)
    }

    override fun hasI18nKey(key: String): Boolean {
        return getSupportedLanguages().any { LanguageManager.getUnified().hasKey(it, key) }
    }

    override fun hasI18nKey(sourceId: String, key: String): Boolean {
        return getSupportedLanguages().any { LanguageManager.getUnified().hasKey(sourceId, it, key) }
    }

    override fun isI18nKeyMatch(title: String, key: String): Boolean {
        return LanguageManager.getUnified().isKeyMatch(title, key)
    }

    override fun isI18nKeyMatch(sourceId: String, title: String, key: String): Boolean {
        return LanguageManager.getUnified().isKeyMatch(sourceId, title, key)
    }

    override fun isI18nKeyStartWith(title: String, key: String): Boolean {
        return LanguageManager.getUnified().isKeyStartWith(title, key)
    }

    override fun isI18nKeyStartWith(sourceId: String, title: String, key: String): Boolean {
        return LanguageManager.getUnified().isKeyStartWith(sourceId, title, key)
    }

    override fun validateI18nSource(sourcePlugin: JavaPlugin, featureByFile: Map<String, String>): I18nValidationResult {
        val result = LanguageManager.getUnified().validateSource(sourcePlugin, featureByFile)
        return I18nValidationResult(result.errors, result.errorsByFeature)
    }

    override fun registerI18nSource(sourceId: String, sourcePlugin: JavaPlugin, fileNames: Set<String>) {
        LanguageManager.getUnified().registerSource(sourceId, sourcePlugin, fileNames)
    }

    override fun unregisterI18nSource(sourceId: String) {
        LanguageManager.getUnified().unregisterSource(sourceId)
    }

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
