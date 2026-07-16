package com.awabi2048.ccsystem.api.gui

/**
 * 入力を受け付けるGUIの範囲を明示するポリシー。
 * 通常のプレイヤーアイテムを判定材料にせず、共通GUIマーカー付き表示物だけを保護する。
 */
data class GuiInventoryPolicy(
    val inputSlots: Set<Int> = emptySet(),
    val allowPlayerInventoryInteraction: Boolean = false
) {
    init {
        require(inputSlots.all { it >= 0 }) { "inputSlots must contain only non-negative slots" }
    }

    fun acceptsTopSlot(slot: Int): Boolean = slot in inputSlots
}

fun interface GuiInventoryPolicyProvider {
    fun guiInventoryPolicy(): GuiInventoryPolicy
}
