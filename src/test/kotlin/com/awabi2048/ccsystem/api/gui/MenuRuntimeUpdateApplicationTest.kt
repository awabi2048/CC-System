package com.awabi2048.ccsystem.api.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuRuntimeUpdateApplicationTest {
    @Test
    fun `navigate result distinguishes a successful open from an open failure`() {
        val target = route("target")
        val succeeded = application(
            kind = MenuRuntimeUpdateKind.NAVIGATE,
            expectedRoute = target,
            observedRoute = target,
            beforeRevision = 10,
            afterRevision = 11,
            applied = true,
        )
        val failed = application(
            kind = MenuRuntimeUpdateKind.NAVIGATE,
            expectedRoute = target,
            observedRoute = route("source"),
            beforeRevision = 10,
            afterRevision = 10,
            applied = false,
            failureReason = MenuRuntimeUpdateFailureReason.OPEN_FAILED,
        )

        assertTrue(succeeded.attempted)
        assertTrue(succeeded.applied)
        assertEquals(target, succeeded.observedRoute)
        assertEquals(11, succeeded.afterRevision)
        assertEquals(MenuRuntimeUpdateFailureReason.NONE, succeeded.failureReason)
        assertTrue(failed.attempted)
        assertFalse(failed.applied)
        assertEquals(MenuRuntimeUpdateFailureReason.OPEN_FAILED, failed.failureReason)
    }

    @Test
    fun `replace and refresh retain their observed route and revision`() {
        val target = route("target")
        val replaced = application(
            kind = MenuRuntimeUpdateKind.REPLACE,
            expectedRoute = target,
            observedRoute = target,
            beforeRevision = 20,
            afterRevision = 21,
            applied = true,
        )
        val refreshed = application(
            kind = MenuRuntimeUpdateKind.REFRESH,
            expectedRoute = target,
            observedRoute = target,
            beforeRevision = 21,
            afterRevision = 22,
            applied = true,
        )

        assertEquals(MenuRuntimeUpdateKind.REPLACE, replaced.kind)
        assertEquals(target, replaced.expectedRoute)
        assertEquals(21, replaced.afterRevision)
        assertEquals(MenuRuntimeUpdateKind.REFRESH, refreshed.kind)
        assertEquals(target, refreshed.observedRoute)
        assertEquals(22, refreshed.afterRevision)
    }

    @Test
    fun `back without history records the fallback as a failed declared update`() {
        val application = application(
            kind = MenuRuntimeUpdateKind.BACK,
            expectedRoute = null,
            observedRoute = null,
            beforeRevision = 30,
            afterRevision = null,
            applied = false,
            failureReason = MenuRuntimeUpdateFailureReason.NO_HISTORY,
        )

        assertTrue(application.attempted)
        assertFalse(application.applied)
        assertEquals(MenuRuntimeUpdateFailureReason.NO_HISTORY, application.failureReason)
        assertNull(application.observedRoute)
    }

    @Test
    fun `close none and exceptions remain distinguishable`() {
        val closed = application(
            kind = MenuRuntimeUpdateKind.CLOSE,
            expectedRoute = null,
            observedRoute = null,
            beforeRevision = 40,
            afterRevision = null,
            applied = true,
        )
        val none = MenuRuntimeUpdateApplication.notAttempted(
            kind = MenuRuntimeUpdateKind.NONE,
            beforeRevision = 41,
        )
        val exception = application(
            kind = MenuRuntimeUpdateKind.REFRESH,
            expectedRoute = route("source"),
            observedRoute = route("source"),
            beforeRevision = 42,
            afterRevision = 42,
            applied = false,
            failureReason = MenuRuntimeUpdateFailureReason.EXCEPTION,
        )

        assertTrue(closed.attempted)
        assertTrue(closed.applied)
        assertFalse(none.attempted)
        assertFalse(none.applied)
        assertEquals(MenuRuntimeUpdateFailureReason.NOT_APPLICABLE, none.failureReason)
        assertTrue(exception.attempted)
        assertFalse(exception.applied)
        assertEquals(MenuRuntimeUpdateFailureReason.EXCEPTION, exception.failureReason)
    }

    private fun application(
        kind: MenuRuntimeUpdateKind,
        expectedRoute: MenuRuntimeRouteSnapshot?,
        observedRoute: MenuRuntimeRouteSnapshot?,
        beforeRevision: Long?,
        afterRevision: Long?,
        applied: Boolean,
        failureReason: MenuRuntimeUpdateFailureReason = MenuRuntimeUpdateFailureReason.NONE,
    ) = MenuRuntimeUpdateApplication(
        attempted = true,
        applied = applied,
        kind = kind,
        expectedRoute = expectedRoute,
        observedRoute = observedRoute,
        beforeRevision = beforeRevision,
        afterRevision = afterRevision,
        failureReason = failureReason,
    )

    private fun route(id: String) = MenuRuntimeRouteSnapshot("test", id, emptyMap())
}
