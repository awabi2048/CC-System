package com.awabi2048.ccsystem.core.gesturegui

import kotlin.math.min

/**
 * 矩形の縦内側・横外側枠を構成する四辺です。
 *
 * 位置はGesture GUIのローカル画面座標で保持します。ワールド座標へ変換する処理は
 * renderer側で対象Visualと同じposeを使い、上下辺を縦方向の内側、左右辺を横方向の
 * 外側へずらします。そのため、画面のyaw/pitchや背面側への回転で上下左右が反転する
 * ことはありません。
 */
internal data class GestureGuiOutlineSegment(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

internal object GestureGuiOutlineGeometry {
    /**
     * 上下辺を縦方向の内側、左右辺を横方向の外側へ太さ分だけずらした四辺を返します。
     *
     * 上下辺は対象幅より両端を太さ分広げ、左右辺は対象高さいっぱいにすることで、
     * 横方向の外側へ出した辺と角を連続させます。各寸法は正値のままなので、Display
     * transformへ負の縮尺を渡す必要がなく、Minecraftクライアント側の表裏反転も発生
     * しません。
     */
    fun segments(width: Double, height: Double, thicknessRatio: Double): List<GestureGuiOutlineSegment> {
        require(width.isFinite() && height.isFinite() && width > 0.0 && height > 0.0) {
            "gesture GUI outline target size must be positive and finite"
        }
        require(thicknessRatio.isFinite() && thicknessRatio > 0.0 && thicknessRatio < 0.5) {
            "gesture GUI outline thicknessRatio must be finite and between 0 and 0.5"
        }

        val thickness = min(width, height) * thicknessRatio
        val outerWidth = width + thickness * 2.0
        return listOf(
            // 上下辺は縦方向だけを内側へ戻し、横方向は外側幅を保ちます。
            GestureGuiOutlineSegment(0.0, (height - thickness) / 2.0, outerWidth, thickness),
            GestureGuiOutlineSegment(0.0, -(height - thickness) / 2.0, outerWidth, thickness),
            // 左右辺は横方向だけを外側へ出し、縦方向は対象高さいっぱいにします。
            GestureGuiOutlineSegment(-(width + thickness) / 2.0, 0.0, thickness, height),
            GestureGuiOutlineSegment((width + thickness) / 2.0, 0.0, thickness, height),
        )
    }
}
