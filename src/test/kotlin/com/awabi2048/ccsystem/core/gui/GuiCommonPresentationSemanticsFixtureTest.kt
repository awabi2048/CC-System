package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.*
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import java.nio.file.Files
import java.nio.file.Path

class GuiCommonPresentationSemanticsFixtureTest {
    private val factory = MenuPresentationSemanticsFactory { _, _, _ -> "クリック" }

    @Test
    fun `item materialization never strips composed lore according to semantic role`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/com/awabi2048/ccsystem/core/gui/GuiElementServiceImpl.kt"),
        )
        val itemBody = source.substringAfter("override fun item(spec: GuiItemSpec)")
            .substringBefore("override fun menuEntry")

        assertTrue(itemBody.contains("val lore = lore(spec.lore)"))
        assertTrue(!itemBody.contains("nameOnlyRole"))
        assertTrue(!itemBody.contains("emptyList() else lore"))
    }

    @Test
    fun `action lore stays canonical immediately before item meta for semantic action roles`() {
        val renderer = LoreServiceImpl { _, _, _ -> "クリック" }
        val action = GuiLoreLine.Interaction(null, setOf(ClickType.LEFT, ClickType.RIGHT), "実行する")

        listOf(
            GuiElementRole.CONFIRM to MenuPresentationProfile.SINGLE_CUSTOM_ACTION,
            GuiElementRole.CANCEL to MenuPresentationProfile.SINGLE_CUSTOM_ACTION,
            GuiElementRole.NAVIGATION to MenuPresentationProfile.PAGE_NAVIGATION,
            GuiElementRole.ACTION to MenuPresentationProfile.SINGLE_CUSTOM_ACTION,
        ).forEach { (role, profile) ->
            val lore = GuiLoreComposer.compose(GuiLoreSpec.None, listOf(action))
            val rendered = renderer.render(lore)
            val semantics = factory.create(
                GuiNameSpec.FixedLabel(Component.text("操作")),
                lore,
                profile,
            )

            val renderedText = rendered.map(PlainTextComponentSerializer.plainText()::serialize)
            assertTrue(renderedText.first().isNotBlank())
            assertEquals(1, renderedText.count { it.contains("実行する") })
            assertEquals(listOf(MenuLoreLineKind.ACTION), semantics.lore.blocks.flatMap { it.lines }.map { it.kind })
            assertEquals(setOf(ClickType.LEFT, ClickType.RIGHT), semantics.lore.blocks.single().lines.single().action?.acceptedClicks)
            assertEquals(action.label, semantics.lore.blocks.single().lines.single().action?.actionLabel)
            assertTrue(MenuPresentationSemanticsValidator.violations(semantics).isEmpty())
            assertTrue(role in setOf(GuiElementRole.CONFIRM, GuiElementRole.CANCEL, GuiElementRole.NAVIGATION, GuiElementRole.ACTION))
        }
    }

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
    fun `open refresh and inspection share one canonical standard background path`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/com/awabi2048/ccsystem/core/gui/MenuRuntimeServiceImpl.kt"),
        )

        assertEquals(3, Regex("canonicalElements\\(view\\)").findAll(source).count())
        assertTrue(source.contains("val runtimeElements = canonicalElements(view)"))
        assertTrue(source.contains("slots = canonicalElements(view)"))
        assertTrue(!source.contains("view.elements.associateBy { it.slot }"))
        assertTrue(source.contains("semanticElements.backgroundEntry(slot, material)"))
    }

    @Test
    fun `raw item snapshots do not infer background role`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/com/awabi2048/ccsystem/core/gui/MenuRuntimeServiceImpl.kt"),
        )

        assertTrue(source.contains("element?.role ?: GuiItemMarker.role(item)"))
        assertTrue(!source.contains("item.type == Material.BLACK_STAINED_GLASS_PANE"))
        assertTrue(!source.contains("item.type == Material.GRAY_STAINED_GLASS_PANE"))
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
    fun `back semantic union matches runtime standard clicks and renders one action line`() {
        val action = GuiLoreLine.Interaction(null, MenuAcceptedClicks.STANDARD, "戻る")
        val lore = GuiLoreComposer.compose(GuiLoreSpec.None, listOf(action))
        val semantics = factory.create(
            GuiNameSpec.FixedLabel(Component.text("戻る")),
            lore,
            MenuPresentationProfile.PAGE_NAVIGATION,
        )
        val runtime = MenuInteraction.Back()
        val rendered = LoreServiceImpl { _, _, _ -> "クリック" }.render(lore)
            .map(PlainTextComponentSerializer.plainText()::serialize)

        assertEquals(MenuAcceptedClicks.STANDARD, runtime.acceptedClicks)
        assertEquals(MenuAcceptedClicks.STANDARD, semantics.lore.blocks.single().lines.single().action?.acceptedClicks)
        assertEquals("クリック", semantics.lore.blocks.single().lines.single().action?.operationLabel)
        assertEquals("戻る", semantics.lore.blocks.single().lines.single().action?.actionLabel)
        assertTrue(rendered.first().isNotBlank())
        assertEquals(1, rendered.count { it.contains("戻る") })
        assertEquals(MenuPresentationProfile.PAGE_NAVIGATION, semantics.profile)
        assertTrue(MenuPresentationSemanticsValidator.violations(semantics).isEmpty())

        val source = Files.readString(
            Path.of("src/main/kotlin/com/awabi2048/ccsystem/core/gui/GuiElementServiceImpl.kt"),
        )
        val backBuilder = source.substringAfter("override fun backEntry")
            .substringBefore("override fun pageNavigationEntry")
        assertTrue(backBuilder.contains("actions = listOf(GuiMenuActionIntent.Back)"))
        assertTrue(!backBuilder.contains("GuiLoreSpec.NameOnly"))
        val menuEntryBuilder = source.substringAfter("override fun menuEntry")
            .substringBefore("override fun menuDisplay")
        assertTrue(menuEntryBuilder.contains("if (implicitBack)"))
        assertTrue(menuEntryBuilder.contains("emptyList()"))
        assertTrue(menuEntryBuilder.contains("GuiElementRole.NAVIGATION, GuiElementRole.BACK"))
    }

    @Test
    fun `page navigation common builder has the same canonical standard union`() {
        listOf(GuiMenuActionIntent.Direction.PREVIOUS, GuiMenuActionIntent.Direction.NEXT).forEach { direction ->
            val spec = GuiMenuEntrySpec(
                0,
                Material.ARROW,
                GuiNameSpec.FixedLabel(Component.text(direction.name)),
                GuiElementRole.NAVIGATION,
                actions = listOf(GuiMenuActionIntent.Page(direction, "page", "移動")),
            )
            val action = spec.expandedActions().single()
            val lore = GuiMenuEntryLoreFactory.build(spec, listOf(action), null)
            val semantics = factory.create(spec.name, lore, MenuPresentationProfile.PAGE_NAVIGATION)

            assertEquals(MenuAcceptedClicks.STANDARD, action.acceptedClicks)
            assertEquals(MenuAcceptedClicks.STANDARD, semantics.lore.blocks.single().lines.single().action?.acceptedClicks)
            assertEquals(MenuPresentationProfile.PAGE_NAVIGATION, semantics.profile)
            assertTrue(MenuPresentationSemanticsValidator.violations(semantics).isEmpty())
        }
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
