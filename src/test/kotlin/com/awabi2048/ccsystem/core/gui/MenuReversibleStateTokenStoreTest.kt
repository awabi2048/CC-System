package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuReversibleContract
import com.awabi2048.ccsystem.api.gui.MenuReversibleInteractionContext
import com.awabi2048.ccsystem.api.gui.MenuReversibleOpaqueState
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateSnapshot
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateFailureReason
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateProvider
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateProviderDefinition
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateRetention
import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRuntimeRouteSnapshot
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuReversibleStateTokenStoreTest {
    @Test
    fun `registry rejects duplicate owner bound ids without replacing the existing provider`() {
        val registry = MenuReversibleStateProviderRegistryImpl()
        val first = definition("test", "state")
        registry.register(first)

        assertThrows(IllegalStateException::class.java) { registry.register(definition("test", "state")) }
        assertEquals(first, registry.definition("test:state"))
    }

    @Test
    fun `token is player bound and one shot`() {
        val store = store()
        val player = UUID.randomUUID()
        val token = issue(store, player)

        assertTakeFailure(store.take(token, UUID.randomUUID()), MenuReversibleStateFailureReason.TOKEN_WRONG_PLAYER)
        assertInstanceOf(MenuReversibleStateTokenStore.TakeResult.Taken::class.java, store.take(token, player))
        assertTakeFailure(store.take(token, player), MenuReversibleStateFailureReason.TOKEN_ALREADY_USED)
    }

    @Test
    fun `expiry clear and capacity eviction invalidate unconsumed tokens`() {
        val clock = MutableClock(Instant.parse("2026-08-01T00:00:00Z"))
        val expiring = store(ttl = Duration.ofSeconds(1), clock = clock)
        val player = UUID.randomUUID()
        val expired = issue(expiring, player)
        clock.advance(Duration.ofSeconds(2))
        assertTakeFailure(expiring.take(expired, player), MenuReversibleStateFailureReason.TOKEN_EXPIRED)

        val clearing = store()
        val cleared = issue(clearing, player)
        clearing.clear(player)
        assertTakeFailure(clearing.take(cleared, player), MenuReversibleStateFailureReason.TOKEN_EXPIRED)

        val bounded = store(capacity = 1)
        val evicted = issue(bounded, player)
        issue(bounded, player)
        assertTakeFailure(bounded.take(evicted, player), MenuReversibleStateFailureReason.TOKEN_EXPIRED)
    }

    @Test
    fun `concurrent restore acquisition has exactly one winner`() {
        val store = store()
        val player = UUID.randomUUID()
        val token = issue(store, player)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val results = (1..2).map {
                executor.submit<MenuReversibleStateTokenStore.TakeResult> {
                    start.await(5, TimeUnit.SECONDS)
                    store.take(token, player)
                }
            }
            start.countDown()
            val taken = results.count { it.get(5, TimeUnit.SECONDS) is MenuReversibleStateTokenStore.TakeResult.Taken }
            assertEquals(1, taken)
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `provider generation unregister invalidates tokens and prevents capture completion from issuing stale state`() {
        val registry = MenuReversibleStateProviderRegistryImpl()
        val store = store()
        registry.addInvalidationListener { registration -> store.clearProviderGeneration(registration.generation) }
        registry.register(definition("provider", "state"))
        val registration = requireNotNull(registry.registration("provider:state"))
        val player = UUID.randomUUID()
        val token = issue(
            store,
            player,
            registration.generation,
            providerCurrent = { registry.registration("provider:state")?.generation == registration.generation },
        )

        registry.unregister("provider", "state")
        assertTakeFailure(store.take(token, player), MenuReversibleStateFailureReason.TOKEN_EXPIRED)

        val captureStarted = CountDownLatch(1)
        val finishCapture = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            registry.register(definition("provider", "state"))
            val capturedRegistration = requireNotNull(registry.registration("provider:state"))
            val result = executor.submit<MenuReversibleStateTokenStore.IssueResult> {
                captureStarted.countDown()
                finishCapture.await(5, TimeUnit.SECONDS)
                store.issue(
                    player,
                    "audit-run",
                    MenuRoute("test", "route"),
                    interaction(),
                    stateSnapshot(),
                    capturedRegistration.generation,
                    providerCurrent = {
                        registry.registration("provider:state")?.generation == capturedRegistration.generation
                    },
                    runCurrent = { true },
                )
            }
            assertTrue(captureStarted.await(5, TimeUnit.SECONDS))
            registry.unregister("provider", "state")
            finishCapture.countDown()

            val invalidated = assertInstanceOf(MenuReversibleStateTokenStore.IssueResult.Invalidated::class.java, result.get(5, TimeUnit.SECONDS))
            assertEquals(MenuReversibleStateFailureReason.PROVIDER_GENERATION_MISMATCH, invalidated.reason)
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `trace binding is retained once and issuance requires the captured run to remain current`() {
        val store = store()
        val player = UUID.randomUUID()
        val token = issue(store, player)

        val binding = MenuReversibleStateTokenStore.TraceBinding(
            "audit-run",
            8L,
            MenuRuntimeRouteSnapshot("test", "route", emptyMap()),
            4L,
        )
        assertEquals(MenuReversibleStateTokenStore.BindResult.Bound, store.bind(token, player, binding))
        val entry = assertInstanceOf(MenuReversibleStateTokenStore.PeekResult.Found::class.java, store.peek(token, player)).entry
        assertEquals(binding, entry.binding)
        val duplicate = assertInstanceOf(MenuReversibleStateTokenStore.BindResult.Failed::class.java, store.bind(token, player, binding))
        assertEquals(MenuReversibleStateFailureReason.TOKEN_ALREADY_BOUND, duplicate.reason)

        val rejected = store.issue(
            player,
            "old-run",
            MenuRoute("test", "route"),
            interaction(),
            stateSnapshot(),
            UUID.randomUUID(),
            providerCurrent = { true },
            runCurrent = { false },
        )
        val invalidated = assertInstanceOf(MenuReversibleStateTokenStore.IssueResult.Invalidated::class.java, rejected)
        assertEquals(MenuReversibleStateFailureReason.RUN_MISMATCH, invalidated.reason)
    }

    @Test
    fun `token drops live attributes and restore interaction is immutable`() {
        val store = store()
        val player = UUID.randomUUID()
        val attributes = linkedMapOf<String, Any>("handle" to Any())
        val arguments = linkedMapOf("world" to "before")
        val interaction = MenuReversibleInteractionContext(
            4,
            ClickType.LEFT,
            "toggle",
            "test:capability",
            MenuReversibleContract("test:state", mapOf("operation" to "toggle")),
            3L,
            arguments,
            attributes,
            mapOf("page" to "1"),
        )
        val issued = store.issue(
            player,
            "audit-run",
            MenuRoute("test", "route"),
            interaction,
            stateSnapshot(),
            UUID.randomUUID(),
            providerCurrent = { true },
            runCurrent = { true },
        ) as MenuReversibleStateTokenStore.IssueResult.Issued

        arguments["world"] = "after"
        attributes["later"] = "not retained"
        val entry = (store.peek(issued.token, player) as MenuReversibleStateTokenStore.PeekResult.Found).entry
        val restore = entry.interaction.restoreContext()
        assertEquals(mapOf("world" to "before"), restore.arguments)
        assertEquals(mapOf("page" to "1"), restore.routePayload)
        assertTrue(restore.javaClass.methods.none { it.name == "getAttributes" })
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (restore.arguments as MutableMap<String, String>)["world"] = "forbidden"
        }
    }

    private fun store(
        ttl: Duration = Duration.ofMinutes(1),
        capacity: Int = 8,
        clock: Clock = Clock.systemUTC(),
    ) = MenuReversibleStateTokenStore(MenuReversibleStateRetention(ttl, capacity), clock)

    private fun issue(
        store: MenuReversibleStateTokenStore,
        player: UUID,
        generation: UUID = UUID.randomUUID(),
        providerCurrent: () -> Boolean = { true },
        runCurrent: () -> Boolean = { true },
    ): com.awabi2048.ccsystem.api.gui.MenuReversibleStateToken =
        (store.issue(
            player,
            "audit-run",
            MenuRoute("test", "route"),
            interaction(),
            stateSnapshot(),
            generation,
            providerCurrent,
            runCurrent,
        ) as MenuReversibleStateTokenStore.IssueResult.Issued).token

    private fun interaction() = MenuReversibleInteractionContext(
        4,
        ClickType.LEFT,
        "toggle",
        null,
        MenuReversibleContract("test:state", mapOf("key" to "value")),
        3L,
    )

    private fun assertTakeFailure(
        result: MenuReversibleStateTokenStore.TakeResult,
        reason: MenuReversibleStateFailureReason,
    ) {
        val failure = assertInstanceOf(MenuReversibleStateTokenStore.TakeResult.Failed::class.java, result)
        assertEquals(reason, failure.reason)
    }

    private fun definition(owner: String, id: String) = MenuReversibleStateProviderDefinition(owner, id, object : MenuReversibleStateProvider {
        override fun capture(context: com.awabi2048.ccsystem.api.gui.MenuReversibleStateCaptureContext) =
            com.awabi2048.ccsystem.api.gui.MenuReversibleProviderCaptureResult.Captured(State)

        override fun restore(context: com.awabi2048.ccsystem.api.gui.MenuReversibleStateRestoreContext) =
            com.awabi2048.ccsystem.api.gui.MenuReversibleProviderRestoreResult.Restored
    })

    private fun stateSnapshot() = MenuReversibleStateSnapshot.capture(State)

    private data object State : MenuReversibleOpaqueState {
        override fun snapshot(): Any = mapOf("state" to "captured")
    }

    private class MutableClock(private var current: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId): Clock = this
        override fun instant(): Instant = current
        fun advance(duration: Duration) {
            current = current.plus(duration)
        }
    }
}
