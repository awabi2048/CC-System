package com.awabi2048.ccsystem.core.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.*
import com.awabi2048.ccsystem.api.gesturegui.html.GestureGuiHtmlEnvironment
import com.awabi2048.ccsystem.api.gesturegui.layout.*
import com.awabi2048.ccsystem.core.gesturegui.html.GestureGuiHtml
import java.lang.reflect.Proxy
import net.kyori.adventure.text.Component
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** issue #25のレビューで再現した入力を、HTMLから描画・操作面まで通して検証します。 */
class GestureGuiLayoutRegressionTest {
    private val panel = GestureGuiPanel(width = 2.0, height = 1.0, frameWidth = 0.05)
    private val material = Proxy.newProxyInstance(
        BlockData::class.java.classLoader, arrayOf(BlockData::class.java),
    ) { _, _, _ -> null } as BlockData
    private val environment = object : GestureGuiHtmlEnvironment {
        override fun buttonBackground(background: String?, classes: Set<String>, id: String?) = material
        override fun containerBackground(background: String, classes: Set<String>, id: String?) = material
        override fun outlineBlock(classes: Set<String>, id: String?) = material
        override fun blockData(material: String, classes: Set<String>, id: String?) = this@GestureGuiLayoutRegressionTest.material
        override fun itemStack(reference: String, classes: Set<String>, id: String?): ItemStack = error("このテストでは使用しません")
        override fun textSize(tag: String, classes: Set<String>, id: String?) = 0.006
    }
    private val actions: Map<String, (GestureGuiActionContext) -> Unit> = mapOf("go" to {})

    private fun parse(html: String) = GestureGuiHtml.parse(html, environment, "review.html", panel)
    private fun resolve(html: String): ResolvedGestureGui {
        val parsed = parse(html)
        assertTrue(parsed.diagnostics.isEmpty(), parsed.diagnostics.toString())
        return GestureGuiLayoutEngine.layout(parsed.document)
    }
    private fun compile(resolved: ResolvedGestureGui) = GestureGuiLayoutCompiler.compile(resolved, "test", actions)
    private fun width(bounds: GestureGuiBounds) = bounds.maxX - bounds.minX
    private fun height(bounds: GestureGuiBounds) = bounds.maxY - bounds.minY
    private fun nodes(node: ResolvedGestureGuiNode): List<ResolvedGestureGuiNode> = listOf(node) + node.children.flatMap(::nodes)

    @Test
    fun `通常Blockの描画と操作面を同じ領域で切り抜く`() {
        val resolved = resolve("""
            <div id="view" style="width: 0.5; height: 0.5; overflow: hidden">
              <mc-block id="tile" action="go" material="stone" style="width: 1; height: 0.2"/>
            </div>
        """)
        val compiled = compile(resolved)
        val visual = compiled.view.visuals.single() as GestureGuiVisual.Block
        val element = compiled.view.definition.elements.single()
        assertEquals(0.5, visual.width, 1e-9)
        assertEquals(width(element.bounds), visual.width, 1e-9)
        assertEquals((element.bounds.minX + element.bounds.maxX) / 2, visual.x, 1e-9)
        assertEquals(visual.visualId, element.targetVisualId)
        assertTrue(compiled.diagnostics.any { it.code == GestureGuiLayoutErrorCode.LAYOUT_OVERFLOW })
    }

    @Test
    fun `完全に領域外の入れ子CLIPは空のまま伝播する`() {
        val resolved = resolve("""
            <div id="outer" style="width: 0.5; height: 0.5; overflow: hidden">
              <div id="inner" style="position: absolute; left: 1; width: 0.5; height: 0.5; overflow: hidden">
                <button id="outside" action="go">領域外</button>
              </div>
            </div>
        """)
        nodes(resolved.root).drop(1).forEach {
            assertEquals(GestureGuiClip.Empty, it.effectiveClip)
            assertNull(it.interactionBounds)
        }
        val compiled = compile(resolved)
        assertTrue(compiled.view.visuals.isEmpty())
        assertTrue(compiled.view.definition.elements.isEmpty())
    }

    @Test
    fun `部分的にはみ出す文字とその操作面を一緒に除去する`() {
        val resolved = resolve("""
            <div style="width: 0.5; height: 0.5; overflow: hidden">
              <p id="text" action="go" style="width: 1; height: 0.2">文字</p>
            </div>
        """)
        val compiled = compile(resolved)
        assertTrue(compiled.view.visuals.isEmpty())
        assertTrue(compiled.view.definition.elements.isEmpty())
        assertTrue(compiled.diagnostics.any { it.code == GestureGuiLayoutErrorCode.CLIPPED_INTERACTION })
    }

    @Test
    fun `子の表示が全て消えた複合部品の操作も除去する`() {
        val resolved = resolve("""
            <div style="width: 0.5; height: 0.5; overflow: hidden">
              <div id="action" action="go" style="width: 0.5; height: 0.3">
                <p style="width: 1; height: 0.1">領域外を含む文字</p>
              </div>
            </div>
        """)
        assertTrue(compile(resolved).view.definition.elements.isEmpty())
    }

    @Test
    fun `BoxとColumnは確定高さから割合と残余を配分する`() {
        for (layout in listOf("display: block", "display: flex; flex-direction: column")) {
            val resolved = resolve("""
                <div style="$layout; width: 1; height: 0.8">
                  <p id="half" style="height: 50%">半分</p>
                  <p id="rest" style="height: 1fr">残り</p>
                </div>
            """)
            assertTrue(resolved.diagnostics.isEmpty(), resolved.diagnostics.toString())
            resolved.root.children.forEach { assertEquals(0.4, height(it.borderBounds), 1e-9) }
            assertEquals(resolved.root.children[0].borderBounds.minY, resolved.root.children[1].borderBounds.maxY, 1e-9)
        }
    }

    @Test
    fun `Fractionの配分はpaddingとmarginとgapを差し引く`() {
        val resolved = resolve("""
            <div style="height: 0.9; padding: 0.05; gap: 0.05">
              <p style="height: 0.1">固定</p>
              <p style="height: 1fr; margin: 0.05 0">一部</p>
              <p style="height: 3fr">三部</p>
            </div>
        """)
        assertTrue(resolved.diagnostics.isEmpty(), resolved.diagnostics.toString())
        val children = resolved.root.children
        assertEquals(0.125, height(children[1].borderBounds), 1e-9)
        assertEquals(0.375, height(children[2].borderBounds), 1e-9)
        assertEquals(resolved.root.contentBounds.minY, children.last().borderBounds.minY, 1e-9)
    }

    @Test
    fun `背景を追加しても内容の寸法と絶対配置とIDが変わらない`() {
        for (size in listOf("height: 0.4", "height: auto")) {
            fun html(background: String) = """
                <div style="width: 1; height: 0.8">
                  <div id="card" action="go" style="width: 50%; $size; margin: 0.02; padding: 0.03; position: absolute; left: 0.1; top: 0.1; $background">
                    <p id="label" style="height: 0.1">内容</p>
                  </div>
                </div>
            """
            val plain = resolve(html(""))
            val decorated = resolve(html("background: stone"))
            assertTrue(decorated.diagnostics.isEmpty(), decorated.diagnostics.toString())
            val before = nodes(plain.root).single { it.nodeId == "label" }
            val after = nodes(decorated.root).single { it.nodeId == "label" }
            assertEquals(before.borderBounds, after.borderBounds)
            assertEquals(1, nodes(decorated.root).count { it.nodeId == "card" })
            val card = nodes(decorated.root).single { it.nodeId == "card" }
            assertEquals(if (size == "height: auto") 0.16 else 0.4, height(card.borderBounds), 1e-9)
            assertEquals(compile(plain).view.definition.elements.single().bounds, compile(decorated).view.definition.elements.single().bounds)
        }
    }

    @Test
    fun `HTMLのViewportは既定でCLIPとなり明示VISIBLEだけが解除する`() {
        fun html(overflow: String) = """
            <gesture-viewport id="view" style="width: 0.5; height: 0.5; $overflow">
              <mc-block material="stone" style="width: 1; height: 0.2"/>
            </gesture-viewport>
        """
        val clipped = resolve(html(""))
        assertTrue(clipped.root.effectiveClip is GestureGuiClip.Region)
        assertEquals(0.5, (compile(clipped).view.visuals.single() as GestureGuiVisual.Block).width, 1e-9)
        val visible = resolve(html("overflow: visible"))
        assertEquals(GestureGuiClip.Unbounded, visible.root.effectiveClip)
        assertEquals(1.0, (compile(visible).view.visuals.single() as GestureGuiVisual.Block).width, 1e-9)
    }

    @Test
    fun `未対応CSSと適用対象外CSSをインラインとスタイルシートの両方で診断する`() {
        for (html in listOf(
            "<p style='color: red; animation: spin 1s; padding: 0.1'>文字</p>",
            "<style>p { color: red; animation: spin 1s; padding: 0.1; }</style><p>文字</p>",
        )) {
            val parsed = parse(html)
            for (property in listOf("color", "animation", "padding")) {
                assertTrue(parsed.diagnostics.any { it.code == GestureGuiLayoutErrorCode.UNSUPPORTED_PROPERTY && it.message.contains(property) })
            }
        }
    }

    @Test
    fun `不正font-sizeを例外ではなく診断にする`() {
        for (tag in listOf("p", "span", "button")) for (value in listOf("0", "-1", "NaN", "Infinity", "不正")) {
            val parsed = parse("<$tag action='go' style='font-size: $value'>文字</$tag>")
            assertTrue(parsed.diagnostics.any { it.code == GestureGuiLayoutErrorCode.UNSUPPORTED_PROPERTY })
            compile(GestureGuiLayoutEngine.layout(parsed.document))
        }
    }

    @Test
    fun `余白と最小最大寸法の不正トークンを捨てずに診断する`() {
        val parsed = parse("<div style='padding: 0.1 bad; width: 0.5; min-width: bad; max-width: NaN'><p>文字</p></div>")
        assertEquals(3, parsed.diagnostics.size)
        assertTrue(parsed.diagnostics.all { it.code == GestureGuiLayoutErrorCode.UNSUPPORTED_PROPERTY })
    }

    @Test
    fun `Custom出力の枠とBlockを切り抜き消えた文字の操作面を除去する`() {
        val resolved = resolve("""
            <gesture-viewport style="width: 0.5; height: 0.5">
              <custom id="graph" renderer="graph"/>
            </gesture-viewport>
        """)
        val compiled = GestureGuiLayoutCompiler.compile(resolved, "custom", actions, mapOf(
            "graph" to GestureGuiCustomRenderer { _, node ->
                val bounds = node.borderBounds
                val x = (bounds.minX + bounds.maxX) / 2
                val y = (bounds.minY + bounds.maxY) / 2
                GestureGuiCustomRenderResult(
                    visuals = listOf(
                        GestureGuiVisual.Block("block", x, y, 1.0, 0.2, material, outline = GestureGuiOutline(material)),
                        GestureGuiVisual.Text("text", x + 0.25, y, Component.text("境界")),
                    ),
                    elements = listOf(
                        GestureGuiCustomElement("block-action", GestureGuiBounds(x - 0.5, y - 0.1, x + 0.5, y + 0.1), actionId = "go", targetVisualId = "block"),
                        GestureGuiCustomElement("text-action", bounds, actionId = "go", targetVisualId = "text"),
                    ),
                    visualBounds = mapOf("text" to GestureGuiBounds(x + 0.2, y - 0.05, x + 0.3, y + 0.05)),
                )
            },
        ))
        assertFalse(compiled.view.visuals.any { it is GestureGuiVisual.Text })
        assertTrue(compiled.view.visuals.size > 1)
        compiled.view.visuals.filterIsInstance<GestureGuiVisual.Block>().forEach {
            assertTrue(resolved.root.effectiveClip.contains(GestureGuiBounds(it.x - it.width / 2, it.y - it.height / 2, it.x + it.width / 2, it.y + it.height / 2)))
        }
        val element = compiled.view.definition.elements.single()
        assertEquals("graph/block", element.targetVisualId)
        assertEquals(0.5, width(element.bounds), 1e-9)
    }

    @Test
    fun `Custom文字の表示矩形不足を診断して操作も除去する`() {
        val resolved = resolve("<gesture-viewport><custom id='graph' renderer='graph'/></gesture-viewport>")
        val compiled = GestureGuiLayoutCompiler.compile(resolved, "custom", actions, mapOf(
            "graph" to GestureGuiCustomRenderer { _, node -> GestureGuiCustomRenderResult(
                visuals = listOf(GestureGuiVisual.Text("text", 0.0, 0.0, Component.text("文字"))),
                elements = listOf(GestureGuiCustomElement("action", node.borderBounds, actionId = "go", targetVisualId = "text")),
            ) },
        ))
        assertTrue(compiled.diagnostics.any { it.code == GestureGuiLayoutErrorCode.CUSTOM_VISUAL_BOUNDS_MISSING })
        assertTrue(compiled.view.visuals.isEmpty())
        assertTrue(compiled.view.definition.elements.isEmpty())
    }

    @Test
    fun `Custom文字とホバー参照を同じ名前空間へ変換する`() {
        val resolved = resolve("<gesture-viewport><custom id='graph' renderer='graph'/></gesture-viewport>")
        var handled = 0
        val compiled = GestureGuiLayoutCompiler.compile(resolved, "custom", mapOf("go" to { handled++ }), mapOf(
            "graph" to GestureGuiCustomRenderer { _, _ -> GestureGuiCustomRenderResult(
                visuals = listOf(
                    GestureGuiVisual.Text("text", 0.0, 0.0, Component.text("文字")),
                    GestureGuiVisual.Block("block", 0.0, 0.0, 0.2, 0.2, material),
                ),
                elements = listOf(GestureGuiCustomElement(
                    "action", GestureGuiBounds(-0.1, -0.1, 0.1, 0.1), actionId = "go", targetVisualId = "text",
                    hoverText = GestureGuiHoverText(
                        Component.text("補足"), 0.0, 0.0, replacesVisualId = "text",
                        hoverBlockVisualId = "block", hoverBlockData = material,
                    ),
                )),
                visualBounds = mapOf("text" to GestureGuiBounds(-0.1, -0.05, 0.1, 0.05)),
            ) },
        ))
        assertTrue(compiled.diagnostics.isEmpty(), compiled.diagnostics.toString())
        val element = compiled.view.definition.elements.single()
        assertEquals("graph/text", element.targetVisualId)
        assertEquals("graph/text", element.hoverText!!.replacesVisualId)
        assertEquals("graph/block", element.hoverText.hoverBlockVisualId)
        compiled.view.onAction(GestureGuiActionContext(
            java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "custom", element.elementId,
            GestureGuiGesture.PRIMARY, 1L,
        ))
        assertEquals(1, handled)
    }

    @Test
    fun `Customの存在しない表示参照を例外にせず診断する`() {
        val resolved = resolve("<custom id='graph' renderer='graph'/>")
        val compiled = GestureGuiLayoutCompiler.compile(resolved, "custom", actions, mapOf(
            "graph" to GestureGuiCustomRenderer { _, node -> GestureGuiCustomRenderResult(
                visuals = emptyList(),
                elements = listOf(GestureGuiCustomElement("action", node.borderBounds, actionId = "go", targetVisualId = "missing")),
            ) },
        ))
        assertTrue(compiled.diagnostics.any { it.code == GestureGuiLayoutErrorCode.UNKNOWN_VISUAL })
        assertTrue(compiled.view.definition.elements.isEmpty())
    }
}
