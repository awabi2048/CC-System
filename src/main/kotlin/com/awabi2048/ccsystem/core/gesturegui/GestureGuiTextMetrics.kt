package com.awabi2048.ccsystem.core.gesturegui

/**
 * 画面上の論理文字サイズをMinecraftのTextDisplay変形倍率へ変換します。
 * 公開APIへクライアント実装固有の極端な倍率値を漏らさないための境界です。
 */
internal object GestureGuiTextMetrics {
    private const val DISPLAY_SCALE_PER_LOGICAL_UNIT = 40.0

    fun toDisplayScale(logicalSize: Double): Float =
        (logicalSize * DISPLAY_SCALE_PER_LOGICAL_UNIT).toFloat()
}
