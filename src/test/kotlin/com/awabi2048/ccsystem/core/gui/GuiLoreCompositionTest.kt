package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreFrame
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GuiLoreCompositionTest {
    private val service = LoreServiceImpl { _, _, _ -> "クリック" }
    private val action = GuiLoreLine.Interaction(null, com.awabi2048.ccsystem.api.gui.MenuGesture.ANY, "メニュー")

    @Test
    fun `opaque content remains intact and action is last`() {
        val spec = service.compose(
            GuiLoreSpec.Opaque(listOf(net.kyori.adventure.text.Component.text("既存Lore"))),
            listOf(action),
        )

        assertEquals(listOf("既存Lore", "", "クリックでメニュー"), plain(service.render(spec)))
    }

    @Test
    fun `embedded capability blocks remain ordered in completed specification`() {
        val spec = service.compose(
            GuiLoreSpec.Blocks(listOf(
                GuiLoreBlock(listOf(GuiLoreLine.Text("説明"))),
                GuiLoreBlock(listOf(GuiLoreLine.Data("状態", "有効", "§a"))),
            )),
            listOf(GuiLoreLine.Interaction(null, com.awabi2048.ccsystem.api.gui.MenuGesture.ANY, "開く")),
        )

        val composed = spec as GuiLoreSpec.WithActions
        val blocks = composed.base as GuiLoreSpec.Blocks
        assertEquals(2, blocks.blocks.size)
        assertEquals("説明", (blocks.blocks.first().lines.single() as GuiLoreLine.Text).text)
        assertEquals("開く", composed.actions.single().label)
        assertTrue(plain(service.render(spec)).any { it.contains("開く") })
    }

    @Test
    fun `framed blocks preserve every requested frame and none adds no separator`() {
        val block = GuiLoreBlock(listOf(GuiLoreLine.Text("body")))
        val rendered = GuiLoreFrame.entries.associateWith { frame ->
            plain(service.render(GuiLoreSpec.FramedBlocks(listOf(block), frame)))
        }

        assertEquals(listOf("body"), rendered.getValue(GuiLoreFrame.NONE))
        assertEquals(2, rendered.getValue(GuiLoreFrame.TOP).size)
        assertEquals("body", rendered.getValue(GuiLoreFrame.TOP).last())
        assertEquals("body", rendered.getValue(GuiLoreFrame.BOTTOM).first())
        assertEquals(2, rendered.getValue(GuiLoreFrame.BOTTOM).size)
        assertEquals(3, rendered.getValue(GuiLoreFrame.BOTH).size)
    }

    private fun plain(lines: List<net.kyori.adventure.text.Component>): List<String> =
        lines.map(PlainTextComponentSerializer.plainText()::serialize)
}
