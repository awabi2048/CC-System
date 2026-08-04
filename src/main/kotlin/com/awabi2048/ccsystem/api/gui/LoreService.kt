package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component

interface LoreService {
    fun render(spec: GuiLoreSpec): List<Component>

    /** Returns the standard separator used by CC-System's lore and message composition. */
    fun separator(): String

    /** 意味ブロックを保持したまま、操作群を最後の独立ブロックへ合成する。 */
    fun compose(spec: GuiLoreSpec, actions: List<GuiLoreLine.Interaction>): GuiLoreSpec
}
