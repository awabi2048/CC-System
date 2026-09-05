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

    /**
     * 追従poseの再計算間隔です。20TPS前提で1tickごとに更新します(20Hz)。
     *
     * 追従更新では補完を無効化して即時確定(スナップ)させるため、毎tickのteleport
     * でも追いかけ再生が起きず、背景と内容物の適用時刻差によるティアを抑えます。
     * 間引きが必要になった場合はこの定数だけを戻せば10Hz運用へ切り替えられます。
     * 視線の再調整判定は毎tick継続し、3秒規則の時間単位は変えません。
     */
    const val FOLLOW_POSE_INTERVAL_TICKS: Long = 1L

    /** 追従更新を無視する水平移動のデッドバンドです（ブロック単位）。 */
    const val FOLLOW_POSITION_DEADBAND: Double = 0.002

    /** 追従更新を無視するyaw変化のデッドバンドです（度）。 */
    const val FOLLOW_YAW_DEADBAND_DEGREES: Float = 0.2f

    /** 追従pose更新の判定結果です。計測ログと更新可否を同じ分岐で共有します。 */
    internal enum class FollowPoseDecision {
        UPDATE,
        SKIP_INTERVAL,
        SKIP_DEADBAND,
    }

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

    /** 前回適用tickから間隔が経過したかを返します。初回は必ず true です。 */
    fun isFollowIntervalElapsed(nowTick: Long, lastAppliedTick: Long): Boolean =
        lastAppliedTick < 0L || nowTick - lastAppliedTick >= FOLLOW_POSE_INTERVAL_TICKS

    /**
     * 追従poseを適用すべきかを判定します。
     *
     * interval未経過を最優先で切り捨て、次に微小移動を切り捨てます。
     * anchor側に前回適用位置を残すことで、切り捨て分の移動は次回へ累積します。
     * Y移動も追従対象に含め、ジャンプや段差で描画と判定が分岐しないようにします。
     */
    fun decideFollowPose(
        nowTick: Long,
        lastAppliedTick: Long,
        deltaX: Double,
        deltaY: Double,
        deltaZ: Double,
        yawDeltaAbs: Float,
    ): FollowPoseDecision {
        if (!isFollowIntervalElapsed(nowTick, lastAppliedTick)) return FollowPoseDecision.SKIP_INTERVAL
        val movedSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
        val deadbandSq = FOLLOW_POSITION_DEADBAND * FOLLOW_POSITION_DEADBAND
        if (movedSq < deadbandSq && yawDeltaAbs < FOLLOW_YAW_DEADBAND_DEGREES) {
            return FollowPoseDecision.SKIP_DEADBAND
        }
        return FollowPoseDecision.UPDATE
    }
}
