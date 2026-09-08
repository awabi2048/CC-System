package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiBounds
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel

/**
 * Layout Engine の出力である解決済みノードです。
 *
 * Display Entity を直接生成せず、この中間表現で validation / debug / lint を
 * 行えるようにします。座標系は既存 Low-level API と同じ画面中央原点です。
 */
data class ResolvedGestureGuiNode(
    /** padding を含む外形です。 */
    val borderBounds: GestureGuiBounds,
    /** 子の containing block となる内側領域です。 */
    val contentBounds: GestureGuiBounds,
    /** 無制限・矩形・空を区別した、祖先から継承する切り抜き状態です。 */
    val effectiveClip: GestureGuiClip,
    /** 重なり解決用の深さです。compiler が layer へ写像します。 */
    val resolvedZ: Int,
    /** null は非操作ノードを表します。visual との二重管理は行いません。 */
    val interactionBounds: GestureGuiBounds?,
    val nodeId: String?,
    val actionId: String?,
    /**
     * 解決元の宣言ノードです。compiler が見た目（文言・素材等）を取得します。
     * Resolved 層自体はこの参照を読まず、bounds のみで検証します。
     */
    val source: GestureGuiNode,
    val children: List<ResolvedGestureGuiNode> = emptyList(),
) {
    init {
        require(resolvedZ >= 0) { "resolved z must be non-negative" }
    }
}

/** Layout Engine の出力文書です。 */
data class ResolvedGestureGui(
    val root: ResolvedGestureGuiNode,
    val diagnostics: List<GestureGuiLayoutDiagnostic>,
    val panel: GestureGuiPanel,
)
