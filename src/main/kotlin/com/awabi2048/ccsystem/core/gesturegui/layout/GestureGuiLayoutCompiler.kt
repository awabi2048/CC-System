package com.awabi2048.ccsystem.core.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiAccess
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiActionContext
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiBounds
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiElement
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenDefinition
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiView
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVisual
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiBlock
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCustom
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCustomElement
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiClip
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHover
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHoverBlock
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHoverPosition
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHoverTarget
import com.awabi2048.ccsystem.core.gesturegui.GestureGuiOutlineGeometry
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCustomRenderer
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiItem
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutDiagnostic
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutErrorCode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiText
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiHoverText
import com.awabi2048.ccsystem.api.gesturegui.layout.ResolvedGestureGui
import com.awabi2048.ccsystem.api.gesturegui.layout.ResolvedGestureGuiNode
import java.util.IdentityHashMap
import java.util.UUID

/**
 * 解決済みレイアウトを既存 Low-level API へ変換します。
 *
 * 既存 `GestureGuiView` / `GestureGuiVisual` / `GestureGuiElement` は
 * renderer IR / Low-level API として維持し、この compiler の出力先とします。
 * interaction bounds は解決結果から自動生成し、visual と hitbox の二重管理を避けます。
 */
object GestureGuiLayoutCompiler {
    const val MAX_LAYER: Int = 40

    /**
     * 解決済み文書を1画面へ変換します。
     *
     * @param actionHandlers action ID から処理への対応表です。HTML の `action="..."`
     * 属性や宣言ノードの actionId はここへ接続します。
     * @param customRenderers rendererId から Custom renderer への対応表です。
     * @param source 診断用の発生源表記です（例 "editor.html:42"、直接構築時は null）。
     */
    fun compile(
        resolved: ResolvedGestureGui,
        screenId: String,
        actionHandlers: Map<String, (GestureGuiActionContext) -> Unit> = emptyMap(),
        customRenderers: Map<String, GestureGuiCustomRenderer> = emptyMap(),
        access: GestureGuiAccess = GestureGuiAccess.OWNER_ONLY,
        allowlist: Set<UUID> = emptySet(),
        source: String? = null,
    ): GestureGuiCompiledScreen {
        require(screenId.isNotBlank()) { "compiled screenId must not be blank" }
        val ctx = Context(actionHandlers, customRenderers, source)
        ctx.diagnostics += resolved.diagnostics
        // 第1走査で表示を確定し、第2走査で操作面・ホバーを作ります。
        // ホバーの Node 参照が前方ノードを指せるよう、visual ID 対応を先に完成させます。
        visitVisuals(resolved.root, ctx)
        visitElements(resolved.root, ctx)
        val definition = GestureGuiScreenDefinition(
            screenId = screenId,
            elements = ctx.elements.toList(),
            access = access,
            allowlist = allowlist,
        )
        val view = GestureGuiView(
            definition = definition,
            visuals = ctx.visuals.toList(),
            panel = resolved.panel,
            onAction = { action ->
                ctx.elementActions[action.elementId]?.let { actionId ->
                    ctx.actionHandlers[actionId]?.invoke(action)
                }
            },
        )
        return GestureGuiCompiledScreen(view, ctx.diagnostics.toList())
    }

    private class Context(
        val actionHandlers: Map<String, (GestureGuiActionContext) -> Unit>,
        val customRenderers: Map<String, GestureGuiCustomRenderer>,
        val source: String?,
    ) {
        val visuals = mutableListOf<GestureGuiVisual>()
        val elements = mutableListOf<GestureGuiElement>()
        val elementActions = mutableMapOf<String, String>()
        val diagnostics = mutableListOf<GestureGuiLayoutDiagnostic>()
        val usedIds = mutableSetOf<String>()
        var autoCounter = 0
        /** 生成・可視化された visual の対応です（ノード同一性基準、前方参照の解決用）。 */
        val nodeVisuals = IdentityHashMap<ResolvedGestureGuiNode, String>()
        val nodeVisualObjs = IdentityHashMap<ResolvedGestureGuiNode, GestureGuiVisual>()
        val nodeVisible = IdentityHashMap<ResolvedGestureGuiNode, Boolean>()
        /** 宣言ノード ID から visual ID への対応です（ホバー置換の解決用、先勝ち）。 */
        val nodeIdVisuals = mutableMapOf<String, String>()
        /** Custom スコープの visual ID 対応です（ホバー参照の名前空間化用）。 */
        val customVisualMaps = IdentityHashMap<ResolvedGestureGuiNode, Map<String, String>>()
        /** 第1走査で実行した Custom renderer の出力です（第2走査の再実行を避けます）。 */
        val customOutputs = IdentityHashMap<ResolvedGestureGuiNode, CustomOutput>()

        /** 画面内一意の ID を払い出します。重複時は接尾辞を付けて衝突を回避します。 */
        fun uniqueId(base: String?): String {
            var candidate = base?.takeIf(String::isNotBlank) ?: run {
                autoCounter += 1
                "auto-$autoCounter"
            }
            while (!usedIds.add(candidate)) {
                autoCounter += 1
                candidate = "${base ?: "auto"}-$autoCounter"
            }
            return candidate
        }

        fun add(code: GestureGuiLayoutErrorCode, nodeId: String?, parentId: String?, message: String) {
            diagnostics += GestureGuiLayoutDiagnostic(code, nodeId, parentId, message, source)
        }
    }

    /** 第1走査で確定した Custom renderer の出力です。 */
    private data class CustomOutput(
        val visualIdMap: Map<String, String>,
        val declaredIds: Set<String>,
        val elements: List<GestureGuiCustomElement>,
    )

    private fun layerOf(resolvedZ: Int): Int = resolvedZ.coerceAtLeast(0).coerceAtMost(MAX_LAYER - 1) + 1

    /**
     * 同一要素内の文言・品目を背景より少し浮かせます。
     *
     * 解決深度が同じ兄弟は同一層になるため、Block背景とText・Itemが同一面で
     * z-fightingを起こします。旧来の層分離（背景4・文言20等）相当として、
     * 非Block系リーフだけ1層前面へ置きます。枠線（背景＋1）と面が重なるのは
     * 縁のみで、中央の文言・品目とは干渉しません。
     */
    private fun floatLayer(resolvedZ: Int): Int = (layerOf(resolvedZ) + 1).coerceAtMost(MAX_LAYER)

    private fun centerX(bounds: GestureGuiBounds): Double = (bounds.minX + bounds.maxX) / 2.0

    private fun centerY(bounds: GestureGuiBounds): Double = (bounds.minY + bounds.maxY) / 2.0

    /** 第1走査で表示を確定します。操作面・ホバーは第2走査で作ります。 */
    private fun visitVisuals(node: ResolvedGestureGuiNode, ctx: Context): Boolean {
        if (node.effectiveClip == GestureGuiClip.Empty) {
            nodeVisible(node, false, ctx)
            return false
        }
        val bounds = node.borderBounds
        val declared = node.source
        if (declared is GestureGuiCustom) return visitCustomVisuals(node, declared, ctx)
        val visualId = ctx.uniqueId(node.nodeId?.let { "visual-$it" })
        val visual = when (declared) {
            is GestureGuiText -> GestureGuiVisual.Text(
                visualId, centerX(bounds), centerY(bounds), declared.text,
                declared.size, declared.lineWidth, floatLayer(node.resolvedZ),
                declared.seeThrough, declared.alignment,
            )
            is GestureGuiBlock -> GestureGuiVisual.Block(
                visualId, centerX(bounds), centerY(bounds),
                bounds.maxX - bounds.minX, bounds.maxY - bounds.minY,
                declared.blockData, layerOf(node.resolvedZ), declared.glowColor, declared.outline,
            )
            is GestureGuiItem -> GestureGuiVisual.Item(
                visualId, centerX(bounds), centerY(bounds), declared.item.clone(),
                declared.scale, floatLayer(node.resolvedZ), declared.glowColor,
            )
            else -> null
        }
        if (visual != null) {
            val visible = emitVisual(visual, bounds, node.effectiveClip, ctx)
            nodeVisible(node, visible, ctx)
            if (visible) {
                ctx.nodeVisuals[node] = visualId
                ctx.nodeVisualObjs[node] = visual
                node.nodeId?.let { ctx.nodeIdVisuals.putIfAbsent(it, visualId) }
            }
            return visible
        }
        // 全ての子の表示が消えた複合部品に、透明な操作面だけを残さないようにします。
        val visible = node.children.map { visitVisuals(it, ctx) }.any { it }
        nodeVisible(node, visible || node.children.isEmpty(), ctx)
        return visible || node.children.isEmpty()
    }

    private fun nodeVisible(node: ResolvedGestureGuiNode, visible: Boolean, ctx: Context) {
        ctx.nodeVisible[node] = visible
    }

    /** 第2走査で操作面・ホバーを作ります。 */
    private fun visitElements(node: ResolvedGestureGuiNode, ctx: Context) {
        val declared = node.source
        if (declared is GestureGuiCustom) {
            visitCustomElements(node, declared, ctx)
            return
        }
        val targetVisualId = ctx.nodeVisuals[node]
        if (targetVisualId != null || node.children.isEmpty()) {
            if (ctx.nodeVisible[node] == false) clippedInteraction(node, ctx)
            else addElement(node, targetVisualId, ctx)
        } else if (node.children.isNotEmpty()) {
            // 子が全て消えた容器には操作面を作りません（第1走査の可視性に従います）。
            if (ctx.nodeVisible[node] == false) clippedInteraction(node, ctx)
            else addElement(node, targetVisualId = null, ctx = ctx)
        }
        node.children.forEach { visitElements(it, ctx) }
    }

    private fun clippedInteraction(node: ResolvedGestureGuiNode, ctx: Context) {
        if (node.interactionBounds != null) ctx.add(
            GestureGuiLayoutErrorCode.CLIPPED_INTERACTION, node.nodeId, null,
            "interaction removed with clipped visual: ${node.nodeId ?: "<anonymous>"}",
        )
    }

    /**
     * 通常ノードと Custom 出力の共通切り抜き口です。
     * Text / Item は画素の部分切り抜きを持たないため、表示矩形全体が収まる場合だけ残します。
     * bounds は宣言ノードの割当領域、または Custom renderer が申告する表示矩形です。
     */
    private fun emitVisual(
        visual: GestureGuiVisual,
        bounds: GestureGuiBounds,
        clip: GestureGuiClip,
        ctx: Context,
    ): Boolean {
        if (visual !is GestureGuiVisual.Block) {
            if (!clip.contains(bounds)) return false
            ctx.visuals += visual
            return true
        }
        val cropped = clip.clip(bounds) ?: return false
        val outline = visual.outline
        val thickness = outline?.let { minOf(visual.width, visual.height) * it.thicknessRatio } ?: 0.0
        val outlinedBounds = GestureGuiBounds(bounds.minX - thickness, bounds.minY, bounds.maxX + thickness, bounds.maxY)
        if (clip.contains(outlinedBounds)) {
            ctx.visuals += visual
            return true
        }
        ctx.visuals += visual.copy(
            x = centerX(cropped), y = centerY(cropped),
            width = cropped.maxX - cropped.minX, height = cropped.maxY - cropped.minY, outline = null,
        )
        // 枠は元の四辺を個別に切ります。切断面へ新しい枠を描いたり、左右枠が領域外へ漏れたりしません。
        if (outline != null) GestureGuiOutlineGeometry.segments(
            visual.width, visual.height, outline.thicknessRatio,
        ).forEachIndexed { index, segment ->
            val x = visual.x + segment.x
            val y = visual.y + segment.y
            val rect = clip.clip(GestureGuiBounds(
                x - segment.width / 2.0, y - segment.height / 2.0,
                x + segment.width / 2.0, y + segment.height / 2.0,
            )) ?: return@forEachIndexed
            ctx.visuals += GestureGuiVisual.Block(
                visualId = ctx.uniqueId("${visual.visualId}/outline-$index"),
                x = centerX(rect), y = centerY(rect),
                width = rect.maxX - rect.minX, height = rect.maxY - rect.minY,
                blockData = outline.blockData, layer = (visual.layer + 1).coerceAtMost(MAX_LAYER),
            )
        }
        return true
    }

    /**
     * 宣言ノード由来の操作面を追加します。
     *
     * interaction が null（clip 消滅等）の場合は Engine 側の診断に委ねて生成しません。
     * gestureGuard は入力時再評価のためそのまま受渡し、画面再構築は不要です。
     * actionId のない consumeInput ノードは、受付 gesture 空の無音消費面を作ります。
     */
    private fun addElement(node: ResolvedGestureGuiNode, targetVisualId: String?, ctx: Context) {
        val declared = node.source
        val actionId = node.actionId
        // 消費面は action なしの入力管理（余白解除・バリア等）に用います。
        val consumeOnly = actionId == null && declared.consumeInput
        if (actionId == null && !consumeOnly) return
        val interaction = node.interactionBounds ?: return
        val gestures = declared.acceptedGestures
        if (actionId != null) {
            if (gestures.isEmpty()) return
            if (actionId !in ctx.actionHandlers) {
                ctx.add(
                    GestureGuiLayoutErrorCode.UNKNOWN_ACTION,
                    node.nodeId,
                    null,
                    "unknown action reference: $actionId",
                )
                return
            }
        }
        val elementId = ctx.uniqueId(node.nodeId ?: "element")
        val hover = buildHover(node, targetVisualId, ctx)
        ctx.elements += GestureGuiElement(
            elementId = elementId,
            bounds = interaction,
            acceptedGestures = if (consumeOnly) emptySet() else gestures,
            hoverText = hover,
            targetVisualId = targetVisualId,
            gestureGuard = declared.gestureGuard,
        )
        if (actionId != null) ctx.elementActions[elementId] = actionId
    }

    /**
     * 宣言ホバーを低レベル hoverText へ変換します。
     *
     * 自動位置は解決済み border bounds 基準、固定位置は画面中央原点のまま用います。
     * tooltip は意図的な逸脱として扱い、元ノードの clip では切りません。
     */
    private fun buildHover(
        node: ResolvedGestureGuiNode,
        ownVisualId: String?,
        ctx: Context,
    ): GestureGuiHoverText? {
        val spec: GestureGuiHover = node.source.hover ?: return null
        val border = node.borderBounds
        val (x, y) = when (val position = spec.position) {
            is GestureGuiHoverPosition.Fixed -> position.x to position.y
            is GestureGuiHoverPosition.Auto -> {
                val center = centerX(border) to centerY(border)
                when (position.anchor) {
                    GestureGuiHoverPosition.Anchor.ABOVE -> centerX(border) to border.maxY + position.gap
                    GestureGuiHoverPosition.Anchor.BELOW -> centerX(border) to border.minY - position.gap
                    GestureGuiHoverPosition.Anchor.CENTER -> center
                }
            }
        }
        val replaces = when (val target = spec.replacement) {
            is GestureGuiHoverTarget.None -> null
            is GestureGuiHoverTarget.Self -> ownVisualId ?: run {
                ctx.add(
                    GestureGuiLayoutErrorCode.UNKNOWN_VISUAL,
                    node.nodeId,
                    null,
                    "hover replacement has no visual: ${node.nodeId ?: "<anonymous>"}",
                )
                null
            }
            is GestureGuiHoverTarget.Node -> ctx.nodeIdVisuals[target.nodeId] ?: run {
                ctx.add(
                    GestureGuiLayoutErrorCode.UNKNOWN_VISUAL,
                    node.nodeId,
                    null,
                    "unknown hover replacement node: ${target.nodeId}",
                )
                null
            }
        }
        var hoverBlockId: String? = null
        var hoverBlockData: org.bukkit.block.data.BlockData? = null
        spec.blockReplacement?.let { replacement ->
            val targetId = when (val target = replacement.target) {
                is GestureGuiHoverTarget.None -> null
                is GestureGuiHoverTarget.Self -> ownVisualId
                is GestureGuiHoverTarget.Node -> ctx.nodeIdVisuals[target.nodeId]
            }
            val targetVisual = targetId?.let { id ->
                ctx.nodeVisualObjs.entries.firstOrNull { it.value.visualId == id }?.value
            }
            if (targetId != null && targetVisual is GestureGuiVisual.Block) {
                hoverBlockId = targetId
                hoverBlockData = replacement.blockData
            } else {
                ctx.add(
                    GestureGuiLayoutErrorCode.UNKNOWN_VISUAL,
                    node.nodeId,
                    null,
                    "hover block replacement requires a Block visual",
                )
            }
        }
        // 置換対象と同じ深さ近傍へ浮かせ、対象なしは既定層へ置きます。
        val layer = if (ownVisualId != null) (layerOf(node.resolvedZ) + 2).coerceAtMost(MAX_LAYER) else 30
        return GestureGuiHoverText(
            text = spec.text,
            x = x,
            y = y,
            size = spec.size,
            lineWidth = spec.lineWidth,
            layer = layer,
            replacesVisualId = replaces,
            hoverBlockVisualId = hoverBlockId,
            hoverBlockData = hoverBlockData,
        )
    }

    private fun visitCustomVisuals(node: ResolvedGestureGuiNode, declared: GestureGuiCustom, ctx: Context): Boolean {
        val renderer = ctx.customRenderers[declared.rendererId]
        if (renderer == null) {
            ctx.add(GestureGuiLayoutErrorCode.CUSTOM_RENDERER_MISSING, node.nodeId, null,
                "custom renderer is not registered: ${declared.rendererId}")
            nodeVisible(node, false, ctx)
            return false
        }
        val prefix = node.nodeId ?: "custom"
        val result = renderer.render(declared, node)
        val visualIdMap = mutableMapOf<String, String>()
        val declaredIds = mutableSetOf<String>()
        result.visuals.forEach { visual ->
            if (!declaredIds.add(visual.visualId)) {
                ctx.add(GestureGuiLayoutErrorCode.DUPLICATE_ID, node.nodeId, null,
                    "duplicate custom visual id: ${visual.visualId}")
                return@forEach
            }
            val bounds = if (visual is GestureGuiVisual.Block) GestureGuiBounds(
                visual.x - visual.width / 2.0, visual.y - visual.height / 2.0,
                visual.x + visual.width / 2.0, visual.y + visual.height / 2.0,
            ) else result.visualBounds[visual.visualId]
            if (bounds == null && node.effectiveClip != GestureGuiClip.Unbounded) {
                ctx.add(GestureGuiLayoutErrorCode.CUSTOM_VISUAL_BOUNDS_MISSING, node.nodeId, null,
                    "custom visual bounds required for clipping: ${visual.visualId}")
                return@forEach
            }
            val mappedId = ctx.uniqueId("$prefix/${visual.visualId}")
            val renamed = when (visual) {
                is GestureGuiVisual.Block -> visual.copy(visualId = mappedId)
                is GestureGuiVisual.Text -> visual.copy(visualId = mappedId)
                is GestureGuiVisual.Item -> visual.copy(visualId = mappedId, item = visual.item.clone())
            }
            // 無制限時は矩形を参照しません。既存 low-level 出力はそのまま利用可能です。
            if (emitVisual(renamed, bounds ?: node.borderBounds, node.effectiveClip, ctx)) {
                visualIdMap[visual.visualId] = mappedId
            }
        }
        ctx.customVisualMaps[node] = visualIdMap
        ctx.customOutputs[node] = CustomOutput(visualIdMap, declaredIds, result.elements)
        val visible = visualIdMap.isNotEmpty() || result.visuals.isEmpty()
        nodeVisible(node, visible, ctx)
        return visible
    }

    private fun visitCustomElements(node: ResolvedGestureGuiNode, declared: GestureGuiCustom, ctx: Context) {
        // 第1走査で renderer を実行済みの場合のみ、操作面を作ります。
        // 未登録時は第1走査で診断済みのため何もしません。
        if (declared.rendererId !in ctx.customRenderers) return
        val prefix = node.nodeId ?: "custom"
        // 第1走査で確定した出力を用い、renderer の再実行は行いません。
        val output = ctx.customOutputs[node] ?: return
        val visualIdMap = output.visualIdMap
        val declaredIds = output.declaredIds
        output.elements.forEach { custom ->
            val actionId = custom.actionId ?: declared.actionId
            val consumeOnly = actionId == null && custom.consumeInput
            if (actionId != null && actionId !in ctx.actionHandlers) {
                ctx.add(GestureGuiLayoutErrorCode.UNKNOWN_ACTION, node.nodeId, null,
                    "unknown action reference: $actionId")
                return@forEach
            }
            if (actionId == null && !consumeOnly) return@forEach
            val target = custom.targetVisualId
            val bounds = node.effectiveClip.clip(custom.bounds)
            if (bounds == null || (target != null && target !in visualIdMap)) {
                ctx.add(
                    if (target != null && target !in declaredIds) GestureGuiLayoutErrorCode.UNKNOWN_VISUAL
                    else GestureGuiLayoutErrorCode.CLIPPED_INTERACTION,
                    node.nodeId, null, "custom interaction removed: ${custom.elementId}",
                )
                return@forEach
            }
            if (custom.acceptedGestures.isEmpty() && !consumeOnly) {
                ctx.add(GestureGuiLayoutErrorCode.EMPTY_HITBOX, node.nodeId, null,
                    "custom interaction has no accepted gestures: ${custom.elementId}")
                return@forEach
            }
            val elementId = ctx.uniqueId("$prefix/${custom.elementId}")
            // ホバー内の参照も同じ名前空間へ写し、消えた対象への参照を残しません。
            val hover = custom.hoverText?.let { hover ->
                val hoverBlock = hover.hoverBlockVisualId?.let(visualIdMap::get)
                hover.copy(
                    replacesVisualId = hover.replacesVisualId?.let(visualIdMap::get),
                    hoverBlockVisualId = hoverBlock,
                    hoverBlockData = if (hoverBlock != null) hover.hoverBlockData else null,
                )
            }
            ctx.elements += GestureGuiElement(
                elementId = elementId,
                bounds = bounds,
                acceptedGestures = if (consumeOnly) emptySet() else custom.acceptedGestures,
                hoverText = hover,
                targetVisualId = target?.let(visualIdMap::get),
                gestureGuard = custom.gestureGuard,
            )
            if (actionId != null) ctx.elementActions[elementId] = actionId
        }
        if (ctx.nodeVisible[node] == false) clippedInteraction(node, ctx)
        else addElement(node, targetVisualId = null, ctx = ctx)
    }

}

/** compiler の出力である1画面と診断の一覧です。 */
data class GestureGuiCompiledScreen(
    val view: GestureGuiView,
    val diagnostics: List<GestureGuiLayoutDiagnostic>,
)
