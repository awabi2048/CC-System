package com.awabi2048.ccsystem.features.misc.gesturegui

import com.awabi2048.ccsystem.api.CCSystemAPI
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiAccess
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiTextAlignment
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiView
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiAbsoluteOffsets
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiBlock
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiBox
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiDocument
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutFacade
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiNode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiSizeSpec
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiText
import com.awabi2048.ccsystem.api.localization.generated.GestureGuiKeys
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * フォント実測用の管理者向け画面です（`/cc gesture-gui measure`）。
 *
 * 幅係数・行ピッチ・太字補正・特殊グリフ幅は、人間が目盛りから読み取り、
 * Kotlin 定数（`GestureGuiTextMetrics` 周辺）へ手動反映します。
 * 自動記録・設定ファイル書き戻し・画像解析は行いません。
 *
 * 目盛りと文字は同一画面上にあるため、パネル傾きによる視差補正は不要です。
 * 測定対象の文字列は再現性が必要なため、ローカライズせず固定値とします
 * （枠組み文言のみローカライズします）。
 *
 * 読み取り手順:
 * - 幅: 文字は画面中央揃えのため、中央の 0 から右端までの読みを 2 倍します。
 * - 行ピッチ: 右下の縦目盛りで複数行テキストの行間を読みます。
 * - 既知幅バー: 左端からの既知幅で、目盛りと実ブロック幅の対応を検証します。
 */
class GestureGuiMeasureController(private val api: CCSystemAPI) {
    fun open(player: Player) {
        api.getGestureGuiService().open(player, listOf(view(player)))
    }

    fun close(player: Player): Boolean = api.getGestureGuiService().close(player.uniqueId)

    private fun view(owner: Player): GestureGuiView {
        val panel = GestureGuiPanel()
        val nodes = mutableListOf<GestureGuiNode>()

        nodes += text(
            panel, "measure-title",
            Component.text(localized(owner, GestureGuiKeys.GESTURE_GUI_MEASURE_TITLE)),
            size = 0.009, cx = 0.0, cy = 0.455, w = 1.2, h = 0.04,
        )
        nodes += text(
            panel, "measure-note",
            Component.text(localized(owner, GestureGuiKeys.GESTURE_GUI_MEASURE_NOTE)),
            size = 0.0045, cx = 0.0, cy = 0.425, w = 1.8, h = 0.02,
        )

        horizontalRuler(nodes, panel, yRail = 0.355)

        // 中央基準線です。幅測定はここから右端までの距離を2倍して読みます。
        nodes += block(panel, "measure-hairline", HAIRLINE, 0.0, 0.12, 0.004, 0.44)

        SAMPLES.forEachIndexed { index, sample ->
            nodes += text(
                panel, "measure-${sample.id}", sample.text, sample.size,
                cx = 0.0, cy = 0.29 - index * SAMPLE_PITCH, w = 1.9, h = 0.035,
            )
        }

        referenceBars(nodes, panel)

        // 行ピッチ測定です。複数行テキストと縦目盛りで行間を読み取ります。
        nodes += text(
            panel, "measure-multiline",
            Component.text(MULTILINE_SAMPLE), 0.005,
            cx = -0.45, cy = -0.38, w = 0.9, h = 0.12,
        )
        verticalRuler(nodes, panel, xRail = 0.35, yTop = -0.36, span = 0.12)

        nodes += text(
            panel, "measure-exit",
            Component.text(localized(owner, GestureGuiKeys.GESTURE_GUI_EXIT_GUIDANCE)),
            size = 0.0045, cx = 0.0, cy = -0.472, w = 1.0, h = 0.02,
        )

        return compile(screenRoot(nodes), panel, "gesture-gui-measure")
    }

    /**
     * 横目盛り帯です。中央を 0 とし左右対称に、細目盛り 0.02・太目盛り 0.1・
     * 数値ラベル 0.2 間隔で引きます。ラベル値は中央からの距離です。
     */
    private fun horizontalRuler(nodes: MutableList<GestureGuiNode>, panel: GestureGuiPanel, yRail: Double) {
        nodes += block(panel, "ruler-rail", RAIL, 0.0, yRail, RULER_HALF * 2.0, RAIL_HEIGHT)
        var i = 0
        while (i <= TICK_COUNT) {
            val x = -RULER_HALF + i * MINOR_TICK
            val major = i % MAJOR_EVERY == 0
            val length = if (major) MAJOR_LENGTH else MINOR_LENGTH
            nodes += block(
                panel, "ruler-tick-$i", if (major) MAJOR else MINOR,
                x, yRail - RAIL_HEIGHT / 2.0 - length / 2.0, TICK_WIDTH, length,
            )
            if (i % LABEL_EVERY == 0) {
                nodes += text(
                    panel, "ruler-label-$i",
                    Component.text("%.1f".format(kotlin.math.abs(x))), 0.0035,
                    cx = x, cy = yRail + 0.022, w = 0.06, h = 0.014,
                )
            }
            i += 1
        }
    }

    /** 縦目盛りです。上端を 0 とし下向きに引きます。行間の読み取り用です。 */
    private fun verticalRuler(nodes: MutableList<GestureGuiNode>, panel: GestureGuiPanel, xRail: Double, yTop: Double, span: Double) {
        nodes += block(panel, "vruler-rail", RAIL, xRail, yTop - span / 2.0, RAIL_HEIGHT, span)
        val count = (span / MINOR_TICK).toInt()
        var i = 0
        while (i <= count) {
            val y = yTop - i * MINOR_TICK
            val major = i % MAJOR_EVERY == 0
            val length = if (major) MAJOR_LENGTH else MINOR_LENGTH
            nodes += block(
                panel, "vruler-tick-$i", if (major) MAJOR else MINOR,
                xRail + RAIL_HEIGHT / 2.0 + length / 2.0, y, length, 0.004,
            )
            if (major) {
                nodes += text(
                    panel, "vruler-label-$i",
                    Component.text("%.1f".format(i * MINOR_TICK)), 0.0035,
                    cx = xRail - 0.05, cy = y, w = 0.05, h = 0.014,
                )
            }
            i += 1
        }
    }

    /**
     * 既知幅の参照バーです。左端からの実ブロック幅と目盛りの対応を検証します。
     * ブロック矩形はフォント係数に依存しないため、目盛り自身の校正にも使えます。
     */
    private fun referenceBars(nodes: MutableList<GestureGuiNode>, panel: GestureGuiPanel) {
        REFERENCE_WIDTHS.forEachIndexed { index, width ->
            val y = -0.135 - index * 0.035
            nodes += block(panel, "ref-$index", REFERENCE, RULER_LEFT + width / 2.0, y, width, 0.014)
            nodes += text(
                panel, "ref-label-$index",
                Component.text("%.1f".format(width)), 0.0035,
                cx = RULER_LEFT + width + 0.05, cy = y, w = 0.06, h = 0.014,
            )
        }
    }

    // ─── 配置・構築の共通処理 ──────────────────────────────

    private fun contentSize(panel: GestureGuiPanel): Pair<Double, Double> =
        (panel.width - panel.frameWidth * 2.0) to (panel.height - panel.frameWidth * 2.0)

    /** 画面中央原点の矩形を、パネル内容域基準の絶対オフセットへ変換します。 */
    private fun at(panel: GestureGuiPanel, cx: Double, cy: Double, w: Double, h: Double): GestureGuiAbsoluteOffsets {
        val (contentWidth, contentHeight) = contentSize(panel)
        return GestureGuiAbsoluteOffsets(
            left = cx - w / 2.0 + contentWidth / 2.0,
            top = contentHeight / 2.0 - cy - h / 2.0,
        )
    }

    private fun block(panel: GestureGuiPanel, id: String, material: Material, cx: Double, cy: Double, w: Double, h: Double): GestureGuiNode =
        GestureGuiBlock(
            blockData = Bukkit.createBlockData(material),
            width = GestureGuiSizeSpec.Fixed(w),
            height = GestureGuiSizeSpec.Fixed(h),
            id = id,
            absolute = at(panel, cx, cy, w, h),
        )

    private fun text(
        panel: GestureGuiPanel,
        id: String,
        text: Component,
        size: Double,
        cx: Double,
        cy: Double,
        w: Double,
        h: Double,
        alignment: GestureGuiTextAlignment = GestureGuiTextAlignment.CENTER,
    ): GestureGuiNode = GestureGuiText(
        text = text,
        size = size,
        // 折り返しを抑え、1行の実測幅を読めるようにします。
        lineWidth = 800,
        alignment = alignment,
        id = id,
        width = GestureGuiSizeSpec.Fixed(w),
        height = GestureGuiSizeSpec.Fixed(h),
        absolute = at(panel, cx, cy, w, h),
    )

    private fun compile(root: GestureGuiNode, panel: GestureGuiPanel, screenId: String): GestureGuiView {
        val resolved = GestureGuiLayoutFacade.layout(GestureGuiDocument(root, panel))
        resolved.diagnostics.forEach { diagnostic ->
            Bukkit.getLogger().warning("[GestureGuiMeasure] layout diagnostic: $diagnostic")
        }
        val compiled = GestureGuiLayoutFacade.compile(
            resolved,
            screenId,
            access = GestureGuiAccess.PUBLIC,
        )
        compiled.diagnostics.forEach { diagnostic ->
            Bukkit.getLogger().warning("[GestureGuiMeasure] compile diagnostic: $diagnostic")
        }
        return compiled.view
    }

    private fun screenRoot(children: List<GestureGuiNode>): GestureGuiNode = GestureGuiBox(
        children = children,
        width = GestureGuiSizeSpec.Percent(1.0),
        height = GestureGuiSizeSpec.Percent(1.0),
    )

    private fun localized(player: Player, key: com.awabi2048.ccsystem.api.localization.LocalizationKey<String>): String =
        api.getLocalized(player, key)

    private data class Sample(val id: String, val size: Double, val text: Component)

    companion object {
        // ─── 目盛り（幾何不変条件をテストで検証するため internal）──
        /** 細目盛りの間隔（ブロック）です。読み取り精度の基準です。 */
        internal const val MINOR_TICK = 0.02
        /** 太目盛りは細目盛りの5本ごと（0.1間隔）です。 */
        internal const val MAJOR_EVERY = 5
        /** 数値ラベルは0.2間隔です。 */
        internal const val LABEL_EVERY = 10
        /** 横目盛りの片側幅です。中央を 0 とし左右対称に引きます。 */
        internal const val RULER_HALF = 0.9
        /** 目盛り本数の終端添字です（半幅0.9・間隔0.02 → -0.9〜+0.9で91本）。 */
        internal const val TICK_COUNT = 90
        internal const val MINOR_LENGTH = 0.02
        internal const val MAJOR_LENGTH = 0.05
        internal const val TICK_WIDTH = 0.005
        internal const val RAIL_HEIGHT = 0.006
        /** 参照バー・測定行の左端位置です。 */
        internal const val RULER_LEFT = -0.95
        internal const val SAMPLE_PITCH = 0.042

        private val RAIL = Material.WHITE_CONCRETE
        private val MAJOR = Material.WHITE_CONCRETE
        private val MINOR = Material.GRAY_CONCRETE
        private val REFERENCE = Material.CYAN_CONCRETE
        private val HAIRLINE = Material.GRAY_STAINED_GLASS

        /** 参照バーの既知幅（ブロック）です。 */
        internal val REFERENCE_WIDTHS = listOf(0.2, 0.4, 0.6, 0.8, 1.0)

        // ─── 測定文字列（固定・ローカライズ対象外）────────────
        private const val ASCII_SAMPLE = "The quick brown fox jumps over the lazy dog 0123456789"
        private const val JAPANESE_SAMPLE = "起動条件と実行内容を編集します。"
        private const val GLYPH_SAMPLE = "◀ ▶ 🗑 ＋ －"
        private const val MULTILINE_SAMPLE = "line 1 あいう\nline 2 かきく\nline 3 さしす"

        /** 幅測定の対象です。サイズはテーマの文字階調（title/label/body/caption）に合わせます。 */
        private val SAMPLES = listOf(
            Sample("s0045", 0.0045, Component.text(ASCII_SAMPLE)),
            Sample("s0050", 0.0050, Component.text(ASCII_SAMPLE)),
            Sample("s0055", 0.0055, Component.text(ASCII_SAMPLE)),
            Sample("s0060", 0.0060, Component.text(ASCII_SAMPLE)),
            Sample("s-ja", 0.0050, Component.text(JAPANESE_SAMPLE)),
            Sample("s-bold", 0.0050, Component.text(ASCII_SAMPLE).decorate(TextDecoration.BOLD)),
            Sample("s-glyphs", 0.0050, Component.text(GLYPH_SAMPLE)),
            Sample("s-g-prev", 0.0050, Component.text("◀")),
            Sample("s-g-del", 0.0050, Component.text("🗑")),
            Sample("s-g-add", 0.0050, Component.text("＋")),
        )
    }
}
