package com.awabi2048.ccsystem.core.input

import com.awabi2048.ccsystem.api.input.PlayerInteractionChannel
import com.awabi2048.ccsystem.api.input.PlayerInteractionClaim
import com.awabi2048.ccsystem.api.input.PlayerInteractionClaimService
import java.util.UUID

class PlayerInteractionClaimServiceImpl : PlayerInteractionClaimService {
    private data class Key(val playerId: UUID, val channel: PlayerInteractionChannel)
    private data class Ownership(val owner: String, var references: Int)

    /* Bukkitの入力処理はメインスレッドが原則ですが、外部APIからの照会にも一貫した結果を返します。 */
    private val claims = mutableMapOf<Key, Ownership>()

    @Synchronized
    override fun claim(
        playerId: UUID,
        channel: PlayerInteractionChannel,
        owner: String,
    ): PlayerInteractionClaim? {
        require(owner.isNotBlank()) { "interaction claim owner must not be blank" }
        val key = Key(playerId, channel)
        val current = claims[key]
        if (current != null && current.owner != owner) return null
        if (current == null) claims[key] = Ownership(owner, 1) else current.references++
        return ClaimHandle(playerId, channel, owner)
    }

    @Synchronized
    override fun ownerOf(playerId: UUID, channel: PlayerInteractionChannel): String? =
        claims[Key(playerId, channel)]?.owner

    @Synchronized
    override fun release(playerId: UUID, channel: PlayerInteractionChannel, owner: String): Boolean =
        releaseReference(Key(playerId, channel), owner, releaseEveryReference = true)

    @Synchronized
    override fun releaseAll(playerId: UUID) {
        claims.keys.removeIf { it.playerId == playerId }
    }

    @Synchronized
    override fun releaseAll(playerId: UUID, owner: String) {
        claims.entries.removeIf { (key, value) -> key.playerId == playerId && value.owner == owner }
    }

    @Synchronized
    private fun closeHandle(key: Key, owner: String): Boolean =
        releaseReference(key, owner, releaseEveryReference = false)

    private fun releaseReference(key: Key, owner: String, releaseEveryReference: Boolean): Boolean {
        val current = claims[key]?.takeIf { it.owner == owner } ?: return false
        if (releaseEveryReference || --current.references == 0) claims.remove(key)
        return true
    }

    private inner class ClaimHandle(
        override val playerId: UUID,
        override val channel: PlayerInteractionChannel,
        override val owner: String,
    ) : PlayerInteractionClaim {
        @Volatile
        private var closed = false

        override val active: Boolean
            get() = !closed && isClaimedBy(playerId, channel, owner)

        override fun close() {
            synchronized(this) {
                if (closed) return
                closed = true
            }
            closeHandle(Key(playerId, channel), owner)
        }
    }
}
