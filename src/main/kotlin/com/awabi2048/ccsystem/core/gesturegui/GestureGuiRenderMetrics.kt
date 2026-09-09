package com.awabi2048.ccsystem.core.gesturegui

import java.util.concurrent.atomic.AtomicLong

/**
 * Gesture GUI の描画・判定コストを推測ではなく計測するための軽量カウンタです。
 *
 * 本番では常時加算のみ（低コスト）とし、負荷試験を行わない今回は単体試験と
 * 動作確認の裏付けに使います。将来の TPS/MSPT 比較にも接続できます。
 */
internal object GestureGuiRenderMetrics {
    val tickCount = AtomicLong()
    val candidateEvaluations = AtomicLong()
    val lodTransitions = AtomicLong()
    val hitTests = AtomicLong()
    val virtualSpawns = AtomicLong()
    val virtualUpdates = AtomicLong()
    val virtualMoves = AtomicLong()
    val virtualDestroys = AtomicLong()
    val catcherMoves = AtomicLong()
    val logicalViewUpdates = AtomicLong()

    fun snapshot(): Map<String, Long> = mapOf(
        "tickCount" to tickCount.get(),
        "candidateEvaluations" to candidateEvaluations.get(),
        "lodTransitions" to lodTransitions.get(),
        "hitTests" to hitTests.get(),
        "virtualSpawns" to virtualSpawns.get(),
        "virtualUpdates" to virtualUpdates.get(),
        "virtualMoves" to virtualMoves.get(),
        "virtualDestroys" to virtualDestroys.get(),
        "catcherMoves" to catcherMoves.get(),
        "logicalViewUpdates" to logicalViewUpdates.get(),
    )

    fun reset() {
        tickCount.set(0)
        candidateEvaluations.set(0)
        lodTransitions.set(0)
        hitTests.set(0)
        virtualSpawns.set(0)
        virtualUpdates.set(0)
        virtualMoves.set(0)
        virtualDestroys.set(0)
        catcherMoves.set(0)
        logicalViewUpdates.set(0)
    }
}
