package com.awabi2048.ccsystem.core.queue.storage

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.queue.model.ChunkTask
import com.awabi2048.ccsystem.core.queue.model.ContentType
import com.awabi2048.ccsystem.core.queue.model.TaskState
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * チャンクタスクキューのYAML永続化を担当するマネージャー
 */
object QueueStorageManager {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val plugin get() = CCSystem.instance

    private fun getStorageFile(): File {
        val fileName = plugin.config.getString("chunk_task_queue.queue_data_file", "queue_data.yml")!!
        return File(plugin.dataFolder, fileName)
    }

    /**
     * タスクリストをYAMLファイルに保存します
     *
     * @param tasks 保存するタスクリスト（WAITING/PROCESSING状態のもののみ保存）
     */
    fun saveQueue(tasks: List<ChunkTask>) {
        val file = getStorageFile()
        val yaml = YamlConfiguration()

        val tasksToSave = tasks.filter {
            it.status == TaskState.WAITING || it.status == TaskState.PROCESSING
        }

        tasksToSave.forEachIndexed { index, task ->
            val path = "queue.$index"
            yaml.set("$path.taskId", task.taskId)
            yaml.set("$path.contentType", task.contentType.name)
            yaml.set("$path.worldName", task.worldName)
            yaml.set("$path.addedTime", task.addedTime.format(formatter))
            // サーバー再起動時はPROCESSING→WAITINGに戻す（処理途中で中断されたため）
            val savedState = if (task.status == TaskState.PROCESSING) TaskState.WAITING else task.status
            yaml.set("$path.status", savedState.name)
        }

        yaml.save(file)
        plugin.logger.fine("キューデータを保存しました: ${tasksToSave.size}件")
    }

    /**
     * YAMLファイルからタスクリストを読み込みます
     *
     * @return 復元されたタスクリスト
     */
    fun loadQueue(): List<ChunkTask> {
        val file = getStorageFile()
        if (!file.exists()) return emptyList()

        val yaml = YamlConfiguration.loadConfiguration(file)
        val queueSection = yaml.getConfigurationSection("queue") ?: return emptyList()

        val tasks = mutableListOf<ChunkTask>()

        for (key in queueSection.getKeys(false)) {
            val task = deserializeTask(yaml, "queue.$key") ?: continue
            tasks.add(task)
        }

        plugin.logger.info("キューデータを復元しました: ${tasks.size}件")
        return tasks
    }

    /**
     * YAMLの指定パスからChunkTaskをデシリアライズします。
     * 必須フィールドが欠損または不正な場合はnullを返します。
     */
    private fun deserializeTask(yaml: org.bukkit.configuration.file.YamlConfiguration, path: String): ChunkTask? {
        val taskId = yaml.getString("$path.taskId") ?: return null
        val contentTypeStr = yaml.getString("$path.contentType") ?: return null
        val worldName = yaml.getString("$path.worldName") ?: return null
        val addedTimeStr = yaml.getString("$path.addedTime") ?: return null
        val statusStr = yaml.getString("$path.status") ?: return null

        val contentType = ContentType.fromString(contentTypeStr) ?: run {
            plugin.logger.warning("不明なContentType: $contentTypeStr (taskId=$taskId) をスキップします")
            return null
        }
        val addedTime = runCatching { LocalDateTime.parse(addedTimeStr, formatter) }.getOrNull() ?: run {
            plugin.logger.warning("addedTimeのパースに失敗: $addedTimeStr (taskId=$taskId) をスキップします")
            return null
        }
        val status = TaskState.fromString(statusStr) ?: TaskState.WAITING

        return ChunkTask(taskId, contentType, worldName, addedTime, status)
    }
}
