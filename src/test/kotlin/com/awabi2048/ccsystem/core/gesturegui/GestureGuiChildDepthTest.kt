package com.awabi2048.ccsystem.core.gesturegui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GestureGuiChildDepthTest {
    @Test
    fun `each child-parent and sibling stack gap is half the prior quarter-block spacing`() {
        assertEquals(0.125, GestureGuiChildDepth.childOffset(0), 1.0e-9)
        assertEquals(0.25, GestureGuiChildDepth.childOffset(1), 1.0e-9)
        assertEquals(0.125, GestureGuiChildDepth.childOffset(1) - GestureGuiChildDepth.childOffset(0), 1.0e-9)
    }

    @Test
    fun `modal overlay offset follows the same half-size stack gap`() {
        assertEquals(0.0, GestureGuiChildDepth.modalOverlayOffset(0), 1.0e-9)
        assertEquals(0.125, GestureGuiChildDepth.modalOverlayOffset(1), 1.0e-9)
        assertEquals(0.25, GestureGuiChildDepth.modalOverlayOffset(2), 1.0e-9)
    }
}
