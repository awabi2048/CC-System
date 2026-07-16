package com.awabi2048.ccsystem.api.input

import java.util.UUID

/** プレイヤーを対象とする一時入力を、機能間で排他的に扱うための共有サービス。 */
interface PlayerInteractionClaimService {
    fun tryClaim(playerId: UUID, owner: String): Boolean
    fun ownerOf(playerId: UUID): String?
    fun isClaimedBy(playerId: UUID, owner: String): Boolean
    fun release(playerId: UUID, owner: String): Boolean
    fun releaseAll(playerId: UUID)
}
