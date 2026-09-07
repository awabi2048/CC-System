package com.awabi2048.ccsystem.core.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiActionContext
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiBounds
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVisual
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiBlock
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiColumn
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCustom
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCustomElement
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCustomRenderer
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCustomRenderResult
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiDocument
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutErrorCode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiRow
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiSizeSpec
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiText
import java.lang.reflect.Proxy
import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.block.data.BlockData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** compiler（Resolved → GestureGuiView）のテストです。サーバーなしで実行できます。 */
class GestureGuiLayoutCompilerTest {
    private val panel = GestureGuiPanel(width = 2.0, height = 1.0, frameWidth = 0.05)

    private fun text(id: String, actionId: String? = null) = GestureGuiText(
        text = Component.text(id),
        id = id,
        actionId = actionId,
    )

    private fun blockData(): BlockData = Proxy.newProxyInstance(
        BlockData::class.java.classLoader,
        arrayOf(BlockData::class.java),
    ) { _, _, _ -> null } as BlockData

    @Test
    fun `text and container compile to visuals and auto-generated elements`() {
        val document = GestureGuiDocument(
            GestureGuiColumn(
                children = listOf(
                    text("title"),
                    text("ok", actionId = "confirm"),
                ),
                id = "root",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document, knownActionIds = setOf("confirm"))
        var handled = 0
        val compiled = GestureGuiLayoutCompiler.compile(
            resolved,
            screenId = "confirm-screen",
            actionHandlers = mapOf("confirm" to { _: GestureGuiActionContext -> handled += 1 }),
        )
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        assertEquals(2, compiled.view.visuals.size)
        assertEquals(1, compiled.view.definition.elements.size)
        val element = compiled.view.definition.elements.single()
        // interaction bounds は解決結果から自動生成され、visual と一致します。
        val visual = compiled.view.visuals.single { it.visualId == element.targetVisualId }
        assertTrue(visual is GestureGuiVisual.Text)
        // onAction が action ID 経由で処理へ接続されます。
        compiled.view.onAction(
            GestureGuiActionContext(
                ownerId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                screenId = "confirm-screen",
                elementId = element.elementId,
                gesture = GestureGuiGesture.PRIMARY,
                revision = 1L,
            ),
        )
        assertEquals(1, handled)
    }

    @Test
    fun `block maps bounds and delegates unknown actions to diagnostics`() {
        val document = GestureGuiDocument(
            GestureGuiRow(
                children = listOf(
                    GestureGuiBlock(
                        blockData = blockData(),
                        width = GestureGuiSizeSpec.Fixed(0.4),
                        height = GestureGuiSizeSpec.Fixed(0.2),
                        id = "card",
                        actionId = "missing",
                    ),
                ),
                id = "row",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document)
        val compiled = GestureGuiLayoutCompiler.compile(resolved, screenId = "s")
        assertTrue(
            compiled.diagnostics.any { it.code == GestureGuiLayoutErrorCode.UNKNOWN_ACTION },
            "diagnostics: ${compiled.diagnostics}",
        )
        // 未知 action の操作面は生成されませんが、visual 自体は生成されます。
        assertEquals(1, compiled.view.visuals.size)
        assertTrue(compiled.view.definition.elements.isEmpty())
        val visual = compiled.view.visuals.single() as GestureGuiVisual.Block
        assertEquals(0.4, visual.width, 1.0e-9)
        assertEquals(0.2, visual.height, 1.0e-9)
    }

    @Test
    fun `custom renderer output is namespaced and routed`() {
        val document = GestureGuiDocument(
            GestureGuiCustom(
                rendererId = "graph",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
                id = "graph",
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document)
        var handledElement: String? = null
        val compiled = GestureGuiLayoutCompiler.compile(
            resolved,
            screenId = "graph-screen",
            actionHandlers = mapOf("select-node" to { action: GestureGuiActionContext ->
                handledElement = action.elementId
            }),
            customRenderers = mapOf(
                "graph" to GestureGuiCustomRenderer { _, _ ->
                    GestureGuiCustomRenderResult(
                        visuals = listOf(
                            GestureGuiVisual.Text(
                                visualId = "node-label",
                                x = 0.0,
                                y = 0.0,
                                text = Component.text("node"),
                            ),
                        ),
                        elements = listOf(
                            GestureGuiCustomElement(
                                elementId = "node-1",
                                bounds = GestureGuiBounds(-0.1, -0.1, 0.1, 0.1),
                                actionId = "select-node",
                                targetVisualId = "node-label",
                            ),
                        ),
                    )
                },
            ),
        )
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        assertEquals("graph/node-label", compiled.view.visuals.single().visualId)
        val element = compiled.view.definition.elements.single()
        assertEquals("graph/node-1", element.elementId)
        assertEquals("graph/node-label", element.targetVisualId)
        compiled.view.onAction(
            GestureGuiActionContext(
                ownerId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                screenId = "graph-screen",
                elementId = element.elementId,
                gesture = GestureGuiGesture.PRIMARY,
                revision = 1L,
            ),
        )
        assertEquals(element.elementId, handledElement)
    }

    @Test
    fun `missing custom renderer is reported`() {
        val document = GestureGuiDocument(
            GestureGuiCustom(
                rendererId = "graph",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
                id = "graph",
            ),
            panel,
        )
        val compiled = GestureGuiLayoutCompiler.compile(
            GestureGuiLayoutEngine.layout(document),
            screenId = "s",
        )
        assertTrue(
            compiled.diagnostics.any { it.code == GestureGuiLayoutErrorCode.CUSTOM_RENDERER_MISSING },
            "diagnostics: ${compiled.diagnostics}",
        )
    }
}
