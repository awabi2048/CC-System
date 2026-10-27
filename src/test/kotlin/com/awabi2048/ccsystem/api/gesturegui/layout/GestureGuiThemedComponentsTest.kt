package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.theme.GestureGuiTheme
import java.lang.reflect.Proxy
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.data.BlockData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * テーマ適用部品のテストです。サーバーなしで実行できます。
 * 状態（選択・設定済・無効等）から規則どおりの素材・枠・操作面が
 * 導かれることを検証します。
 */
class GestureGuiThemedComponentsTest {
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

    private fun bgOf(node: GestureGuiNode): GestureGuiBlock =
        (node as GestureGuiOverlay).children.filterIsInstance<GestureGuiBlock>().single()

    private fun labelOf(node: GestureGuiNode): GestureGuiText =
        (node as GestureGuiOverlay).children.filterIsInstance<GestureGuiText>().single()

    @Test
    fun `selected tab extends fixed width rightward and keeps label box at base width`() {
        val tab = GestureGuiComponents.tab(
            id = "tab-1",
            label = Component.text("項目"),
            actionId = "tab-1",
            theme = theme,
            selected = true,
            baseWidth = GestureGuiSizeSpec.Fixed(0.4),
        ) as GestureGuiOverlay
        // 0.4 * 1.15 は浮動小数点で厳密値を持たないため、成分比較に留めます。
        assertEquals(0.4 * 1.15, (tab.width as GestureGuiSizeSpec.Fixed).value, 1e-9)
        assertSame(theme.accentDeep, bgOf(tab).blockData)
        val label = labelOf(tab)
        assertEquals(GestureGuiSizeSpec.Fixed(0.4), label.width)
        // 左端固定のため、ラベル箱の交差軸は先頭寄せです。
        assertEquals(GestureGuiCrossAlignment.START, tab.crossAlignment)
    }

    @Test
    fun `unselected tab keeps base width and surface background`() {
        val tab = GestureGuiComponents.tab(
            id = "tab-1",
            label = Component.text("項目"),
            actionId = "tab-1",
            theme = theme,
            selected = false,
            baseWidth = GestureGuiSizeSpec.Fixed(0.4),
        ) as GestureGuiOverlay
        assertEquals(GestureGuiSizeSpec.Fixed(0.4), tab.width)
        assertSame(theme.surface, bgOf(tab).blockData)
    }

    @Test
    fun `warning tab renders red label`() {
        val tab = GestureGuiComponents.tab(
            id = "tab-1",
            label = Component.text("項目"),
            actionId = "tab-1",
            theme = theme,
            selected = false,
            warning = true,
        )
        assertEquals(NamedTextColor.RED, labelOf(tab).text.color())
    }

    @Test
    fun `pager disables edge arrows with consume surface`() {
        val pager = GestureGuiComponents.pager(
            id = "pager",
            theme = theme,
            status = Component.text("1/3"),
            prevActionId = null,
            nextActionId = "page-next",
        ) as GestureGuiRow
        val prev = pager.children[0] as GestureGuiOverlay
        val next = pager.children[2] as GestureGuiOverlay
        assertNull(prev.actionId)
        assertTrue(prev.consumeInput)
        assertSame(theme.disabled, bgOf(prev).blockData)
        assertEquals("page-next", next.actionId)
        assertSame(theme.accent, bgOf(next).blockData)
        assertTrue((pager.children[1] as GestureGuiText).text.let { it == Component.text("1/3") })
    }

    @Test
    fun `choice card maps selection and configured state to outlines`() {
        val selected = GestureGuiComponents.choiceCard(
            id = "c", label = Component.text("a"), theme = theme,
            actionId = "pick", selected = true, configured = true,
        ) as GestureGuiOverlay
        assertSame(theme.outlineSelected, bgOf(selected).outline?.blockData)

        val configured = GestureGuiComponents.choiceCard(
            id = "c", label = Component.text("a"), theme = theme,
            actionId = "pick", configured = true,
        ) as GestureGuiOverlay
        assertSame(theme.outlineSet, bgOf(configured).outline?.blockData)

        val plain = GestureGuiComponents.choiceCard(
            id = "c", label = Component.text("a"), theme = theme,
            actionId = "pick",
        ) as GestureGuiOverlay
        assertNull(bgOf(plain).outline)
        assertSame(theme.surface, bgOf(plain).blockData)
        assertEquals(setOf(GestureGuiGesture.PRIMARY), plain.acceptedGestures)
    }

    @Test
    fun `toggle card shows value color and disabled card consumes input`() {
        val on = GestureGuiComponents.choiceCard(
            id = "t", label = Component.text("sw"), theme = theme,
            actionId = "toggle", toggleValue = true,
        ) as GestureGuiOverlay
        assertSame(theme.on, bgOf(on).blockData)

        val off = GestureGuiComponents.choiceCard(
            id = "t", label = Component.text("sw"), theme = theme,
            actionId = "toggle", toggleValue = false,
        ) as GestureGuiOverlay
        assertSame(theme.off, bgOf(off).blockData)

        val disabled = GestureGuiComponents.choiceCard(
            id = "t", label = Component.text("sw"), theme = theme,
            actionId = "toggle", enabled = false,
        ) as GestureGuiOverlay
        assertSame(theme.disabled, bgOf(disabled).blockData)
        assertNull(disabled.actionId)
        assertTrue(disabled.consumeInput)
        assertTrue(disabled.acceptedGestures.isEmpty())
        // 無効項目は灰色文字です。
        assertEquals(NamedTextColor.GRAY, labelOf(disabled).text.color())
    }

    @Test
    fun `value card joins label and current value`() {
        val card = GestureGuiComponents.valueCard(
            id = "v",
            label = Component.text("周期"),
            value = Component.text("20"),
            theme = theme,
            actionId = "edit",
            configured = true,
        ) as GestureGuiOverlay
        val label = labelOf(card).text
        // 「項目名 現在値」の結合表示です。
        assertEquals(Component.text("周期").append(Component.text(" ")).append(Component.text("20")), label)
        assertSame(theme.outlineSet, bgOf(card).outline?.blockData)
    }

    @Test
    fun `list row places content then trailing actions`() {
        val content = GestureGuiComponents.choiceCard(
            id = "row-card", label = Component.text("a"), theme = theme, actionId = "a",
        )
        val action = GestureGuiComponents.button(
            id = "row-del", label = Component.text("x"), actionId = "del",
            background = theme.danger,
        )
        val row = GestureGuiComponents.listRow(id = "row-1", content = content, actions = listOf(action)) as GestureGuiRow
        assertEquals(2, row.children.size)
        assertEquals("row-card", row.children[0].id)
        assertEquals("row-del", row.children[1].id)
    }
}
