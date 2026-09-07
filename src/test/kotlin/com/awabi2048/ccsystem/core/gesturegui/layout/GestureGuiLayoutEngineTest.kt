package com.awabi2048.ccsystem.core.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiAbsoluteOffsets
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiBox
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiColumn
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiDocument
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiGrid
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutErrorCode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiMainArrangement
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiOverflow
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiOverlay
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiRow
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiSizeSpec
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiText
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiViewport
import net.kyori.adventure.text.Component
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Layout Engine の純粋計算テストです。
 * Text のみを用い、Bukkit サーバーなしで実行できます。
 */
class GestureGuiLayoutEngineTest {
    private val panel = GestureGuiPanel(width = 2.0, height = 1.0, frameWidth = 0.05)
    private val contentWidth = 1.9
    private val contentHeight = 0.9
    private val contentMinX = -0.95
    private val contentMaxY = 0.45

    private fun text(
        id: String,
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
        actionId: String? = null,
        acceptedGestures: Set<GestureGuiGesture> = if (actionId != null) {
            setOf(GestureGuiGesture.PRIMARY)
        } else {
            emptySet()
        },
    ) = GestureGuiText(
        text = Component.text(id),
        id = id,
        width = width,
        height = height,
        actionId = actionId,
        acceptedGestures = acceptedGestures,
    )

    @Test
    fun `row splits fractions evenly`() {
        val document = GestureGuiDocument(
            GestureGuiRow(
                children = listOf(
                    text("a", width = GestureGuiSizeSpec.Fraction(1.0)),
                    text("b", width = GestureGuiSizeSpec.Fraction(1.0)),
                ),
                id = "row",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document)
        assertTrue(resolved.diagnostics.isEmpty(), "diagnostics: ${resolved.diagnostics}")
        val children = resolved.root.children
        assertEquals(2, children.size)
        assertEquals(contentMinX, children[0].borderBounds.minX, 1.0e-9)
        assertEquals(contentMinX + contentWidth / 2.0, children[0].borderBounds.maxX, 1.0e-9)
        assertEquals(contentMinX + contentWidth / 2.0, children[1].borderBounds.minX, 1.0e-9)
        assertEquals(contentMinX + contentWidth, children[1].borderBounds.maxX, 1.0e-9)
    }

    @Test
    fun `column stacks fixed heights with gap`() {
        val document = GestureGuiDocument(
            GestureGuiColumn(
                children = listOf(
                    text("a", height = GestureGuiSizeSpec.Fixed(0.2)),
                    text("b", height = GestureGuiSizeSpec.Fixed(0.3)),
                ),
                id = "column",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
                gap = 0.1,
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document)
        assertTrue(resolved.diagnostics.isEmpty(), "diagnostics: ${resolved.diagnostics}")
        val children = resolved.root.children
        assertEquals(contentMaxY, children[0].borderBounds.maxY, 1.0e-9)
        assertEquals(contentMaxY - 0.2, children[0].borderBounds.minY, 1.0e-9)
        assertEquals(contentMaxY - 0.2 - 0.1, children[1].borderBounds.maxY, 1.0e-9)
        assertEquals(contentMaxY - 0.2 - 0.1 - 0.3, children[1].borderBounds.minY, 1.0e-9)
        // Text の横幅は内容域いっぱいに広がります。
        assertEquals(contentMinX, children[0].borderBounds.minX, 1.0e-9)
        assertEquals(contentMinX + contentWidth, children[0].borderBounds.maxX, 1.0e-9)
    }

    @Test
    fun `reject overflow is reported and visible overflows silently`() {
        val wide = text("wide", width = GestureGuiSizeSpec.Fixed(5.0), height = GestureGuiSizeSpec.Fixed(0.1))
        val rejected = GestureGuiLayoutEngine.layout(
            GestureGuiDocument(
                GestureGuiBox(
                    children = listOf(wide),
                    id = "box",
                    width = GestureGuiSizeSpec.Percent(1.0),
                    height = GestureGuiSizeSpec.Percent(1.0),
                    overflow = GestureGuiOverflow.REJECT,
                ),
                panel,
            ),
        )
        assertTrue(
            rejected.diagnostics.any { it.code == GestureGuiLayoutErrorCode.REJECT_OVERFLOW && it.nodeId == "wide" },
            "diagnostics: ${rejected.diagnostics}",
        )
        val allowed = GestureGuiLayoutEngine.layout(
            GestureGuiDocument(
                GestureGuiBox(
                    children = listOf(wide),
                    id = "box",
                    width = GestureGuiSizeSpec.Percent(1.0),
                    height = GestureGuiSizeSpec.Percent(1.0),
                    overflow = GestureGuiOverflow.VISIBLE,
                ),
                panel,
            ),
        )
        assertTrue(allowed.diagnostics.isEmpty(), "diagnostics: ${allowed.diagnostics}")
    }

    @Test
    fun `viewport clips content and interaction`() {
        val document = GestureGuiDocument(
            GestureGuiViewport(
                content = GestureGuiBox(
                    children = listOf(text("inner", actionId = "act")),
                    id = "inner-box",
                    width = GestureGuiSizeSpec.Fixed(5.0),
                    height = GestureGuiSizeSpec.Percent(1.0),
                ),
                id = "viewport",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document, knownActionIds = setOf("act"))
        assertTrue(
            resolved.diagnostics.any { it.code == GestureGuiLayoutErrorCode.LAYOUT_OVERFLOW },
            "diagnostics: ${resolved.diagnostics}",
        )
        val viewport = resolved.root
        assertNotNull(viewport.effectiveClip)
        assertEquals(contentMinX, viewport.effectiveClip!!.minX, 1.0e-9)
        val inner = viewport.children.single().children.single()
        // interaction は viewport 域で切り抜かれます。
        assertNotNull(inner.interactionBounds)
        assertEquals(contentMinX, inner.interactionBounds!!.minX, 1.0e-9)
        assertTrue(inner.interactionBounds!!.maxX < inner.borderBounds.maxX)
    }

    @Test
    fun `interaction bounds match border when unclipped`() {
        val document = GestureGuiDocument(
            GestureGuiRow(
                children = listOf(text("button", actionId = "press")),
                id = "row",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document, knownActionIds = setOf("press"))
        val button = resolved.root.children.single()
        assertEquals(button.borderBounds, button.interactionBounds)
    }

    @Test
    fun `duplicate ids and unknown actions are reported`() {
        val document = GestureGuiDocument(
            GestureGuiColumn(
                children = listOf(
                    text("dup", actionId = "missing"),
                    text("dup"),
                ),
                id = "column",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document, knownActionIds = setOf("ok"))
        assertTrue(
            resolved.diagnostics.any { it.code == GestureGuiLayoutErrorCode.DUPLICATE_ID && it.nodeId == "dup" },
            "diagnostics: ${resolved.diagnostics}",
        )
        assertTrue(
            resolved.diagnostics.any { it.code == GestureGuiLayoutErrorCode.UNKNOWN_ACTION },
            "diagnostics: ${resolved.diagnostics}",
        )
    }

    @Test
    fun `action without gestures yields empty hitbox`() {
        val node = text("nogesture", actionId = "act", acceptedGestures = emptySet())
        val resolved = GestureGuiLayoutEngine.layout(
            GestureGuiDocument(
                GestureGuiRow(
                    children = listOf(node),
                    id = "row",
                    width = GestureGuiSizeSpec.Percent(1.0),
                    height = GestureGuiSizeSpec.Percent(1.0),
                ),
                panel,
            ),
            knownActionIds = setOf("act"),
        )
        assertTrue(
            resolved.diagnostics.any { it.code == GestureGuiLayoutErrorCode.EMPTY_HITBOX },
            "diagnostics: ${resolved.diagnostics}",
        )
        assertNull(resolved.root.children.single().interactionBounds)
    }

    @Test
    fun `grid places children row-major`() {
        val document = GestureGuiDocument(
            GestureGuiGrid(
                children = listOf(text("c0"), text("c1"), text("c2")),
                columns = 2,
                id = "grid",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document)
        assertTrue(resolved.diagnostics.isEmpty(), "diagnostics: ${resolved.diagnostics}")
        val children = resolved.root.children
        assertEquals(3, children.size)
        val cellW = contentWidth / 2.0
        assertEquals(contentMinX, children[0].borderBounds.minX, 1.0e-9)
        assertEquals(contentMinX + cellW, children[1].borderBounds.minX, 1.0e-9)
        assertEquals(contentMinX, children[2].borderBounds.minX, 1.0e-9)
        // 2行目の上端は1行目の下端より下です。
        assertTrue(children[2].borderBounds.maxY < children[0].borderBounds.minY)
    }

    @Test
    fun `overlay stacks children with increasing z`() {
        val document = GestureGuiDocument(
            GestureGuiOverlay(
                children = listOf(text("back"), text("front")),
                id = "overlay",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document)
        assertTrue(resolved.diagnostics.isEmpty(), "diagnostics: ${resolved.diagnostics}")
        val children = resolved.root.children
        assertTrue(children[1].resolvedZ > children[0].resolvedZ)
    }

    @Test
    fun `absolute offsets position the node`() {
        val document = GestureGuiDocument(
            GestureGuiBox(
                children = listOf(
                    GestureGuiText(
                        text = Component.text("float"),
                        id = "float",
                        width = GestureGuiSizeSpec.Fixed(0.5),
                        height = GestureGuiSizeSpec.Fixed(0.2),
                        absolute = GestureGuiAbsoluteOffsets(left = 0.1, top = 0.1),
                    ),
                ),
                id = "box",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document)
        assertTrue(resolved.diagnostics.isEmpty(), "diagnostics: ${resolved.diagnostics}")
        val floating = resolved.root.children.single()
        assertEquals(contentMinX + 0.1, floating.borderBounds.minX, 1.0e-9)
        assertEquals(contentMaxY - 0.1, floating.borderBounds.maxY, 1.0e-9)
    }

    @Test
    fun `center arrangement offsets the row`() {
        val document = GestureGuiDocument(
            GestureGuiRow(
                children = listOf(text("a", width = GestureGuiSizeSpec.Fixed(0.5))),
                id = "row",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Percent(1.0),
                mainArrangement = GestureGuiMainArrangement.CENTER,
            ),
            panel,
        )
        val resolved = GestureGuiLayoutEngine.layout(document)
        val child = resolved.root.children.single()
        assertEquals(contentMinX + (contentWidth - 0.5) / 2.0, child.borderBounds.minX, 1.0e-9)
    }
}
