package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiMenuEntryAction
import com.awabi2048.ccsystem.api.gui.GuiMenuEntrySpec

internal object GuiMenuEntryLoreFactory {
    fun build(
        spec: GuiMenuEntrySpec,
        enabledActions: List<GuiMenuEntryAction>,
        viewer: org.bukkit.entity.Player?,
    ): GuiLoreSpec {
        if (spec.role == GuiElementRole.CONFIRM || spec.role == GuiElementRole.CANCEL) {
            // 確定・キャンセルはNameだけを操作対象の識別情報として表示し、
            // 説明ブロックや自動操作案内で確認画面の視認性を崩さない。
            return GuiLoreSpec.NameOnly
        }
        val base = if (spec.semanticLoreBlocks.isNotEmpty()) {
            GuiLoreSpec.Blocks(spec.semanticLoreBlocks)
        } else {
            val blocks = buildList {
                block(spec.description.map(GuiLoreLine::Text))
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
            }
            if (blocks.isEmpty()) GuiLoreSpec.None else GuiLoreSpec.Blocks(blocks)
        }
        return GuiLoreComposer.compose(base, actionLines(enabledActions, viewer))
    }

    private fun MutableList<GuiLoreBlock>.block(lines: List<GuiLoreLine>) {
        if (lines.isNotEmpty()) add(GuiLoreBlock(lines))
    }

    fun actionLines(
        actions: List<GuiMenuEntryAction>,
        viewer: org.bukkit.entity.Player?,
    ): List<GuiLoreLine.Interaction> {
        return actions.map { action ->
            GuiLoreLine.Interaction(viewer, action.acceptedClicks, action.label)
        }
    }
}
