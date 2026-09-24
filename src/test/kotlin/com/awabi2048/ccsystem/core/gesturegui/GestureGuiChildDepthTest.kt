package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiChildDepthTest {
    @Test
    fun `each child-parent and sibling stack gap is half the prior quarter-block spacing`() {
        assertEquals(0.125, GestureGuiChildDepth.childOffset(0, -1), 1.0e-9)
        assertEquals(0.125, GestureGuiChildDepth.childOffset(1, 0), 1.0e-9)
        assertEquals(0.25, GestureGuiChildDepth.childOffset(1, -1), 1.0e-9)
        assertEquals(0.125, GestureGuiChildDepth.childOffset(2, 1), 1.0e-9)
    }

    @Test
    fun `modal overlay belongs immediately behind each child`() {
        assertEquals(0.005, GestureGuiChildDepth.OVERLAY_RECESS, 1.0e-9)
        assertEquals(0.04, GestureGuiChildDepth.OVERLAY_RECESS / GestureGuiChildDepth.STACK_GAP, 1.0e-9)
    }

    @Test
    fun `overlay pose follows child orientation and uses parent covering size`() {
        val child = GestureGuiScreenPose(
            2, 0.0,
            GestureGuiVector3(4.0, 5.0, 6.0),
            GestureGuiVector3(1.0, 0.0, 0.0),
            GestureGuiVector3(0.0, 1.0, 0.0),
            GestureGuiVector3(0.0, 0.0, 1.0),
            1.0, 0.6,
        )
        val overlay = GestureGuiChildDepth.overlayPose(child, 2.0, 1.0)
        assertEquals(6.005, overlay.center.z, 1.0e-9)
        assertEquals(child.normal, overlay.normal)
        assertEquals(2.0, overlay.width, 1.0e-9)
        assertEquals(1.0, overlay.height, 1.0e-9)
    }

    @Test
    fun `overlay remains between parent visuals and child background at every stack depth`() {
        val contentFront = (GestureGuiVirtualScreens.MAX_LAYER + GestureGuiVirtualScreens.OUTLINE_LAYER_OFFSET) *
            GestureGuiVirtualScreens.LAYER_DEPTH
        for (childIndex in 0..2) {
            for (parentChildIndex in -1 until childIndex) {
                val childDepth = GestureGuiChildDepth.childOffset(childIndex, parentChildIndex)
                val overlayDepth = childDepth - GestureGuiChildDepth.OVERLAY_RECESS
                assertTrue(contentFront < overlayDepth, "遮蔽面は親画面の部品より手前です: $childIndex/$parentChildIndex")
                assertTrue(overlayDepth < childDepth, "遮蔽面は子画面の背景より後ろです: $childIndex/$parentChildIndex")
                assertEquals(0.005, childDepth - overlayDepth, 1.0e-9)
            }
        }
    }
}
