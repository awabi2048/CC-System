package com.awabi2048.ccsystem.core.queue.model

/**
 * チャンクタスクの処理状態
 */
enum class TaskState {
    /** キューで待機中 */
    WAITING,

    /** 現在処理中 */
    PROCESSING,

    /** 処理完了 */
    COMPLETED,

    /** 処理失敗 */
    FAILED;

    companion object {
        /**
         * 文字列からTaskStateを取得します。
         * 不正な文字列の場合はnullを返します。
         */
        fun fromString(value: String): TaskState? {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}
