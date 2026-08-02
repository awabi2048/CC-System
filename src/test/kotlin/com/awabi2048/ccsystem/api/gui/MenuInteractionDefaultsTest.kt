package com.awabi2048.ccsystem.api.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MenuInteractionDefaultsTest {
    @Test
    fun `single action and back default to any click`() {
        assertEquals(MenuAcceptedClicks.STANDARD, MenuInteraction.Action("open").acceptedClicks)
        assertEquals(MenuAcceptedClicks.STANDARD, MenuInteraction.Back().acceptedClicks)
        assertEquals(MenuAcceptedClicks.STANDARD, MenuInteraction.Capability("owner:capability").acceptedClicks)
    }
}
