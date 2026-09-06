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
    @JvmField val GESTURE_GUI_DEMO_STATUS_TITLE = LocalizationKey.text("gesture_gui.demo.status.title")
    @JvmField val GESTURE_GUI_DEMO_STATUS_DESCRIPTION = LocalizationKey.text("gesture_gui.demo.status.description")
    @JvmField val GESTURE_GUI_DEMO_STATUS_HEALTH = LocalizationKey.text("gesture_gui.demo.status.health")
    @JvmField val GESTURE_GUI_DEMO_STATUS_ENERGY = LocalizationKey.text("gesture_gui.demo.status.energy")
    @JvmField val GESTURE_GUI_DEMO_STATUS_READY = LocalizationKey.text("gesture_gui.demo.status.ready")
    @JvmField val GESTURE_GUI_DEMO_CHOICE_TITLE = LocalizationKey.text("gesture_gui.demo.choice.title")
    @JvmField val GESTURE_GUI_DEMO_CHOICE_DESCRIPTION = LocalizationKey.text("gesture_gui.demo.choice.description")
    @JvmField val GESTURE_GUI_DEMO_CHOICE_BUILDER = LocalizationKey.text("gesture_gui.demo.choice.builder")
    @JvmField val GESTURE_GUI_DEMO_CHOICE_EXPLORER = LocalizationKey.text("gesture_gui.demo.choice.explorer")
    @JvmField val GESTURE_GUI_DEMO_CHOICE_TRADER = LocalizationKey.text("gesture_gui.demo.choice.trader")
    @JvmField val GESTURE_GUI_DEMO_CHOICE_GUARDIAN = LocalizationKey.text("gesture_gui.demo.choice.guardian")
    @JvmField val GESTURE_GUI_EXIT_GUIDANCE = LocalizationKey.text("gesture_gui.exit_guidance")
    @JvmField val GESTURE_GUI_DEBUG_SUBTITLE = LocalizationKey.text(
        "gesture_gui.debug.subtitle",
        setOf("inside", "envelope", "hit", "dist", "range", "motion", "dirty", "dummy", "displacement"),
    )
    @JvmField val GESTURE_GUI_DEBUG_RESUMMON = LocalizationKey.text(
        "gesture_gui.debug.resummon",
        setOf("displacement"),
    )
    @JvmField val GESTURE_GUI_DEBUG_RESUMMON_FAILED = LocalizationKey.text(
        "gesture_gui.debug.resummon_failed",
        setOf("detail"),
    )
    @JvmField val GESTURE_GUI_DEBUG_DUMMY_STARTED = LocalizationKey.text(
        "gesture_gui.debug.dummy_started",
        setOf("displacement"),
    )
    @JvmField val GESTURE_GUI_DEBUG_DUMMY_RESTORED = LocalizationKey.text(
        "gesture_gui.debug.dummy_restored",
        setOf("displacement"),
    )
    @JvmField val GESTURE_GUI_DEBUG_MAIN_RESTORED_FOR_UPDATE =
        LocalizationKey.text("gesture_gui.debug.main_restored_for_update")
    @JvmField val GESTURE_GUI_DEBUG_UNPINNED = LocalizationKey.text("gesture_gui.debug.unpinned")

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
        GESTURE_GUI_DEMO_STATUS_TITLE,
        GESTURE_GUI_DEMO_STATUS_DESCRIPTION,
        GESTURE_GUI_DEMO_STATUS_HEALTH,
        GESTURE_GUI_DEMO_STATUS_ENERGY,
        GESTURE_GUI_DEMO_STATUS_READY,
        GESTURE_GUI_DEMO_CHOICE_TITLE,
        GESTURE_GUI_DEMO_CHOICE_DESCRIPTION,
        GESTURE_GUI_DEMO_CHOICE_BUILDER,
        GESTURE_GUI_DEMO_CHOICE_EXPLORER,
        GESTURE_GUI_DEMO_CHOICE_TRADER,
        GESTURE_GUI_DEMO_CHOICE_GUARDIAN,
        GESTURE_GUI_EXIT_GUIDANCE,
        GESTURE_GUI_DEBUG_SUBTITLE,
        GESTURE_GUI_DEBUG_RESUMMON,
        GESTURE_GUI_DEBUG_RESUMMON_FAILED,
        GESTURE_GUI_DEBUG_DUMMY_STARTED,
        GESTURE_GUI_DEBUG_DUMMY_RESTORED,
        GESTURE_GUI_DEBUG_MAIN_RESTORED_FOR_UPDATE,
        GESTURE_GUI_DEBUG_UNPINNED,
    )
}
