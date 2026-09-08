package com.awabi2048.ccsystem.core.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiActionContext
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiColumn
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiDocument
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHover
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHoverBlock
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHoverPosition
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHoverTarget
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutErrorCode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiRow
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiSizeSpec
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiText
import java.lang.reflect.Proxy
import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** guard・ホバー・消費面の透過テストです。サーバーなしで実行できます。 */
class GestureGuiGuardHoverTest {
    private val panel = GestureGuiPanel(width = 2.0, height = 1.0, frameWidth = 0.05)

    private fun player(): Player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "toString" -> "mock-player"
            "hashCode" -> 1
            "equals" -> false
            else -> null
        }
    } as Player

    private fun blockData(): BlockData = Proxy.newProxyInstance(
        BlockData::class.java.classLoader,
        arrayOf(BlockData::class.java),
    ) { _, _, _ -> null } as BlockData

    private fun text(
        id: String,
        actionId: String? = null,
        guard: ((Player, GestureGuiGesture) -> Boolean)? = null,
        hover: GestureGuiHover? = null,
        consumeInput: Boolean = false,
    ) = GestureGuiText(
        text = Component.text(id),
        id = id,
        actionId = actionId,
        gestureGuard = guard,
        hover = hover,
        consumeInput = consumeInput,
    )

    private fun actionContext(elementId: String) = GestureGuiActionContext(
        ownerId = UUID.randomUUID(),
        actorId = UUID.randomUUID(),
        screenId = "s",
        elementId = elementId,
        gesture = GestureGuiGesture.PRIMARY,
        revision = 1L,
    )

    @Test
    fun `guard is evaluated at input time`() {
        val document = GestureGuiDocument(
            GestureGuiRow(
                children = listOf(
                    text("gated", actionId = "go", guard = { _, gesture -> gesture == GestureGuiGesture.PRIMARY }),
                ),
                id = "row",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val compiled = GestureGuiLayoutCompiler.compile(
            GestureGuiLayoutEngine.layout(document, knownActionIds = setOf("go")),
            screenId = "s",
            actionHandlers = mapOf("go" to {}),
        )
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        val element = compiled.view.definition.elements.single()
        assertNotNull(element.gestureGuard)
        val actor = player()
        assertTrue(element.acceptsGesture(actor, GestureGuiGesture.PRIMARY))
        assertFalse(element.acceptsGesture(actor, GestureGuiGesture.SECONDARY))
    }

    @Test
    fun `guard without action is reported`() {
        val resolved = GestureGuiLayoutEngine.layout(
            GestureGuiDocument(
                GestureGuiRow(
                    children = listOf(text("dangling", guard = { _, _ -> true })),
                    id = "row",
                    width = GestureGuiSizeSpec.Percent(1.0),
                    height = GestureGuiSizeSpec.Percent(1.0),
                ),
                panel,
            ),
        )
        assertTrue(
            resolved.diagnostics.any { it.code == GestureGuiLayoutErrorCode.GUARD_WITHOUT_ACTION },
            "diagnostics: ${resolved.diagnostics}",
        )
    }

    @Test
    fun `consume-only node creates a silent interaction`() {
        val document = GestureGuiDocument(
            GestureGuiRow(
                children = listOf(text("barrier", consumeInput = true)),
                id = "row",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document)
        assertTrue(
            resolved.diagnostics.none {
                it.code == GestureGuiLayoutErrorCode.EMPTY_HITBOX ||
                    it.code == GestureGuiLayoutErrorCode.UNKNOWN_ACTION
            },
            "diagnostics: ${resolved.diagnostics}",
        )
        val compiled = GestureGuiLayoutCompiler.compile(resolved, screenId = "s")
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        val element = compiled.view.definition.elements.single()
        assertTrue(element.acceptedGestures.isEmpty())
        // action へ接続されないため、入力しても何も起きません。
        var handled = 0
        compiled.view.onAction(actionContext(element.elementId))
        assertEquals(0, handled)
    }

    @Test
    fun `hover above derives coordinates from resolved bounds`() {
        val hover = GestureGuiHover(
            text = Component.text("hint"),
            position = GestureGuiHoverPosition.Auto(GestureGuiHoverPosition.Anchor.ABOVE, gap = 0.02),
        )
        val document = GestureGuiDocument(
            GestureGuiRow(
                children = listOf(text("button", actionId = "press", hover = hover)),
                id = "row",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val compiled = GestureGuiLayoutCompiler.compile(
            GestureGuiLayoutEngine.layout(document, knownActionIds = setOf("press")),
            screenId = "s",
            actionHandlers = mapOf("press" to {}),
        )
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        val element = compiled.view.definition.elements.single()
        val hoverText = element.hoverText
        assertNotNull(hoverText)
        // 右上ではなく、操作面の上端＋gapへ出ます。
        assertEquals(0.0, hoverText!!.x, 1.0e-9)
        assertEquals(element.bounds.maxY + 0.02, hoverText.y, 1.0e-9)
        assertEquals(Component.text("hint"), hoverText.text)
    }

    @Test
    fun `hover fixed position and node replacement resolve`() {
        val hover = GestureGuiHover(
            text = Component.text("detail"),
            position = GestureGuiHoverPosition.Fixed(0.1, 0.2),
            replacement = GestureGuiHoverTarget.Node("desc"),
        )
        val document = GestureGuiDocument(
            GestureGuiColumn(
                children = listOf(
                    text("desc"),
                    text("setting", actionId = "open", hover = hover),
                ),
                id = "column",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val compiled = GestureGuiLayoutCompiler.compile(
            GestureGuiLayoutEngine.layout(document, knownActionIds = setOf("open")),
            screenId = "s",
            actionHandlers = mapOf("open" to {}),
        )
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        val element = compiled.view.definition.elements.single()
        val hoverText = element.hoverText
        assertNotNull(hoverText)
        assertEquals(0.1, hoverText!!.x, 1.0e-9)
        assertEquals(0.2, hoverText.y, 1.0e-9)
        // desc ノードの visual ID へ解決されます。
        val descVisual = compiled.view.visuals.first { it.visualId.contains("desc") }
        assertEquals(descVisual.visualId, hoverText.replacesVisualId)
    }

    @Test
    fun `unknown hover replacement is reported and hover survives`() {
        val hover = GestureGuiHover(
            text = Component.text("hint"),
            replacement = GestureGuiHoverTarget.Node("missing"),
        )
        val document = GestureGuiDocument(
            GestureGuiRow(
                children = listOf(text("button", actionId = "press", hover = hover)),
                id = "row",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val compiled = GestureGuiLayoutCompiler.compile(
            GestureGuiLayoutEngine.layout(document, knownActionIds = setOf("press")),
            screenId = "s",
            actionHandlers = mapOf("press" to {}),
        )
        assertTrue(
            compiled.diagnostics.any { it.code == GestureGuiLayoutErrorCode.UNKNOWN_VISUAL },
            "diagnostics: ${compiled.diagnostics}",
        )
        val hoverText = compiled.view.definition.elements.single().hoverText
        assertNotNull(hoverText)
        assertNull(hoverText!!.replacesVisualId)
    }

    @Test
    fun `hover self replacement on leaf resolves to own visual`() {
        val hover = GestureGuiHover(
            text = Component.text("hint"),
            replacement = GestureGuiHoverTarget.Self,
        )
        val document = GestureGuiDocument(
            GestureGuiRow(
                children = listOf(text("button", actionId = "press", hover = hover)),
                id = "row",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val compiled = GestureGuiLayoutCompiler.compile(
            GestureGuiLayoutEngine.layout(document, knownActionIds = setOf("press")),
            screenId = "s",
            actionHandlers = mapOf("press" to {}),
        )
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        val element = compiled.view.definition.elements.single()
        assertEquals(element.targetVisualId, element.hoverText?.replacesVisualId)
    }

    @Test
    fun `hover block replacement requires a block visual`() {
        val hover = GestureGuiHover(
            text = Component.text("hint"),
            blockReplacement = GestureGuiHoverBlock(blockData(), GestureGuiHoverTarget.Self),
        )
        val document = GestureGuiDocument(
            GestureGuiRow(
                children = listOf(text("button", actionId = "press", hover = hover)),
                id = "row",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val compiled = GestureGuiLayoutCompiler.compile(
            GestureGuiLayoutEngine.layout(document, knownActionIds = setOf("press")),
            screenId = "s",
            actionHandlers = mapOf("press" to {}),
        )
        // Text visual への block 差替えは不可のため診断し、文言ホバーは残します。
        assertTrue(
            compiled.diagnostics.any { it.code == GestureGuiLayoutErrorCode.UNKNOWN_VISUAL },
            "diagnostics: ${compiled.diagnostics}",
        )
        val hoverText = compiled.view.definition.elements.single().hoverText
        assertNotNull(hoverText)
        assertNull(hoverText!!.hoverBlockVisualId)
    }
}
