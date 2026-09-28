package com.awabi2048.ccsystem.core.gesturegui

/**
 * (session, viewer) ごとの表示 LOD です。
 *
 * FULL は近距離の viewer への全 visual 配布、
 * BACKGROUND_ONLY は上下背景パネルのみ（内容・hover・catcher・hit-test なし）、
 * HIDDEN は一切送信しません。
 */
internal enum class GestureViewerLod {
    FULL,
    BACKGROUND_ONLY,
    HIDDEN,
}

/**
 * LOD 境界（10 ブロック・距離二乗比較）と遷移ヒステリシスです。
 *
 * 境界直上での FULL/BACKGROUND_ONLY 往復を防ぐため、外れ側に余裕を持たせます。
 * 閾値自体は固定し、幅のみここで一元管理します。
 *
 * 「10 ブロック以内にいるプレイヤーには内容を視認できる」仕様のため、
 * FULL の判定は距離だけで行い、表示・操作権限は含めません。操作可否は
 * hit-test と canOperate 側の到達検査へ委ね、閲覧は誰にも開きます。
 * canView は範囲外で背景を残すか（BACKGROUND_ONLY）、全て隠すか
 * （HIDDEN）だけに使います。
 */
internal object GestureViewerLodPolicy {
    /** 内容を視認できる距離の上限（ブロック）です。 */
    const val FULL_MAX_DISTANCE: Double = 10.0

    /** FULL から外れたとみなす余裕（ブロック）です。 */
    const val HYSTERESIS: Double = 1.0

    fun resolve(current: GestureViewerLod, distanceSquared: Double, visible: Boolean): GestureViewerLod {
        val enterSquared = FULL_MAX_DISTANCE * FULL_MAX_DISTANCE
        val exitSquared = (FULL_MAX_DISTANCE + HYSTERESIS) * (FULL_MAX_DISTANCE + HYSTERESIS)
        if (distanceSquared <= enterSquared) return GestureViewerLod.FULL
        if (current == GestureViewerLod.FULL && distanceSquared <= exitSquared) {
            return GestureViewerLod.FULL
        }
        return if (visible) GestureViewerLod.BACKGROUND_ONLY else GestureViewerLod.HIDDEN
    }
}
