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

    @Test
    fun `close gesture remains active when gaze temporarily misses the screen`() {
        assertTrue(
            GestureGuiInputCapturePolicy.isCloseGestureActive(
                GestureGuiSessionState.ACTIVE,
                participating = true,
            ),
        )
        assertTrue(
            GestureGuiInputCapturePolicy.isCloseGestureActive(
                GestureGuiSessionState.OPENING,
                participating = true,
            ),
        )
        assertFalse(
            GestureGuiInputCapturePolicy.isCloseGestureActive(
                GestureGuiSessionState.CLOSING,
                participating = true,
            ),
        )
        assertFalse(
            GestureGuiInputCapturePolicy.isCloseGestureActive(
                GestureGuiSessionState.ACTIVE,
                participating = false,
            ),
        )
    }

    @Test
    fun `sneak secondary input is consumed independently of screen gaze`() {
        assertTrue(
            GestureGuiInputCapturePolicy.isSneakSecondarySuppressed(
                GestureGuiSessionState.ACTIVE,
                participating = true,
                sneaking = true,
                secondaryInputEnabled = false,
            ),
        )
        assertFalse(
            GestureGuiInputCapturePolicy.isSneakSecondarySuppressed(
                GestureGuiSessionState.ACTIVE,
                participating = true,
                sneaking = false,
                secondaryInputEnabled = false,
            ),
        )
        assertFalse(
            GestureGuiInputCapturePolicy.isSneakSecondarySuppressed(
                GestureGuiSessionState.ACTIVE,
                participating = true,
                sneaking = true,
                secondaryInputEnabled = true,
            ),
        )
        assertFalse(
            GestureGuiInputCapturePolicy.isSneakSecondarySuppressed(
                GestureGuiSessionState.CLOSING,
                participating = true,
                sneaking = true,
                secondaryInputEnabled = false,
            ),
        )
    }
}
