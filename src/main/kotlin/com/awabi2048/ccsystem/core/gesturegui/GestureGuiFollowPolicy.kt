package com.awabi2048.ccsystem.core.gesturegui

/**
 * プレイヤー追従Gesture GUIの視線再調整条件を一元化します。
 *
 * 視線を一瞬外しただけで画面が回転すると、操作対象を見失いやすくなります。
 * そのため、画面外にいる状態をサーバーティック単位で連続計測し、3秒（60tick）
 * 継続した場合だけ再調整を開始します。サーバーのtick進行を基準にすることで、
 * 実際のGesture GUI更新周期と同じ時間単位で判定できます。
 */
internal object GestureGuiFollowPolicy {
    const val GAZE_REALIGN_DELAY_TICKS: Int = 60

    /** 現在の視線状態を観測した後の連続画面外tick数を返します。 */
    fun nextOutsideTicks(previousTicks: Int, insideScreenArea: Boolean): Int {
        if (insideScreenArea) return 0
        return (previousTicks + 1).coerceAtMost(GAZE_REALIGN_DELAY_TICKS)
    }

    /** 遅延条件を満たし、まだ再調整先を持っていない場合だけ開始を許可します。 */
    fun shouldStartRealignment(
        insideScreenArea: Boolean,
        outsideTicks: Int,
        targetYaw: Float?,
    ): Boolean =
        !insideScreenArea &&
            targetYaw == null &&
            outsideTicks >= GAZE_REALIGN_DELAY_TICKS
}
