package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperation
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationFailureReason
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationResult
import com.awabi2048.ccsystem.api.gui.MenuRoute
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MenuRuntimeExternalFinishResultStoreTest {
    @Test
    fun `asynchronous finish completion retains its detailed failure until cleared`() {
        val playerId = UUID.randomUUID()
        val result = MenuRuntimeOperationResult.failed(
            MenuRuntimeOperation.FINISH_EXTERNAL,
            MenuRoute("test", "route"),
            MenuRuntimeOperationFailureReason.CONTRACT_INVALID,
        )
        val store = MenuRuntimeExternalFinishResultStore()

        store.record(playerId, result)

        assertEquals(result, store.latest(playerId))
        store.clear(playerId)
        assertNull(store.latest(playerId))
    }
}
