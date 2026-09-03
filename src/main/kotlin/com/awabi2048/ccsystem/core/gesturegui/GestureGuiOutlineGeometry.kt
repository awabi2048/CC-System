package com.awabi2048.ccsystem.core.gesturegui

import kotlin.math.min

/**
 * 矩形の内側枠を構成する四辺です。
 *
 * 位置はGesture GUIのローカル画面座標で保持します。ワールド座標へ変換する処理は
 * renderer側で対象Visualと同じposeを使うため、画面のyaw/pitchや背面側への回転で
 * 上下左右が反転することはありません。
 */
internal data class GestureGuiOutlineSegment(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

internal object GestureGuiOutlineGeometry {
    /**
     * 対象矩形の内側に収まる四辺を返します。
     *
     * 上下辺は対象幅いっぱい、左右辺は上下辺の内側だけへ置くことで、角の重複と
     * 中央の欠損を避けます。各寸法は正値のままなので、Display transformへ負の縮尺を
     * 渡す必要がなく、Minecraftクライアント側の表裏反転も発生しません。
     */
    fun segments(width: Double, height: Double, thicknessRatio: Double): List<GestureGuiOutlineSegment> {
        require(width.isFinite() && height.isFinite() && width > 0.0 && height > 0.0) {
            "gesture GUI outline target size must be positive and finite"
        }
        require(thicknessRatio.isFinite() && thicknessRatio > 0.0 && thicknessRatio < 0.5) {
            "gesture GUI outline thicknessRatio must be finite and between 0 and 0.5"
        }

        val thickness = min(width, height) * thicknessRatio
        val innerHeight = height - thickness * 2.0
        return listOf(
            // 上下辺を先に矩形全幅で置き、角を確実に連続させます。
            GestureGuiOutlineSegment(0.0, (height - thickness) / 2.0, width, thickness),
            GestureGuiOutlineSegment(0.0, -(height - thickness) / 2.0, width, thickness),
            GestureGuiOutlineSegment(-(width - thickness) / 2.0, 0.0, thickness, innerHeight),
            GestureGuiOutlineSegment((width - thickness) / 2.0, 0.0, thickness, innerHeight),
        )
    }
}
