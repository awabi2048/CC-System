package com.awabi2048.ccsystem.api.gui

/** Confirmation menus share these slots so feature plugins do not drift in button placement. */
data class GuiConfirmationLayout(
    val size: Int,
    val previewSlot: Int,
    val confirmSlot: Int,
    val cancelSlot: Int
)
