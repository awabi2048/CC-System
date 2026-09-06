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
    private val frozenSkips = AtomicLong(0L)
    private val gazeFrozenSkips = AtomicLong(0L)
    private val stopResummons = AtomicLong(0L)
    private val resSummonedEntities = AtomicLong(0L)
    private val resSummonSkips = AtomicLong(0L)
    private val teleportedEntities = AtomicLong(0L)
    private val backgroundResizeSkips = AtomicLong(0L)
    private val dummyStarts = AtomicLong(0L)
    private val dummyResummons = AtomicLong(0L)
    private val dummyRestores = AtomicLong(0L)

    fun recordEvaluation() {
        evaluations.incrementAndGet()
    }

    fun recordPoseUpdate(teleportedEntityCount: Int) {
        poseUpdates.incrementAndGet()
        teleportedEntities.addAndGet(teleportedEntityCount.toLong())
    }

    /** 移動中の凍結でpose更新をスキップした回数です。 */
    fun recordFrozenSkipped() {
        frozenSkips.incrementAndGet()
    }

    /** 視線が画面内のため再召喚を見送った回数です。 */
    fun recordGazeFrozenSkipped() {
        gazeFrozenSkips.incrementAndGet()
    }

    /** 停止確定で再召喚した回数と生成実体数です。 */
    fun recordStopResummon(entityCount: Int) {
        stopResummons.incrementAndGet()
        resSummonedEntities.addAndGet(entityCount.toLong())
    }

    /** 変位が閾値未満で再召喚を見送った回数です。 */
    fun recordResummonSkippedBelowThreshold() {
        resSummonSkips.incrementAndGet()
    }

    fun recordBackgroundResizeSkipped() {
        backgroundResizeSkips.incrementAndGet()
    }

    /** 移動中に本体をダミーパネルへ切り替えた回数です。 */
    fun recordDummyStart() {
        dummyStarts.incrementAndGet()
    }

    /** ダミー表示のまま停止確定し、本体を再召喚した回数です。 */
    fun recordDummyResummon() {
        dummyResummons.incrementAndGet()
    }

    /** ダミー表示から再召喚を見送り、本体をそのまま復帰させた回数です。 */
    fun recordDummyRestore() {
        dummyRestores.incrementAndGet()
    }

    /** 活動があった場合だけ集計をログへ出し、次区間のため計数を破棄します。 */
    fun maybeLog(plugin: Plugin, tickIndex: Long) {
        if (tickIndex <= 0L || tickIndex % LOG_INTERVAL_TICKS != 0L) return
        val eval = evaluations.getAndSet(0L)
        if (eval == 0L) {
            // 他計数だけ残る状態を避けるため、空区間でも全て破棄します。
            poseUpdates.set(0L)
            frozenSkips.set(0L)
            gazeFrozenSkips.set(0L)
            stopResummons.set(0L)
            resSummonedEntities.set(0L)
            resSummonSkips.set(0L)
            teleportedEntities.set(0L)
            backgroundResizeSkips.set(0L)
            dummyStarts.set(0L)
            dummyResummons.set(0L)
            dummyRestores.set(0L)
            return
        }
        val updates = poseUpdates.getAndSet(0L)
        val frozen = frozenSkips.getAndSet(0L)
        val gazeFrozen = gazeFrozenSkips.getAndSet(0L)
        val resummons = stopResummons.getAndSet(0L)
        val resEntities = resSummonedEntities.getAndSet(0L)
        val resSkips = resSummonSkips.getAndSet(0L)
        val entities = teleportedEntities.getAndSet(0L)
        val resizeSkips = backgroundResizeSkips.getAndSet(0L)
        val dummies = dummyStarts.getAndSet(0L)
        val dummyResummoned = dummyResummons.getAndSet(0L)
        val dummyRestored = dummyRestores.getAndSet(0L)
        val average = if (resummons > 0L) resEntities.toDouble() / resummons.toDouble() else 0.0
        plugin.logger.info(
            "Gesture追従計測: 評価=${eval} 凍結=${frozen} 視線凍結=${gazeFrozen} 再召喚=${resummons} " +
                "再召喚実体計=${resEntities} 平均／再召喚=${"%.1f".format(average)} " +
                "再召喚見送り=${resSkips} " +
                "teleport計=${entities}(更新${updates}) 背景resize省略=${resizeSkips} " +
                "ダミー開始=${dummies} ダミー再召喚=${dummyResummoned} ダミー復帰=${dummyRestored}",
        )
    }
}
