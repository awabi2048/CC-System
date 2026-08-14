package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** Embedded English catalog for Gesture GUI. */
internal object EnUsGestureGuiCatalog {
    const val LOCALE = "en_us"
    const val DOMAIN = "gesture_gui"

    fun entries(): List<EmbeddedLocalizationEntry> = listOf(
        entry("gesture_gui.demo.title", "Gesture GUI"),
        entry("gesture_gui.demo.description", "Look at an element and use its assigned gesture."),
        entry("gesture_gui.demo.primary", "Left click"),
        entry("gesture_gui.demo.secondary", "Right click"),
        entry("gesture_gui.demo.shift_primary", "Shift + left click"),
        entry("gesture_gui.demo.shift_secondary", "Shift + right click"),
        entry("gesture_gui.demo.swap_hand", "F key"),
        entry("gesture_gui.demo.opened", "§aOpened Gesture GUI with {screens} screen(s)."),
        entry("gesture_gui.demo.closed", "§eClosed Gesture GUI."),
        entry("gesture_gui.demo.action", "§bGesture accepted: {gesture}"),
        entry("gesture_gui.demo.usage", "§eUsage: /cc gesture-gui demo [1|2|3|close]"),
        entry("gesture_gui.demo.dialog_close", "Close"),
        entry("gesture_gui.demo.status.title", "Status Layout"),
        entry("gesture_gui.demo.status.description", "An example that separates the icon from status rows."),
        entry("gesture_gui.demo.status.health", "Health: Good"),
        entry("gesture_gui.demo.status.energy", "Energy: Charged"),
        entry("gesture_gui.demo.status.ready", "Readiness: Complete"),
        entry("gesture_gui.demo.choice.title", "Choice Card Layout"),
        entry("gesture_gui.demo.choice.description", "Look at a role and left click to select it."),
        entry("gesture_gui.demo.choice.builder", "Builder"),
        entry("gesture_gui.demo.choice.explorer", "Explorer"),
        entry("gesture_gui.demo.choice.trader", "Trader"),
        entry("gesture_gui.demo.choice.guardian", "Guardian"),
        entry("gesture_gui.exit_guidance", "Shift + jump to close"),
    )

    private fun entry(key: String, value: String) =
        EmbeddedLocalizationEntry(key, EmbeddedLocalizedValue.Text(value), DOMAIN)
}
