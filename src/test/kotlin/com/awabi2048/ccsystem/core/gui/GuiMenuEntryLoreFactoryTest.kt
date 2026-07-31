package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiMenuActionIntent
import com.awabi2048.ccsystem.api.gui.GuiMenuEntryData
import com.awabi2048.ccsystem.api.gui.GuiMenuEntryOption
import com.awabi2048.ccsystem.api.gui.GuiMenuEntrySpec
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiNameSpec
import com.awabi2048.ccsystem.api.gui.GuiValueTone
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GuiMenuEntryLoreFactoryTest {
    @Test
    fun `one action joins the description block`() {
        val spec = GuiMenuEntrySpec(
            slot = 0,
            material = Material.STONE,
            name = GuiNameSpec.Text("設定", com.awabi2048.ccsystem.api.gui.GuiNameStyle.DEFAULT),
            role = GuiElementRole.ACTION,
            description = listOf("項目の説明"),
            actions = listOf(GuiMenuActionIntent.AnyClick("open", "メニューを開く")),
        )

        val lore = GuiMenuEntryLoreFactory.build(spec, spec.expandedActions(), null)
        assertTrue(lore is GuiLoreSpec.Blocks)
        val blocks = (lore as GuiLoreSpec.Blocks).blocks
        assertEquals(1, blocks.size)
        assertTrue(blocks.single().lines[0] is GuiLoreLine.Text)
        assertEquals(GuiLoreLine.Spacer, blocks.single().lines[1])
        assertTrue(blocks.single().lines[2] is GuiLoreLine.Interaction)
    }

    @Test
    fun `without description an action stays separate from data and warnings`() {
        val spec = GuiMenuEntrySpec(
            slot = 0,
            material = Material.STONE,
            name = GuiNameSpec.Empty,
            role = GuiElementRole.ACTION,
            data = listOf(GuiMenuEntryData("値", "現在値", GuiValueTone.DEFAULT)),
            warnings = listOf("注意"),
            actions = listOf(GuiMenuActionIntent.AnyClick("open", "開く")),
        )

        val blocks = (GuiMenuEntryLoreFactory.build(spec, spec.expandedActions(), null) as GuiLoreSpec.Blocks).blocks
        assertEquals(3, blocks.size)
        assertTrue(blocks.last().lines.single() is GuiLoreLine.Interaction)
    }

    @Test
    fun `multiple actions use an independent action block`() {
        val spec = GuiMenuEntrySpec(
            slot = 0,
            material = Material.STONE,
            name = GuiNameSpec.Empty,
            role = GuiElementRole.ACTION,
            description = listOf("項目の説明"),
            actions = listOf(
                GuiMenuActionIntent.LeftRight(
                    GuiMenuActionIntent.AnyClick("left", "左の操作"),
                    GuiMenuActionIntent.AnyClick("right", "右の操作"),
                )
            ),
        )

        val blocks = (GuiMenuEntryLoreFactory.build(spec, spec.expandedActions(), null) as GuiLoreSpec.Blocks).blocks
        assertEquals(2, blocks.size)
        assertEquals(1, blocks.first().lines.size)
        assertEquals(2, blocks.last().lines.size)
    }

    @Test
    fun `semantic action intents produce the required common contracts`() {
        val anySpec = GuiMenuEntrySpec(
            0, Material.STONE, GuiNameSpec.Empty, GuiElementRole.ACTION,
            actions = listOf(GuiMenuActionIntent.AnyClick("any", "クリック")),
        )
        val any = anySpec.expandedActions().single()
        assertEquals(com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks.STANDARD, any.acceptedClicks)

        val leftRightSpec = GuiMenuEntrySpec(
            0, Material.STONE, GuiNameSpec.Empty, GuiElementRole.ACTION,
            actions = listOf(GuiMenuActionIntent.LeftRight(
            GuiMenuActionIntent.AnyClick("left", "左"),
            GuiMenuActionIntent.AnyClick("right", "右"),
            )),
        )
        val leftRight = leftRightSpec.expandedActions()
        assertEquals(
            setOf(com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks.PLAIN_LEFT,
                com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks.PLAIN_RIGHT),
            leftRight.map { it.acceptedClicks }.toSet(),
        )
        val emptyBack = GuiMenuEntrySpec(
            0, Material.STONE, GuiNameSpec.Empty, GuiElementRole.BACK,
            actions = listOf(GuiMenuActionIntent.Back),
        )
        assertEquals(0, emptyBack.expandedActions().size)
        val confirm = GuiMenuEntrySpec(
            0, Material.STONE, GuiNameSpec.Empty, GuiElementRole.CONFIRM,
            actions = listOf(GuiMenuActionIntent.Confirm("confirm", "確認")),
        )
        assertEquals(1, confirm.expandedActions().size)
        val cancel = GuiMenuEntrySpec(
            0, Material.STONE, GuiNameSpec.Empty, GuiElementRole.CANCEL,
            actions = listOf(GuiMenuActionIntent.Cancel("cancel", "取消")),
        )
        assertEquals(1, cancel.expandedActions().size)
        val page = GuiMenuEntrySpec(
            0, Material.STONE, GuiNameSpec.Empty, GuiElementRole.NAVIGATION,
            actions = listOf(GuiMenuActionIntent.Page(
                GuiMenuActionIntent.Direction.NEXT,
                "next",
                "次へ",
            )),
        )
        assertEquals(1, page.expandedActions().size)
    }

    @Test
    fun `list gestures cover same left right shift and middle contracts`() {
        val intents = listOf(
            GuiMenuActionIntent.LeftRightSame("open", "開く"),
            GuiMenuActionIntent.ShiftAny("force", "強制操作"),
            GuiMenuActionIntent.MiddleClick("preview", "プレビュー"),
        )
        val expanded = intents.flatMap { intent ->
            GuiMenuEntrySpec(
                slot = 0,
                material = Material.STONE,
                name = GuiNameSpec.Empty,
                role = GuiElementRole.ACTION,
                actions = listOf(intent),
            ).expandedActions()
        }
        assertEquals(com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks.LEFT_RIGHT, expanded[0].acceptedClicks)
        assertEquals(com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks.SHIFT_LEFT_RIGHT, expanded[1].acceptedClicks)
        assertEquals(com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks.MIDDLE, expanded[2].acceptedClicks)
    }
}
