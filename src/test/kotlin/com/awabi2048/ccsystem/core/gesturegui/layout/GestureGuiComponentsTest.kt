package com.awabi2048.ccsystem.core.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiActionContext
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiComponents
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiDocument
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiNode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiOverlay
import java.lang.reflect.Proxy
import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.block.data.BlockData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Standard Components の展開と端到達（解決→変換→操作接続）のテストです。 */
class GestureGuiComponentsTest {
    private val panel = GestureGuiPanel(width = 2.0, height = 1.0, frameWidth = 0.05)

    private fun blockData(): BlockData = Proxy.newProxyInstance(
        BlockData::class.java.classLoader,
        arrayOf(BlockData::class.java),
    ) { _, _, _ -> null } as BlockData

    private fun actionContext(elementId: String) = GestureGuiActionContext(
        ownerId = UUID.randomUUID(),
        actorId = UUID.randomUUID(),
        screenId = "s",
        elementId = elementId,
        gesture = GestureGuiGesture.PRIMARY,
        revision = 1L,
    )

    @Test
    fun `button expands to background plus label with action`() {
        val node = GestureGuiComponents.button(
            id = "ok",
            label = Component.text("OK"),
            actionId = "confirm",
            background = blockData(),
        )
        assertTrue(node is GestureGuiOverlay)
        assertEquals("confirm", node.actionId)
        assertEquals(2, (node as GestureGuiOverlay).children.size)
    }

    @Test
    fun `confirmation compiles to two routed actions`() {
        val node: GestureGuiNode = GestureGuiComponents.confirmation(
            id = "confirm",
            message = Component.text("Delete?"),
            confirmLabel = Component.text("Yes"),
            cancelLabel = Component.text("No"),
            confirmAction = "yes",
            cancelAction = "no",
            confirmBackground = blockData(),
            cancelBackground = blockData(),
        )
        val resolved = GestureGuiLayoutEngine.layout(
            GestureGuiDocument(node, panel),
            knownActionIds = setOf("yes", "no"),
        )
        assertTrue(resolved.diagnostics.isEmpty(), "diagnostics: ${resolved.diagnostics}")
        val handled = mutableListOf<String>()
        val compiled = GestureGuiLayoutCompiler.compile(
            resolved,
            screenId = "s",
            actionHandlers = mapOf(
                "yes" to { _: GestureGuiActionContext -> handled += "yes" },
                "no" to { _: GestureGuiActionContext -> handled += "no" },
            ),
        )
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        assertEquals(2, compiled.view.definition.elements.size)
        compiled.view.definition.elements.forEach { compiled.view.onAction(actionContext(it.elementId)) }
        assertEquals(listOf("yes", "no").sorted(), handled.sorted())
    }

    @Test
    fun `label value and section compose without diagnostics`() {
        val node = GestureGuiComponents.section(
            id = "s",
            title = Component.text("Settings"),
            children = listOf(
                GestureGuiComponents.labelValue(
                    id = "row",
                    label = Component.text("Size"),
                    value = Component.text("Large"),
                ),
                GestureGuiComponents.tab(
                    id = "tab",
                    label = Component.text("Tab"),
                    actionId = "tab-a",
                    selected = true,
                    selectedBackground = blockData(),
                    unselectedBackground = blockData(),
                ),
            ),
        )
        val resolved = GestureGuiLayoutEngine.layout(
            GestureGuiDocument(node, panel),
            knownActionIds = setOf("tab-a"),
        )
        assertTrue(resolved.diagnostics.isEmpty(), "diagnostics: ${resolved.diagnostics}")
        val compiled = GestureGuiLayoutCompiler.compile(
            resolved,
            screenId = "s",
            actionHandlers = mapOf("tab-a" to {}),
        )
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        assertEquals(1, compiled.view.definition.elements.size)
    }
}
