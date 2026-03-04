package com.awabi2048.ccsystem.core.queue

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.queue.model.ChunkTask
import com.awabi2048.ccsystem.core.queue.model.ContentType
import com.awabi2048.ccsystem.core.queue.model.TaskState
import com.awabi2048.ccsystem.core.queue.storage.QueueStorageManager
import org.bukkit.scheduler.BukkitTask
import java.time.LocalDateTime
import java.util.PriorityQueue
import java.util.UUID

/**
 * チャンク生成タスクキューを管理するシングルトンマネージャー
 *
 * 優先度付きキューで複数のタスクを管理し、先頭から1件ずつ処理します。
 * タスクはYAMLで永続化され、サーバー再起動後も復元されます。
 */
object ChunkTaskQueueManager {

    private val plugin get() = CCSystem.instance

    /** コンテンツ種別の優先度マップ（インデックスが小さいほど高優先度） */
    private var priorityOrder: List<ContentType> = ContentType.entries

    /** 読み込み間隔（Bukkitティック） */
    private var readIntervalTicks: Long = 20L

    /**
     * 待機中タスクを保持する優先度付きキュー
     * - 優先度順（priorityOrderのインデックス昇順）
     * - 同優先度の場合は追加時刻の早い順
     */
    private val waitingQueue: PriorityQueue<ChunkTask> = PriorityQueue(compareBy(
        { priorityOrder.indexOf(it.contentType).let { i -> if (i < 0) Int.MAX_VALUE else i } },
        { it.addedTime }
    ))

    /** 現在処理中のタスク（同時に1件のみ） */
    private var processingTask: ChunkTask? = null

    /** 完了・失敗タスクの履歴（最大100件） */
    private val completedHistory: ArrayDeque<ChunkTask> = ArrayDeque()
    private val historyMaxSize = 100

    /** 定期読み込みタスク */
    private var schedulerTask: BukkitTask? = null

    // ─── 初期化・終了 ──────────────────────────────────────────────

    /**
     * キューマネージャーを初期化します。
     * 設定の読み込みとYAMLからのキュー復元、定期タスク起動を行います。
     */
    fun load() {
        loadConfig()
        restoreFromStorage()
        startQueueProcessor()
    }

    /**
     * キューマネージャーを終了します。
     * 定期タスクを停止し、現在のキューをYAMLに保存します。
     */
    fun unload() {
        stopQueueProcessor()
        saveToStorage()
    }

    /** queue.ymlからキュー設定を読み込みます */
    private fun loadConfig() {
        val orderList = ConfigManager.getChunkTaskQueuePriorityOrder()

        priorityOrder = if (orderList.isNotEmpty()) {
            orderList.mapNotNull { ContentType.fromString(it) }.also { parsed ->
                if (parsed.size != orderList.size) {
                    plugin.logger.warning("chunk_task_queue.priority_orderに不明な値が含まれています。既知の値のみ使用します。")
                }
                // configに含まれていない種別は末尾に追加
                val missing = ContentType.entries.filter { it !in parsed }
                if (missing.isNotEmpty()) {
                    plugin.logger.warning("priority_orderに未定義のContentType: $missing。末尾に追加します。")
                }
            }.let { parsed ->
                val missing = ContentType.entries.filter { it !in parsed }
                parsed + missing
            }
        } else {
            plugin.logger.warning("chunk_task_queue.priority_orderが未設定です。デフォルト順を使用します。")
            ContentType.entries
        }

        readIntervalTicks = ConfigManager.getChunkTaskQueueReadIntervalTicks()
        plugin.logger.info("キュー優先度順: ${priorityOrder.joinToString(" > ")}")
        plugin.logger.info("キュー読み込み間隔: ${readIntervalTicks}tick")
    }

    /** YAMLからキューを復元します */
    private fun restoreFromStorage() {
        val tasks = QueueStorageManager.loadQueue()
        tasks.forEach { task ->
            waitingQueue.add(task)
        }
    }

    /** 現在のキューをYAMLに保存します */
    private fun saveToStorage() {
        val allTasks = buildList {
            processingTask?.let { add(it) }
            addAll(waitingQueue)
        }
        QueueStorageManager.saveQueue(allTasks)
    }

    // ─── スケジューラー ────────────────────────────────────────────

    /**
     * 定期的なキュー読み込みタスクを開始します。
     * 既に起動中の場合はなにもしません。
     */
    fun startQueueProcessor() {
        if (schedulerTask != null) return
        schedulerTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { tryLoadNextTask() },
            readIntervalTicks,
            readIntervalTicks
        )
        plugin.logger.info("チャンクタスクキュープロセッサーを開始しました")
    }

    /**
     * 定期的なキュー読み込みタスクを停止します。
     */
    fun stopQueueProcessor() {
        schedulerTask?.cancel()
        schedulerTask = null
        plugin.logger.info("チャンクタスクキュープロセッサーを停止しました")
    }

    // ─── キュー操作 ────────────────────────────────────────────────

    /**
     * 新しいタスクをキューに追加します。
     * 追加後、即座に読み込み試行を行います。
     *
     * @param contentType コンテンツ種別
     * @param worldName 対象ワールド名
     * @return 追加されたChunkTask
     */
    fun addTask(contentType: ContentType, worldName: String): ChunkTask {
        val task = ChunkTask(
            taskId = UUID.randomUUID().toString(),
            contentType = contentType,
            worldName = worldName,
            addedTime = LocalDateTime.now(),
            status = TaskState.WAITING
        )
        waitingQueue.add(task)
        plugin.logger.info("[キュー追加] taskId=${task.taskId}, type=${contentType.name}, world=$worldName, 待機数=${waitingQueue.size}")
        saveToStorage()

        // 追加直後に読み込み試行
        tryLoadNextTask()

        return task
    }

    /**
     * 指定したタスクIDのタスク状態を更新します。
     * COMPLETED/FAILEDに変更した場合は次のタスクの読み込みをスケジュールします。
     *
     * @param taskId 対象タスクID
     * @param newState 新しい状態
     * @return 更新に成功した場合true、対象タスクが存在しない場合false
     */
    fun updateTaskStatus(taskId: String, newState: TaskState): Boolean {
        val target = findTask(taskId) ?: return false

        val oldState = target.status
        target.status = newState
        plugin.logger.info("[状態変更] taskId=$taskId, $oldState -> $newState")

        when (newState) {
            TaskState.COMPLETED, TaskState.FAILED -> {
                if (processingTask?.taskId == taskId) {
                    processingTask = null
                }
                // 履歴に追加（上限超過時は古いものを削除）
                completedHistory.addLast(target)
                if (completedHistory.size > historyMaxSize) {
                    completedHistory.removeFirst()
                }
                saveToStorage()
                // 次のタスク読み込みはスケジューラーの定期実行に委ねる
            }
            TaskState.WAITING -> {
                if (processingTask?.taskId == taskId) {
                    processingTask = null
                    waitingQueue.add(target)
                }
            }
            TaskState.PROCESSING -> {
                // 外部からPROCESSINGに変更することは通常ないが、念のため
            }
        }

        return true
    }

    /**
     * キューから次のタスクを取得して処理中状態に変更します。
     * 処理中タスクが既にある場合はnullを返します。
     *
     * @return 処理開始したChunkTask、または処理できない場合null
     */
    fun getNextTask(): ChunkTask? {
        if (processingTask != null) return null
        val next = waitingQueue.poll() ?: return null
        next.status = TaskState.PROCESSING
        processingTask = next
        plugin.logger.info("[処理開始] taskId=${next.taskId}, type=${next.contentType.name}, world=${next.worldName}")
        saveToStorage()
        return next
    }

    /**
     * 現在処理中のタスクを取得します。
     *
     * @return 処理中のChunkTask、なければnull
     */
    fun getProcessingTask(): ChunkTask? = processingTask

    /**
     * 現在の待機キューのスナップショットを返します（優先度順）。
     *
     * @return 待機中タスクのリスト
     */
    fun getWaitingQueueSnapshot(): List<ChunkTask> {
        return waitingQueue.sortedWith(compareBy(
            { priorityOrder.indexOf(it.contentType).let { i -> if (i < 0) Int.MAX_VALUE else i } },
            { it.addedTime }
        ))
    }

    /**
     * 完了・失敗タスクの履歴を返します（新しい順）。
     *
     * @return タスク履歴リスト
     */
    fun getCompletedHistory(): List<ChunkTask> = completedHistory.reversed()

    /**
     * 指定IDのタスクを取得します（待機・処理中・履歴を検索）。
     *
     * @return 対象ChunkTask、存在しない場合null
     */
    fun findTask(taskId: String): ChunkTask? {
        if (processingTask?.taskId == taskId) return processingTask
        waitingQueue.firstOrNull { it.taskId == taskId }?.let { return it }
        completedHistory.firstOrNull { it.taskId == taskId }?.let { return it }
        return null
    }

    /**
     * キューの状態をマップ形式で返します。
     *
     * @return キュー状態マップ
     */
    fun getQueueStatus(): Map<String, Any> = mapOf(
        "waitingCount" to waitingQueue.size,
        "processingTask" to (processingTask?.taskId ?: "none"),
        "processingWorld" to (processingTask?.worldName ?: "none"),
        "processingContentType" to (processingTask?.contentType?.name ?: "none"),
        "completedHistoryCount" to completedHistory.size,
        "priorityOrder" to priorityOrder.map { it.name },
        "readIntervalTicks" to readIntervalTicks
    )

    // ─── 内部処理 ──────────────────────────────────────────────────

    /**
     * 次のタスクの読み込みを試みます。
     * 処理中タスクがある場合や待機キューが空の場合は何もしません。
     */
    private fun tryLoadNextTask() {
        if (processingTask != null) return
        if (waitingQueue.isEmpty()) return
        getNextTask()
    }
}
