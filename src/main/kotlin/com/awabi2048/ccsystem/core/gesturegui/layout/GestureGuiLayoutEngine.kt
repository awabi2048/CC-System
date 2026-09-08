package com.awabi2048.ccsystem.core.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiBounds
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiAbsoluteOffsets
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiBlock
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiBox
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiColumn
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiClip
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCrossAlignment
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCustom
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiDocument
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiGrid
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiItem
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutDiagnostic
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutErrorCode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiMainArrangement
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiNode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiOverflow
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiOverlay
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiRow
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiSizeSpec
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiText
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiViewport
import com.awabi2048.ccsystem.api.gesturegui.layout.ResolvedGestureGui
import com.awabi2048.ccsystem.api.gesturegui.layout.ResolvedGestureGuiNode

/**
 * 宣言的文書を解決済みレイアウトへ変換する純粋エンジンです。
 *
 * Bukkit / Display Entity へ依存せず、座標計算と検証だけを行います。
 * 既知の制限（Profile 1）として、完全な intrinsic text sizing は行わず、
 * Text の Auto 高さは `lines * size * TEXT_LINE_HEIGHT_FACTOR` で概算します。
 */
object GestureGuiLayoutEngine {
    /** 退化 bound を避ける最小寸法です（ブロック単位）。 */
    const val MIN_EXTENT: Double = 1.0e-6

    /** Text の Auto 高さ概算に使う行高係数です。 */
    const val TEXT_LINE_HEIGHT_FACTOR: Double = 3.0

    /**
     * 文書を解決します。
     *
     * @param knownActionIds 既知の action ID 集合です。null の場合は action 参照検証を省略します。
     * @param source 診断用の発生源表記です（例 "editor.html:42"、直接構築時は null）。
     */
    fun layout(
        document: GestureGuiDocument,
        knownActionIds: Set<String>? = null,
        source: String? = null,
    ): ResolvedGestureGui {
        val ctx = Context(knownActionIds, source)
        checkIds(document.root, null, ctx)
        val panel = document.panel
        val panelContent = inset(
            boundsOf(-panel.width / 2.0, -panel.height / 2.0, panel.width / 2.0, panel.height / 2.0),
            panel.frameWidth,
            panel.frameWidth,
            panel.frameWidth,
            panel.frameWidth,
        )
        val rootWidth = resolveFillWidth(document.root.width, panelContent.width(), document.root, "<panel>", ctx)
        val rootHeight = when (val spec = document.root.height) {
            is GestureGuiSizeSpec.Fixed -> spec.value
            is GestureGuiSizeSpec.Percent -> spec.ratio * panelContent.height()
            is GestureGuiSizeSpec.Fraction -> panelContent.height()
            is GestureGuiSizeSpec.Auto -> measureHeight(document.root, document.root.id, ctx)
                .coerceAtMost(panelContent.height())
        }
        val rootMinX = panelContent.minX + document.root.margin.left
        // ルートの上端はパネル内容域の上端へ寄せます。
        val rootMaxY = panelContent.maxY - document.root.margin.top
        val rootBorder = rectOf(rootMinX, rootMaxY - rootHeight, rootMinX + rootWidth, rootMaxY)
        reportOverflow(rootBorder, panelContent, overflowOf(document.root), document.root.id, "<panel>", ctx)
        val root = layoutNode(
            document.root,
            rootBorder,
            parentClip = GestureGuiClip.Unbounded,
            parentZ = -1,
            parentId = "<panel>",
            ctx = ctx,
        )
        return ResolvedGestureGui(root, ctx.diagnostics.toList(), panel)
    }

    // ─── 検証 ────────────────────────────────────────────────

    private class Context(
        val knownActionIds: Set<String>?,
        val source: String?,
    ) {
        val diagnostics = mutableListOf<GestureGuiLayoutDiagnostic>()
        val idCounts = mutableMapOf<String, Int>()

        fun add(
            code: GestureGuiLayoutErrorCode,
            nodeId: String?,
            parentId: String?,
            message: String,
        ) {
            diagnostics += GestureGuiLayoutDiagnostic(code, nodeId, parentId, message, source)
        }
    }

    private fun checkIds(node: GestureGuiNode, parentId: String?, ctx: Context) {
        node.id?.let { id ->
            ctx.idCounts[id] = (ctx.idCounts[id] ?: 0) + 1
            if (ctx.idCounts[id] == 2) {
                ctx.add(
                    GestureGuiLayoutErrorCode.DUPLICATE_ID,
                    id,
                    parentId,
                    "duplicate node id: $id",
                )
            }
        }
        node.actionId?.let { action ->
            if (ctx.knownActionIds != null && action !in ctx.knownActionIds) {
                ctx.add(
                    GestureGuiLayoutErrorCode.UNKNOWN_ACTION,
                    node.id,
                    parentId,
                    "unknown action reference: $action",
                )
            }
            if (node.acceptedGestures.isEmpty()) {
                ctx.add(
                    GestureGuiLayoutErrorCode.EMPTY_HITBOX,
                    node.id,
                    parentId,
                    "action node has no accepted gestures: $action",
                )
            }
        }
        // guard は操作面の入力時判定にのみ用います。操作面なしでは評価されません。
        if (node.gestureGuard != null && node.actionId == null) {
            ctx.add(
                GestureGuiLayoutErrorCode.GUARD_WITHOUT_ACTION,
                node.id,
                parentId,
                "guard without action is never evaluated: ${node.id ?: "<anonymous>"}",
            )
        }
        childrenOf(node).forEach { checkIds(it, node.id ?: parentId, ctx) }
    }

    private fun childrenOf(node: GestureGuiNode): List<GestureGuiNode> = when (node) {
        is GestureGuiBox -> node.children
        is GestureGuiRow -> node.children
        is GestureGuiColumn -> node.children
        is GestureGuiGrid -> node.children
        is GestureGuiOverlay -> node.children
        is GestureGuiViewport -> listOf(node.content)
        is GestureGuiText,
        is GestureGuiBlock,
        is GestureGuiItem,
        is GestureGuiCustom,
        -> emptyList()
    }

    private fun overflowOf(node: GestureGuiNode): GestureGuiOverflow = when (node) {
        is GestureGuiBox -> node.overflow
        is GestureGuiRow -> node.overflow
        is GestureGuiColumn -> node.overflow
        is GestureGuiGrid -> node.overflow
        is GestureGuiOverlay -> node.overflow
        is GestureGuiViewport -> node.overflow
        is GestureGuiText,
        is GestureGuiBlock,
        is GestureGuiItem,
        is GestureGuiCustom,
        -> GestureGuiOverflow.VISIBLE
    }

    // ─── 矩形演算（GestureGuiBounds は中央原点・右／上正のまま扱います） ────

    private fun boundsOf(minX: Double, minY: Double, maxX: Double, maxY: Double): GestureGuiBounds {
        var loX = minX
        var hiX = maxX
        var loY = minY
        var hiY = maxY
        if (hiX - loX < MIN_EXTENT) {
            val center = (loX + hiX) / 2.0
            loX = center - MIN_EXTENT / 2.0
            hiX = center + MIN_EXTENT / 2.0
        }
        if (hiY - loY < MIN_EXTENT) {
            val center = (loY + hiY) / 2.0
            loY = center - MIN_EXTENT / 2.0
            hiY = center + MIN_EXTENT / 2.0
        }
        return GestureGuiBounds(loX, loY, hiX, hiY)
    }

    private fun rectOf(minX: Double, minY: Double, maxX: Double, maxY: Double): GestureGuiBounds =
        boundsOf(minX, minY, maxX, maxY)

    private fun GestureGuiBounds.width(): Double = maxX - minX

    private fun GestureGuiBounds.height(): Double = maxY - minY

    private fun inset(
        bounds: GestureGuiBounds,
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
    ): GestureGuiBounds = boundsOf(
        bounds.minX + left,
        bounds.minY + bottom,
        bounds.maxX - right,
        bounds.maxY - top,
    )

    /** 子の containing 内容域からの逸脱量を方向付きで返します。空なら逸脱なしです。 */
    private fun overflows(child: GestureGuiBounds, containing: GestureGuiBounds): Map<String, Double> =
        buildMap {
            val left = containing.minX - child.minX
            if (left > 0.0) put("left", left)
            val right = child.maxX - containing.maxX
            if (right > 0.0) put("right", right)
            val bottom = containing.minY - child.minY
            if (bottom > 0.0) put("bottom", bottom)
            val top = child.maxY - containing.maxY
            if (top > 0.0) put("top", top)
        }

    private fun reportOverflow(
        child: GestureGuiBounds,
        containing: GestureGuiBounds,
        overflow: GestureGuiOverflow,
        nodeId: String?,
        parentId: String?,
        ctx: Context,
    ) {
        if (overflow == GestureGuiOverflow.VISIBLE) return
        val amounts = overflows(child, containing)
        if (amounts.isEmpty()) return
        val detail = amounts.entries.joinToString(", ") { (direction, amount) -> "$direction overflow: $amount" }
        ctx.add(
            if (overflow == GestureGuiOverflow.REJECT) {
                GestureGuiLayoutErrorCode.REJECT_OVERFLOW
            } else {
                GestureGuiLayoutErrorCode.LAYOUT_OVERFLOW
            },
            nodeId,
            parentId,
            "node ${nodeId ?: "<anonymous>"} overflows ${parentId ?: "<panel>"} ($detail)",
        )
    }

    // ─── 高さ概算（縦積み wrap 用） ────────────────────────────

    private fun textHeight(node: GestureGuiText): Double = node.lines * node.size * TEXT_LINE_HEIGHT_FACTOR

    /**
     * 縦方向の内容高さを概算します。Percent / Fraction は不明 bound のため 0 扱い＋診断です。
     * 呼び出しは各ノード1回に限り、診断の重複を避けます。
     */
    private fun measureHeight(node: GestureGuiNode, parentId: String?, ctx: Context): Double {
        // フローからの除外は親が行います。絶対配置ノード自身のAuto寸法は内容を測定します。
        when (val spec = node.height) {
            is GestureGuiSizeSpec.Fixed -> return spec.value
            is GestureGuiSizeSpec.Percent,
            is GestureGuiSizeSpec.Fraction,
            -> {
                ctx.add(
                    GestureGuiLayoutErrorCode.INVALID_SIZE,
                    node.id,
                    parentId,
                    "relative height inside auto wrap resolves to 0: ${node.id ?: "<anonymous>"}",
                )
                return 0.0
            }
            is GestureGuiSizeSpec.Auto -> Unit
        }
        return when (node) {
            is GestureGuiText -> textHeight(node)
            is GestureGuiBlock,
            is GestureGuiItem,
            is GestureGuiCustom,
            -> {
                ctx.add(
                    GestureGuiLayoutErrorCode.AUTO_SIZE_UNSUPPORTED,
                    node.id,
                    parentId,
                    "auto height of non-text leaf resolves to 0, specify an explicit size: ${node.id ?: "<anonymous>"}",
                )
                0.0
            }
            is GestureGuiBox -> measureStackHeight(node.children, node.padding, node.gap, node.id, ctx)
            is GestureGuiColumn -> measureStackHeight(node.children, node.padding, node.gap, node.id, ctx)
            is GestureGuiRow -> {
                val inFlow = node.children.filter { it.absolute == null }
                if (inFlow.isEmpty()) return node.padding.vertical
                val childHeights = inFlow.map { measureCrossHeight(it, node.id, ctx) }
                (childHeights.maxOrNull() ?: 0.0) + node.padding.vertical
            }
            is GestureGuiGrid -> {
                val inFlow = node.children.filter { it.absolute == null }
                if (inFlow.isEmpty()) return node.padding.vertical
                val rows = node.rows ?: ((inFlow.size + node.columns - 1) / node.columns).coerceAtLeast(1)
                val childHeights = inFlow.map { measureCrossHeight(it, node.id, ctx) }
                val maxCell = childHeights.maxOrNull() ?: 0.0
                maxCell * rows + node.gap * (rows - 1).coerceAtLeast(0) + node.padding.vertical
            }
            is GestureGuiOverlay -> {
                val inFlow = node.children.filter { it.absolute == null }
                if (inFlow.isEmpty()) return node.padding.vertical
                (inFlow.maxOf { measureCrossHeight(it, node.id, ctx) }) + node.padding.vertical
            }
            is GestureGuiViewport -> measureHeight(node.content, node.id, ctx) + node.padding.vertical
        }
    }

    private fun measureStackHeight(
        children: List<GestureGuiNode>,
        padding: com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiEdgeInsets,
        gap: Double,
        parentId: String?,
        ctx: Context,
    ): Double {
        val inFlow = children.filter { it.absolute == null }
        if (inFlow.isEmpty()) return padding.vertical
        return inFlow.sumOf { measureHeight(it, parentId, ctx) + it.margin.vertical } +
            gap * (inFlow.size - 1).coerceAtLeast(0) + padding.vertical
    }

    private fun measureCrossHeight(node: GestureGuiNode, parentId: String?, ctx: Context): Double {
        if (node.absolute != null) return 0.0
        return when (val spec = node.height) {
            is GestureGuiSizeSpec.Fixed -> spec.value
            is GestureGuiSizeSpec.Percent,
            is GestureGuiSizeSpec.Fraction,
            -> {
                ctx.add(
                    GestureGuiLayoutErrorCode.INVALID_SIZE,
                    node.id,
                    parentId,
                    "relative height inside auto wrap resolves to 0: ${node.id ?: "<anonymous>"}",
                )
                0.0
            }
            is GestureGuiSizeSpec.Auto -> when (node) {
                is GestureGuiText -> textHeight(node)
                is GestureGuiBox,
                is GestureGuiRow,
                is GestureGuiColumn,
                is GestureGuiGrid,
                is GestureGuiOverlay,
                is GestureGuiViewport,
                -> measureHeight(node, parentId, ctx)
                is GestureGuiBlock,
                is GestureGuiItem,
                is GestureGuiCustom,
                -> 0.0
            }
        }
    }

    // ─── 本体 ────────────────────────────────────────────────

    private fun resolveFillWidth(
        spec: GestureGuiSizeSpec,
        bound: Double,
        node: GestureGuiNode,
        parentId: String?,
        ctx: Context,
    ): Double = when (spec) {
        is GestureGuiSizeSpec.Fixed -> spec.value
        is GestureGuiSizeSpec.Percent -> spec.ratio * bound
        is GestureGuiSizeSpec.Fraction -> bound
        is GestureGuiSizeSpec.Auto -> bound
    }.also {
        if (!it.isFinite() || it < 0.0) {
            ctx.add(
                GestureGuiLayoutErrorCode.INVALID_SIZE,
                node.id,
                parentId,
                "invalid resolved width: ${node.id ?: "<anonymous>"}",
            )
        }
    }.coerceAtLeast(MIN_EXTENT)

    private fun layoutNode(
        node: GestureGuiNode,
        border: GestureGuiBounds,
        parentClip: GestureGuiClip,
        parentZ: Int,
        parentId: String?,
        ctx: Context,
    ): ResolvedGestureGuiNode {
        val content = contentOf(node, border)
        // 子の切り抜きは自ノードの overflow で決まります。VISIBLE は親の clip を透過します。
        val ownClip = if (overflowOf(node) == GestureGuiOverflow.VISIBLE) {
            parentClip
        } else {
            parentClip.intersect(content)
        }
        val z = parentZ + 1
        val children = layoutChildren(node, border, content, ownClip, z, ctx)
        val interaction = interactionBounds(node, border, ownClip, parentId, ctx)
        return ResolvedGestureGuiNode(
            borderBounds = border,
            contentBounds = content,
            effectiveClip = ownClip,
            resolvedZ = z,
            interactionBounds = interaction,
            nodeId = node.id,
            actionId = node.actionId,
            source = node,
            children = children,
        )
    }

    private fun contentOf(node: GestureGuiNode, border: GestureGuiBounds): GestureGuiBounds {
        val padding = when (node) {
            is GestureGuiBox -> node.padding
            is GestureGuiRow -> node.padding
            is GestureGuiColumn -> node.padding
            is GestureGuiGrid -> node.padding
            is GestureGuiOverlay -> node.padding
            is GestureGuiViewport -> node.padding
            is GestureGuiText,
            is GestureGuiBlock,
            is GestureGuiItem,
            is GestureGuiCustom,
            -> return border
        }
        return inset(border, padding.left, padding.top, padding.right, padding.bottom)
    }

    private fun interactionBounds(
        node: GestureGuiNode,
        border: GestureGuiBounds,
        clip: GestureGuiClip,
        parentId: String?,
        ctx: Context,
    ): GestureGuiBounds? {
        // ホバー面とバリアはactionなしでもhit testへ参加します。
        // 配置寸法には影響せず、入力管理のための解決済み領域だけを追加します。
        val inputOnly = node.hover != null || node.consumeInput
        if (node.actionId == null && !inputOnly) return null
        if (node.acceptedGestures.isEmpty() && !inputOnly) return null
        val clipped = clip.clip(border) ?: run {
            ctx.add(
                GestureGuiLayoutErrorCode.CLIPPED_INTERACTION,
                node.id,
                parentId,
                "interaction clipped away: ${node.id ?: "<anonymous>"}",
            )
            return null
        }
        return clipped
    }

    private fun layoutChildren(
        node: GestureGuiNode,
        border: GestureGuiBounds,
        content: GestureGuiBounds,
        clip: GestureGuiClip,
        z: Int,
        ctx: Context,
    ): List<ResolvedGestureGuiNode> {
        return when (node) {
            is GestureGuiBox -> layoutStack(node.children, content, clip, z, node.id, node.gap, node.overflow, node.crossAlignment, ctx)
            is GestureGuiColumn -> layoutStack(node.children, content, clip, z, node.id, node.gap, node.overflow, node.crossAlignment, ctx, node.mainArrangement)
            is GestureGuiRow -> layoutRow(node, content, clip, z, ctx)
            is GestureGuiGrid -> layoutGrid(node, content, clip, z, ctx)
            is GestureGuiOverlay -> layoutOverlay(node, content, clip, z, ctx)
            is GestureGuiViewport -> {
                val contentBorder = contentBorderFill(node.content, content, node.id, ctx)
                reportOverflow(contentBorder, content, node.overflow, node.content.id, node.id, ctx)
                listOf(
                    layoutNode(
                        node.content,
                        contentBorder,
                        clip,
                        z,
                        node.id,
                        ctx,
                    ),
                )
            }
            is GestureGuiText,
            is GestureGuiBlock,
            is GestureGuiItem,
            is GestureGuiCustom,
            -> emptyList()
        }
    }

    // ─── 縦積み（Box / Column） ──────────────────────────────

    private fun layoutStack(
        children: List<GestureGuiNode>,
        content: GestureGuiBounds,
        clip: GestureGuiClip,
        z: Int,
        parentId: String?,
        gap: Double,
        overflow: GestureGuiOverflow,
        cross: GestureGuiCrossAlignment,
        ctx: Context,
        main: GestureGuiMainArrangement = GestureGuiMainArrangement.START,
    ): List<ResolvedGestureGuiNode> {
        val inFlow = children.filter { it.absolute == null }
        val absolute = children.filter { it.absolute != null }
        if (inFlow.isEmpty()) {
            return absolute.map { layoutAbsolute(it, content, clip, z, parentId, overflow, ctx) }
        }
        // 測定時と異なり配置時は親の内容高さが確定しています。
        // Percent は内容高さ基準、Fraction は余白・gap・確定寸法を引いた残りを配分します。
        val sizes = inFlow.map { child ->
            when (val spec = child.height) {
                is GestureGuiSizeSpec.Fixed -> spec.value
                is GestureGuiSizeSpec.Percent -> spec.ratio * content.height()
                is GestureGuiSizeSpec.Fraction -> 0.0
                is GestureGuiSizeSpec.Auto -> measureHeight(child, parentId, ctx)
            }
        }.toMutableList()
        val reserved = sizes.sum() + inFlow.sumOf { it.margin.vertical } +
            gap * (inFlow.size - 1).coerceAtLeast(0)
        val remaining = (content.height() - reserved).coerceAtLeast(0.0)
        val weights = inFlow.map { (it.height as? GestureGuiSizeSpec.Fraction)?.weight ?: 0.0 }
        // 大きな有限重みでも合計が Infinity にならないよう最大重みで正規化します。
        val maxWeight = weights.maxOrNull() ?: 0.0
        if (maxWeight > 0.0) {
            val totalWeight = weights.sumOf { it / maxWeight }
            weights.forEachIndexed { index, weight ->
                if (weight > 0.0) sizes[index] = remaining * (weight / maxWeight) / totalWeight
            }
        }
        val mains = sizes.mapIndexed { index, size -> size + inFlow[index].margin.vertical }
        val total = mains.sum() + gap * (inFlow.size - 1).coerceAtLeast(0)
        var cursorTop = when (main) {
            GestureGuiMainArrangement.START -> content.maxY
            GestureGuiMainArrangement.CENTER -> content.maxY - ((content.height() - total) / 2.0).coerceAtLeast(0.0)
            GestureGuiMainArrangement.END -> content.minY + total
            GestureGuiMainArrangement.SPACE_BETWEEN -> content.maxY
        }
        val effectiveGap = if (main == GestureGuiMainArrangement.SPACE_BETWEEN && inFlow.size > 1) {
            gap + ((content.height() - total) / (inFlow.size - 1)).coerceAtLeast(0.0)
        } else {
            gap
        }
        val placed = inFlow.mapIndexed { index, child ->
            if (index > 0) cursorTop -= effectiveGap
            val childMain = mains[index] - child.margin.vertical
            val childMaxY = cursorTop - child.margin.top
            val childMinY = childMaxY - childMain
            val crossW = resolveCrossWidth(child, content.width(), cross, parentId, ctx)
            val childMinX = crossOffset(child, content, crossW, cross) + child.margin.left
            val border = rectOf(childMinX, childMinY, childMinX + crossW, childMaxY)
            reportOverflow(border, content, overflow, child.id, parentId, ctx)
            cursorTop = childMinY - child.margin.bottom
            layoutNode(child, border, clip, z, parentId, ctx)
        }
        return placed + absolute.map { layoutAbsolute(it, content, clip, z, parentId, overflow, ctx) }
    }

    private fun resolveCrossWidth(
        child: GestureGuiNode,
        contentWidth: Double,
        cross: GestureGuiCrossAlignment,
        parentId: String?,
        ctx: Context,
    ): Double {
        if (child.absolute != null) return contentWidth
        return when (val spec = child.width) {
            is GestureGuiSizeSpec.Fixed -> spec.value
            is GestureGuiSizeSpec.Percent -> spec.ratio * contentWidth
            is GestureGuiSizeSpec.Fraction -> {
                ctx.add(
                    GestureGuiLayoutErrorCode.INVALID_SIZE,
                    child.id,
                    parentId,
                    "fraction width on cross axis resolves to 0: ${child.id ?: "<anonymous>"}",
                )
                0.0
            }
            is GestureGuiSizeSpec.Auto -> when (child) {
                is GestureGuiText -> contentWidth - child.margin.horizontal
                is GestureGuiBox,
                is GestureGuiRow,
                is GestureGuiColumn,
                is GestureGuiGrid,
                is GestureGuiOverlay,
                is GestureGuiViewport,
                -> if (cross == GestureGuiCrossAlignment.STRETCH) {
                    contentWidth - child.margin.horizontal
                } else {
                    // 横 wrap は未対応のため fill します（Profile 1 の制限）。
                    contentWidth - child.margin.horizontal
                }
                is GestureGuiBlock,
                is GestureGuiItem,
                is GestureGuiCustom,
                -> if (cross == GestureGuiCrossAlignment.STRETCH) {
                    contentWidth - child.margin.horizontal
                } else {
                    ctx.add(
                        GestureGuiLayoutErrorCode.AUTO_SIZE_UNSUPPORTED,
                        child.id,
                        parentId,
                        "auto width of non-text leaf resolves to 0, specify an explicit size: ${child.id ?: "<anonymous>"}",
                    )
                    0.0
                }
            }
        }.coerceAtLeast(MIN_EXTENT)
    }

    private fun crossOffset(
        child: GestureGuiNode,
        content: GestureGuiBounds,
        crossWidth: Double,
        cross: GestureGuiCrossAlignment,
    ): Double {
        val free = (content.width() - crossWidth - child.margin.horizontal).coerceAtLeast(0.0)
        return content.minX + when (cross) {
            GestureGuiCrossAlignment.START -> 0.0
            GestureGuiCrossAlignment.CENTER -> free / 2.0
            GestureGuiCrossAlignment.END -> free
            GestureGuiCrossAlignment.STRETCH -> 0.0
        }
    }

    // ─── Row ─────────────────────────────────────────────────

    private fun layoutRow(
        node: GestureGuiRow,
        content: GestureGuiBounds,
        clip: GestureGuiClip,
        z: Int,
        ctx: Context,
    ): List<ResolvedGestureGuiNode> {
        val inFlow = node.children.filter { it.absolute == null }
        val absolute = node.children.filter { it.absolute != null }
        if (inFlow.isEmpty()) {
            return absolute.map { layoutAbsolute(it, content, clip, z, node.id, node.overflow, ctx) }
        }
        // 主軸（横）: Fixed / Percent 確定 → 残りを Fraction と Auto(Text含む) で配分します。
        data class Main(val child: GestureGuiNode, var size: Double, val weight: Double)
        val mains = inFlow.map { child ->
            when (val spec = child.width) {
                is GestureGuiSizeSpec.Fixed -> Main(child, spec.value, 0.0)
                is GestureGuiSizeSpec.Percent -> Main(child, spec.ratio * content.width(), 0.0)
                is GestureGuiSizeSpec.Fraction -> Main(child, 0.0, spec.weight)
                is GestureGuiSizeSpec.Auto -> when (child) {
                    is GestureGuiText -> Main(child, 0.0, 1.0)
                    is GestureGuiBox,
                    is GestureGuiRow,
                    is GestureGuiColumn,
                    is GestureGuiGrid,
                    is GestureGuiOverlay,
                    is GestureGuiViewport,
                    -> Main(child, 0.0, 1.0)
                    is GestureGuiBlock,
                    is GestureGuiItem,
                    is GestureGuiCustom,
                    -> {
                        ctx.add(
                            GestureGuiLayoutErrorCode.AUTO_SIZE_UNSUPPORTED,
                            child.id,
                            node.id,
                            "auto width of non-text leaf resolves to 0, specify an explicit size: ${child.id ?: "<anonymous>"}",
                        )
                        Main(child, 0.0, 0.0)
                    }
                }
            }
        }
        val fixedSum = mains.sumOf { it.size + it.child.margin.horizontal } +
            node.gap * (inFlow.size - 1).coerceAtLeast(0)
        val remaining = (content.width() - fixedSum).coerceAtLeast(0.0)
        val totalWeight = mains.sumOf { it.weight }
        if (totalWeight > 0.0) {
            mains.filter { it.weight > 0.0 }.forEach { it.size = remaining * it.weight / totalWeight }
        }
        // 交差軸（縦）: Auto は STRETCH で fill、それ以外は概算 wrap です。
        fun crossHeight(child: GestureGuiNode): Double = when (val spec = child.height) {
            is GestureGuiSizeSpec.Fixed -> spec.value
            is GestureGuiSizeSpec.Percent -> spec.ratio * content.height()
            is GestureGuiSizeSpec.Fraction -> {
                ctx.add(
                    GestureGuiLayoutErrorCode.INVALID_SIZE,
                    child.id,
                    node.id,
                    "fraction height on cross axis resolves to 0: ${child.id ?: "<anonymous>"}",
                )
                0.0
            }
            is GestureGuiSizeSpec.Auto -> when (child) {
                is GestureGuiText -> textHeight(child)
                is GestureGuiBox,
                is GestureGuiRow,
                is GestureGuiColumn,
                is GestureGuiGrid,
                is GestureGuiOverlay,
                is GestureGuiViewport,
                -> if (node.crossAlignment == GestureGuiCrossAlignment.STRETCH) {
                    content.height() - child.margin.vertical
                } else {
                    measureHeight(child, node.id, ctx)
                }
                is GestureGuiBlock,
                is GestureGuiItem,
                is GestureGuiCustom,
                -> if (node.crossAlignment == GestureGuiCrossAlignment.STRETCH) {
                    content.height() - child.margin.vertical
                } else {
                    ctx.add(
                        GestureGuiLayoutErrorCode.AUTO_SIZE_UNSUPPORTED,
                        child.id,
                        node.id,
                        "auto height of non-text leaf resolves to 0, specify an explicit size: ${child.id ?: "<anonymous>"}",
                    )
                    0.0
                }
            }
        }.coerceAtLeast(MIN_EXTENT)
        val heights = mains.map { crossHeight(it.child) }
        val usedWidth = mains.sumOf { it.size + it.child.margin.horizontal } +
            node.gap * (inFlow.size - 1).coerceAtLeast(0)
        var cursorLeft = when (node.mainArrangement) {
            GestureGuiMainArrangement.START -> content.minX
            GestureGuiMainArrangement.CENTER -> content.minX + ((content.width() - usedWidth) / 2.0).coerceAtLeast(0.0)
            GestureGuiMainArrangement.END -> content.maxX - usedWidth
            GestureGuiMainArrangement.SPACE_BETWEEN -> content.minX
        }
        val effectiveGap = if (node.mainArrangement == GestureGuiMainArrangement.SPACE_BETWEEN && inFlow.size > 1) {
            node.gap + ((content.width() - usedWidth) / (inFlow.size - 1)).coerceAtLeast(0.0)
        } else {
            node.gap
        }
        val placed = mains.mapIndexed { index, main ->
            if (index > 0) cursorLeft += effectiveGap
            cursorLeft += main.child.margin.left
            val height = heights[index]
            val minY = when (node.crossAlignment) {
                GestureGuiCrossAlignment.START -> content.minY + main.child.margin.bottom
                GestureGuiCrossAlignment.CENTER -> content.minY + ((content.height() - height) / 2.0).coerceAtLeast(0.0)
                GestureGuiCrossAlignment.END -> content.maxY - height - main.child.margin.top
                GestureGuiCrossAlignment.STRETCH -> content.minY + main.child.margin.bottom
            }
            val border = rectOf(cursorLeft, minY, cursorLeft + main.size, minY + height)
            reportOverflow(border, content, node.overflow, main.child.id, node.id, ctx)
            cursorLeft += main.size + main.child.margin.right
            layoutNode(main.child, border, clip, z, node.id, ctx)
        }
        return placed + absolute.map { layoutAbsolute(it, content, clip, z, node.id, node.overflow, ctx) }
    }

    // ─── Grid ────────────────────────────────────────────────

    private fun layoutGrid(
        node: GestureGuiGrid,
        content: GestureGuiBounds,
        clip: GestureGuiClip,
        z: Int,
        ctx: Context,
    ): List<ResolvedGestureGuiNode> {
        val inFlow = node.children.filter { it.absolute == null }
        val absolute = node.children.filter { it.absolute != null }
        if (inFlow.isEmpty()) {
            return absolute.map { layoutAbsolute(it, content, clip, z, node.id, node.overflow, ctx) }
        }
        val rows = node.rows ?: ((inFlow.size + node.columns - 1) / node.columns).coerceAtLeast(1)
        val cellW = (content.width() - node.gap * (node.columns - 1).coerceAtLeast(0)) / node.columns
        val cellH = (content.height() - node.crossGap * (rows - 1).coerceAtLeast(0)) / rows
        val placed = inFlow.mapIndexed { index, child ->
            val column = index % node.columns
            val row = index / node.columns
            if (row >= rows) {
                ctx.add(
                    GestureGuiLayoutErrorCode.LAYOUT_OVERFLOW,
                    child.id,
                    node.id,
                    "node ${child.id ?: "<anonymous>"} exceeds grid rows of ${node.id ?: "<grid>"}",
                )
            }
            val cellMinX = content.minX + column * (cellW + node.gap)
            val cellMaxY = content.maxY - row * (cellH + node.crossGap)
            val cell = rectOf(cellMinX, cellMaxY - cellH, cellMinX + cellW, cellMaxY)
            val border = cellBorderFill(child, cell, node.id, ctx)
            reportOverflow(border, content, node.overflow, child.id, node.id, ctx)
            layoutNode(child, border, clip, z, node.id, ctx)
        }
        return placed + absolute.map { layoutAbsolute(it, content, clip, z, node.id, node.overflow, ctx) }
    }

    /** Grid / Overlay セル内では Auto を fill として扱います（診断なし）。 */
    private fun cellBorderFill(child: GestureGuiNode, cell: GestureGuiBounds, parentId: String?, ctx: Context): GestureGuiBounds {
        val width = when (val spec = child.width) {
            is GestureGuiSizeSpec.Fixed -> spec.value
            is GestureGuiSizeSpec.Percent -> spec.ratio * cell.width()
            is GestureGuiSizeSpec.Fraction -> cell.width() - child.margin.horizontal
            is GestureGuiSizeSpec.Auto -> cell.width() - child.margin.horizontal
        }.coerceAtLeast(MIN_EXTENT)
        val height = when (val spec = child.height) {
            is GestureGuiSizeSpec.Fixed -> spec.value
            is GestureGuiSizeSpec.Percent -> spec.ratio * cell.height()
            is GestureGuiSizeSpec.Fraction -> cell.height() - child.margin.vertical
            is GestureGuiSizeSpec.Auto -> when (child) {
                is GestureGuiText -> textHeight(child)
                else -> cell.height() - child.margin.vertical
            }
        }.coerceAtLeast(MIN_EXTENT)
        return rectOf(
            cell.minX + child.margin.left,
            cell.minY + child.margin.bottom,
            cell.minX + child.margin.left + width,
            cell.minY + child.margin.bottom + height,
        )
    }

    // ─── Overlay ─────────────────────────────────────────────

    private fun layoutOverlay(
        node: GestureGuiOverlay,
        content: GestureGuiBounds,
        clip: GestureGuiClip,
        z: Int,
        ctx: Context,
    ): List<ResolvedGestureGuiNode> {
        val inFlow = node.children.filter { it.absolute == null }
        val absolute = node.children.filter { it.absolute != null }
        val placed = inFlow.mapIndexed { index, child ->
            val border = cellBorderFill(child, content, node.id, ctx)
            // 固定寸の子は配置指定で寄せます。fill の子は原点のままです。
            val freeW = (content.width() - (border.maxX - border.minX) - child.margin.horizontal).coerceAtLeast(0.0)
            val freeH = (content.height() - (border.maxY - border.minY) - child.margin.vertical).coerceAtLeast(0.0)
            val shiftX = when (node.crossAlignment) {
                GestureGuiCrossAlignment.START -> 0.0
                GestureGuiCrossAlignment.CENTER -> freeW / 2.0
                GestureGuiCrossAlignment.END -> freeW
                GestureGuiCrossAlignment.STRETCH -> 0.0
            }
            val shiftY = when (node.mainArrangement) {
                GestureGuiMainArrangement.START -> 0.0
                GestureGuiMainArrangement.CENTER -> freeH / 2.0
                GestureGuiMainArrangement.END -> freeH
                GestureGuiMainArrangement.SPACE_BETWEEN -> 0.0
            }
            val shifted = rectOf(
                border.minX + shiftX,
                border.minY + shiftY,
                border.maxX + shiftX,
                border.maxY + shiftY,
            )
            reportOverflow(shifted, content, node.overflow, child.id, node.id, ctx)
            layoutNode(child, shifted, clip, z + index, node.id, ctx)
        }
        return placed + absolute.map { layoutAbsolute(it, content, clip, z, node.id, node.overflow, ctx) }
    }

    // ─── Viewport 内容・絶対配置 ─────────────────────────────

    private fun contentBorderFill(
        child: GestureGuiNode,
        content: GestureGuiBounds,
        parentId: String?,
        ctx: Context,
    ): GestureGuiBounds {
        val width = when (val spec = child.width) {
            is GestureGuiSizeSpec.Fixed -> spec.value
            is GestureGuiSizeSpec.Percent -> spec.ratio * content.width()
            is GestureGuiSizeSpec.Fraction -> content.width() - child.margin.horizontal
            is GestureGuiSizeSpec.Auto -> content.width() - child.margin.horizontal
        }.coerceAtLeast(MIN_EXTENT)
        val height = when (val spec = child.height) {
            is GestureGuiSizeSpec.Fixed -> spec.value
            is GestureGuiSizeSpec.Percent -> spec.ratio * content.height()
            is GestureGuiSizeSpec.Fraction -> content.height() - child.margin.vertical
            is GestureGuiSizeSpec.Auto -> when (child) {
                is GestureGuiText -> textHeight(child)
                is GestureGuiBox,
                is GestureGuiRow,
                is GestureGuiColumn,
                is GestureGuiGrid,
                is GestureGuiOverlay,
                is GestureGuiViewport,
                -> measureHeight(child, parentId, ctx).coerceAtMost(content.height() - child.margin.vertical)
                is GestureGuiBlock,
                is GestureGuiItem,
                is GestureGuiCustom,
                -> content.height() - child.margin.vertical
            }
        }.coerceAtLeast(MIN_EXTENT)
        return rectOf(
            content.minX + child.margin.left,
            content.maxY - child.margin.top - height,
            content.minX + child.margin.left + width,
            content.maxY - child.margin.top,
        )
    }

    private fun layoutAbsolute(
        child: GestureGuiNode,
        content: GestureGuiBounds,
        clip: GestureGuiClip,
        z: Int,
        parentId: String?,
        overflow: GestureGuiOverflow,
        ctx: Context,
    ): ResolvedGestureGuiNode {
        val offsets: GestureGuiAbsoluteOffsets = child.absolute
            ?: return layoutNode(child, content, clip, z, parentId, ctx)
        val width = when (val spec = child.width) {
            is GestureGuiSizeSpec.Fixed -> spec.value
            is GestureGuiSizeSpec.Percent -> spec.ratio * content.width()
            is GestureGuiSizeSpec.Fraction -> {
                ctx.add(
                    GestureGuiLayoutErrorCode.INVALID_SIZE,
                    child.id,
                    parentId,
                    "fraction size on absolute node resolves to 0: ${child.id ?: "<anonymous>"}",
                )
                0.0
            }
            is GestureGuiSizeSpec.Auto -> if (offsets.left != null && offsets.right != null) {
                content.width() - offsets.left - offsets.right - child.margin.horizontal
            } else {
                content.width() - (offsets.left ?: 0.0) - (offsets.right ?: 0.0) - child.margin.horizontal
            }
        }.coerceAtLeast(MIN_EXTENT)
        val height = when (val spec = child.height) {
            is GestureGuiSizeSpec.Fixed -> spec.value
            is GestureGuiSizeSpec.Percent -> spec.ratio * content.height()
            is GestureGuiSizeSpec.Fraction -> {
                ctx.add(
                    GestureGuiLayoutErrorCode.INVALID_SIZE,
                    child.id,
                    parentId,
                    "fraction size on absolute node resolves to 0: ${child.id ?: "<anonymous>"}",
                )
                0.0
            }
            is GestureGuiSizeSpec.Auto -> if (child is GestureGuiText) {
                textHeight(child)
            } else if (offsets.top != null && offsets.bottom != null) {
                content.height() - offsets.top - offsets.bottom - child.margin.vertical
            } else {
                when (child) {
                    is GestureGuiBox,
                    is GestureGuiRow,
                    is GestureGuiColumn,
                    is GestureGuiGrid,
                    is GestureGuiOverlay,
                    is GestureGuiViewport,
                    -> measureHeight(child, parentId, ctx)
                    is GestureGuiBlock,
                    is GestureGuiItem,
                    is GestureGuiCustom,
                    -> {
                        ctx.add(
                            GestureGuiLayoutErrorCode.AUTO_SIZE_UNSUPPORTED,
                            child.id,
                            parentId,
                            "auto size of non-text leaf resolves to 0, specify an explicit size: ${child.id ?: "<anonymous>"}",
                        )
                        0.0
                    }
                    is GestureGuiText -> textHeight(child)
                }
            }
        }.coerceAtLeast(MIN_EXTENT)
        val minX = if (offsets.left != null) {
            content.minX + offsets.left + child.margin.left
        } else if (offsets.right != null) {
            content.maxX - offsets.right - child.margin.right - width
        } else {
            content.minX + child.margin.left
        }
        // top は内容域上端基準、bottom は内容域下端基準です（Y は上方向正）。
        val maxY = if (offsets.top != null) {
            content.maxY - offsets.top - child.margin.top
        } else if (offsets.bottom != null) {
            content.minY + offsets.bottom + child.margin.bottom + height
        } else {
            content.maxY - child.margin.top
        }
        val border = rectOf(minX, maxY - height, minX + width, maxY)
        reportOverflow(border, content, overflow, child.id, parentId, ctx)
        return layoutNode(child, border, clip, z, parentId, ctx)
    }
}
