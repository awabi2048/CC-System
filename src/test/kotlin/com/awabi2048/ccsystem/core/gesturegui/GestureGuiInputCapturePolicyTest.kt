package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiSessionState
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiInputCapturePolicyTest {
    @Test
    fun `opening keeps participant input captured while entities are appearing`() {
        assertTrue(
            GestureGuiInputCapturePolicy.isActive(
                GestureGuiSessionState.OPENING,
                participating = true,
                lookingAtScreen = false,
            ),
        )
        assertFalse(
            GestureGuiInputCapturePolicy.isActive(
                GestureGuiSessionState.OPENING,
                participating = false,
                lookingAtScreen = true,
            ),
        )
    }

    @Test
    fun `active participant input is captured only while looking at the screen`() {
        assertTrue(
            GestureGuiInputCapturePolicy.isActive(
                GestureGuiSessionState.ACTIVE,
                participating = true,
                lookingAtScreen = true,
            ),
        )
        assertFalse(
            GestureGuiInputCapturePolicy.isActive(
                GestureGuiSessionState.ACTIVE,
                participating = true,
                lookingAtScreen = false,
            ),
        )
    }

    @Test
    fun `closing and non-participant input are never captured`() {
        assertFalse(
            GestureGuiInputCapturePolicy.isActive(
                GestureGuiSessionState.CLOSING,
                participating = true,
                lookingAtScreen = true,
            ),
        )
        assertFalse(
            GestureGuiInputCapturePolicy.isActive(
                GestureGuiSessionState.ACTIVE,
                participating = false,
                lookingAtScreen = true,
            ),
        )
    }
}
