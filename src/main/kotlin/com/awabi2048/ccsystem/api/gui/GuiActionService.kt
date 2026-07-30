package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player

/** メニューの操作案内を、全プラグインで同じ翻訳・書式から生成します。 */
interface GuiActionService {
    fun singleClick(player: Player?, action: String): GuiLoreLine.SingleAction

    fun single(
        player: Player?,
        operation: String,
        action: String,
    ): GuiLoreLine.SingleAction

    fun cycle(player: Player?): GuiLoreLine.Action

    fun clickLabel(player: Player?, click: GuiClickLabel): String
}

enum class GuiClickLabel(val i18nKey: String) {
    ANY("lore.click.any"),
    MIDDLE("lore.click.middle"),
    LEFT("lore.click.left"),
    RIGHT("lore.click.right"),
    LEFT_RIGHT("lore.click.left_right"),
    SHIFT_ANY("lore.click.shift_any"),
    SHIFT_LEFT("lore.click.shift_left"),
    SHIFT_RIGHT("lore.click.shift_right"),
}
