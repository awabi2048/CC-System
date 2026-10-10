package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.theme.GestureGuiTheme
import java.lang.reflect.Proxy
import net.kyori.adventure.text.Component
import org.bukkit.block.data.BlockData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 領域足場とページ分割のテストです。サーバーなしで実行できます。
 */
class GestureGuiScaffoldTest {
    private fun blockData(): BlockData = Proxy.newProxyInstance(
        BlockData::class.java.classLoader,
        arrayOf(BlockData::class.java),
    ) { _, _, _ -> null } as BlockData

    private val theme = GestureGuiTheme(
        surface = blockData(),
        accent = blockData(),
        accentDeep = blockData(),
        danger = blockData(),
        back = blockData(),
        on = blockData(),
        off = blockData(),
        outlineSelected = blockData(),
        outlineSet = blockData(),
        disabled = blockData(),
        divider = blockData(),
        scrim = blockData(),
        scrimDanger = blockData(),
    )

    private fun box(id: String): GestureGuiNode =
        GestureGuiBox(children = emptyList(), id = id)

    private fun childIds(node: GestureGuiNode): List<String?> = when (node) {
        is GestureGuiBox -> node.children
        is GestureGuiRow -> node.children
        is GestureGuiColumn -> node.children
        is GestureGuiGrid -> node.children
        is GestureGuiOverlay -> node.children
        else -> emptyList()
    }.map { it.id }

    @Test
    fun `paged screen reserves footer then splits main area top and bottom`() {
        val screen = GestureGuiScaffold.pagedScreen(
            id = "screen",
            theme = theme,
            header = box("hdr"),
            top = box("cards"),
            bottom = box("bottom"),
            footer = box("ftr"),
        ) as GestureGuiColumn

        assertEquals(listOf("screen-content", "ftr"), childIds(screen))

        val content = screen.children[0] as GestureGuiBox
        assertEquals(GestureGuiSizeSpec.Fraction(1.0), content.height)

        val main = content.children.single() as GestureGuiColumn
        assertEquals(GestureGuiMainArrangement.SPACE_BETWEEN, main.mainArrangement)
        assertEquals(listOf("screen-top", "screen-bottom"), childIds(main))

        val top = main.children[0] as GestureGuiColumn
        assertEquals(listOf("hdr", "cards"), childIds(top))

        // 両グループ同居のため境界線が自動挿入され、下揃えグループの先頭に置きます。
        val bottom = main.children[1] as GestureGuiColumn
        assertEquals(listOf("screen-group-divider", "bottom"), childIds(bottom))
    }

    @Test
    fun `divider appears only when both groups exist`() {
        val withoutBottom = GestureGuiScaffold.pagedScreen(
            id = "s", theme = theme, footer = box("f"), top = box("t"),
        ) as GestureGuiColumn
        val main = ((withoutBottom.children[0] as GestureGuiBox).children.single() as GestureGuiColumn)
        assertEquals(listOf("s-top", "s-bottom"), childIds(main))
        assertTrue(childIds(main.children[1]).isEmpty())

        val withBoth = GestureGuiScaffold.pagedScreen(
            id = "s", theme = theme, footer = box("f"), top = box("t"), bottom = box("b"),
        ) as GestureGuiColumn
        val bottom = ((withBoth.children[0] as GestureGuiBox).children.single() as GestureGuiColumn).children[1]
        assertEquals(listOf("s-group-divider", "b"), childIds(bottom))
    }

    @Test
    fun `footer row keeps left and right bands`() {
        val footer = GestureGuiScaffold.footerRow(
            id = "footer",
            left = box("pager"),
            right = box("back"),
        ) as GestureGuiRow
        assertEquals(listOf("footer-left", "footer-right"), childIds(footer))
        val left = footer.children[0] as GestureGuiBox
        assertEquals(listOf("pager"), childIds(left))
        assertEquals(GestureGuiSizeSpec.Fraction(1.0), left.width)
    }

    @Test
    fun `footer band stays when one side is empty`() {
        val footer = GestureGuiScaffold.footerRow(id = "f", left = null, right = box("back")) as GestureGuiRow
        assertEquals(listOf("f-left", "f-right"), childIds(footer))
        assertTrue(childIds(footer.children[0]).isEmpty())
    }

    @Test
    fun `pagination computes count clamped page and slice`() {
        val page = GestureGuiPagination.plan(itemCount = 13, rowsPerPage = 3, columns = 2, requestedPage = 1)
        assertEquals(6, page.pageSize)
        assertEquals(3, page.count)
        assertEquals(1, page.index)
        assertEquals(6, page.start)
        assertEquals(12, page.endExclusive)
        assertEquals(listOf(6, 7, 8, 9, 10, 11), GestureGuiPagination.pageOf((0 until 13).toList(), page))
    }

    @Test
    fun `pagination clamps requested page and keeps one page for empty list`() {
        val clamped = GestureGuiPagination.plan(itemCount = 3, rowsPerPage = 2, requestedPage = 9)
        assertEquals(1, clamped.index)
        assertEquals(2, clamped.count)
        assertEquals(2, clamped.start)
        assertEquals(3, clamped.endExclusive)

        val empty = GestureGuiPagination.plan(itemCount = 0, rowsPerPage = 2)
        assertEquals(1, empty.count)
        assertEquals(0, empty.start)
        assertEquals(0, empty.endExclusive)
    }

    @Test
    fun `pagination rejects non-positive rows and columns`() {
        assertThrows(IllegalArgumentException::class.java) {
            GestureGuiPagination.plan(itemCount = 1, rowsPerPage = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GestureGuiPagination.plan(itemCount = 1, rowsPerPage = 1, columns = 0)
        }
    }

    @Test
    fun `description area hosts a body and optional detail with replaceable id`() {
        val area = GestureGuiComponents.description(
            id = "desc",
            body = Component.text("body"),
            detail = Component.text("detail"),
            theme = theme,
        ) as GestureGuiColumn
        val texts = area.children.filterIsInstance<GestureGuiText>()
        assertEquals(listOf("desc", "desc-detail"), texts.map { it.id })
        assertTrue(texts.all { it.size == theme.textCaption })
    }
}
