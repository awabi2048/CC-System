package com.awabi2048.ccsystem.core.gesturegui

/**
 * TextDisplayの法線方向レイヤーを解決します。
 *
 * Gesture GUIでは、BlockDisplayの標準コンテンツ面をlayer 4として使用し、
 * それより前面のlayerへItemDisplayやTextDisplayを重ねます。TextDisplayだけを
 * 画面中心基準で単純に半分へ縮めると、layer 6の文字がlayer 4のボタンの背面へ
 * 回り込むため、標準コンテンツ面からの距離を半分にする補間として定義します。
 * これにより、layer 4のボタン上のlayer 6の文字はlayer 5へ、既定layer 20の
 * 文字はlayer 12へ移動し、既存の面より前面という関係を保ちます。
 */
internal object GestureGuiTextDepth {
    /** 標準的なボタン面を表すBlockDisplayのlayerです。 */
    const val CONTENT_SURFACE_LAYER: Double = 4.0

    /** コンテンツ面からTextDisplayまでの距離を残す割合です。 */
    const val SURFACE_DISTANCE_RATIO: Double = 0.5

    fun effectiveLayer(layer: Int): Double = if (layer <= CONTENT_SURFACE_LAYER) {
        layer.toDouble()
    } else {
        CONTENT_SURFACE_LAYER + (layer - CONTENT_SURFACE_LAYER) * SURFACE_DISTANCE_RATIO
    }
}
