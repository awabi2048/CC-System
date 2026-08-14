package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks
import com.awabi2048.ccsystem.api.localization.LocalizationKey
import com.awabi2048.ccsystem.api.localization.generated.CommonKeys
import org.bukkit.event.inventory.ClickType

internal object GuiInteractionLabelResolver {
    fun languageKey(clicks: Set<ClickType>): LocalizationKey<String> = when (clicks) {
        MenuAcceptedClicks.STANDARD -> CommonKeys.LORE_CLICK_ANY
        MenuAcceptedClicks.LEFT -> CommonKeys.LORE_CLICK_LEFT
        MenuAcceptedClicks.RIGHT -> CommonKeys.LORE_CLICK_RIGHT
        MenuAcceptedClicks.LEFT_RIGHT -> CommonKeys.LORE_CLICK_LEFT_RIGHT
        MenuAcceptedClicks.PLAIN_LEFT -> CommonKeys.LORE_CLICK_LEFT
        MenuAcceptedClicks.PLAIN_RIGHT -> CommonKeys.LORE_CLICK_RIGHT
        MenuAcceptedClicks.PLAIN_LEFT_RIGHT -> CommonKeys.LORE_CLICK_LEFT_RIGHT
        MenuAcceptedClicks.SHIFT_LEFT -> CommonKeys.LORE_CLICK_SHIFT_LEFT
        MenuAcceptedClicks.SHIFT_RIGHT -> CommonKeys.LORE_CLICK_SHIFT_RIGHT
        MenuAcceptedClicks.SHIFT_LEFT_RIGHT -> CommonKeys.LORE_CLICK_SHIFT_ANY
        MenuAcceptedClicks.MIDDLE -> CommonKeys.LORE_CLICK_MIDDLE
        else -> error("Unsupported menu action click set: $clicks")
    }
}
