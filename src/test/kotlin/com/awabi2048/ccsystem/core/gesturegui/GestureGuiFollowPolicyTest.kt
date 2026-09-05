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
    fun `follow pose updates at ten hertz intervals`() {
        // 20TPS前提で2tickごとに更新します。初回は必ず更新します。
        assertTrue(GestureGuiFollowPolicy.isFollowIntervalElapsed(0L, -1L))
        assertTrue(GestureGuiFollowPolicy.isFollowIntervalElapsed(10L, 8L))
        assertFalse(GestureGuiFollowPolicy.isFollowIntervalElapsed(9L, 8L))
    }

    @Test
    fun `follow pose skips micro movements within the interval`() {
        val decision = GestureGuiFollowPolicy.decideFollowPose(
            nowTick = 10L,
            lastAppliedTick = 8L,
            deltaX = 0.001,
            deltaZ = 0.0,
            yawDeltaAbs = 0.05f,
        )

        assertEquals(GestureGuiFollowPolicy.FollowPoseDecision.SKIP_DEADBAND, decision)
    }

    @Test
    fun `follow pose updates on large movements after the interval`() {
        val moved = GestureGuiFollowPolicy.decideFollowPose(
            nowTick = 10L,
            lastAppliedTick = 8L,
            deltaX = 0.05,
            deltaZ = 0.0,
            yawDeltaAbs = 0.0f,
        )
        val rotated = GestureGuiFollowPolicy.decideFollowPose(
            nowTick = 10L,
            lastAppliedTick = 8L,
            deltaX = 0.0,
            deltaZ = 0.0,
            yawDeltaAbs = 1.0f,
        )
        val throttled = GestureGuiFollowPolicy.decideFollowPose(
            nowTick = 9L,
            lastAppliedTick = 8L,
            deltaX = 1.0,
            deltaZ = 0.0,
            yawDeltaAbs = 10.0f,
        )

        assertEquals(GestureGuiFollowPolicy.FollowPoseDecision.UPDATE, moved)
        assertEquals(GestureGuiFollowPolicy.FollowPoseDecision.UPDATE, rotated)
        assertEquals(GestureGuiFollowPolicy.FollowPoseDecision.SKIP_INTERVAL, throttled)
    }
}
