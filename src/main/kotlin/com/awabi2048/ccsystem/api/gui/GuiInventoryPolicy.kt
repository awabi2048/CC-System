package com.awabi2048.ccsystem.api.gui

/**
 * 入力を受け付けるGUIの範囲を明示するポリシー。
 * 通常のプレイヤーアイテムを判定材料にせず、共通GUIマーカー付き表示物だけを保護する。
 */
enum class PlayerInventoryInteraction {
    BLOCKED,
    INTERACTIVE,
    SELECTION,
}

data class GuiInventoryPolicy(
    val inputSlots: Set<Int> = emptySet(),
    val playerInventoryInteraction: PlayerInventoryInteraction = PlayerInventoryInteraction.BLOCKED,
) {
    constructor(
        inputSlots: Set<Int> = emptySet(),
        allowPlayerInventoryInteraction: Boolean,
    ) : this(
        inputSlots,
        if (allowPlayerInventoryInteraction) {
            PlayerInventoryInteraction.INTERACTIVE
        } else {
            PlayerInventoryInteraction.BLOCKED
        },
    )

    init {
        require(inputSlots.all { it >= 0 }) { "inputSlots must contain only non-negative slots" }
    }

    fun acceptsTopSlot(slot: Int): Boolean = slot in inputSlots

    val allowPlayerInventoryInteraction: Boolean
        get() = playerInventoryInteraction != PlayerInventoryInteraction.BLOCKED

    val capturesPlayerInventoryClick: Boolean
        get() = playerInventoryInteraction == PlayerInventoryInteraction.SELECTION
}

fun interface GuiInventoryPolicyProvider {
    fun guiInventoryPolicy(): GuiInventoryPolicy
}
