package com.awabi2048.ccsystem.api.gui

/** Confirmation menus share these slots so feature plugins do not drift in button placement. */
data class GuiConfirmationLayout(
    val size: Int,
    val previewSlot: Int,
    val confirmSlot: Int,
    val cancelSlot: Int
)

/** Paged 54-slot list menus share navigation and footer slots across projects. */
data class GuiPagedListLayout(
    val size: Int,
    val previousPageSlot: Int,
    val nextPageSlot: Int,
    val backSlot: Int,
    val infoSlot: Int,
    val itemSlots: List<Int>
)

/** 54-slot settings menus use a shared footer while keeping feature-specific body slots local. */
data class GuiSettingsLayout(
    val size: Int,
    val backSlot: Int,
    val infoSlot: Int
)

/** Three-option 45-slot menus use the same left, center, right, and footer positions. */
data class GuiThreeChoiceLayout(
    val size: Int,
    val leftSlot: Int,
    val centerSlot: Int,
    val rightSlot: Int,
    val backSlot: Int
)
