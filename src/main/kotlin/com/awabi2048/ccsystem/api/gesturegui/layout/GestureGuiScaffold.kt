package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.theme.GestureGuiTheme

/**
 * 一覧・設定系画面の領域足場です。
 *
 * 画面を文脈・主要内容・ナビゲーション・補助操作の領域へ分ける規則
 * （cross-system-ui-design.md のレイアウト規則）を構造として保持します。
 * 呼び出し側は「どの領域に何を置くか」だけを渡し、領域どうしの位置関係
 * （下端の操作行を先行確保、上段は上揃え・下揃えは下端固定）は足場が決めます。
 *
 * 構造は次のとおりです。
 *
 * ```
 * Column (画面内容域)
 *   ├─ Box (Fraction)                    … 本文領域
 *   │    └─ Column (space-between)
 *   │         ├─ Column id-top    … 上段グループ（見出し＋ページ対象）
 *   │         └─ Column id-bottom … 下揃えグループ（境界線＋下端固定行）
 *   └─ Row id-footer                     … 操作行（左帯・右帯）
 * ```
 *
 * 各領域には `id-top` / `id-bottom` / `id-footer` / `id-group-divider` の
 * 派生 ID を付与し、ホバー置換先や診断の発生源として参照できます。
 */
object GestureGuiScaffold {
    /**
     * 上段グループ・下揃えグループ・操作行で構成する画面本体です。
     *
     * @param header 文脈領域です（見出し・現在値等）。上段グループの先頭へ置きます。
     * @param top 上段グループです（ページ対象のカードグリッド等）。null の場合は領域を空にします。
     * @param bottom 下揃えグループです。本文領域の下端へ固定します。null の場合は領域を空にします。
     * @param footer 操作行です。下端へ固定します（[footerRow] を推奨します）。
     * @param showDivider 上段・下揃えの両方が非空のとき、グループ境界線を自動挿入するかです。
     *   両グループ同居の画面だけに置く規則のため、標準では自動判定です。
     * @param gap 本文領域内の領域間余白です。
     */
    fun pagedScreen(
        id: String,
        theme: GestureGuiTheme,
        footer: GestureGuiNode,
        header: GestureGuiNode? = null,
        top: GestureGuiNode? = null,
        bottom: GestureGuiNode? = null,
        showDivider: Boolean = top != null && bottom != null,
        gap: Double = 0.02,
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode {
        val topColumn = GestureGuiColumn(
            children = listOfNotNull(header, top),
            id = "$id-top",
            width = GestureGuiSizeSpec.Percent(1.0),
            height = GestureGuiSizeSpec.Auto,
            gap = gap,
        )
        val bottomColumn = GestureGuiColumn(
            children = buildList {
                if (showDivider) add(GestureGuiComponents.divider("$id-group-divider", theme))
                bottom?.let(::add)
            },
            id = "$id-bottom",
            width = GestureGuiSizeSpec.Percent(1.0),
            height = GestureGuiSizeSpec.Auto,
            gap = gap,
        )
        return GestureGuiColumn(
            children = listOf(
                GestureGuiBox(
                    children = listOf(
                        GestureGuiColumn(
                            children = listOf(topColumn, bottomColumn),
                            id = "$id-main",
                            width = GestureGuiSizeSpec.Percent(1.0),
                            height = GestureGuiSizeSpec.Percent(1.0),
                            mainArrangement = GestureGuiMainArrangement.SPACE_BETWEEN,
                        ),
                    ),
                    id = "$id-content",
                    width = GestureGuiSizeSpec.Percent(1.0),
                    height = GestureGuiSizeSpec.Fraction(1.0),
                ),
                footer,
            ),
            id = id,
            width = GestureGuiSizeSpec.Percent(1.0),
            height = GestureGuiSizeSpec.Percent(1.0),
            margin = margin,
        )
    }

    /**
     * 下端操作行です。左帯（ページ移動・主操作）と右帯（戻る・閉じる）の
     * 2区画へ分け、区画は同幅で固定します。片方だけの画面でも帯の位置は不変です。
     *
     * @param left 左帯の内容です（[GestureGuiComponents.pager] 等）。null でも帯は確保します。
     * @param right 右帯の内容です（戻るボタン等）。null でも帯は確保します。
     */
    fun footerRow(
        id: String,
        left: GestureGuiNode?,
        right: GestureGuiNode?,
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.10),
        gap: Double = 0.02,
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = GestureGuiRow(
        children = listOf(
            footerCell("$id-left", left),
            footerCell("$id-right", right),
        ),
        id = id,
        width = GestureGuiSizeSpec.Percent(1.0),
        height = height,
        gap = gap,
        margin = margin,
    )

    /** 帯の位置を維持するため、空の帯も区画として残します。 */
    private fun footerCell(id: String, content: GestureGuiNode?): GestureGuiNode = GestureGuiBox(
        children = listOfNotNull(content),
        id = id,
        width = GestureGuiSizeSpec.Fraction(1.0),
        height = GestureGuiSizeSpec.Auto,
    )
}
