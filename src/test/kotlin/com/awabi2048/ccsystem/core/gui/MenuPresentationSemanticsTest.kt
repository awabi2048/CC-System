package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MenuPresentationSemanticsTest {
    private val factory = MenuPresentationSemanticsFactory { _, _, _ -> "クリック" }

    @Test
    fun `composer semantics preserve canonical blocks and standard action label`() {
        val actions = listOf(GuiLoreLine.Interaction(null, MenuGesture.ANY, "開く"))
        val lore = GuiLoreComposer.compose(
            GuiLoreSpec.Blocks(listOf(
                GuiLoreBlock(listOf(GuiLoreLine.Text("説明"))),
                GuiLoreBlock(listOf(GuiLoreLine.Data("値", "1", "§f"))),
                GuiLoreBlock(listOf(GuiLoreLine.Option("選択", true, "§e", "§7"))),
                GuiLoreBlock(listOf(GuiLoreLine.Warning("警告"))),
                GuiLoreBlock(listOf(GuiLoreLine.Danger("危険"))),
            )),
            actions,
        )
        val semantics = factory.create(
            GuiNameSpec.FixedLabel(Component.text("設定")), lore, MenuPresentationProfile.SINGLE_STANDARD_ACTION,
        )
        assertEquals(MenuNameSemantic.FIXED_LABEL, semantics.name)
        assertEquals(GuiLoreFrame.BOTH, semantics.lore.frame)
        assertEquals(
            listOf(MenuLoreLineKind.DESCRIPTION, MenuLoreLineKind.DATA, MenuLoreLineKind.CHOICE,
                MenuLoreLineKind.WARNING, MenuLoreLineKind.DANGER, MenuLoreLineKind.ACTION),
            semantics.lore.blocks.flatMap { it.lines }.map { it.kind },
        )
        val action = semantics.lore.blocks.last().lines.single().action!!
        assertEquals(MenuAcceptedClicks.STANDARD, action.acceptedClicks)
        assertEquals("クリック", action.operationLabel)
        assertEquals("開く", action.actionLabel)
        assertTrue(MenuPresentationSemanticsValidator.violations(semantics).isEmpty())
    }

    @Test
    fun `action only has no leading spacer and stays in the same frame`() {
        val lore = GuiLoreComposer.compose(
            GuiLoreSpec.None,
            listOf(GuiLoreLine.Interaction(null, MenuGesture.ANY, "実行")),
        )
        val semantics = factory.create(GuiNameSpec.Empty, lore, MenuPresentationProfile.SINGLE_STANDARD_ACTION)
        assertEquals(GuiLoreFrame.BOTH, semantics.lore.frame)
        assertEquals(MenuLoreLineKind.ACTION, semantics.lore.blocks.single().lines.single().kind)
        assertTrue(MenuPresentationSemanticsValidator.violations(semantics).isEmpty())
    }

    @Test
    fun `validator rejects wrong order and disabled action`() {
        val badLore = MenuLoreSemantics(MenuLoreSemanticSource.STRUCTURED, GuiLoreFrame.BOTH, listOf(
            MenuLoreBlockSemantics(listOf(
                MenuLoreLineSemantics(MenuLoreLineKind.WARNING),
                MenuLoreLineSemantics(MenuLoreLineKind.DESCRIPTION),
            )),
        ))
        assertTrue(MenuPresentationSemanticsValidator.violations(
            MenuElementPresentationSemantics(MenuNameSemantic.FIXED_LABEL, badLore, MenuPresentationProfile.DISABLED),
        ).containsAll(listOf("LORE_KIND_ORDER", "DISABLED_REASON_MISSING")))
    }

    @Test
    fun `name kinds legacy opacity and defensive collections are preserved by semantic copy`() {
        assertEquals(MenuNameSemantic.TARGET_IDENTITY, factory.create(
            GuiNameSpec.TargetIdentity(Component.text("world")), GuiLoreSpec.None, MenuPresentationProfile.LIST_TARGET,
        ).name)
        assertEquals(MenuNameSemantic.OPAQUE, factory.create(
            GuiNameSpec.Text("legacy", GuiNameStyle.DEFAULT), GuiLoreSpec.None, MenuPresentationProfile.UNKNOWN,
        ).name)
        val clicks = linkedSetOf(ClickType.LEFT)
        val action = MenuLoreActionSemantics(
            GuiInputGesture.MenuClicks(clicks), clicks, "左クリック", "実行",
        )
        clicks += ClickType.RIGHT
        assertEquals(setOf(ClickType.LEFT), action.acceptedClicks)

        val semantics = factory.create(
            GuiNameSpec.FixedLabel(Component.text("固定")), GuiLoreSpec.None, MenuPresentationProfile.DISPLAY_ONLY,
        )
        assertEquals(semantics, semantics.copy())
    }

    @Test
    fun `explicit disabled semantics retain reason and no action`() {
        val reason = Component.text("利用できません")
        val semantics = factory.create(
            GuiNameSpec.FixedLabel(Component.text("設定")), GuiLoreSpec.None,
            MenuPresentationProfile.DISABLED, reason,
        )
        assertEquals(reason, semantics.disabledReason)
        assertTrue(MenuPresentationSemanticsValidator.violations(semantics).isEmpty())
    }

    @Test
    fun `inspection copy path retains presentation semantics`() {
        val semantics = factory.create(
            GuiNameSpec.FixedLabel(Component.text("固定")), GuiLoreSpec.None,
            MenuPresentationProfile.DISPLAY_ONLY,
        )
        val slot = MenuRuntimeInspectionSlotSnapshot(
            0, Material.STONE, 1, Component.text("固定"), emptyList(), false,
            GuiElementRole.CONTENT, true,
            MenuRuntimeInspectionInteractionSnapshot(MenuRuntimeInteractionKind.DISPLAY_ONLY),
        ).also { it.presentationSemantics = semantics }
        assertEquals(semantics, slot.copyWithPresentationSemantics().presentationSemantics)
    }
}
