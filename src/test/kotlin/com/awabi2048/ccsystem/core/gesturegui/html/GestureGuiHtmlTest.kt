package com.awabi2048.ccsystem.core.gesturegui.html

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiActionContext
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.html.GestureGuiHtmlEnvironment
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutErrorCode
import com.awabi2048.ccsystem.core.gesturegui.layout.GestureGuiLayoutCompiler
import com.awabi2048.ccsystem.core.gesturegui.layout.GestureGuiLayoutEngine
import java.lang.reflect.Proxy
import java.util.UUID
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 制限付き HTML frontend のテストです。サーバーなしで実行できます。 */
class GestureGuiHtmlTest {
    private val panel = GestureGuiPanel(width = 2.0, height = 1.0, frameWidth = 0.05)

    private fun blockData(): BlockData = Proxy.newProxyInstance(
        BlockData::class.java.classLoader,
        arrayOf(BlockData::class.java),
    ) { _, _, _ -> null } as BlockData

    private val environment = object : GestureGuiHtmlEnvironment {
        override fun buttonBackground(background: String?, classes: Set<String>, id: String?): BlockData = blockData()
        override fun containerBackground(background: String, classes: Set<String>, id: String?): BlockData? = blockData()
        override fun outlineBlock(classes: Set<String>, id: String?): BlockData? = blockData()
        override fun blockData(material: String, classes: Set<String>, id: String?): BlockData = blockData()
        override fun itemStack(reference: String, classes: Set<String>, id: String?): ItemStack =
            ItemStack(Material.STONE)
        override fun textSize(tag: String, classes: Set<String>, id: String?): Double = 0.006
    }

    @Test
    fun `confirmation html compiles to two routed actions`() {
        val html = """
            <section id="confirm" style="display: flex; flex-direction: column; gap: 0.03;">
              <p id="message">Delete?</p>
              <div id="actions" style="display: flex; gap: 0.03;">
                <button id="yes-button" action="yes">Yes</button>
                <button id="no-button" action="no">No</button>
              </div>
            </section>
        """.trimIndent()
        val parsed = GestureGuiHtml.parse(html, environment, "confirm.html", panel)
        assertTrue(parsed.diagnostics.isEmpty(), "diagnostics: ${parsed.diagnostics}")
        val resolved = GestureGuiLayoutEngine.layout(parsed.document, knownActionIds = setOf("yes", "no"))
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
        compiled.view.definition.elements.forEach {
            compiled.view.onAction(
                GestureGuiActionContext(
                    ownerId = UUID.randomUUID(),
                    actorId = UUID.randomUUID(),
                    screenId = "s",
                    elementId = it.elementId,
                    gesture = GestureGuiGesture.PRIMARY,
                    revision = 1L,
                ),
            )
        }
        assertEquals(listOf("yes", "no").sorted(), handled.sorted())
    }

    @Test
    fun `stylesheet cascade applies and inline wins`() {
        val html = """
            <style>
              button { width: 0.4; }
              .wide { width: 0.8; }
              #special { width: 0.5; }
            </style>
            <div id="row" style="display: flex;">
              <button id="a" class="wide" action="a">A</button>
              <button id="special" class="wide" action="b">B</button>
              <button id="c" class="wide" style="width: 0.2;" action="c">C</button>
            </div>
        """.trimIndent()
        val parsed = GestureGuiHtml.parse(html, environment, "cascade.html", panel)
        assertTrue(parsed.diagnostics.isEmpty(), "diagnostics: ${parsed.diagnostics}")
        val resolved = GestureGuiLayoutEngine.layout(parsed.document)
        // ルート自体が div#row の Row のため、直接子を見ます。
        val row = resolved.root
        val widths = row.children.map { it.borderBounds.maxX - it.borderBounds.minX }
        assertEquals(0.8, widths[0], 1.0e-9)
        assertEquals(0.5, widths[1], 1.0e-9)
        assertEquals(0.2, widths[2], 1.0e-9)
    }

    @Test
    fun `unknown tags hoist children and unsupported properties are reported`() {
        val html = """
            <div id="root">
              <marquee id="m">Hello</marquee>
              <p id="t" style="float: left; width: 0.5;">Text</p>
            </div>
        """.trimIndent()
        val parsed = GestureGuiHtml.parse(html, environment, "odd.html", panel)
        assertTrue(
            parsed.diagnostics.any { it.code == GestureGuiLayoutErrorCode.UNSUPPORTED_TAG && it.message.contains("marquee") },
            "diagnostics: ${parsed.diagnostics}",
        )
        assertTrue(
            parsed.diagnostics.any { it.code == GestureGuiLayoutErrorCode.UNSUPPORTED_PROPERTY && it.message.contains("float") },
            "diagnostics: ${parsed.diagnostics}",
        )
        assertEquals("odd.html", parsed.diagnostics.first().source)
        // 未知要素の子文言は巻き上げられて解決できます。
        val resolved = GestureGuiLayoutEngine.layout(parsed.document)
        assertTrue(resolved.diagnostics.isEmpty(), "diagnostics: ${resolved.diagnostics}")
    }

    @Test
    fun `grid viewport and mc-block render`() {
        // mc-item は Paper レジストリなしに ItemStack を生成できないため、
        // 単体テストでは BlockData プロキシで生成可能な mc-block で検証します。
        val html = """
            <gesture-viewport id="view" style="width: 1.0; height: 0.5;">
              <div id="grid" style="display: grid;" data-columns="2">
                <mc-block id="tile" material="stone" />
                <p id="label">Stone</p>
              </div>
            </gesture-viewport>
        """.trimIndent()
        val parsed = GestureGuiHtml.parse(html, environment, "view.html", panel)
        assertTrue(parsed.diagnostics.isEmpty(), "diagnostics: ${parsed.diagnostics}")
        val resolved = GestureGuiLayoutEngine.layout(parsed.document)
        assertTrue(resolved.diagnostics.isEmpty(), "diagnostics: ${resolved.diagnostics}")
        val compiled = GestureGuiLayoutCompiler.compile(resolved, screenId = "s")
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        assertTrue(compiled.view.visuals.any { it.visualId.contains("tile") })
    }

    @Test
    fun `mismatched tags are reported`() {
        val parsed = GestureGuiHtml.parse("<div><p>Text</div>", environment, "broken.html", panel)
        assertTrue(
            parsed.diagnostics.any { it.code == GestureGuiLayoutErrorCode.HTML_PARSE_ERROR },
            "diagnostics: ${parsed.diagnostics}",
        )
    }

    @Test
    fun `gestures attribute controls accepted gestures`() {
        val html = """
            <div id="row" style="display: flex;">
              <button id="a" action="a" gestures="click">A</button>
              <button id="b" action="b" gestures="primary shift_secondary">B</button>
              <button id="c" action="c">C</button>
            </div>
        """.trimIndent()
        val parsed = GestureGuiHtml.parse(html, environment, "gestures.html", panel)
        assertTrue(parsed.diagnostics.isEmpty(), "diagnostics: ${parsed.diagnostics}")
        val compiled = GestureGuiLayoutCompiler.compile(
            GestureGuiLayoutEngine.layout(parsed.document, knownActionIds = setOf("a", "b", "c")),
            screenId = "s",
            actionHandlers = mapOf("a" to {}, "b" to {}, "c" to {}),
        )
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        // 宣言順に対応します（a: click略記、b: 明示列挙、c: 既定PRIMARY）。
        val gestures = compiled.view.definition.elements.map { it.acceptedGestures }
        assertEquals(
            setOf(GestureGuiGesture.PRIMARY, GestureGuiGesture.SHIFT_PRIMARY),
            gestures[0],
        )
        assertEquals(
            setOf(GestureGuiGesture.PRIMARY, GestureGuiGesture.SHIFT_SECONDARY),
            gestures[1],
        )
        assertEquals(setOf(GestureGuiGesture.PRIMARY), gestures[2])
    }

    @Test
    fun `color and line-width apply to text`() {
        val html = """
            <div id="root">
              <p id="gray" style="color: gray; line-width: 200;">Gray</p>
              <p id="plain">Plain</p>
            </div>
        """.trimIndent()
        val parsed = GestureGuiHtml.parse(html, environment, "color.html", panel)
        assertTrue(parsed.diagnostics.isEmpty(), "diagnostics: ${parsed.diagnostics}")
        val compiled = GestureGuiLayoutCompiler.compile(
            GestureGuiLayoutEngine.layout(parsed.document),
            screenId = "s",
        )
        assertTrue(compiled.diagnostics.isEmpty(), "diagnostics: ${compiled.diagnostics}")
        val texts = compiled.view.visuals.filterIsInstance<com.awabi2048.ccsystem.api.gesturegui.GestureGuiVisual.Text>()
        assertEquals(200, texts.single { it.visualId.contains("gray") }.lineWidth)
        assertEquals(
            net.kyori.adventure.text.format.NamedTextColor.GRAY,
            texts.single { it.visualId.contains("gray") }.text.color(),
        )
        assertEquals(160, texts.single { it.visualId.contains("plain") }.lineWidth)
    }

    @Test
    fun `unknown gestures token and color are reported`() {
        val html = """
            <div id="root">
              <button id="a" action="a" gestures="warp">A</button>
              <p id="t" style="color: blurple;">T</p>
              <p id="w" style="line-width: 0;">W</p>
            </div>
        """.trimIndent()
        val parsed = GestureGuiHtml.parse(html, environment, "bad-values.html", panel)
        assertTrue(
            parsed.diagnostics.any {
                it.code == GestureGuiLayoutErrorCode.UNSUPPORTED_PROPERTY && it.message.contains("warp")
            },
            "diagnostics: ${parsed.diagnostics}",
        )
        assertTrue(
            parsed.diagnostics.any {
                it.code == GestureGuiLayoutErrorCode.UNSUPPORTED_PROPERTY && it.message.contains("blurple")
            },
            "diagnostics: ${parsed.diagnostics}",
        )
        assertTrue(
            parsed.diagnostics.any {
                it.code == GestureGuiLayoutErrorCode.UNSUPPORTED_PROPERTY && it.message.contains("line-width")
            },
            "diagnostics: ${parsed.diagnostics}",
        )
        assertEquals("bad-values.html", parsed.diagnostics.first().source)
    }
}
