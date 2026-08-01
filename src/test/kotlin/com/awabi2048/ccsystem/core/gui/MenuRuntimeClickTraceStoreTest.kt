package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuActionSafety
import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickDisposition
import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickTrace
import com.awabi2048.ccsystem.api.gui.MenuRuntimeRouteSnapshot
import java.util.UUID
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MenuRuntimeClickTraceStoreTest {
    @Test
    fun `trace buffer is bounded and resets sequence for a new run`() {
        val store = MenuRuntimeClickTraceStore(capacityPerPlayer = 2)
        val playerId = UUID.randomUUID()
        assertEquals("audit-a", store.start(playerId, "audit-a"))

        repeat(3) { index ->
            val identity = store.next(playerId)
            store.append(playerId, trace(identity.runId, identity.sequence, playerId, index))
        }
        assertEquals(listOf(2L, 3L), store.all(playerId).map(MenuRuntimeClickTrace::sequence))

        assertEquals("audit-b", store.start(playerId, "audit-b"))
        assertEquals(1L, store.next(playerId).sequence)
        assertNull(store.latest(playerId))
    }

    @Test
    fun `owner cleanup only removes matching route traces`() {
        val store = MenuRuntimeClickTraceStore()
        val playerId = UUID.randomUUID()
        val matching = store.next(playerId)
        store.append(playerId, trace(matching.runId, matching.sequence, playerId, 0, "owner-a"))
        val retained = store.next(playerId)
        store.append(playerId, trace(retained.runId, retained.sequence, playerId, 1, "owner-b"))

        store.clearOwner("owner-a")
        assertEquals(listOf("owner-b"), store.all(playerId).mapNotNull { it.beforeRoute?.owner })
    }

    private fun trace(runId: String, sequence: Long, playerId: UUID, slot: Int, owner: String = "owner"): MenuRuntimeClickTrace =
        MenuRuntimeClickTrace(
            runId,
            sequence,
            playerId,
            1,
            MenuRuntimeRouteSnapshot(owner, "route", emptyMap()),
            slot,
            ClickType.LEFT,
            true,
            false,
            MenuRuntimeClickDisposition.DISPLAY_ONLY,
            null,
            null,
            null,
            emptyMap(),
            MenuActionSafety.UNSPECIFIED,
            null,
            null,
            null,
            1,
            MenuRuntimeRouteSnapshot(owner, "route", emptyMap()),
        )
}
