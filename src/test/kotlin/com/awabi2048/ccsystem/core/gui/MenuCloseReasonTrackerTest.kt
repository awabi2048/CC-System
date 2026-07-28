package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuCloseReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MenuCloseReasonTrackerTest {
    @Test
    fun `unmarked close is treated as user dismissal`() {
        val tracker = MenuCloseReasonTracker<Any>()
        assertEquals(MenuCloseReason.USER_DISMISSED, tracker.consume(Any()))
    }

    @Test
    fun `marked close reason is consumed once`() {
        val tracker = MenuCloseReasonTracker<Any>()
        val inventory = Any()

        tracker.mark(inventory, MenuCloseReason.ROUTE_REPLACED)

        assertEquals(MenuCloseReason.ROUTE_REPLACED, tracker.consume(inventory))
        assertEquals(MenuCloseReason.USER_DISMISSED, tracker.consume(inventory))
    }

    @Test
    fun `cleared marker falls back to user dismissal`() {
        val tracker = MenuCloseReasonTracker<Any>()
        val inventory = Any()

        tracker.mark(inventory, MenuCloseReason.RUNTIME_CLOSED)
        tracker.clear(inventory)

        assertEquals(MenuCloseReason.USER_DISMISSED, tracker.consume(inventory))
    }
}
