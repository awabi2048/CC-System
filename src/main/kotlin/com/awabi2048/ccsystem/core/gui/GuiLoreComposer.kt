package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec

/** Loreの意味ブロックと操作ブロックを合成する唯一の実装です。 */
internal object GuiLoreComposer {
    fun compose(
        base: GuiLoreSpec,
        actions: List<GuiLoreLine.Interaction>,
    ): GuiLoreSpec {
        if (actions.isEmpty() || base == GuiLoreSpec.NameOnly) return base
        require(base !is GuiLoreSpec.WithActions) { "Lore actions must be composed only once" }
        return GuiLoreSpec.WithActions(base, actions)
    }

    fun actionBlock(actions: List<GuiLoreLine.Interaction>): GuiLoreBlock =
        GuiLoreBlock(actions)
}
