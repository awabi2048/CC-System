package com.awabi2048.ccsystem.api.input

import java.util.UUID

/**
 * CC-Systemが仲裁するプレイヤー入力です。
 *
 * Bukkitイベント種別ではなく、利用者が行った操作の意味で分けます。これにより、
 * Inventory、Gesture GUI、外部プラグインがイベント優先度へ依存せず競合を回避できます。
 */
enum class PlayerInteractionChannel {
    PRIMARY,
    SECONDARY,
    SHIFT_PRIMARY,
    SHIFT_SECONDARY,
    SWAP_HAND,
    SHIFT_JUMP,
}

/** 取得した入力所有権です。closeは何度呼んでも安全です。 */
interface PlayerInteractionClaim : AutoCloseable {
    val playerId: UUID
    val channel: PlayerInteractionChannel
    val owner: String
    val active: Boolean

    override fun close()
}

/** プレイヤー入力をチャネルごとに排他的に扱うための共有サービスです。 */
interface PlayerInteractionClaimService {
    /** 取得できなかった場合はnullを返します。同一ownerによる再取得も別handleとして数えます。 */
    fun claim(playerId: UUID, channel: PlayerInteractionChannel, owner: String): PlayerInteractionClaim?

    fun ownerOf(playerId: UUID, channel: PlayerInteractionChannel): String?

    fun isClaimed(playerId: UUID, channel: PlayerInteractionChannel): Boolean = ownerOf(playerId, channel) != null

    fun isClaimedBy(playerId: UUID, channel: PlayerInteractionChannel, owner: String): Boolean =
        ownerOf(playerId, channel) == owner

    fun release(playerId: UUID, channel: PlayerInteractionChannel, owner: String): Boolean

    fun releaseAll(playerId: UUID)

    fun releaseAll(playerId: UUID, owner: String)
}
