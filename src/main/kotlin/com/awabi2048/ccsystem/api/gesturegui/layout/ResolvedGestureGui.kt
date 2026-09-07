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
    /** null は無制限（VISIBLE 連鎖）を表します。 */
    val effectiveClip: GestureGuiBounds?,
    /** 重なり解決用の深さです。compiler が layer へ写像します。 */
    val resolvedZ: Int,
    /** null は非操作ノードを表します。visual との二重管理は行いません。 */
    val interactionBounds: GestureGuiBounds?,
    val nodeId: String?,
    val actionId: String?,
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
