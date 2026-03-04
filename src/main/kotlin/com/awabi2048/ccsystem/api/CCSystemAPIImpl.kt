package com.awabi2048.ccsystem.api

import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import com.awabi2048.ccsystem.core.queue.ChunkTaskQueueManager
import com.awabi2048.ccsystem.core.queue.model.ChunkTask
import com.awabi2048.ccsystem.core.queue.model.ContentType
import com.awabi2048.ccsystem.core.queue.model.TaskState
import org.bukkit.entity.Player

/**
 * CC-System APIの実装クラス
 * LanguageManagerおよびChunkTaskQueueManagerをラップして他のプラグインに機能を提供します
 */
internal class CCSystemAPIImpl : CCSystemAPI {
    
    override fun getPlayerLanguage(player: Player): String {
        val defaultLang = ConfigManager.getDefaultLanguage()
        return PlayerDataManager.getString(player.uniqueId, "lang", defaultLang) ?: defaultLang
    }
    
    override fun setPlayerLanguage(player: Player, language: String) {
        LanguageManager.setPlayerLang(player, language)
    }
    
    override fun getSupportedLanguages(): Set<String> {
        // サポートされている言語は ja_jp と en_us
        return setOf("ja_jp", "en_us")
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
