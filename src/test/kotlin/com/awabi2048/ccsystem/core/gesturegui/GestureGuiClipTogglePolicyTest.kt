package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiSessionState
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiClipTogglePolicyTest {
    @Test
    fun `pin is allowed only while following in an active session`() {
        assertTrue(GestureGuiClipTogglePolicy.canPin(GestureGuiSessionState.ACTIVE, null))
        assertFalse(GestureGuiClipTogglePolicy.canPin(GestureGuiSessionState.ACTIVE, Any()))
        assertFalse(GestureGuiClipTogglePolicy.canPin(GestureGuiSessionState.OPENING, null))
        assertFalse(GestureGuiClipTogglePolicy.canPin(GestureGuiSessionState.CLOSING, null))
    }

    @Test
    fun `unpin is allowed only while fixed in an active session`() {
        assertTrue(GestureGuiClipTogglePolicy.canUnpin(GestureGuiSessionState.ACTIVE, Any()))
        assertFalse(GestureGuiClipTogglePolicy.canUnpin(GestureGuiSessionState.ACTIVE, null))
        assertFalse(GestureGuiClipTogglePolicy.canUnpin(GestureGuiSessionState.OPENING, Any()))
        assertFalse(GestureGuiClipTogglePolicy.canUnpin(GestureGuiSessionState.CLOSING, Any()))
    }

    @Test
    fun `pin and unpin form a toggle round trip`() {
        var fixedAnchor: Any? = null
        val state = GestureGuiSessionState.ACTIVE

        // 追従中 → 固定へ遷移
        assertTrue(GestureGuiClipTogglePolicy.canPin(state, fixedAnchor))
        fixedAnchor = Any()
        // 固定中 → 解除へ遷移
        assertTrue(GestureGuiClipTogglePolicy.canUnpin(state, fixedAnchor))
        assertFalse(GestureGuiClipTogglePolicy.canPin(state, fixedAnchor))
        fixedAnchor = null
        // 解除後 → 再度固定へ遷移できる
        assertTrue(GestureGuiClipTogglePolicy.canPin(state, fixedAnchor))
        assertFalse(GestureGuiClipTogglePolicy.canUnpin(state, fixedAnchor))
    }
}
