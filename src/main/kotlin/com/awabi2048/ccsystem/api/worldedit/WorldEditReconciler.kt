package com.awabi2048.ccsystem.api.worldedit

import org.bukkit.plugin.Plugin

/**
 * FAWE等の非同期編集に対応する遅延確定の汎用器です。
 *
 * `PlayerCommandPreprocessEvent` はコマンド実行前に発生するため、実ブロックの
 * 移動完了を次tick以降で再確認し、成功したものだけ台帳へ反映します。
 * 移動先だけが先に配置された途中状態で確定すると重複登録を招くため、
 * [isReady] には移動元消去と移動先出現の両方を含めてください。
 */
object WorldEditReconciler {
    /** FAWEの大規模編集を非同期キューで処理するための再試行間隔です。 */
    const val RETRY_INTERVAL_TICKS = 1L

    /** 完了確認を諦めるまでの試行回数です。 */
    const val MAX_ATTEMPTS = 40

    /** 確定に必要な連続成功回数です。単発成功の途中状態で確定しません。 */
    const val REQUIRED_CONFIRMATIONS = 2

    /**
     * [isReady] が連続成功するまで再試行し、確定または期限切れを通知します。
     *
     * 新しい操作が同じ対象へ発行された場合に古い再試行が干渉しないよう、
     * [isStillCurrent] が偽になった時点で何もせず終了します。期限切れ時は
     * 台帳を変更せず [onTimeout] だけを呼び、途中状態での削除・再登録による
     * データ消失を防ぎます。いずれのコールバックもメインスレッドで実行します。
     */
    fun runUntilReady(
        plugin: Plugin,
        isStillCurrent: () -> Boolean,
        isReady: () -> Boolean,
        onReady: () -> Unit,
        onTimeout: (attempts: Int) -> Unit,
        intervalTicks: Long = RETRY_INTERVAL_TICKS,
        maxAttempts: Int = MAX_ATTEMPTS,
        requiredConfirmations: Int = REQUIRED_CONFIRMATIONS,
    ) {
        scheduleAttempt(
            plugin = plugin,
            isStillCurrent = isStillCurrent,
            isReady = isReady,
            onReady = onReady,
            onTimeout = onTimeout,
            intervalTicks = intervalTicks,
            maxAttempts = maxAttempts,
            requiredConfirmations = requiredConfirmations,
            attempt = 0,
            confirmations = 0,
        )
    }

    private fun scheduleAttempt(
        plugin: Plugin,
        isStillCurrent: () -> Boolean,
        isReady: () -> Boolean,
        onReady: () -> Unit,
        onTimeout: (attempts: Int) -> Unit,
        intervalTicks: Long,
        maxAttempts: Int,
        requiredConfirmations: Int,
        attempt: Int,
        confirmations: Int,
    ) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (!isStillCurrent()) return@Runnable
            val nextConfirmations = if (isReady()) confirmations + 1 else 0
            if (nextConfirmations >= requiredConfirmations) {
                if (!isStillCurrent()) return@Runnable
                onReady()
                return@Runnable
            }
            if (attempt >= maxAttempts) {
                onTimeout(attempt)
                return@Runnable
            }
            scheduleAttempt(
                plugin = plugin,
                isStillCurrent = isStillCurrent,
                isReady = isReady,
                onReady = onReady,
                onTimeout = onTimeout,
                intervalTicks = intervalTicks,
                maxAttempts = maxAttempts,
                requiredConfirmations = requiredConfirmations,
                attempt = attempt + 1,
                confirmations = nextConfirmations,
            )
        }, intervalTicks)
    }
}
