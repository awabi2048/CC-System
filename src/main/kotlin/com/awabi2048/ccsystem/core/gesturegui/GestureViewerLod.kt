package com.awabi2048.ccsystem.core.gesturegui

/**
 * (session, viewer) ごとの表示 LOD です。
 *
 * FULL は近距離かつ操作可能な viewer への全 visual 配布、
 * BACKGROUND_ONLY は上下背景パネルのみ（内容・hover・catcher・hit-test なし）、
 * HIDDEN は一切送信しません。
 */
internal enum class GestureViewerLod {
    FULL,
    BACKGROUND_ONLY,
    HIDDEN,
}

/**
 * LOD 境界（15 ブロック・距離二乗比較）と遷移ヒステリシスです。
 *
 * 境界直上での FULL/BACKGROUND_ONLY 往復を防ぐため、外れ側に余裕を持たせます。
 * 閾値自体は固定し、幅のみここで一元管理します。
 */
internal object GestureViewerLodPolicy {
    /** FULL とみなす距離の上限（ブロック）です。 */
    const val FULL_MAX_DISTANCE: Double = 15.0

    /** BACKGROUND_ONLY から外れたとみなす余裕（ブロック）です。 */
    const val HYSTERESIS: Double = 1.0

    fun resolve(current: GestureViewerLod, distanceSquared: Double, operable: Boolean, visible: Boolean): GestureViewerLod {
        if (!visible) return GestureViewerLod.HIDDEN
        if (!operable) {
            // 操作権限なし viewer は背景のみ受信します。
            return GestureViewerLod.BACKGROUND_ONLY
        }
        val enterSquared = FULL_MAX_DISTANCE * FULL_MAX_DISTANCE
        val exitSquared = (FULL_MAX_DISTANCE + HYSTERESIS) * (FULL_MAX_DISTANCE + HYSTERESIS)
        return when (current) {
            GestureViewerLod.FULL ->
                if (distanceSquared > exitSquared) GestureViewerLod.BACKGROUND_ONLY else GestureViewerLod.FULL
            GestureViewerLod.BACKGROUND_ONLY, GestureViewerLod.HIDDEN ->
                if (distanceSquared <= enterSquared) GestureViewerLod.FULL else GestureViewerLod.BACKGROUND_ONLY
        }
    }
}
