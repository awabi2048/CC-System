package com.awabi2048.ccsystem.api.gui

/**
 * レイアウトの責務境界を表す。
 * FREEは共通のサイズ・戻る位置だけを借り、本文スロットは機能側が意図を持って管理する画面で使う。
 */
enum class GuiLayoutStyle {
    CONFIRMATION_45,
    PAGED_LIST_54,
    SETTINGS_54,
    THREE_CHOICE_45,
    FREE_45,
    FREE_54
}

/** Confirmation menus share these slots so feature plugins do not drift in button placement. */
data class GuiConfirmationLayout(
    val size: Int,
    val previewSlot: Int,
    val confirmSlot: Int,
    val cancelSlot: Int
) {
    val style: GuiLayoutStyle = GuiLayoutStyle.CONFIRMATION_45
}

/** Paged 54-slot list menus share navigation and footer slots across projects. */
data class GuiPagedListLayout(
    val size: Int,
    val previousPageSlot: Int,
    val nextPageSlot: Int,
    val backSlot: Int,
    val infoSlot: Int,
    val itemSlots: List<Int>
) {
    val style: GuiLayoutStyle = GuiLayoutStyle.PAGED_LIST_54
}

/** 54-slot settings menus use a shared footer while keeping feature-specific body slots local. */
data class GuiSettingsLayout(
    val size: Int,
    val backSlot: Int,
    val infoSlot: Int
) {
    val style: GuiLayoutStyle = GuiLayoutStyle.SETTINGS_54
}

/** Three-option 45-slot menus use the same left, center, right, and footer positions. */
data class GuiThreeChoiceLayout(
    val size: Int,
    val leftSlot: Int,
    val centerSlot: Int,
    val rightSlot: Int,
    val backSlot: Int
) {
    val style: GuiLayoutStyle = GuiLayoutStyle.THREE_CHOICE_45
}

/** Free-form menus still declare the shared shell they use, while feature code owns body slots explicitly. */
data class GuiFreeLayout(
    val size: Int,
    val backSlot: Int,
    val style: GuiLayoutStyle
)
