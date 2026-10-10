package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.html.GestureGuiHtmlEnvironment
import com.awabi2048.ccsystem.api.gesturegui.theme.GestureGuiThemes
import com.awabi2048.ccsystem.core.gesturegui.layout.GestureGuiLayoutEngine
import java.lang.reflect.Proxy
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `.html` リソース同梱・基底スタイルシート適用のテストです。サーバーなしで実行できます。
 */
class GestureGuiHtmlResourceTest {
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
    fun `resource parses with bundled base stylesheet`() {
        val parsed = GestureGuiLayoutFacade.parseHtmlResource(
            resourcePath = "gesture-gui/test-screen.html",
            loader = javaClass.classLoader,
            environment = environment,
            panel = panel,
        )
        assertTrue(parsed.diagnostics.isEmpty(), "diagnostics: ${parsed.diagnostics}")

        // 語彙クラス由来の構造を確認します（screen → content + footer）。
        val screen = parsed.document.root as GestureGuiColumn
        assertEquals(GestureGuiSizeSpec.Percent(1.0), screen.height)
        assertEquals(2, screen.children.size)

        val content = screen.children[0] as GestureGuiColumn
        assertEquals(GestureGuiSizeSpec.Fraction(1.0), content.height)
        assertEquals(GestureGuiMainArrangement.SPACE_BETWEEN, content.mainArrangement)

        val footer = screen.children[1] as GestureGuiRow
        assertEquals(GestureGuiSizeSpec.Fixed(0.10), footer.height)
        // 帯の区画は等幅 Fraction で、片側が空でも帯を維持します。
        assertEquals(2, footer.children.size)
        assertTrue(footer.children.all { (it as GestureGuiBox).width == GestureGuiSizeSpec.Fraction(1.0) })

        val resolved = GestureGuiLayoutEngine.layout(parsed.document)
        assertTrue(resolved.diagnostics.isEmpty(), "diagnostics: ${resolved.diagnostics}")
    }

    @Test
    fun `document name defaults to resource file name`() {
        val parsed = GestureGuiLayoutFacade.parseHtmlResource(
            resourcePath = "gesture-gui/broken.html",
            loader = javaClass.classLoader,
            environment = environment,
            panel = panel,
        )
        assertTrue(
            parsed.diagnostics.any {
                it.code == GestureGuiLayoutErrorCode.UNSUPPORTED_TAG && it.source == "broken.html"
            },
            "diagnostics: ${parsed.diagnostics}",
        )
    }

    @Test
    fun `missing resource is an explicit error`() {
        assertThrows(IllegalArgumentException::class.java) {
            GestureGuiLayoutFacade.parseHtmlResource(
                resourcePath = "gesture-gui/not-found.html",
                loader = javaClass.classLoader,
                environment = environment,
            )
        }
    }

    @Test
    fun `bundled base stylesheet is available from main resources`() {
        val css = GestureGuiThemes.baseStyleSheet()
        assertTrue(css.contains(".screen"), css)
        assertTrue(css.contains(".card"), css)
        assertTrue(css.contains(".tab.selected"), css)
    }
}
