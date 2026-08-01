package com.awabi2048.ccsystem.api.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuRuntimeDetailedOperationResultTest {
    @Test
    fun `detailed operation results retain every wrapper operation`() {
        val route = MenuRoute("test", "menu")
        val base = MenuRuntimeOperationResult.succeeded(MenuRuntimeOperation.OPEN, route)

        listOf(
            MenuRuntimeOperation.REOPEN_CURRENT,
            MenuRuntimeOperation.OPEN_EPHEMERAL,
            MenuRuntimeOperation.RESUME_EXTERNAL,
            MenuRuntimeOperation.FINISH_EXTERNAL,
            MenuRuntimeOperation.BACK,
            MenuRuntimeOperation.INSPECT,
        ).forEach { operation ->
            assertEquals(operation, base.forOperation(operation).operation)
        }
    }

    @Test
    fun `inspection result requires a snapshot precisely when successful`() {
        val route = MenuRoute("test", "menu")
        val failed = MenuRuntimeInspectionResult(
            MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.INSPECT,
                route,
                MenuRuntimeOperationFailureReason.CONTRACT_INVALID,
            ),
        )

        assertFalse(failed.operationResult.successful)
        assertTrue(failed.snapshot == null)
    }

    @Test
    fun `pending operation is neither a success nor a failure`() {
        val pending = MenuRuntimeOperationResult.pending(
            MenuRuntimeOperation.FINISH_EXTERNAL,
            MenuRoute("test", "menu"),
        )

        assertFalse(pending.terminal)
        assertFalse(pending.successful)
        assertTrue(pending.failure == null)
    }
}
