package com.awabi2048.ccsystem.api

import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.queue.ChunkTaskQueueManager
import com.awabi2048.ccsystem.core.queue.model.ChunkTask
import com.awabi2048.ccsystem.core.queue.model.ContentType
import com.awabi2048.ccsystem.core.queue.model.TaskState
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/**
 * CC-System APIの実装クラス
 * LanguageManagerおよびChunkTaskQueueManagerをラップして他のプラグインに機能を提供します
 */
internal class CCSystemAPIImpl : CCSystemAPI {
    
    override fun getPlayerLanguage(player: Player): String {
        return LanguageManager.getPlayerLanguageCode(player)
    }
    
    override fun getSupportedLanguages(): Set<String> {
        return LanguageManager.getSupportedLanguages()
    }

    override fun getI18nString(player: Player?, key: String, placeholders: Map<String, Any>): String {
        return LanguageManager.getUnified().getString(player, key, placeholders)
    }

    override fun getI18nStringList(player: Player?, key: String, placeholders: Map<String, Any>): List<String> {
        return LanguageManager.getUnified().getStringList(player, key, placeholders)
    }

    override fun getI18nComponent(player: Player?, key: String, placeholders: Map<String, Any>): Component {
        return LanguageManager.getUnified().getComponent(player, key, placeholders)
    }

    override fun getI18nComponentList(player: Player?, key: String, placeholders: Map<String, Any>): List<Component> {
        return LanguageManager.getUnified().getComponentList(player, key, placeholders)
    }

    override fun hasI18nKey(key: String): Boolean {
        return getSupportedLanguages().any { LanguageManager.getUnified().hasKey(it, key) }
    }

    override fun isI18nKeyMatch(title: String, key: String): Boolean {
        return LanguageManager.getUnified().isKeyMatch(title, key)
    }

    override fun isI18nKeyStartWith(title: String, key: String): Boolean {
        return LanguageManager.getUnified().isKeyStartWith(title, key)
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
