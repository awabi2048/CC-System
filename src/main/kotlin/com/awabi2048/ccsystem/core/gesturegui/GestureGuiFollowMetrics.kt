package com.awabi2048.ccsystem.core.gesturegui

import org.bukkit.plugin.Plugin
import java.util.concurrent.atomic.AtomicLong

/**
 * 追従pose更新の計測値を集約します。
 *
 * 表示ティアの原因切り分け（更新頻度・teleport数・間引き効果）を、実サーバーで
 * before／after比較できることが目的です。判定の正本には使わず、記録専用です。
 * ログは30秒（600tick）ごとに、活動があった場合だけ info へ出します。
 */
internal object GestureGuiFollowMetrics {
    private const val LOG_INTERVAL_TICKS: Long = 600L

    private val evaluations = AtomicLong(0L)
    private val poseUpdates = AtomicLong(0L)
    private val skippedInterval = AtomicLong(0L)
    private val skippedDeadband = AtomicLong(0L)
    private val teleportedEntities = AtomicLong(0L)
    private val backgroundResizeSkips = AtomicLong(0L)

    fun recordEvaluation() {
        evaluations.incrementAndGet()
    }

    fun recordPoseUpdate(teleportedEntityCount: Int) {
        poseUpdates.incrementAndGet()
        teleportedEntities.addAndGet(teleportedEntityCount.toLong())
    }

    fun recordSkippedInterval() {
        skippedInterval.incrementAndGet()
    }

    fun recordSkippedDeadband() {
        skippedDeadband.incrementAndGet()
    }

    fun recordBackgroundResizeSkipped() {
        backgroundResizeSkips.incrementAndGet()
    }

    /** 活動があった場合だけ集計をログへ出し、次区間のため計数を破棄します。 */
    fun maybeLog(plugin: Plugin, tickIndex: Long) {
        if (tickIndex <= 0L || tickIndex % LOG_INTERVAL_TICKS != 0L) return
        val eval = evaluations.getAndSet(0L)
        if (eval == 0L) {
            // 他計数だけ残る状態を避けるため、空区間でも全て破棄します。
            poseUpdates.set(0L)
            skippedInterval.set(0L)
            skippedDeadband.set(0L)
            teleportedEntities.set(0L)
            backgroundResizeSkips.set(0L)
            return
        }
        val updates = poseUpdates.getAndSet(0L)
        val interval = skippedInterval.getAndSet(0L)
        val deadband = skippedDeadband.getAndSet(0L)
        val entities = teleportedEntities.getAndSet(0L)
        val resizeSkips = backgroundResizeSkips.getAndSet(0L)
        val average = if (updates > 0L) entities.toDouble() / updates.toDouble() else 0.0
        plugin.logger.info(
            "Gesture追従計測: 評価=${eval} 更新=${updates} 間引き=${interval} " +
                "微小 skip=${deadband} teleport計=${entities} 平均／更新=${"%.1f".format(average)} " +
                "背景resize省略=${resizeSkips}",
        )
    }
}
