package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuActionSafety
import com.awabi2048.ccsystem.api.gui.MenuInteraction
import com.awabi2048.ccsystem.api.gui.MenuInteractionBranch
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInteractionKind
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
            ),
        )

        assertEquals(MenuRuntimeInteractionKind.CAPABILITY, snapshot.kind)
        assertEquals("test:world-settings", snapshot.capabilityId)
        assertEquals(mapOf("world_uuid" to "world-1"), snapshot.arguments)
        assertEquals(marker, snapshot.attributes["opaque"])
        assertEquals(setOf(ClickType.LEFT, ClickType.RIGHT), snapshot.acceptedClicks)
        assertEquals(MenuActionSafety.NAVIGATION_ONLY, snapshot.safety)
        assertEquals(MenuActionSafety.REVERSIBLE, snapshot.safetyByClick[ClickType.RIGHT])
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
}
