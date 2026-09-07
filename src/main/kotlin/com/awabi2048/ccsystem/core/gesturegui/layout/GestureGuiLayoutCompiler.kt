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

    private fun visit(node: ResolvedGestureGuiNode, ctx: Context) {
        when (val declared = node.source) {
            is GestureGuiText -> {
                val visualId = ctx.uniqueId(node.nodeId?.let { "visual-$it" })
                node.borderBounds.let { bounds ->
                    ctx.visuals += GestureGuiVisual.Text(
                        visualId = visualId,
                        x = centerX(bounds),
                        y = centerY(bounds),
                        text = declared.text,
                        size = declared.size,
                        lineWidth = declared.lineWidth,
                        layer = layerOf(node.resolvedZ),
                        seeThrough = declared.seeThrough,
                        alignment = declared.alignment,
                    )
                }
                addElement(node, visualId, ctx)
            }
            is GestureGuiBlock -> {
                val visualId = ctx.uniqueId(node.nodeId?.let { "visual-$it" })
                node.borderBounds.let { bounds ->
                    ctx.visuals += GestureGuiVisual.Block(
                        visualId = visualId,
                        x = centerX(bounds),
                        y = centerY(bounds),
                        width = bounds.maxX - bounds.minX,
                        height = bounds.maxY - bounds.minY,
                        blockData = declared.blockData,
                        layer = layerOf(node.resolvedZ),
                        glowColor = declared.glowColor,
                        outline = declared.outline,
                    )
                }
                addElement(node, visualId, ctx)
            }
            is GestureGuiItem -> {
                val visualId = ctx.uniqueId(node.nodeId?.let { "visual-$it" })
                node.borderBounds.let { bounds ->
                    ctx.visuals += GestureGuiVisual.Item(
                        visualId = visualId,
                        x = centerX(bounds),
                        y = centerY(bounds),
                        item = declared.item.clone(),
                        scale = declared.scale,
                        layer = layerOf(node.resolvedZ),
                        glowColor = declared.glowColor,
                    )
                }
                addElement(node, visualId, ctx)
            }
            is GestureGuiCustom -> visitCustom(node, declared, ctx)
            else -> addElement(node, targetVisualId = null, ctx = ctx)
        }
        // Custom の子は renderer が生成するため宣言ツリー側には存在しません。
        if (node.source !is GestureGuiCustom) {
            node.children.forEach { visit(it, ctx) }
        }
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

    private fun visitCustom(node: ResolvedGestureGuiNode, declared: GestureGuiCustom, ctx: Context) {
        val renderer = ctx.customRenderers[declared.rendererId]
        if (renderer == null) {
            ctx.add(
                GestureGuiLayoutErrorCode.CUSTOM_RENDERER_MISSING,
                node.nodeId,
                null,
                "custom renderer is not registered: ${declared.rendererId}",
            )
            return
        }
        val prefix = node.nodeId ?: "custom"
        val result = renderer.render(declared, node)
        val visualIdMap = mutableMapOf<String, String>()
        result.visuals.forEach { visual ->
            val mappedId = ctx.uniqueId("$prefix/${visual.visualId}")
            visualIdMap[visual.visualId] = mappedId
            ctx.visuals += when (visual) {
                is GestureGuiVisual.Block -> visual.copy(visualId = mappedId)
                is GestureGuiVisual.Text -> visual.copy(visualId = mappedId)
                is GestureGuiVisual.Item -> visual.copy(visualId = mappedId, item = visual.item.clone())
            }
        }
        result.elements.forEach { custom ->
            val actionId = custom.actionId ?: declared.actionId ?: return@forEach
            if (actionId !in ctx.actionHandlers) {
                ctx.add(
                    GestureGuiLayoutErrorCode.UNKNOWN_ACTION,
                    node.nodeId,
                    null,
                    "unknown action reference: $actionId",
                )
                return@forEach
            }
            val elementId = ctx.uniqueId("$prefix/${custom.elementId}")
            ctx.elements += GestureGuiElement(
                elementId = elementId,
                bounds = custom.bounds,
                acceptedGestures = custom.acceptedGestures,
                hoverText = custom.hoverText,
                targetVisualId = custom.targetVisualId?.let { visualIdMap[it] ?: "$prefix/$it" },
            )
            ctx.elementActions[elementId] = actionId
        }
        // Custom ノード自体の interaction（領域全体の操作面）も宣言どおり生成します。
        addElement(node, targetVisualId = null, ctx = ctx)
    }
}

/** compiler の出力である1画面と診断の一覧です。 */
data class GestureGuiCompiledScreen(
    val view: GestureGuiView,
    val diagnostics: List<GestureGuiLayoutDiagnostic>,
)
