package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** ジェスチャーGUIと検証用画面で使用する型付きローカライズキーです。 */
object GestureGuiKeys {
    @JvmField val GESTURE_GUI_DEMO_TITLE = LocalizationKey.text("gesture_gui.demo.title")
    @JvmField val GESTURE_GUI_DEMO_DESCRIPTION = LocalizationKey.text("gesture_gui.demo.description")
    @JvmField val GESTURE_GUI_DEMO_PRIMARY = LocalizationKey.text("gesture_gui.demo.primary")
    @JvmField val GESTURE_GUI_DEMO_SECONDARY = LocalizationKey.text("gesture_gui.demo.secondary")
    @JvmField val GESTURE_GUI_DEMO_SHIFT_PRIMARY = LocalizationKey.text("gesture_gui.demo.shift_primary")
    @JvmField val GESTURE_GUI_DEMO_SHIFT_SECONDARY = LocalizationKey.text("gesture_gui.demo.shift_secondary")
    @JvmField val GESTURE_GUI_DEMO_SWAP_HAND = LocalizationKey.text("gesture_gui.demo.swap_hand")
    @JvmField val GESTURE_GUI_DEMO_OPENED = LocalizationKey.text("gesture_gui.demo.opened", setOf("screens"))
    @JvmField val GESTURE_GUI_DEMO_CLOSED = LocalizationKey.text("gesture_gui.demo.closed")
    @JvmField val GESTURE_GUI_DEMO_ACTION = LocalizationKey.text("gesture_gui.demo.action", setOf("gesture"))
    @JvmField val GESTURE_GUI_DEMO_USAGE = LocalizationKey.text("gesture_gui.demo.usage")
    @JvmField val GESTURE_GUI_DEMO_DIALOG_CLOSE = LocalizationKey.text("gesture_gui.demo.dialog_close")
    @JvmField val GESTURE_GUI_EXIT_GUIDANCE = LocalizationKey.text("gesture_gui.exit_guidance")

    internal fun all(): List<LocalizationKey<*>> = listOf(
        GESTURE_GUI_DEMO_TITLE,
        GESTURE_GUI_DEMO_DESCRIPTION,
        GESTURE_GUI_DEMO_PRIMARY,
        GESTURE_GUI_DEMO_SECONDARY,
        GESTURE_GUI_DEMO_SHIFT_PRIMARY,
        GESTURE_GUI_DEMO_SHIFT_SECONDARY,
        GESTURE_GUI_DEMO_SWAP_HAND,
        GESTURE_GUI_DEMO_OPENED,
        GESTURE_GUI_DEMO_CLOSED,
        GESTURE_GUI_DEMO_ACTION,
        GESTURE_GUI_DEMO_USAGE,
        GESTURE_GUI_DEMO_DIALOG_CLOSE,
        GESTURE_GUI_EXIT_GUIDANCE,
    )
}
