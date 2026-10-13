package com.awabi2048.ccsystem.core.gesturegui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * フォント幾何モデルの整合性と実測確定値を固定するテストです。
 * 値は 2026-10-13 の実機計測（計測画面の画素解析）に基づきます。
 */
class GestureGuiFontMetricsTest {
    private val epsilon = 1.0e-9

    @Test
    fun `行送りとpx換算係数は行高係数と整合する`() {
        val m = GestureGuiFontMetrics
        assertTrue(abs(m.PX_TO_BLOCK * m.LINE_ADVANCE_PX - m.LINE_HEIGHT_FACTOR) < epsilon)
    }

    @Test
    fun `実測確定値が固定される`() {
        val m = GestureGuiFontMetrics
        // 計測結果の再確認時だけ更新します。
        assertEquals(10.0, m.LINE_HEIGHT_FACTOR, epsilon)
        assertEquals(10.0, m.LINE_ADVANCE_PX, epsilon)
        assertEquals(1.0, m.PX_TO_BLOCK, epsilon)
        assertEquals(9.4, m.WIDE_ADVANCE_PX, epsilon)
        assertTrue(m.SIZE_CORRECTION.isEmpty())
    }

    @Test
    fun `パングラムは実測値に近い幅を返す`() {
        val m = GestureGuiFontMetrics
        // "The quick brown fox jumps over the lazy dog 0123456789" の実測は
        // size=0.005 で 1.49 ブロック（≈298px）でした。モデルは ±2% 以内を維持します。
        val pangram = "The quick brown fox jumps over the lazy dog 0123456789"
        val px = m.estimateWidthPx(pangram)
        assertTrue(abs(px - 298.0) / 298.0 < 0.02, "pangram px=$px")
        assertEquals(1.49, m.estimateWidthBlocks(pangram, 0.005), 0.03)
    }

    @Test
    fun `ASCIIテキストは文字送り表で見積もる`() {
        val m = GestureGuiFontMetrics
        // "abc" は 3文字とも既定送りです。
        assertEquals(3 * m.DEFAULT_ADVANCE_PX, m.estimateWidthPx("abc"), epsilon)
        // 空白は専用の送りを使います。
        assertEquals(m.DEFAULT_ADVANCE_PX + m.SPACE_ADVANCE_PX, m.estimateWidthPx("a "), epsilon)
        // 狭幅グリフ（i=2, l=3, t=4, f=5, k=5）は個別送りを使います。
        assertEquals(2.0 + 3.0 + 4.0 + 5.0 + 5.0, m.estimateWidthPx("iltfk"), epsilon)
    }

    @Test
    fun `日本語は全角送りで見積もる`() {
        val m = GestureGuiFontMetrics
        assertEquals(3 * m.WIDE_ADVANCE_PX, m.estimateWidthPx("あいう"), epsilon)
        assertTrue(m.isWide("あ".codePointAt(0)))
        // 日本語15文字の実測は字あたり 9.38px でした。
        assertEquals(9.4, m.advancePx("あ".codePointAt(0)), epsilon)
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
        // U+1F5D1（🗑）はサロゲートペアですが、1グリフとして個別実測値を適用します。
        assertEquals(7.0, m.estimateWidthPx("🗑"), epsilon)
    }

    @Test
    fun `特殊グリフは実測の個別送りを使う`() {
        val m = GestureGuiFontMetrics
        // 実測で全角より狭いことが判明したグリフです。
        assertEquals(7.5, m.advancePx(0x25C0), epsilon) // ◀
        assertEquals(7.5, m.advancePx(0x25B6), epsilon) // ▶
        assertEquals(6.0, m.advancePx(0xFF0B), epsilon) // ＋
        assertEquals(6.0, m.advancePx(0xFF0D), epsilon) // －
    }

    @Test
    fun `ブロック換算は係数とサイズ補正に従う`() {
        val m = GestureGuiFontMetrics
        val size = 0.005
        val expected = size * m.PX_TO_BLOCK * m.DEFAULT_ADVANCE_PX * 2
        assertEquals(expected, m.estimateWidthBlocks("ab", size), epsilon)
    }
}
