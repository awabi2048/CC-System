package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuReversibleInteractionContext
import com.awabi2048.ccsystem.api.gui.MenuReversibleProviderState
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateFailureReason
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateRetention
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateToken
import com.awabi2048.ccsystem.api.gui.MenuRoute
import java.time.Clock
import java.time.Instant
import java.util.ArrayDeque
import java.util.UUID

/**
 * 一回限り token の所有権を短時間だけ保持します。provider callback はこの store の lock 外で実行します。
 */
internal class MenuReversibleStateTokenStore(
    private val retention: MenuReversibleStateRetention = MenuReversibleStateRetention(),
    private val clock: Clock = Clock.systemUTC(),
) {
    private val lock = Any()
    private val entries = linkedMapOf<UUID, Entry>()
    private val consumed = ArrayDeque<UUID>()
    private val expired = ArrayDeque<UUID>()
    private val consumedSet = hashSetOf<UUID>()
    private val expiredSet = hashSetOf<UUID>()

    fun issue(
        playerId: UUID,
        runId: String,
        route: MenuRoute,
        interaction: MenuReversibleInteractionContext,
        state: MenuReversibleProviderState,
    ): MenuReversibleStateToken = synchronized(lock) {
        evictExpiredLocked(clock.instant())
        val id = UUID.randomUUID()
        while (entries.size >= retention.capacity) {
            val evictedId = entries.entries.first().key
            entries.remove(evictedId)
            rememberExpiredLocked(evictedId)
        }
        entries[id] = Entry(
            playerId = playerId,
            runId = runId,
            route = route,
            interaction = interaction,
            state = state,
            expiresAt = clock.instant().plus(retention.ttl),
        )
        MenuReversibleStateToken(id)
    }

    /**
     * Restore callback の実行権を一度だけ引き渡します。wrong player では token を消費しません。
     */
    fun take(token: MenuReversibleStateToken, playerId: UUID): TakeResult = synchronized(lock) {
        evictExpiredLocked(clock.instant())
        val entry = entries[token.value]
        if (entry == null) {
            return@synchronized TakeResult.Failed(
                when {
                    token.value in expiredSet -> MenuReversibleStateFailureReason.TOKEN_EXPIRED
                    token.value in consumedSet -> MenuReversibleStateFailureReason.TOKEN_ALREADY_USED
                    else -> MenuReversibleStateFailureReason.TOKEN_UNKNOWN
                },
            )
        }
        if (entry.playerId != playerId) {
            return@synchronized TakeResult.Failed(MenuReversibleStateFailureReason.TOKEN_WRONG_PLAYER)
        }
        entries.remove(token.value)
        rememberConsumedLocked(token.value)
        TakeResult.Taken(entry)
    }

    fun clear(playerId: UUID) = synchronized(lock) {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.playerId != playerId) continue
            iterator.remove()
            rememberExpiredLocked(entry.key)
        }
    }

    fun boundPlayerId(token: MenuReversibleStateToken): UUID? = synchronized(lock) {
        evictExpiredLocked(clock.instant())
        entries[token.value]?.playerId
    }

    fun missingReason(token: MenuReversibleStateToken): MenuReversibleStateFailureReason? = synchronized(lock) {
        evictExpiredLocked(clock.instant())
        if (token.value in entries) return@synchronized null
        when {
            token.value in expiredSet -> MenuReversibleStateFailureReason.TOKEN_EXPIRED
            token.value in consumedSet -> MenuReversibleStateFailureReason.TOKEN_ALREADY_USED
            else -> MenuReversibleStateFailureReason.TOKEN_UNKNOWN
        }
    }

    fun clearOwner(owner: String) = synchronized(lock) {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.route.owner != owner) continue
            iterator.remove()
            rememberExpiredLocked(entry.key)
        }
    }

    fun evictExpired() = synchronized(lock) { evictExpiredLocked(clock.instant()) }

    private fun evictExpiredLocked(now: Instant) {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.expiresAt > now) continue
            iterator.remove()
            rememberExpiredLocked(entry.key)
        }
    }

    private fun rememberConsumedLocked(id: UUID) {
        remember(id, consumed, consumedSet)
    }

    private fun rememberExpiredLocked(id: UUID) {
        remember(id, expired, expiredSet)
    }

    private fun remember(id: UUID, order: ArrayDeque<UUID>, values: MutableSet<UUID>) {
        if (!values.add(id)) return
        order.addLast(id)
        while (order.size > retention.capacity) {
            values.remove(order.removeFirst())
        }
    }

    sealed interface TakeResult {
        data class Taken(val entry: Entry) : TakeResult

        data class Failed(val reason: MenuReversibleStateFailureReason) : TakeResult
    }

    data class Entry(
        val playerId: UUID,
        val runId: String,
        val route: MenuRoute,
        val interaction: MenuReversibleInteractionContext,
        val state: MenuReversibleProviderState,
        val expiresAt: Instant,
    )
}
