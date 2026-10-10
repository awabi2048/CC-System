package com.awabi2048.ccsystem.features.misc.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * 計測画面の幾何不変条件です。
 *
 * 画面の見た目は実機で人間が検証するため、ここでは「目盛りが正しく均一・対称に引ける」
 * 「参照バーが内容域からはみ出さない」といった純粋な配置計算のみを固定します。
 */
class GestureGuiMeasureGeometryTest {
    private val epsilon = 1.0e-9

    @Test
    fun `細目盛りがレール両端にちょうど収まる`() {
        val half = GestureGuiMeasureController.RULER_HALF
        val tick = GestureGuiMeasureController.MINOR_TICK
        val count = GestureGuiMeasureController.TICK_COUNT

        // 半幅が細目盛り間隔の整数倍であり、本数が -half〜+half を過不足なく覆うこと。
        assertTrue(abs(half / tick - (half / tick).toInt()) < epsilon, "半幅は細目盛りの整数倍である必要があります")
        assertTrue(abs(count * tick - half * 2.0) < epsilon, "目盛り本数がレール全幅を覆いません")
    }

    @Test
    fun `太目盛りと数値ラベルが細目盛りに同期する`() {
        val tick = GestureGuiMeasureController.MINOR_TICK
        assertTrue(abs(GestureGuiMeasureController.MAJOR_EVERY * tick - 0.1) < epsilon)
        assertTrue(abs(GestureGuiMeasureController.LABEL_EVERY * tick - 0.2) < epsilon)
        // 数値ラベルは太目盛り位置にのみ置く（読み取り対応を崩さないため）。
        assertTrue(GestureGuiMeasureController.LABEL_EVERY % GestureGuiMeasureController.MAJOR_EVERY == 0)
    }

    @Test
    fun `参照バーと目盛りがパネル内容域に収まる`() {
        val contentWidth = GestureGuiPanel().width - GestureGuiPanel().frameWidth * 2.0
        val half = contentWidth / 2.0
        val left = GestureGuiMeasureController.RULER_LEFT

        assertTrue(GestureGuiMeasureController.RULER_HALF < half, "横目盛りが内容域を超えます")
        GestureGuiMeasureController.REFERENCE_WIDTHS.forEach { width ->
            val rightEdge = left + width + 0.05 + 0.03 // バー右端＋ラベル領域の余裕分
            assertTrue(left > -half && rightEdge < half, "参照バー $width が内容域からはみ出します")
        }
    }

    @Test
    fun `測定行が目盛りと参照バーの間に収まる`() {
        val pitch = GestureGuiMeasureController.SAMPLE_PITCH
        val first = 0.29
        val last = first - 9 * pitch
        // 10行分が、横目盛り下端（0.32付近）と参照バー上端（-0.135）の間に収まること。
        assertTrue(last > -0.135 + pitch, "測定行が参照バー領域に侵入します")
        assertTrue(first < 0.355 - GestureGuiMeasureController.MAJOR_LENGTH, "測定行が横目盛りに侵入します")
    }
}
