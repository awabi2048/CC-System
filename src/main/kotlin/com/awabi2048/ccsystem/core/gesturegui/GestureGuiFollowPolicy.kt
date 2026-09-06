package com.awabi2048.ccsystem.core.gesturegui

/**
 * プレイヤー追従Gesture GUIの移動・停止判定を一元化します。
 *
 * 追従の方針は「移動中は凍結・停止時に再召喚」です。移動中の毎tick teleportは
 * 背景と内容物の適用時刻差でティアを招くため、移動中はposeを一切更新せず、
 * 停止が確定してから再召喚で正確な位置へ確定させます。
 * 視線再調整(旧3秒規則)は停止時再召喚へ統合したため無効化しています。
 * 視線系の関数はテスト互換のために残しますが、tickからは呼びません。
 */
internal object GestureGuiFollowPolicy {
    const val GAZE_REALIGN_DELAY_TICKS: Int = 60

    /**
     * 視線再調整の有効化フラグです。停止時再召喚が向きを確定させるため、
     * 旧来の3秒スイープは使いません。falseのままtick側の再調整分岐全体を
     * 迂回します。
     */
    const val FOLLOW_GAZE_REALIGNMENT_ENABLED: Boolean = false

    /** 追従更新を無視する水平移動のデッドバンドです（ブロック単位）。 */
    const val FOLLOW_POSITION_DEADBAND: Double = 0.002

    /**
     * 無移動が何tick続けば停止確定とするかです。20TPS前提で3tick=約0.15秒です。
     * 確定したらその場へ再召喚し、完全に固定します。
     */
    const val STOP_SETTLE_TICKS: Long = 3L

    /**
     * 停止確定時に再召喚を行う最小移動量です(ブロック単位の距離)。
     *
     * 前回確定位置からの変位がこの値未満の場合は、実体の作り直しを行わず
     * 基準位置だけを更新します。微動のたびに再召喚してちらつかせないためです。
     */
    const val RESUMMON_MIN_DISTANCE: Double = 0.5

    /** 移動・停止の判定結果です。 */
    internal enum class FollowMotionState {
        /** 移動中です。poseは凍結し、停止tickを更新します。 */
        MOVING,
        /** 停止中ですが確定前です。何もしません。 */
        SETTLING,
        /** 停止が確定しました。その場へ再召喚します。 */
        STOPPED,
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

    /**
     * デッドバンドを超える位置移動があったかを返します。
     *
     * 移動判定はPosition専用です。yawの変化は移動とみなしません。
     * Y移動も追従対象に含め、ジャンプや段差で描画と判定が分岐しないようにします。
     */
    fun isSignificantMotion(
        deltaX: Double,
        deltaY: Double,
        deltaZ: Double,
    ): Boolean {
        val movedSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
        val deadbandSq = FOLLOW_POSITION_DEADBAND * FOLLOW_POSITION_DEADBAND
        return movedSq >= deadbandSq
    }

    /**
     * 移動・停止の状態を判定します。
     *
     * 移動中はMOVINGを返し、呼び出し側で停止tickを更新します。無移動が
     * [STOP_SETTLE_TICKS]続けばSTOPPEDを返します。一度も動いていない
     * セッション(lastMotionTick < 0)はSETTLINGのままにし、再召喚しません。
     */
    fun decideFollowMotion(
        nowTick: Long,
        lastMotionTick: Long,
        moved: Boolean,
    ): FollowMotionState = when {
        moved -> FollowMotionState.MOVING
        lastMotionTick < 0L -> FollowMotionState.SETTLING
        nowTick - lastMotionTick >= STOP_SETTLE_TICKS -> FollowMotionState.STOPPED
        else -> FollowMotionState.SETTLING
    }

    /**
     * 停止確定時に再召喚を行うべきかを返します。
     *
     * 前回確定位置からの変位が[RESUMMON_MIN_DISTANCE]未満の場合は作り直さず、
     * 基準位置の更新だけに留めます。
     */
    fun shouldResummonOnStop(
        deltaX: Double,
        deltaY: Double,
        deltaZ: Double,
    ): Boolean {
        val distSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
        return distSq >= RESUMMON_MIN_DISTANCE * RESUMMON_MIN_DISTANCE
    }

    /**
     * 移動中に本体をダミーパネルへ切り替えてよいかを返します。
     *
     * 停止時再召喚と同一のゲート（視線外かつ閾値以上の変位）の通過を要求します。
     * 小さな揺れや視線が画面内にある間はダミーを開始せず、従来どおり凍結を維持します。
     * ゲートを維持することで、停止時に再召喚が見送られる移動ではダミー自体が
     * 開始されず、本体復帰の不整合が生じません。
     */
    fun shouldStartDummyFollow(
        insideScreenArea: Boolean,
        deltaX: Double,
        deltaY: Double,
        deltaZ: Double,
    ): Boolean = !insideScreenArea && shouldResummonOnStop(deltaX, deltaY, deltaZ)
}
