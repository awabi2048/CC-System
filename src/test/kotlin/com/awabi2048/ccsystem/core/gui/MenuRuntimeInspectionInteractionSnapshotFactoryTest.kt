package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuActionSafety
import com.awabi2048.ccsystem.api.gui.MenuInteraction
import com.awabi2048.ccsystem.api.gui.MenuInteractionBranch
import com.awabi2048.ccsystem.api.gui.MenuReversibleContract
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInteractionKind
import com.awabi2048.ccsystem.api.gui.MenuRuntimeSlotKind
import com.awabi2048.ccsystem.api.gui.MenuRuntimeSlotSnapshot
import com.awabi2048.ccsystem.api.gui.GuiElementRole
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MenuRuntimeInspectionInteractionSnapshotFactoryTest {
    @Test
    fun `capability inspection keeps opaque attributes and per click safety`() {
        val marker = Any()
        val snapshot = MenuRuntimeInspectionInteractionSnapshotFactory.create(
            MenuInteraction.Capability(
                capabilityId = "test:world-settings",
                arguments = mapOf("world_uuid" to "world-1"),
                attributes = mapOf("opaque" to marker),
                acceptedClicks = setOf(ClickType.LEFT, ClickType.RIGHT),
                safety = MenuActionSafety.NAVIGATION_ONLY,
                safetyByClick = mapOf(ClickType.RIGHT to MenuActionSafety.REVERSIBLE),
                reversibleContractByClick = mapOf(
                    ClickType.RIGHT to MenuReversibleContract("audit:world", mapOf("world" to "world-1")),
                ),
            ),
        )

        assertEquals(MenuRuntimeInteractionKind.CAPABILITY, snapshot.kind)
        assertEquals("test:world-settings", snapshot.capabilityId)
        assertEquals(mapOf("world_uuid" to "world-1"), snapshot.arguments)
        assertEquals(marker, snapshot.attributes["opaque"])
        assertEquals(setOf(ClickType.LEFT, ClickType.RIGHT), snapshot.acceptedClicks)
        assertEquals(MenuActionSafety.NAVIGATION_ONLY, snapshot.safety)
        assertEquals(MenuActionSafety.REVERSIBLE, snapshot.safetyByClick[ClickType.RIGHT])
        assertEquals("audit:world", snapshot.reversibleContractsByClick[ClickType.RIGHT]?.providerId)
        assertEquals(mapOf("world" to "world-1"), snapshot.reversibleContractsByClick[ClickType.RIGHT]?.arguments)
    }

    @Test
    fun `mixed branches retain final interaction diagnostics`() {
        val snapshot = MenuRuntimeInspectionInteractionSnapshotFactory.create(
            MenuInteraction.ClickBranches(
                listOf(
                    MenuInteractionBranch(
                        setOf(ClickType.LEFT),
                        MenuInteraction.Capability(
                            capabilityId = "test:open",
                            acceptedClicks = setOf(ClickType.LEFT),
                            safety = MenuActionSafety.NAVIGATION_ONLY,
                        ),
                    ),
                    MenuInteractionBranch(
                        setOf(ClickType.RIGHT),
                        MenuInteraction.Action(
                            actionId = "toggle",
                            acceptedClicks = setOf(ClickType.RIGHT),
                            safety = MenuActionSafety.REVERSIBLE,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(MenuRuntimeInteractionKind.CLICK_BRANCHES, snapshot.kind)
        assertEquals(setOf(ClickType.LEFT, ClickType.RIGHT), snapshot.acceptedClicks)
        assertEquals(MenuRuntimeInteractionKind.CAPABILITY, snapshot.branches[0].interaction.kind)
        assertEquals("test:open", snapshot.branches[0].interaction.capabilityId)
        assertEquals(MenuRuntimeInteractionKind.ACTION, snapshot.branches[1].interaction.kind)
        assertEquals("toggle", snapshot.branches[1].interaction.actionId)
        assertEquals(MenuActionSafety.NAVIGATION_ONLY, snapshot.safetyByClick[ClickType.LEFT])
        assertEquals(MenuActionSafety.REVERSIBLE, snapshot.safetyByClick[ClickType.RIGHT])
    }

    @Test
    fun `normal runtime slot and inspection use the same complete interaction snapshot`() {
        val interaction = MenuInteraction.ClickBranches(
            listOf(
                MenuInteractionBranch(
                    setOf(ClickType.LEFT),
                    MenuInteraction.Capability(
                        capabilityId = "test:open",
                        arguments = mapOf("world" to "one"),
                        attributes = mapOf("source" to "runtime"),
                        acceptedClicks = setOf(ClickType.LEFT),
                        safety = MenuActionSafety.NAVIGATION_ONLY,
                    ),
                ),
                MenuInteractionBranch(
                    setOf(ClickType.RIGHT),
                    MenuInteraction.Back(setOf(ClickType.RIGHT)),
                ),
            ),
        )
        val complete = MenuRuntimeInspectionInteractionSnapshotFactory.create(interaction)
        val runtimeSlot = MenuRuntimeSlotSnapshot(
            slot = 4,
            kind = MenuRuntimeSlotKind.ACTION,
            material = Material.STONE,
            amount = 1,
            name = Component.text("Test"),
            lore = emptyList(),
            glint = false,
            role = GuiElementRole.ACTION,
            interactionKind = MenuRuntimeInteractionKind.CLICK_BRANCHES,
            actionId = null,
            capabilityId = null,
            acceptedClicks = setOf(ClickType.LEFT, ClickType.RIGHT),
            payload = emptyMap(),
            enabled = true,
            safety = MenuActionSafety.UNSPECIFIED,
            safetyByClick = emptyMap(),
            branches = emptyList(),
            interaction = complete,
        )

        assertEquals(complete, runtimeSlot.interaction)
        val retained = requireNotNull(runtimeSlot.interaction)
        assertEquals(MenuRuntimeInteractionKind.CAPABILITY, retained.branches[0].interaction.kind)
        assertEquals("test:open", retained.branches[0].interaction.capabilityId)
        assertEquals(mapOf("world" to "one"), retained.branches[0].interaction.arguments)
        assertEquals("runtime", retained.branches[0].interaction.attributes["source"])
        assertEquals(MenuRuntimeInteractionKind.BACK, retained.branches[1].interaction.kind)
    }
}
