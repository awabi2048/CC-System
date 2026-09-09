package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose

/**
 * (sessionId, viewerId) ごとの描画済み状態です。
 *
 * 無変化の FULL→FULL では packet を送らない（0 packet 原則）ための正本であり、
 * LOD 遷移時は不足分・過剰分だけを spawn/destroy します。
 * virtual entity ID は viewer ごとに追跡し、HIDDEN 遷移時に残さず破棄します。
 */
internal data class GestureViewerRenderState(
    var visibilityLevel: GestureViewerLod = GestureViewerLod.HIDDEN,
    var screenRevision: Long = -1L,
    var poseRevision: Long = -1L,
    val visualRevisionById: MutableMap<String, Long> = mutableMapOf(),
    /** viewer へ送信済みの virtual entity ID 群です。 */
    val liveVirtualIds: MutableSet<Int> = mutableSetOf(),
    /** 論理キー（screenId/visualId 等）から viewer 側 virtual ID への対応です。 */
    val virtualIdByKey: MutableMap<String, Int> = mutableMapOf(),
    /** 論理キーごとの内容 fingerprint です。変化分だけ metadata 更新します。 */
    val contentFingerprintByKey: MutableMap<String, String> = mutableMapOf(),
    /** 画面キーごとの送信済み pose です。変化時は destroy+spawn で移動します。 */
    val poseByScreenKey: MutableMap<String, GestureGuiScreenPose> = mutableMapOf(),
    /**
     * hover 置換でこの viewer へ隠している通常 visual の ID 群です。
     * 置換中は対象を描画対象から外し、hover テキストだけを前面に送ります。
     */
    val hiddenVisualIds: MutableSet<String> = mutableSetOf(),
    /** Block 本体だけを隠している visual の ID 群です。 */
    val hiddenVisualBodyIds: MutableSet<String> = mutableSetOf(),
)
