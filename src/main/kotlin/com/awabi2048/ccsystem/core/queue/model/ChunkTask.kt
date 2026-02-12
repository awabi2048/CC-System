package com.awabi2048.ccsystem.core.queue.model

import java.time.LocalDateTime

/**
 * チャンク生成タスクを表すデータクラス
 *
 * @param taskId タスクの一意識別子 (UUID)
 * @param contentType コンテンツ種別
 * @param worldName 対象ワールド名
 * @param addedTime キューに追加された時刻
 * @param status 現在の処理状態
 */
data class ChunkTask(
    val taskId: String,
    val contentType: ContentType,
    val worldName: String,
    val addedTime: LocalDateTime,
    var status: TaskState = TaskState.WAITING
)
