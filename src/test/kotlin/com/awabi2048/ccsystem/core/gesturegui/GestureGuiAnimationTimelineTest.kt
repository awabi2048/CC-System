package com.awabi2048.ccsystem.core.gesturegui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GestureGuiAnimationTimelineTest {
    @Test
    fun `open and close keep point phase for three ticks and every transition lasts three ticks`() {
        assertEquals(3, GestureGuiAnimationTimeline.TRANSITION_TICKS)
        assertEquals(3, GestureGuiAnimationTimeline.POINT_HOLD_TICKS)
        assertEquals(7L, GestureGuiAnimationTimeline.OPEN_TO_LINE_DELAY)
        assertEquals(13L, GestureGuiAnimationTimeline.OPEN_COMPLETE_DELAY)
        assertEquals(9L, GestureGuiAnimationTimeline.CLOSE_TO_ZERO_DELAY)
        assertEquals(12L, GestureGuiAnimationTimeline.CLOSE_COMPLETE_DELAY)
    }
}
