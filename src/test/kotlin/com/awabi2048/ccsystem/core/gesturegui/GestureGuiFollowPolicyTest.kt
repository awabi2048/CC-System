package com.awabi2048.ccsystem.core.gesturegui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiFollowPolicyTest {
    @Test
    fun `realignment starts only after sixty consecutive ticks outside the screen`() {
        var outsideTicks = 0

        repeat(GestureGuiFollowPolicy.GAZE_REALIGN_DELAY_TICKS - 1) {
            outsideTicks = GestureGuiFollowPolicy.nextOutsideTicks(outsideTicks, insideScreenArea = false)
        }

        assertEquals(GestureGuiFollowPolicy.GAZE_REALIGN_DELAY_TICKS - 1, outsideTicks)
        assertFalse(GestureGuiFollowPolicy.shouldStartRealignment(false, outsideTicks, null))

        outsideTicks = GestureGuiFollowPolicy.nextOutsideTicks(outsideTicks, insideScreenArea = false)

        assertEquals(GestureGuiFollowPolicy.GAZE_REALIGN_DELAY_TICKS, outsideTicks)
        assertTrue(GestureGuiFollowPolicy.shouldStartRealignment(false, outsideTicks, null))
    }

    @Test
    fun `returning to the screen resets the continuous outside duration`() {
        val outsideTicks = GestureGuiFollowPolicy.nextOutsideTicks(42, insideScreenArea = true)

        assertEquals(0, outsideTicks)
        assertFalse(GestureGuiFollowPolicy.shouldStartRealignment(true, outsideTicks, null))
        assertFalse(GestureGuiFollowPolicy.shouldStartRealignment(false, outsideTicks, 90.0f))
    }

    @Test
    fun `significant motion uses position only without axis filtering`() {
        // 移動判定はPosition専用です。X/Y/Zのどれか1軸だけの移動でも
        // 移動として扱います(Y軸のみのジャンプ・段差を含む)。
        // yawの変化は移動とみなしません。
        assertTrue(GestureGuiFollowPolicy.isSignificantMotion(0.05, 0.0, 0.0))
        assertTrue(GestureGuiFollowPolicy.isSignificantMotion(0.0, 0.05, 0.0))
        assertTrue(GestureGuiFollowPolicy.isSignificantMotion(0.0, 0.0, 0.05))
        assertFalse(GestureGuiFollowPolicy.isSignificantMotion(0.001, 0.0, 0.0))
    }

    @Test
    fun `stop is confirmed only after three still ticks following motion`() {
        // 移動中はMOVINGを返します。
        assertEquals(
            GestureGuiFollowPolicy.FollowMotionState.MOVING,
            GestureGuiFollowPolicy.decideFollowMotion(nowTick = 100L, lastMotionTick = 90L, moved = true),
        )
        // 一度も動いていないセッションは再召喚しません。
        assertEquals(
            GestureGuiFollowPolicy.FollowMotionState.SETTLING,
            GestureGuiFollowPolicy.decideFollowMotion(nowTick = 100L, lastMotionTick = -1L, moved = false),
        )
        // 停止2tickまでは確定しません。
        assertEquals(
            GestureGuiFollowPolicy.FollowMotionState.SETTLING,
            GestureGuiFollowPolicy.decideFollowMotion(nowTick = 102L, lastMotionTick = 100L, moved = false),
        )
        // 停止3tickで確定します。
        assertEquals(
            GestureGuiFollowPolicy.FollowMotionState.STOPPED,
            GestureGuiFollowPolicy.decideFollowMotion(nowTick = 103L, lastMotionTick = 100L, moved = false),
        )
    }

    @Test
    fun `resummon on stop requires half a meter of displacement`() {
        // 前回確定位置から0.5m未満の変位では実体を作り直しません。
        assertFalse(GestureGuiFollowPolicy.shouldResummonOnStop(0.2, 0.0, 0.0))
        assertFalse(GestureGuiFollowPolicy.shouldResummonOnStop(0.0, 0.4, 0.0))
        // 0.5m以上の変位で再召喚します。
        assertTrue(GestureGuiFollowPolicy.shouldResummonOnStop(0.5, 0.0, 0.0))
        assertTrue(GestureGuiFollowPolicy.shouldResummonOnStop(0.0, 0.0, -2.5))
        assertTrue(GestureGuiFollowPolicy.shouldResummonOnStop(0.3, 0.4, 0.0))
    }

    @Test
    fun `dummy follow starts only through the same gate as resummon on stop`() {
        // 視線が画面内の間はダミーを開始せず、凍結を維持します。
        assertFalse(GestureGuiFollowPolicy.shouldStartDummyFollow(true, 2.0, 0.0, 0.0))
        // 閾値未満の変位ではダミーを開始しません。
        assertFalse(GestureGuiFollowPolicy.shouldStartDummyFollow(false, 0.2, 0.0, 0.0))
        assertFalse(GestureGuiFollowPolicy.shouldStartDummyFollow(false, 0.0, 0.4, 0.0))
        // 視線外かつ0.5m以上の変位でダミーを開始します。
        assertTrue(GestureGuiFollowPolicy.shouldStartDummyFollow(false, 0.5, 0.0, 0.0))
        assertTrue(GestureGuiFollowPolicy.shouldStartDummyFollow(false, 0.0, 0.0, -2.5))
        assertTrue(GestureGuiFollowPolicy.shouldStartDummyFollow(false, 0.3, 0.4, 0.0))
    }
}
