package com.awabi2048.ccsystem.core.gesturegui

/**
 * (sessionId, viewerId) ごとの描画済み状態です。
 *
 * 無変化の FULL→FULL では packet を送らない（0 packet 原則）ための正本であり、
 * LOD 遷移時は不足分・過剰分だけを spawn/destroy します。
 */
internal data class GestureViewerRenderState(
    var visibilityLevel: GestureViewerLod = GestureViewerLod.HIDDEN,
    var screenRevision: Long = -1L,
    var poseRevision: Long = -1L,
    val visualRevisionById: MutableMap<String, Long> = mutableMapOf(),
    /** viewer へ送信済みの virtual entity ID 群です。 */
    val liveVirtualIds: MutableSet<Int> = mutableSetOf(),
)
