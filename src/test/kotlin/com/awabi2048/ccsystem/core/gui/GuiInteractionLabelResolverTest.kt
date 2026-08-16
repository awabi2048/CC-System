package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GuiInteractionLabelResolverTest {
    @Test
    fun `shift left and right share the generic shift click label`() {
        assertEquals(
            setOf(ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT),
            MenuAcceptedClicks.SHIFT_LEFT_RIGHT,
        )
        assertEquals(
            "lore.click.shift_any",
            GuiInteractionLabelResolver.languageKey(MenuAcceptedClicks.SHIFT_LEFT_RIGHT).id,
        )
    }

    @Test
    fun `generic shift click label is distinct from one-sided labels`() {
        assertTrue(MenuAcceptedClicks.SHIFT_LEFT_RIGHT != MenuAcceptedClicks.SHIFT_LEFT)
        assertTrue(MenuAcceptedClicks.SHIFT_LEFT_RIGHT != MenuAcceptedClicks.SHIFT_RIGHT)
    }
}
