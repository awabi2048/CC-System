package com.awabi2048.ccsystem.api.gui

import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MenuRuntimeDiagnosticsTest {
    @Test
    fun `existing actions retain unspecified safety by default`() {
        assertEquals(MenuActionSafety.UNSPECIFIED, MenuInteraction.Action("open").safety)
        assertEquals(MenuActionSafety.UNSPECIFIED, MenuCapabilityAction(
            id = "open",
            trigger = MenuCapabilityTrigger.LEFT,
            textProvider = MenuCapabilityActionTextProvider { "Open" },
            handler = MenuCapabilityActionHandler { MenuActionResult.Ignored },
        ).safety)
    }

    @Test
    fun `gesture and branch actions retain their declared safety`() {
        val gesture = GuiMenuActionIntent.GestureAction(
            actionId = "open",
            gesture = MenuGesture.ANY,
            label = "Open",
            safety = MenuActionSafety.NAVIGATION_ONLY,
        ).expand().single()
        assertEquals(MenuActionSafety.NAVIGATION_ONLY, gesture.safety)

        val branches = GuiMenuActionIntent.LeftRight(
            GuiMenuActionIntent.AnyClick(
                "left",
                "Left",
                safety = MenuActionSafety.REVERSIBLE,
                reversibleContract = MenuReversibleContract("audit:left"),
            ),
            GuiMenuActionIntent.AnyClick("right", "Right", safety = MenuActionSafety.IRREVERSIBLE),
        ).expand()
        assertEquals(MenuActionSafety.REVERSIBLE, branches.single { it.actionId == "left" }.safety)
        assertEquals(MenuActionSafety.IRREVERSIBLE, branches.single { it.actionId == "right" }.safety)
    }

    @Test
    fun `runtime interactions resolve safety for the actual click`() {
        val action = MenuInteraction.Action(
            actionId = "capability",
            acceptedClicks = setOf(ClickType.LEFT, ClickType.RIGHT),
            safety = MenuActionSafety.UNSPECIFIED,
            safetyByClick = mapOf(
                ClickType.LEFT to MenuActionSafety.NAVIGATION_ONLY,
                ClickType.RIGHT to MenuActionSafety.REVERSIBLE,
            ),
            reversibleContractByClick = mapOf(ClickType.RIGHT to MenuReversibleContract("audit:capability")),
        )
        assertEquals(MenuActionSafety.NAVIGATION_ONLY, action.safetyFor(ClickType.LEFT))
        assertEquals(MenuActionSafety.REVERSIBLE, action.safetyFor(ClickType.RIGHT))

        val capability = ResolvedMenuCapability(
            capabilityId = "test:world",
            presentation = MenuCapabilityPresentation(
                GuiItemSpec(
                    org.bukkit.Material.STONE,
                    GuiNameSpec.Empty,
                    GuiLoreSpec.None,
                    GuiElementRole.CONTENT,
                    1,
                ),
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
                    MenuReversibleContract("audit:toggle"),
                ),
            ),
        )
        assertEquals(MenuActionSafety.UNSPECIFIED, capability.safety)
        assertEquals(MenuActionSafety.NAVIGATION_ONLY, capability.safetyByClick[ClickType.LEFT])
        assertEquals(MenuActionSafety.REVERSIBLE, capability.safetyByClick[ClickType.RIGHT])
    }

    @Test
    fun `action result diagnostics retain every update variant`() {
        assertEquals(MenuRuntimeActionResultKind.IGNORED, MenuRuntimeActionResultKind.from(MenuActionResult.Ignored))
        assertEquals(MenuRuntimeActionResultKind.REJECTED, MenuRuntimeActionResultKind.from(MenuActionResult.Rejected()))
        assertNull(MenuRuntimeUpdateSnapshot.from(MenuActionResult.Ignored))
        assertNull(MenuRuntimeUpdateSnapshot.from(MenuActionResult.Rejected()))
        assertEquals(MenuRuntimeUpdateKind.NONE, MenuRuntimeUpdateSnapshot.from(MenuActionResult.Success(MenuUpdate.None))?.kind)
        assertEquals(MenuRuntimeUpdateKind.REFRESH, MenuRuntimeUpdateSnapshot.from(MenuActionResult.Success(MenuUpdate.Refresh))?.kind)
        assertEquals(MenuRuntimeUpdateKind.CLOSE, MenuRuntimeUpdateSnapshot.from(MenuActionResult.Success(MenuUpdate.Close))?.kind)
        assertEquals(MenuRuntimeUpdateKind.BACK, MenuRuntimeUpdateSnapshot.from(MenuActionResult.Success(MenuUpdate.Back))?.kind)
        assertEquals(
            MenuRuntimeUpdateKind.REPLACE,
            MenuRuntimeUpdateSnapshot.from(MenuActionResult.Success(MenuUpdate.Replace(MenuRoute("test", "replace"))))?.kind,
        )
        assertEquals(
            MenuRuntimeUpdateKind.NAVIGATE,
            MenuRuntimeUpdateSnapshot.from(MenuActionResult.Success(MenuUpdate.Navigate(MenuRoute("test", "navigate"))))?.kind,
        )
    }
}
