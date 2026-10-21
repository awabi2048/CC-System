package com.awabi2048.ccsystem.core.gesturegui

/**
 * Gesture GUI のフォント幾何モデルです。
 *
 * 各値は `/cc gesture-gui measure` 計測画面の実機読み取り（2026-10-13、画素解析）で
 * 確定した計測値です。パングラム4サイズで幅が size に比例することを確認済みのため
 * 線形モデルとし、[SIZE_CORRECTION] は空です。
 *
 * 計測条件: TextDisplay スケール = size × 40。計測精度は概ね ±2 画面px
 * （ブロック換算 ±0.006、フォントpx換算 @0.005 で ±1.2px）です。
 */
internal object GestureGuiFontMetrics {
    /**
     * 1行の高さ（ブロック）を論理文字サイズへ掛ける係数です。
     * 複数行テキストの行ピッチ実測からの値です（9.98 → 10.0 に丸め）。
     */
    const val LINE_HEIGHT_FACTOR: Double = 10.0

    /** 行送り（フォントpx/行）の実測値です。 */
    const val LINE_ADVANCE_PX: Double = 10.0

    /**
     * 論理サイズ 1.0 あたり 1 フォントpx が占めるブロック幅です。
     * パングラム4サイズ（0.0045〜0.006）の実測で 1.005〜1.011 を確認し、
     * [LINE_HEIGHT_FACTOR] / [LINE_ADVANCE_PX] = 1.0 と整合します。
     */
    const val PX_TO_BLOCK: Double = LINE_HEIGHT_FACTOR / LINE_ADVANCE_PX

    /** 既定の文字送り（px/字）です。パングラム実測で検証済み。 */
    const val DEFAULT_ADVANCE_PX: Double = 6.0

    /**
     * 全角グリフの文字送り（px/字）です。
     * 日本語16文字（句点込み）の実測では 8.8px/字前後、句点を除いた読みで 9.0〜9.1px/字
     * とばらつきがあったため、vanilla 参照値かつ実測範囲の中央である 9.0 を採用します。
     */
    const val WIDE_ADVANCE_PX: Double = 9.0

    /** 空白の文字送り（px/字）です。パングラム内空白で検証済み。 */
    const val SPACE_ADVANCE_PX: Double = 4.0

    /**
     * 太字装飾が各グリフへ加える幅（px/字）です。
     * 実測で +0.97px/字を確認し 1.0 を維持します。
     */
    const val BOLD_EXTRA_PX: Double = 1.0

    /**
     * 論理サイズ別の幅補正係数です。
     * 実測で線形性が確認されたため空（＝全サイズで線形モデル）を維持します。
     */
    val SIZE_CORRECTION: Map<Double, Double> = emptyMap()

    /**
     * 既定送りより狭い ASCII グリフの文字送り（px/字）です。
     * vanilla の advance 表に従います。うち i・l・t・f・k はパングラム実測で
     * 集合的に検証済みです。
     */
    private val NARROW_ADVANCE_PX: Map<Char, Double> = mapOf(
        '!' to 2.0, '\'' to 2.0, ',' to 2.0, '.' to 2.0,
        ':' to 2.0, ';' to 2.0, 'i' to 2.0,
        'l' to 3.0,
        '(' to 4.0, ')' to 4.0, 'I' to 4.0, '[' to 4.0, ']' to 4.0, 't' to 4.0,
        'f' to 5.0, 'k' to 5.0,
    )

    /**
     * 計測画面で個別実測した特殊グリフの文字送り（px/字）です。
     * 実測はインク幅 + 両側ベアリング約1px で推定し、グリフ列の合計幅で整合確認済み
     * （◀6.3 / 🗑5.7 / ＋5.1 のインクpx実測、列合計 50.1px に対し 50.0px）。
     */
    private val MEASURED_GLYPH_ADVANCE_PX: Map<Int, Double> = mapOf(
        0x25C0 to 7.5,   // ◀ BLACK LEFT-POINTING TRIANGLE
        0x25B6 to 7.5,   // ▶ BLACK RIGHT-POINTING TRIANGLE（対称と仮定）
        0x1F5D1 to 7.0,  // 🗑 WASTEBASKET
        0xFF0B to 6.0,   // ＋ FULLWIDTH PLUS SIGN
        0xFF0D to 6.0,   // － FULLWIDTH HYPHEN-MINUS
    )

    /** テキストの推定幅（フォントpx）です。 */
    fun estimateWidthPx(text: String, bold: Boolean = false): Double {
        var px = 0.0
        val extra = if (bold) BOLD_EXTRA_PX else 0.0
        var i = 0
        while (i < text.length) {
            val cp = Character.codePointAt(text, i)
            px += advancePx(cp) + extra
            i += Character.charCount(cp)
        }
        return px
    }

    /** テキストの推定幅（ブロック）です。 */
    fun estimateWidthBlocks(text: String, size: Double, bold: Boolean = false): Double =
        size * PX_TO_BLOCK * estimateWidthPx(text, bold) * (SIZE_CORRECTION[size] ?: 1.0)

    /** グリフの文字送り（px）です。個別実測 → 空白 → 狭幅 → 全角 → 既定の順で解決します。 */
    fun advancePx(codePoint: Int): Double {
        MEASURED_GLYPH_ADVANCE_PX[codePoint]?.let { return it }
        if (codePoint == 0x20) return SPACE_ADVANCE_PX
        if (codePoint in 0x21..0x7E) {
            NARROW_ADVANCE_PX[codePoint.toChar()]?.let { return it }
            return DEFAULT_ADVANCE_PX
        }
        return if (isWide(codePoint)) WIDE_ADVANCE_PX else DEFAULT_ADVANCE_PX
    }

    /**
     * 全角扱いとするコードポイント範囲です。
     * CJK・全角ASCII・BMP外グリフを含みます。
     * なお 0xFF00..0xFF60 の一部（＋－など）は実測で約6pxと狭いことが判明しており、
     * [MEASURED_GLYPH_ADVANCE_PX] の個別値が優先されます。
     */
    fun isWide(codePoint: Int): Boolean = when (codePoint) {
        in 0x1100..0x115F,   // ハングル字母
        in 0x2E80..0x9FFF,   // CJK 部首・かな・漢字等
        in 0xA000..0xA4CF,
        in 0xAC00..0xD7A3,   // ハングル音節
        in 0xF900..0xFAFF,   // CJK 互換漢字
        in 0xFE30..0xFE6F,   // CJK 互換形
        in 0xFF00..0xFF60,   // 全角ASCII
        in 0x1F300..0x1FAFF, // BMP 外の絵文字・記号
        in 0x20000..0x3FFFD  // BMP 外の CJK 拡張
        -> true
        else -> false
    }
}

