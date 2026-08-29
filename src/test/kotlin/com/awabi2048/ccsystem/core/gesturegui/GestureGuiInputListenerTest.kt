package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiInputListenerTest {
    @Test
    fun `retryable consumption cancels the current event without deduplicating the retry`() {
        val result = GestureGuiDispatchResult.RETRYABLE_CONSUMED

        assertTrue(result.consumed)
        assertFalse(result.deduplicate)
        assertTrue(GestureGuiDispatchResult.ACTION_HANDLED.deduplicate)
    }

    @Test
    fun `an unhandled first event does not suppress a same tick retry`() {
        val gate = GestureGuiInputDeduplicator()
        val key = GestureGuiInputKey(UUID.randomUUID(), GestureGuiGesture.PRIMARY)

        assertFalse(gate.isHandled(key, 42))
        gate.record(key, 42, deduplicate = false)
        assertFalse(gate.isHandled(key, 42))

        gate.record(key, 42, deduplicate = true)
        assertTrue(gate.isHandled(key, 42))
        assertFalse(gate.isHandled(key, 43))
    }
}
