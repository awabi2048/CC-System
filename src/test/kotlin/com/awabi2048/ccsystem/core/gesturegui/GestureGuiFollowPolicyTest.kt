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
    fun `significant motion includes every single axis without filtering`() {
        // 追従に軸の除外はありません。X/Y/Zのどれか1軸だけの移動でも
        // 移動として扱います(Y軸のみのジャンプ・段差を含む)。
        assertTrue(GestureGuiFollowPolicy.isSignificantMotion(0.05, 0.0, 0.0, 0.0f))
        assertTrue(GestureGuiFollowPolicy.isSignificantMotion(0.0, 0.05, 0.0, 0.0f))
        assertTrue(GestureGuiFollowPolicy.isSignificantMotion(0.0, 0.0, 0.05, 0.0f))
        assertTrue(GestureGuiFollowPolicy.isSignificantMotion(0.0, 0.0, 0.0, 1.0f))
        assertFalse(GestureGuiFollowPolicy.isSignificantMotion(0.001, 0.0, 0.0, 0.05f))
    }

    @Test
    fun `stop is confirmed only after ten still ticks following motion`() {
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
        // 停止9tickまでは確定しません。
        assertEquals(
            GestureGuiFollowPolicy.FollowMotionState.SETTLING,
            GestureGuiFollowPolicy.decideFollowMotion(nowTick = 109L, lastMotionTick = 100L, moved = false),
        )
        // 停止10tickで確定します。
        assertEquals(
            GestureGuiFollowPolicy.FollowMotionState.STOPPED,
            GestureGuiFollowPolicy.decideFollowMotion(nowTick = 110L, lastMotionTick = 100L, moved = false),
        )
    }
}
