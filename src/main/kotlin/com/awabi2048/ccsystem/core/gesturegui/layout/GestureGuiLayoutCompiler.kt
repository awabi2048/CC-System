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
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiClip
import com.awabi2048.ccsystem.core.gesturegui.GestureGuiOutlineGeometry
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCustomRenderer
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiItem
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutDiagnostic
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutErrorCode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiText
import com.awabi2048.ccsystem.api.gesturegui.layout.ResolvedGestureGui
import com.awabi2048.ccsystem.api.gesturegui.layout.ResolvedGestureGuiNode
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
        visit(resolved.root, ctx)
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

    private fun layerOf(resolvedZ: Int): Int = resolvedZ.coerceAtLeast(0).coerceAtMost(MAX_LAYER - 1) + 1

    private fun centerX(bounds: GestureGuiBounds): Double = (bounds.minX + bounds.maxX) / 2.0

    private fun centerY(bounds: GestureGuiBounds): Double = (bounds.minY + bounds.maxY) / 2.0

    private fun visit(node: ResolvedGestureGuiNode, ctx: Context): Boolean {
        if (node.effectiveClip == GestureGuiClip.Empty) return false
        val bounds = node.borderBounds
        val declared = node.source
        if (declared is GestureGuiCustom) return visitCustom(node, declared, ctx)
        val visualId = ctx.uniqueId(node.nodeId?.let { "visual-$it" })
        val visual = when (declared) {
            is GestureGuiText -> GestureGuiVisual.Text(
                visualId, centerX(bounds), centerY(bounds), declared.text,
                declared.size, declared.lineWidth, layerOf(node.resolvedZ),
                declared.seeThrough, declared.alignment,
            )
            is GestureGuiBlock -> GestureGuiVisual.Block(
                visualId, centerX(bounds), centerY(bounds),
                bounds.maxX - bounds.minX, bounds.maxY - bounds.minY,
                declared.blockData, layerOf(node.resolvedZ), declared.glowColor, declared.outline,
            )
            is GestureGuiItem -> GestureGuiVisual.Item(
                visualId, centerX(bounds), centerY(bounds), declared.item.clone(),
                declared.scale, layerOf(node.resolvedZ), declared.glowColor,
            )
            else -> null
        }
        if (visual != null) {
            val visible = emitVisual(visual, bounds, node.effectiveClip, ctx)
            if (visible) addElement(node, visualId, ctx)
            else clippedInteraction(node, ctx)
            return visible
        }
        // 全ての子の表示が消えた複合部品に、透明な操作面だけを残さないようにします。
        val visible = node.children.map { visit(it, ctx) }.any { it }
        if (visible || node.children.isEmpty()) addElement(node, targetVisualId = null, ctx = ctx)
        else clippedInteraction(node, ctx)
        return visible
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
     * interaction が null（clip 消滅等）の場合は Engine 側の診断に委ねて生成しません。
     */
    private fun addElement(node: ResolvedGestureGuiNode, targetVisualId: String?, ctx: Context) {
        val actionId = node.actionId ?: return
        val interaction = node.interactionBounds ?: return
        val gestures = node.source.acceptedGestures
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
        val elementId = ctx.uniqueId(node.nodeId ?: "element")
        ctx.elements += GestureGuiElement(
            elementId = elementId,
            bounds = interaction,
            acceptedGestures = gestures,
            targetVisualId = targetVisualId,
        )
        ctx.elementActions[elementId] = actionId
    }

    private fun visitCustom(node: ResolvedGestureGuiNode, declared: GestureGuiCustom, ctx: Context): Boolean {
        val renderer = ctx.customRenderers[declared.rendererId]
        if (renderer == null) {
            ctx.add(GestureGuiLayoutErrorCode.CUSTOM_RENDERER_MISSING, node.nodeId, null,
                "custom renderer is not registered: ${declared.rendererId}")
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
        result.elements.forEach { custom ->
            val actionId = custom.actionId ?: declared.actionId ?: return@forEach
            if (actionId !in ctx.actionHandlers) {
                ctx.add(GestureGuiLayoutErrorCode.UNKNOWN_ACTION, node.nodeId, null,
                    "unknown action reference: $actionId")
                return@forEach
            }
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
            if (custom.acceptedGestures.isEmpty()) {
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
                elementId = elementId, bounds = bounds, acceptedGestures = custom.acceptedGestures,
                hoverText = hover, targetVisualId = target?.let(visualIdMap::get),
            )
            ctx.elementActions[elementId] = actionId
        }
        if (visualIdMap.isNotEmpty() || result.visuals.isEmpty()) addElement(node, targetVisualId = null, ctx = ctx)
        else clippedInteraction(node, ctx)
        return visualIdMap.isNotEmpty()
    }

}

/** compiler の出力である1画面と診断の一覧です。 */
data class GestureGuiCompiledScreen(
    val view: GestureGuiView,
    val diagnostics: List<GestureGuiLayoutDiagnostic>,
)
