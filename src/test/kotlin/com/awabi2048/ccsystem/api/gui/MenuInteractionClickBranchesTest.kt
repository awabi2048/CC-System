package com.awabi2048.ccsystem.api.gui

import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MenuInteractionClickBranchesTest {
    @Test
    fun `mixed click branches resolve capability and action as their final interactions`() {
        val capability = MenuInteraction.Capability(
            capabilityId = "external:world-settings",
            arguments = mapOf("world_uuid" to "world-1"),
            attributes = mapOf("source" to "mixed"),
            acceptedClicks = setOf(ClickType.LEFT),
            safety = MenuActionSafety.NAVIGATION_ONLY,
        )
        val action = MenuInteraction.Action(
            actionId = "toggle",
            acceptedClicks = setOf(ClickType.RIGHT),
            safety = MenuActionSafety.REVERSIBLE,
        )
        val interaction = MenuInteraction.ClickBranches(
            listOf(
                MenuInteractionBranch(setOf(ClickType.LEFT), capability),
                MenuInteractionBranch(setOf(ClickType.RIGHT), action),
            ),
        )

        val left = assertInstanceOf(MenuInteraction.Capability::class.java, interaction.resolve(ClickType.LEFT))
        val right = assertInstanceOf(MenuInteraction.Action::class.java, interaction.resolve(ClickType.RIGHT))
        assertEquals("external:world-settings", left.capabilityId)
        assertEquals(mapOf("world_uuid" to "world-1"), left.arguments)
        assertEquals(MenuActionSafety.NAVIGATION_ONLY, left.safetyFor(ClickType.LEFT))
        assertEquals("toggle", right.actionId)
        assertEquals(MenuActionSafety.REVERSIBLE, right.safetyFor(ClickType.RIGHT))
    }

    @Test
    fun `mixed branches reject click contracts that differ from their final interaction`() {
        assertThrows(IllegalArgumentException::class.java) {
            MenuInteractionBranch(
                setOf(ClickType.LEFT),
                MenuInteraction.Action("open", acceptedClicks = setOf(ClickType.RIGHT)),
            )
        }
    }
}
