package com.awabi2048.ccsystem.core.input

import com.awabi2048.ccsystem.api.input.PlayerInteractionClaimService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlayerInteractionClaimServiceImpl : PlayerInteractionClaimService {
    private val claims = ConcurrentHashMap<UUID, String>()

    override fun tryClaim(playerId: UUID, owner: String): Boolean {
        require(owner.isNotBlank()) { "interaction claim owner must not be blank" }
        return claims.putIfAbsent(playerId, owner).let { it == null || it == owner }
    }

    override fun ownerOf(playerId: UUID): String? = claims[playerId]

    override fun isClaimedBy(playerId: UUID, owner: String): Boolean = claims[playerId] == owner

    override fun release(playerId: UUID, owner: String): Boolean = claims.remove(playerId, owner)

    override fun releaseAll(playerId: UUID) {
        claims.remove(playerId)
    }
}
