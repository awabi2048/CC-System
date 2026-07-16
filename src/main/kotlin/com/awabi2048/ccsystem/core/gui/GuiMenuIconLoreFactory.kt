package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiMenuIconAction
import com.awabi2048.ccsystem.api.gui.GuiMenuIconSpec

object GuiMenuIconLoreFactory {
    fun build(spec: GuiMenuIconSpec): GuiLoreSpec {
        // Keep content blocks in the canonical display order; actions always terminate the Lore.
        val blocks = buildList {
            block(spec.description.map(GuiLoreLine::Text))
            block(spec.data.map { GuiLoreLine.Data(it.label, it.value, it.valueColor) })
            block(spec.options.map {
                GuiLoreLine.Option(it.label, it.selected, it.selectedColor, it.inactiveColor)
            })
            block(spec.warnings.map(GuiLoreLine::Warning))
            block(spec.dangers.map(GuiLoreLine::Danger))
            block(actionLines(spec.actions.filter(GuiMenuIconAction::enabled)))
        }
        return if (blocks.isEmpty()) GuiLoreSpec.None else GuiLoreSpec.Blocks(blocks)
    }

    private fun MutableList<GuiLoreBlock>.block(lines: List<GuiLoreLine>) {
        if (lines.isNotEmpty()) add(GuiLoreBlock(lines))
    }

    private fun actionLines(actions: List<GuiMenuIconAction>): List<GuiLoreLine> {
        if (actions.isEmpty()) return emptyList()
        if (actions.size > 1) {
            return actions.map { GuiLoreLine.Action(it.operation, it.action) }
        }

        val action = actions.single()
        val resolvedText = requireNotNull(action.resolvedText?.takeIf(String::isNotBlank)) {
            "A single menu action requires resolvedText"
        }
        return listOf(GuiLoreLine.SingleAction(action.operation, action.action, resolvedText))
    }
}
