package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiItemSpec
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiMenuCapabilityInvocationSpec
import com.awabi2048.ccsystem.api.gui.GuiNameSpec
import com.awabi2048.ccsystem.api.gui.MenuActionSafety
import com.awabi2048.ccsystem.api.gui.MenuCapabilityPresentation
import com.awabi2048.ccsystem.api.gui.MenuCapabilityTrigger
import com.awabi2048.ccsystem.api.gui.MenuInteraction
import com.awabi2048.ccsystem.api.gui.MenuReversibleContract
import com.awabi2048.ccsystem.api.gui.ResolvedMenuCapability
import com.awabi2048.ccsystem.api.gui.ResolvedMenuCapabilityAction
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class GuiMenuCapabilityInteractionFactoryTest {
    @Test
    fun `typed capability invocation retains the resolved contract without action payload indirection`() {
        val attributes = mapOf<String, Any>("source" to "world-settings")
        val interaction = GuiMenuCapabilityInteractionFactory.create(
            GuiMenuCapabilityInvocationSpec(
                slot = 4,
                capability = capability(),
                arguments = mapOf("world_uuid" to "world-1"),
                attributes = attributes,
            ),
        )

        assertInstanceOf(MenuInteraction.Capability::class.java, interaction)
        assertEquals("external:open-world-settings", interaction.capabilityId)
        assertEquals(mapOf("world_uuid" to "world-1"), interaction.arguments)
        assertEquals(attributes, interaction.attributes)
        assertEquals(setOf(ClickType.LEFT, ClickType.RIGHT), interaction.acceptedClicks)
        assertEquals(MenuActionSafety.UNSPECIFIED, interaction.safety)
        assertEquals(MenuActionSafety.NAVIGATION_ONLY, interaction.safetyByClick[ClickType.LEFT])
        assertEquals(MenuActionSafety.REVERSIBLE, interaction.safetyByClick[ClickType.RIGHT])
        assertEquals(
            MenuReversibleContract("audit:world", mapOf("operation" to "toggle")),
            interaction.reversibleContractFor(ClickType.RIGHT),
        )
    }

    private fun capability() = ResolvedMenuCapability(
        capabilityId = "external:open-world-settings",
        presentation = MenuCapabilityPresentation(
            GuiItemSpec(Material.STONE, GuiNameSpec.Empty, GuiLoreSpec.None, GuiElementRole.CONTENT, 1),
        ),
        actions = listOf(
            ResolvedMenuCapabilityAction(
                "open",
                MenuCapabilityTrigger.LEFT,
                "Open",
                MenuActionSafety.NAVIGATION_ONLY,
            ),
            ResolvedMenuCapabilityAction(
                "toggle",
                MenuCapabilityTrigger.RIGHT,
                "Toggle",
                MenuActionSafety.REVERSIBLE,
                MenuReversibleContract("audit:world", mapOf("operation" to "toggle")),
            ),
        ),
    )
}
