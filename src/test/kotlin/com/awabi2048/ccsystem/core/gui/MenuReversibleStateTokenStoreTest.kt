package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuReversibleContract
import com.awabi2048.ccsystem.api.gui.MenuReversibleInteractionContext
import com.awabi2048.ccsystem.api.gui.MenuReversibleProviderState
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateFailureReason
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateProvider
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateProviderDefinition
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateRetention
import com.awabi2048.ccsystem.api.gui.MenuRoute
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

    private fun store(
        ttl: Duration = Duration.ofMinutes(1),
        capacity: Int = 8,
        clock: Clock = Clock.systemUTC(),
    ) = MenuReversibleStateTokenStore(MenuReversibleStateRetention(ttl, capacity), clock)

    private fun issue(store: MenuReversibleStateTokenStore, player: UUID) = store.issue(
        player,
        "audit-run",
        MenuRoute("test", "route"),
        MenuReversibleInteractionContext(
            4,
            ClickType.LEFT,
            "toggle",
            null,
            MenuReversibleContract("test:state", mapOf("key" to "value")),
            3L,
        ),
        State,
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

    private data object State : MenuReversibleProviderState

    private class MutableClock(private var current: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId): Clock = this
        override fun instant(): Instant = current
        fun advance(duration: Duration) {
            current = current.plus(duration)
        }
    }
}
