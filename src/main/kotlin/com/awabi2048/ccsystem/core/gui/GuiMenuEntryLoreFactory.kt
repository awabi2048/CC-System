package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiMenuEntryAction
import com.awabi2048.ccsystem.api.gui.GuiMenuEntrySpec

internal object GuiMenuEntryLoreFactory {
    fun build(
        spec: GuiMenuEntrySpec,
        enabledActions: List<GuiMenuEntryAction>,
        viewer: org.bukkit.entity.Player?,
    ): GuiLoreSpec {
        if (spec.semanticLoreBlocks.isNotEmpty()) {
            return buildSemanticBlocks(spec.semanticLoreBlocks, enabledActions, viewer)
        }
        val blocks = buildList {
            val descriptionLines = spec.description.map(GuiLoreLine::Text)
            if (enabledActions.size == 1 && descriptionLines.isNotEmpty()) {
                block(descriptionLines + GuiLoreLine.Spacer + actionLines(enabledActions, viewer))
            } else {
                block(descriptionLines)
            }
            block(spec.data.map { GuiLoreLine.Data(it.label, it.value, it.tone.colorCode) })
            block(spec.options.map {
                GuiLoreLine.Option(
                    it.label,
                    it.selected,
                    com.awabi2048.ccsystem.api.gui.GuiValueTone.PRIMARY.colorCode,
                    com.awabi2048.ccsystem.api.gui.GuiValueTone.MUTED.colorCode,
                )
            })
            block(spec.warnings.map(GuiLoreLine::Warning))
            block(spec.dangers.map(GuiLoreLine::Danger))
            if (enabledActions.size != 1 || descriptionLines.isEmpty()) {
                block(actionLines(enabledActions, viewer))
            }
        }
        return if (blocks.isEmpty()) GuiLoreSpec.None else GuiLoreSpec.Blocks(blocks)
    }

    private fun buildSemanticBlocks(
        source: List<GuiLoreBlock>,
        enabledActions: List<GuiMenuEntryAction>,
        viewer: org.bukkit.entity.Player?,
    ): GuiLoreSpec {
        if (enabledActions.isEmpty()) return GuiLoreSpec.Blocks(source)
        val actionLines = actionLines(enabledActions, viewer)
        if (enabledActions.size == 1) {
            val last = source.last()
            return GuiLoreSpec.Blocks(
                source.dropLast(1) + last.copy(lines = last.lines + GuiLoreLine.Spacer + actionLines)
            )
        }
        return GuiLoreSpec.Blocks(source + GuiLoreBlock(actionLines))
    }

    private fun MutableList<GuiLoreBlock>.block(lines: List<GuiLoreLine>) {
        if (lines.isNotEmpty()) add(GuiLoreBlock(lines))
    }

    fun actionLines(
        actions: List<GuiMenuEntryAction>,
        viewer: org.bukkit.entity.Player?,
    ): List<GuiLoreLine> {
        return actions.map { action ->
            GuiLoreLine.Interaction(viewer, action.acceptedClicks, action.label)
        }
    }
}
