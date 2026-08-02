package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuActionSafety
import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickDisposition
import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickTrace
import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickTraceAwaitException
import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickTraceAwaitFailureReason
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperation
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationResult
import com.awabi2048.ccsystem.api.gui.MenuRuntimeRouteSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeUpdateApplication
import com.awabi2048.ccsystem.api.gui.MenuRuntimeUpdateApplicationState
import com.awabi2048.ccsystem.api.gui.MenuRuntimeUpdateFailureReason
import com.awabi2048.ccsystem.api.gui.MenuRuntimeUpdateKind
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuRuntimeClickTraceStoreTest {
    @Test
    fun `trace buffer is bounded and resets sequence for a new run`() {
        val store = MenuRuntimeClickTraceStore(capacityPerPlayer = 2)
        val playerId = UUID.randomUUID()
        assertEquals("audit-a", store.start(playerId, "audit-a"))

        repeat(3) { index ->
            val identity = store.next(playerId)
            append(store, playerId, identity, index)
        }
        assertEquals(listOf(2L, 3L), store.all(playerId).map(MenuRuntimeClickTrace::sequence))

        assertEquals("audit-b", store.start(playerId, "audit-b"))
        assertEquals(1L, store.next(playerId).sequence)
        assertNull(store.latest(playerId))
    }

    @Test
    fun `stale identity cannot append into a reused run id`() {
        val store = MenuRuntimeClickTraceStore()
        val playerId = UUID.randomUUID()
        store.start(playerId, "audit")
        val stale = store.next(playerId)

        store.start(playerId, "audit")
        append(store, playerId, stale, 0)

        assertNull(store.latest(playerId))
    }

    @Test
    fun `owner cleanup only removes matching route traces`() {
        val store = MenuRuntimeClickTraceStore()
        val playerId = UUID.randomUUID()
        val matching = store.next(playerId)
        append(store, playerId, matching, 0, "owner-a")
        val retained = store.next(playerId)
        append(store, playerId, retained, 1, "owner-b")

        store.clearOwner("owner-a")

        assertEquals(listOf("owner-b"), store.all(playerId).mapNotNull { it.beforeRoute?.owner })
    }

    @Test
    fun `non pending trace completes immediately without registering a waiter`() {
        val store = MenuRuntimeClickTraceStore()
        val playerId = UUID.randomUUID()
        val identity = store.next(playerId)
        val trace = append(store, playerId, identity, 0)

        val waiter = store.awaitTerminal(playerId, identity.runId, identity.sequence)

        assertTrue(waiter.isDone)
        assertEquals(trace, waiter.get())
        assertEquals(trace, store.terminal(playerId, identity.runId, identity.sequence))
    }

    @Test
    fun `unknown run and sequence fail immediately with typed reasons`() {
        val store = MenuRuntimeClickTraceStore()
        val playerId = UUID.randomUUID()
        val runId = store.start(playerId, "active")
        val unrecorded = store.next(playerId)

        assertAwaitFailure(
            store.awaitTerminal(playerId, "missing", 1),
            MenuRuntimeClickTraceAwaitFailureReason.UNKNOWN_RUN,
        )
        assertAwaitFailure(
            store.awaitTerminal(playerId, runId, unrecorded.sequence),
            MenuRuntimeClickTraceAwaitFailureReason.UNKNOWN_SEQUENCE,
        )
    }

    @Test
    fun `owner cleanup cancels a waiter for a removed pending trace`() {
        val store = MenuRuntimeClickTraceStore()
        val playerId = UUID.randomUUID()
        val identity = store.next(playerId)
        append(store, playerId, identity, 0, "owner-a", pending())
        val waiter = store.awaitTerminal(playerId, identity.runId, identity.sequence)

        store.clearOwner("owner-a")

        assertCancelled(waiter)
    }

    @Test
    fun `eviction cancels the pending waiter and makes the old sequence unknown`() {
        val store = MenuRuntimeClickTraceStore(capacityPerPlayer = 1)
        val playerId = UUID.randomUUID()
        val pendingIdentity = store.next(playerId)
        append(store, playerId, pendingIdentity, 0, application = pending())
        val waiter = store.awaitTerminal(playerId, pendingIdentity.runId, pendingIdentity.sequence)

        val replacement = store.next(playerId)
        append(store, playerId, replacement, 1)

        assertCancelled(waiter)
        assertAwaitFailure(
            store.awaitTerminal(playerId, pendingIdentity.runId, pendingIdentity.sequence),
            MenuRuntimeClickTraceAwaitFailureReason.UNKNOWN_SEQUENCE,
        )
    }

    @Test
    fun `pending trace is replaced by a terminal success and completes its waiter`() {
        val store = MenuRuntimeClickTraceStore()
        val playerId = UUID.randomUUID()
        val identity = store.next(playerId)
        append(store, playerId, identity, 0, application = pending())
        val waiter = store.awaitTerminal(playerId, identity.runId, identity.sequence)
        assertFalse(waiter.isDone)

        store.update(playerId, identity) { trace ->
            trace.copy(
                application = trace.application.copy(
                    applied = true,
                    failureReason = MenuRuntimeUpdateFailureReason.NONE,
                    state = MenuRuntimeUpdateApplicationState.TERMINAL,
                    operationResult = MenuRuntimeOperationResult.succeeded(MenuRuntimeOperation.FINISH_EXTERNAL, null),
                ),
            )
        }

        val terminal = waiter.get()
        assertTrue(terminal.application.terminal)
        assertTrue(terminal.application.applied)
        assertEquals(MenuRuntimeUpdateApplicationState.TERMINAL, terminal.application.state)
        assertEquals(terminal, store.terminal(playerId, identity.runId, identity.sequence))
    }

    @Test
    fun `pending trace retains the terminal failure that completes it`() {
        val store = MenuRuntimeClickTraceStore()
        val playerId = UUID.randomUUID()
        val identity = store.next(playerId)
        append(store, playerId, identity, 0, application = pending())
        val waiter = store.awaitTerminal(playerId, identity.runId, identity.sequence)
        val failure = MenuRuntimeOperationResult.failed(
            MenuRuntimeOperation.FINISH_EXTERNAL,
            null,
            com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationFailureReason.INVENTORY_OPEN_FAILED,
        )

        store.update(playerId, identity) { trace ->
            trace.copy(
                application = trace.application.copy(
                    applied = false,
                    failureReason = MenuRuntimeUpdateFailureReason.INVENTORY_OPEN_FAILED,
                    operationResult = failure,
                    state = MenuRuntimeUpdateApplicationState.TERMINAL,
                ),
            )
        }

        val terminal = waiter.get()
        assertTrue(terminal.application.terminal)
        assertFalse(terminal.application.applied)
        assertEquals(MenuRuntimeUpdateFailureReason.INVENTORY_OPEN_FAILED, terminal.application.failureReason)
        assertEquals(failure, terminal.application.operationResult)
    }

    @Test
    fun `concurrent await and run replacement or clear always settles the waiter`() {
        repeat(40) { iteration ->
            val store = MenuRuntimeClickTraceStore()
            val playerId = UUID.randomUUID()
            val identity = store.next(playerId)
            append(store, playerId, identity, 0, application = pending())
            val barrier = CyclicBarrier(2)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val awaited = executor.submit<CompletableFuture<MenuRuntimeClickTrace>> {
                    barrier.await()
                    store.awaitTerminal(playerId, identity.runId, identity.sequence)
                }
                val lifecycle = executor.submit {
                    barrier.await()
                    if (iteration % 2 == 0) store.start(playerId) else store.clear(playerId)
                }

                lifecycle.get(5, TimeUnit.SECONDS)
                assertTrue(awaited.get(5, TimeUnit.SECONDS).isDone)
            } finally {
                executor.shutdownNow()
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
            }
        }
    }

    @Test
    fun `terminal callbacks run after the trace lock is released`() {
        val store = MenuRuntimeClickTraceStore()
        val playerId = UUID.randomUUID()
        val identity = store.next(playerId)
        append(store, playerId, identity, 0, application = pending())
        val waiter = store.awaitTerminal(playerId, identity.runId, identity.sequence)
        val callbackStarted = CountDownLatch(1)
        val releaseCallback = CountDownLatch(1)
        waiter.whenComplete { _, _ ->
            callbackStarted.countDown()
            assertTrue(releaseCallback.await(5, TimeUnit.SECONDS))
        }

        val executor = Executors.newFixedThreadPool(2)
        try {
            val update = executor.submit {
                store.update(playerId, identity) { trace ->
                    trace.copy(application = trace.application.copy(state = MenuRuntimeUpdateApplicationState.TERMINAL))
                }
            }
            assertTrue(callbackStarted.await(5, TimeUnit.SECONDS))

            executor.submit { store.clear(playerId) }.get(1, TimeUnit.SECONDS)
            releaseCallback.countDown()
            update.get(5, TimeUnit.SECONDS)
        } finally {
            releaseCallback.countDown()
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    private fun append(
        store: MenuRuntimeClickTraceStore,
        playerId: UUID,
        identity: MenuRuntimeClickTraceStore.Identity,
        slot: Int,
        owner: String = "owner",
        application: MenuRuntimeUpdateApplication = MenuRuntimeUpdateApplication.notAttempted(),
    ): MenuRuntimeClickTrace = trace(identity.runId, identity.sequence, playerId, slot, owner, application).also {
        store.append(playerId, identity, it)
    }

    private fun pending(): MenuRuntimeUpdateApplication = MenuRuntimeUpdateApplication.pending(
        MenuRuntimeUpdateKind.RESUME,
        route("route"),
        1L,
        MenuRuntimeOperationResult.pending(MenuRuntimeOperation.FINISH_EXTERNAL, null),
    )

    private fun assertAwaitFailure(
        future: CompletableFuture<MenuRuntimeClickTrace>,
        reason: MenuRuntimeClickTraceAwaitFailureReason,
    ) {
        val error = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(error.cause is MenuRuntimeClickTraceAwaitException)
        assertEquals(reason, (error.cause as MenuRuntimeClickTraceAwaitException).reason)
    }

    private fun assertCancelled(future: CompletableFuture<MenuRuntimeClickTrace>) {
        assertTrue(future.isCompletedExceptionally)
        assertThrows(CancellationException::class.java) { future.get() }
    }

    private fun trace(
        runId: String,
        sequence: Long,
        playerId: UUID,
        slot: Int,
        owner: String,
        application: MenuRuntimeUpdateApplication,
    ): MenuRuntimeClickTrace = MenuRuntimeClickTrace(
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
        application,
    )

    private fun route(id: String) = MenuRuntimeRouteSnapshot("owner", id, emptyMap())
}
