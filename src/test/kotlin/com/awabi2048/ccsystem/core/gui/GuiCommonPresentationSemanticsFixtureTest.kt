package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.*
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material

class GuiCommonPresentationSemanticsFixtureTest {
    private val factory = MenuPresentationSemanticsFactory { _, _, _ -> "クリック" }

    @Test
    fun `background fixture is empty structured display only`() {
        val semantics = factory.create(
            GuiNameSpec.Empty,
            GuiLoreSpec.None,
            MenuPresentationProfile.DISPLAY_ONLY,
        )

        assertEquals(MenuNameSemantic.EMPTY, semantics.name)
        assertEquals(MenuLoreSemanticSource.STRUCTURED, semantics.lore.source)
        assertTrue(semantics.lore.blocks.isEmpty())
        assertEquals(MenuPresentationProfile.DISPLAY_ONLY, semantics.profile)
        assertTrue(MenuPresentationSemanticsValidator.violations(semantics).isEmpty())
    }

    @Test
    fun `navigation fixture retains fixed label and typed action without duplicate union`() {
        val action = GuiLoreLine.Interaction(null, setOf(ClickType.LEFT, ClickType.RIGHT), "移動")
        val lore = GuiLoreComposer.compose(GuiLoreSpec.None, listOf(action))
        val semantics = factory.create(
            GuiNameSpec.FixedLabel(Component.text("前のページ")),
            lore,
            MenuPresentationProfile.SINGLE_CUSTOM_ACTION,
        )

        assertEquals(MenuNameSemantic.FIXED_LABEL, semantics.name)
        assertEquals(listOf(MenuLoreLineKind.ACTION), semantics.lore.blocks.flatMap { it.lines }.map { it.kind })
        assertEquals(setOf(ClickType.LEFT, ClickType.RIGHT), semantics.lore.blocks.single().lines.single().action?.acceptedClicks)
        assertTrue(MenuPresentationSemanticsValidator.violations(semantics).isEmpty())
    }

    @Test
    fun `structured entry path renders action components when base lore is none`() {
        val spec = GuiStructuredMenuEntrySpec(
            slot = 0,
            item = GuiItemSpec(
                Material.LEVER,
                GuiNameSpec.FixedLabel(Component.text("切り替え")),
                GuiLoreSpec.None,
                GuiElementRole.ACTION,
                1,
            ),
            actions = listOf(GuiMenuActionIntent.AnyClick("toggle", "切り替える")),
        )
        val enabled = spec.expandedActions().filter(GuiMenuEntryAction::enabled)
        val actionLines = GuiMenuEntryLoreFactory.actionLines(enabled, null)
        val composed = GuiLoreComposer.compose(spec.item.lore, actionLines)
        val rendered = LoreServiceImpl { _, _, _ -> "クリック" }.render(composed)
        val plain = rendered.map(PlainTextComponentSerializer.plainText()::serialize)

        assertTrue(composed is GuiLoreSpec.WithActions)
        assertEquals(1, actionLines.size)
        assertTrue(plain.any { it.contains("切り替える") })
        val semantics = factory.create(spec.item.name, composed, MenuPresentationProfile.SINGLE_STANDARD_ACTION)
        assertEquals(rendered.size, semantics.lore.blocks.sumOf { it.lines.size } + 2)
        assertEquals(1, semantics.lore.blocks.flatMap { it.lines }.count { it.kind == MenuLoreLineKind.ACTION })
    }
}
