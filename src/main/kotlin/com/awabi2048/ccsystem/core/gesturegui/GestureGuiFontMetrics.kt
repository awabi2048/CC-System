package com.awabi2048.ccsystem.core.gesturegui

/**
 * Gesture GUI のフォント幾何モデルです。
 *
 * 現在の各値は Minecraft 既定フォントの内部表（advance px）を参照値として用いた**仮値**です。
 * `/cc gesture-gui measure` の計測画面で実機読み取りを行い、確定値へ置き換えます。
 * 計測手順と画面仕様は「Gesture GUI 宣言レイアウト契約」のフォント計測節を参照します。
 */
internal object GestureGuiFontMetrics {
    /** 1行の高さ（ブロック）を論理文字サイズへ掛ける係数です。仮値。 */
    const val LINE_HEIGHT_FACTOR: Double = 3.0

    /** 既定フォントの行送り（px/行）です。vanilla 仕様の参照値です。 */
    const val VANILLA_LINE_ADVANCE_PX: Double = 9.0

    /**
     * 論理サイズ 1.0 あたり 1 フォントpx が占めるブロック幅です。
     * 「行送り [VANILLA_LINE_ADVANCE_PX] px が [LINE_HEIGHT_FACTOR] ブロックに対応する」
     * という仮定から導出した仮値であり、実機計測で線形性を検証します。
     */
    const val PX_TO_BLOCK: Double = LINE_HEIGHT_FACTOR / VANILLA_LINE_ADVANCE_PX

    /** 既定の文字送り（px/字）です。ASCII 代表値の仮値。 */
    const val DEFAULT_ADVANCE_PX: Double = 6.0

    /** 全角グリフの文字送り（px/字）です。日本語・全角ASCII・BMP外グリフの仮値。 */
    const val WIDE_ADVANCE_PX: Double = 9.0

    /** 空白の文字送り（px/字）です。 */
    const val SPACE_ADVANCE_PX: Double = 4.0

    /** 太字装飾が各グリフへ加える幅（px/字）です。vanilla 仕様の参照値です。 */
    const val BOLD_EXTRA_PX: Double = 1.0

    /**
     * 論理サイズ別の幅補正係数です。
     * 実機計測で非線形が判明した場合にのみ `size -> 係数` を登録します。
     * 既定は空（＝全サイズで線形モデル）です。
     */
    val SIZE_CORRECTION: Map<Double, Double> = emptyMap()

    /**
     * 個別実測の対象とする特殊グリフです。実測するまでは全角扱いとします。
     * 計測画面の測定行と対応します。
     */
    private val SPECIAL_GLYPHS = intArrayOf(0x25C0, 0x25B6, 0x1F5D1, 0xFF0B, 0xFF0D).toSet()

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

    /** グリフの文字送り（px）です。 */
    fun advancePx(codePoint: Int): Double = when {
        codePoint == 0x20 -> SPACE_ADVANCE_PX
        isWide(codePoint) -> WIDE_ADVANCE_PX
        else -> DEFAULT_ADVANCE_PX
    }

    /**
     * 全角扱いとするコードポイント範囲です。
     * CJK・全角ASCII・BMP外グリフおよび計測特殊グリフを含みます。
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
        else -> codePoint in SPECIAL_GLYPHS
    }
}
