package com.awabi2048.ccsystem.core.gesturegui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * フォント幾何モデルの仮値が内部で自己矛盾しないこと、
 * および幅推定の計算規則を固定するテストです。
 * 値自体の正しさは実機計測で検証するため、ここでは構造だけを確認します。
 */
class GestureGuiFontMetricsTest {
    private val epsilon = 1.0e-9

    @Test
    fun `行送りとpx換算係数は行高係数と整合する`() {
        val m = GestureGuiFontMetrics
        assertTrue(abs(m.PX_TO_BLOCK * m.VANILLA_LINE_ADVANCE_PX - m.LINE_HEIGHT_FACTOR) < epsilon)
    }

    @Test
    fun `ASCIIテキストは文字数x既定送りで見積もる`() {
        val m = GestureGuiFontMetrics
        // "abc" は 3文字とも既定送りです。
        assertEquals(3 * m.DEFAULT_ADVANCE_PX, m.estimateWidthPx("abc"), epsilon)
        // 空白は専用の送りを使います。
        assertEquals(m.DEFAULT_ADVANCE_PX + m.SPACE_ADVANCE_PX, m.estimateWidthPx("a "), epsilon)
    }

    @Test
    fun `日本語は全角送りで見積もる`() {
        val m = GestureGuiFontMetrics
        assertEquals(3 * m.WIDE_ADVANCE_PX, m.estimateWidthPx("あいう"), epsilon)
        assertTrue(m.isWide("あ".codePointAt(0)))
        assertTrue(m.isWide("＋".codePointAt(0)))
    }

    @Test
    fun `太字は1文字あたり補正幅を加算する`() {
        val m = GestureGuiFontMetrics
        val plain = m.estimateWidthPx("ab")
        val bold = m.estimateWidthPx("ab", bold = true)
        assertEquals(plain + 2 * m.BOLD_EXTRA_PX, bold, epsilon)
    }

    @Test
    fun `BMP外グリフはサロゲートペアを1文字として数える`() {
        val m = GestureGuiFontMetrics
        // U+1F5D1（🗑）はサロゲートペアですが、1グリフとして全角送りを適用します。
        val emoji = "🗑"
        assertEquals(m.WIDE_ADVANCE_PX, m.estimateWidthPx(emoji), epsilon)
        assertTrue(m.isWide(emoji.codePointAt(0)))
    }

    @Test
    fun `ブロック換算は係数とサイズ補正に従う`() {
        val m = GestureGuiFontMetrics
        val size = 0.005
        val expected = size * m.PX_TO_BLOCK * m.DEFAULT_ADVANCE_PX * 2
        // サイズ補正が空の既定では線形モデルになります。
        assertTrue(m.SIZE_CORRECTION.isEmpty())
        assertEquals(expected, m.estimateWidthBlocks("ab", size), epsilon)
    }
}
