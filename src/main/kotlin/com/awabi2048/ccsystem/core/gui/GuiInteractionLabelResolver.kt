package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks
import org.bukkit.event.inventory.ClickType

internal object GuiInteractionLabelResolver {
    fun languageKey(clicks: Set<ClickType>): String = when (clicks) {
        MenuAcceptedClicks.STANDARD -> "lore.click.any"
        MenuAcceptedClicks.LEFT -> "lore.click.left"
        MenuAcceptedClicks.RIGHT -> "lore.click.right"
        MenuAcceptedClicks.LEFT_RIGHT -> "lore.click.left_right"
        MenuAcceptedClicks.PLAIN_LEFT -> "lore.click.left"
        MenuAcceptedClicks.PLAIN_RIGHT -> "lore.click.right"
        MenuAcceptedClicks.PLAIN_LEFT_RIGHT -> "lore.click.left_right"
        MenuAcceptedClicks.SHIFT_LEFT -> "lore.click.shift_left"
        MenuAcceptedClicks.SHIFT_RIGHT -> "lore.click.shift_right"
        MenuAcceptedClicks.SHIFT_LEFT_RIGHT -> "lore.click.shift_any"
        MenuAcceptedClicks.MIDDLE -> "lore.click.middle"
        else -> error("Unsupported menu action click set: $clicks")
    }
}
