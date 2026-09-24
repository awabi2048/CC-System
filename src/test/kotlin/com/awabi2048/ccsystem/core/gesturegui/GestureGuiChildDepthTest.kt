package com.awabi2048.ccsystem.core.gesturegui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun `overlay remains between parent visuals and child background at every stack depth`() {
        val contentFront = (GestureGuiVirtualScreens.MAX_LAYER + GestureGuiVirtualScreens.OUTLINE_LAYER_OFFSET) *
            GestureGuiVirtualScreens.LAYER_DEPTH
        for (childIndex in 0..2) {
            val overlayDepth = GestureGuiChildDepth.modalOverlayOffset(childIndex) +
                GestureGuiVirtualScreens.MODAL_OVERLAY_LAYER * GestureGuiVirtualScreens.LAYER_DEPTH
            val childDepth = GestureGuiChildDepth.childOffset(childIndex)
            assertTrue(contentFront < overlayDepth, "遮蔽面は親画面の部品より手前です: $childIndex")
            assertTrue(overlayDepth < childDepth, "遮蔽面は子画面の背景より後ろです: $childIndex")
        }
    }
}
