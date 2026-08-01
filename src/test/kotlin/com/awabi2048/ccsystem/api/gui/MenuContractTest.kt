package com.awabi2048.ccsystem.api.gui

import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuContractTest {
    @Test
    fun `rendered action must have a handler and required payload`() {
        val definition = InventoryMenuDefinition(
            owner = "test",
            id = "contract",
            renderer = InventoryMenuRenderer { error("not used") },
            actions = mapOf("open" to MenuActionHandler { MenuActionResult.Success() }),
            actionContracts = mapOf(
                "open" to MenuActionContract(
                    acceptedClicks = MenuAcceptedClicks.STANDARD,
                    requiredPayloadKeys = setOf("world_uuid"),
                )
            ),
        )
        val violations = MenuContractValidator.validate(
            definition,
            listOf(
                MenuActionObservation(
                    slot = 0,
                    interaction = MenuInteraction.Action(
                        actionId = "open",
                        acceptedClicks = MenuAcceptedClicks.STANDARD,
                    ),
                ),
                MenuActionObservation(
                    slot = 1,
                    interaction = MenuInteraction.Action(
                        actionId = "missing",
                        acceptedClicks = MenuAcceptedClicks.STANDARD,
                    ),
                ),
            ),
        )
        assertEquals(2, violations.size)
        assertTrue(violations.any { it.message.contains("required payload") })
        assertTrue(violations.any { it.message.contains("no handler") })
    }

    @Test
    fun `declared click contract must match every rendered branch`() {
        val definition = InventoryMenuDefinition(
            owner = "test",
            id = "click-contract",
            renderer = InventoryMenuRenderer { error("not used") },
            actions = mapOf("switch" to MenuActionHandler { MenuActionResult.Success() }),
            actionContracts = mapOf(
                "switch" to MenuActionContract(acceptedClicks = MenuAcceptedClicks.PLAIN_LEFT)
            ),
        )
        val violations = MenuContractValidator.validate(
            definition,
            listOf(
                MenuActionObservation(
                    slot = 0,
                    interaction = MenuInteraction.Action(
                        "switch",
                        acceptedClicks = setOf(ClickType.LEFT, ClickType.RIGHT),
                    ),
                )
            ),
        )
        assertEquals(1, violations.size)
        assertTrue(violations.single().message.contains("accepted click contract"))
    }

    @Test
    fun `capability interaction does not require a host action handler`() {
        val definition = InventoryMenuDefinition(
            owner = "test",
            id = "capability-contract",
            renderer = InventoryMenuRenderer { error("not used") },
            actions = emptyMap(),
        )

        val violations = MenuContractValidator.validate(
            definition,
            listOf(
                MenuActionObservation(
                    slot = 4,
                    interaction = MenuInteraction.Capability(
                        capabilityId = "external:open-world-settings",
                        arguments = mapOf("world_uuid" to "world-1"),
                        acceptedClicks = MenuAcceptedClicks.STANDARD,
                    ),
                ),
            ),
        )

        assertTrue(violations.isEmpty())
    }
}
