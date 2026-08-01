package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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
    fun `every structured lore line subtype maps exhaustively to a semantic kind`() {
        val lines = listOf(
            GuiLoreLine.Spacer to MenuLoreLineKind.SPACER,
            GuiLoreLine.Separator to MenuLoreLineKind.SEPARATOR,
            GuiLoreLine.Data("d", "v", "§f") to MenuLoreLineKind.DATA,
            GuiLoreLine.ComponentData("d", Component.text("v"), "§f") to MenuLoreLineKind.DATA,
            GuiLoreLine.SubData("d", "v") to MenuLoreLineKind.DATA,
            GuiLoreLine.Metadata("d", "v") to MenuLoreLineKind.DATA,
            GuiLoreLine.Interaction(null, MenuGesture.ANY, "run") to MenuLoreLineKind.ACTION,
            GuiLoreLine.Option("choice", true, "§a", "§7") to MenuLoreLineKind.CHOICE,
            GuiLoreLine.Warning("warning") to MenuLoreLineKind.WARNING,
            GuiLoreLine.Danger("danger") to MenuLoreLineKind.DANGER,
            GuiLoreLine.Text("text") to MenuLoreLineKind.DESCRIPTION,
            GuiLoreLine.StyledText("text", "§f", false) to MenuLoreLineKind.DESCRIPTION,
            GuiLoreLine.StatusData("s", "v", "§f", GuiStatusTone.COMPLETE) to MenuLoreLineKind.DATA,
            GuiLoreLine.StatusComponentData(Component.text("s"), Component.text("v"), GuiStatusTone.COMPLETE) to MenuLoreLineKind.DATA,
            GuiLoreLine.ProgressPath(listOf("a"), 0) to MenuLoreLineKind.DATA,
            GuiLoreLine.UserText("user") to MenuLoreLineKind.DESCRIPTION,
            GuiLoreLine.Component(Component.text("component")) to MenuLoreLineKind.DESCRIPTION,
            GuiLoreLine.Opaque(Component.text("opaque")) to MenuLoreLineKind.UNKNOWN,
        )

        val mapper = MenuPresentationSemanticsFactory::class.java.getDeclaredMethod(
            "lineSemantics",
            GuiLoreLine::class.java,
        ).also { it.isAccessible = true }
        lines.forEach { (line, expected) ->
            val actual = (mapper.invoke(factory, line) as MenuLoreLineSemantics).kind
            assertEquals(expected, actual, line::class.simpleName)
        }
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
        assertEquals(MenuNameSemantic.OPAQUE, factory.create(
            GuiNameSpec.Opaque(Component.text("styled")), GuiLoreSpec.None, MenuPresentationProfile.UNKNOWN,
        ).name)
        GuiLoreFrame.entries.forEach { frame ->
            val lore = GuiLoreSpec.FramedBlocks(
                listOf(GuiLoreBlock(listOf(GuiLoreLine.Text("body")))),
                frame,
            )
            assertEquals(frame, factory.create(
                GuiNameSpec.FixedLabel(Component.text("label")), lore, MenuPresentationProfile.DISPLAY_ONLY,
            ).lore.frame)
        }
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
    fun `disabled semantics reject reasons without visible plain text`() {
        val invalidReasons = listOf<Component?>(
            null,
            Component.empty(),
            Component.empty().color(NamedTextColor.RED),
            Component.text(" \n\t "),
        )
        invalidReasons.forEach { reason ->
            val semantics = factory.create(
                GuiNameSpec.FixedLabel(Component.text("設定")),
                GuiLoreSpec.None,
                MenuPresentationProfile.DISABLED,
                reason,
            )
            assertTrue(
                MenuPresentationSemanticsValidator.violations(semantics)
                    .contains("DISABLED_REASON_MISSING"),
            )
        }
    }

    @Test
    fun `disabled semantics accept visible and translatable reasons`() {
        listOf(
            Component.text("利用できません"),
            Component.translatable("gui.disabled.reason"),
        ).forEach { reason ->
            val semantics = factory.create(
                GuiNameSpec.FixedLabel(Component.text("設定")),
                GuiLoreSpec.None,
                MenuPresentationProfile.DISABLED,
                reason,
            )
            assertFalse(
                MenuPresentationSemanticsValidator.violations(semantics)
                    .contains("DISABLED_REASON_MISSING"),
            )
        }
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
