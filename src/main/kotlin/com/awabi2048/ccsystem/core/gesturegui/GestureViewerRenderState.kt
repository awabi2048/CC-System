package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import org.bukkit.entity.EntityType

/**
 * (sessionId, viewerId) ごとの描画済み状態です。
 *
 * 無変化では packet を送らない（0 packet 原則）ための正本であり、
 * LOD 遷移時は不足分・過剰分だけを spawn/destroy します。
 * virtual entity ID は viewer ごとに追跡し、HIDDEN 遷移時に残さず破棄します。
 *
 * LOD は画面キーごとに保持します。セッション全体で一括にすると、
 * 公開範囲の異なる子画面まで同じ扱いになり、非公開画面が送信されます。
 */
internal data class GestureViewerRenderState(
    var screenRevision: Long = -1L,
    var poseRevision: Long = -1L,
    val visualRevisionById: MutableMap<String, Long> = mutableMapOf(),
    /** viewer へ送信済みの virtual entity ID 群です。 */
    val liveVirtualIds: MutableSet<Int> = mutableSetOf(),
    /** 論理キー（screenId/visualId 等）から viewer 側 virtual ID への対応です。 */
    val virtualIdByKey: MutableMap<String, Int> = mutableMapOf(),
    /** 論理キーごとの内容 fingerprint です。変化分だけ metadata 更新します。 */
    val contentFingerprintByKey: MutableMap<String, String> = mutableMapOf(),
    /** 論理キーごとの送信済み座標です。spawnアンカーとして不変に保ちます。 */
    val pointByKey: MutableMap<String, GestureGuiVirtualScreens.PlacedPoint> = mutableMapOf(),
    /** 論理キーごとの送信済み種別です。型変更は destroy+spawn で作り直します。 */
    val typeByKey: MutableMap<String, EntityType> = mutableMapOf(),
    /** 画面キーごとの送信済み pose です。変化時は移動キーのみ再送します。 */
    val poseByScreenKey: MutableMap<String, GestureGuiScreenPose> = mutableMapOf(),
    /** 画面キーごとの LOD です。画面単位で FULL / BACKGROUND_ONLY / HIDDEN を管理します。 */
    val lodByScreenKey: MutableMap<String, GestureViewerLod> = mutableMapOf(),
    /** 平坦化中の画面キー群です。開幕 pop 演出で背景面へ重ねて送ります。 */
    val flatScreens: MutableSet<String> = mutableSetOf(),
    /**
     * hover 置換でこの viewer へ隠している通常 visual の ID 群です。
     * 置換中は対象を描画対象から外し、hover テキストだけを前面に送ります。
     */
    val hiddenVisualIds: MutableSet<String> = mutableSetOf(),
    /** Block 本体だけを隠している visual の ID 群です。 */
    val hiddenVisualBodyIds: MutableSet<String> = mutableSetOf(),
    /** hover 置換の世代です。変化時は内容 revision とは別に再同期します。 */
    var hoverEpoch: Long = 0L,
    /** viewer へ反映済みの hover 世代です。早期終了の判定に使います。 */
    var syncedHoverEpoch: Long = -1L,
)
