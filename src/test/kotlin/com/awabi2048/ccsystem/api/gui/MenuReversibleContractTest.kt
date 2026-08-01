package com.awabi2048.ccsystem.api.gui

import com.awabi2048.ccsystem.core.gui.GuiMenuCapabilityInteractionFactory
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MenuReversibleContractTest {
    @Test
    fun `action and click branches retain the final reversible contract for each click`() {
        val left = MenuReversibleContract("audit:left", mapOf("mode" to "left"))
        val right = MenuReversibleContract("audit:right", mapOf("mode" to "right"))
        val interaction = MenuInteraction.ClickBranches(
            listOf(
                MenuInteractionBranch(
                    setOf(ClickType.LEFT),
                    MenuInteraction.Action(
                        actionId = "left",
                        acceptedClicks = setOf(ClickType.LEFT),
                        safety = MenuActionSafety.REVERSIBLE,
                        reversibleContract = left,
                    ),
                ),
                MenuInteractionBranch(
                    setOf(ClickType.RIGHT),
                    MenuInteraction.Action(
                        actionId = "right",
                        acceptedClicks = setOf(ClickType.RIGHT),
                        safety = MenuActionSafety.REVERSIBLE,
                        reversibleContract = right,
                    ),
                ),
            ),
        )

        assertEquals(left, (interaction.resolve(ClickType.LEFT) as MenuInteraction.Action).reversibleContractFor(ClickType.LEFT))
        assertEquals(right, (interaction.resolve(ClickType.RIGHT) as MenuInteraction.Action).reversibleContractFor(ClickType.RIGHT))
    }

    @Test
    fun `capability resolution retains click specific reversible contracts in the runtime interaction`() {
        val contract = MenuReversibleContract("audit:world", mapOf("world" to "alpha"))
        val capability = ResolvedMenuCapability(
            capabilityId = "test:world",
            presentation = MenuCapabilityPresentation(
                GuiItemSpec(Material.STONE, GuiNameSpec.Empty, GuiLoreSpec.None, GuiElementRole.CONTENT, 1),
            ),
            actions = listOf(
                ResolvedMenuCapabilityAction("open", MenuCapabilityTrigger.LEFT, "Open"),
                ResolvedMenuCapabilityAction(
                    "toggle",
                    MenuCapabilityTrigger.RIGHT,
                    "Toggle",
                    MenuActionSafety.REVERSIBLE,
                    contract,
                ),
            ),
        )

        val interaction = GuiMenuCapabilityInteractionFactory.create(
            GuiMenuCapabilityInvocationSpec(4, capability, mapOf("world" to "alpha")),
        )

        assertNull(interaction.reversibleContractFor(ClickType.LEFT))
        assertEquals(contract, interaction.reversibleContractFor(ClickType.RIGHT))
    }
}
