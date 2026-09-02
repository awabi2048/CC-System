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
}
