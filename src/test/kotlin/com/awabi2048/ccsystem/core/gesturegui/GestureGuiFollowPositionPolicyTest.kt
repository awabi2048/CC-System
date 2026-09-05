package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiFollowPositionPolicyTest {
    @Test
    fun `vertical movement also requests a follow pose update`() {
        assertTrue(
            GestureGuiFollowPositionPolicy.hasMoved(
                current = GestureGuiVector3(10.0, 65.5, 20.0),
                anchorX = 10.0,
                anchorY = 64.0,
                anchorZ = 20.0,
            ),
        )
    }

    @Test
    fun `unchanged eye position does not request a follow pose update`() {
        assertFalse(
            GestureGuiFollowPositionPolicy.hasMoved(
                current = GestureGuiVector3(10.0, 64.0, 20.0),
                anchorX = 10.0,
                anchorY = 64.0,
                anchorZ = 20.0,
            ),
        )
    }
}
