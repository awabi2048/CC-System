package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiLoreFrame
import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.LoreService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

class LoreServiceImpl : LoreService {
    private data class RenderedLine(
        val component: Component,
        val spacer: Boolean = false,
        val separator: Boolean = false
    )

    override fun render(spec: GuiLoreSpec): List<Component> {
        return when (spec) {
            GuiLoreSpec.None -> emptyList()
            is GuiLoreSpec.Blocks -> renderBlocks(spec.blocks)
            is GuiLoreSpec.Rich -> renderRich(spec.lines, spec.frame)
        }
    }

    private fun renderBlocks(blocks: List<GuiLoreBlock>): List<Component> {
        val lines = buildList {
            blocks.forEachIndexed { index, block ->
                // Blocks の境界は情報のまとまりを保つ空行とし、中間の区切り線は明示指定だけに任せる。
                if (index > 0) add(GuiLoreLine.Spacer)
                addAll(block.lines)
            }
        }
        return renderRich(lines, GuiLoreFrame.BOTH)
    }

    private fun renderRich(lines: List<GuiLoreLine>, frame: GuiLoreFrame): List<Component> {
        if (lines.isEmpty()) return emptyList()
        val separator = RenderedLine(LoreFormatter.separatorComponent(emptyList()), separator = true)
        val content = lines.map(::renderLine)
        val framed = buildList {
            if ((frame == GuiLoreFrame.TOP || frame == GuiLoreFrame.BOTH) &&
                content.firstOrNull()?.separator != true
            ) {
                add(separator)
            }
            addAll(content)
            if ((frame == GuiLoreFrame.BOTTOM || frame == GuiLoreFrame.BOTH) &&
                content.lastOrNull()?.separator != true
            ) {
                add(separator)
            }
        }
        // 意味モデル上の明示的な区切りと標準フレームが隣接した場合は、同じ線を二重表示しない。
        // 行数が可変のLoreでは、余分なSpacerが連続・先頭末尾に残らないよう圧縮する。
        val compressed = mutableListOf<RenderedLine>()
        framed.forEachIndexed { index, line ->
            val previous = compressed.lastOrNull()
            when {
                // 連続する Separator は1つに纏める
                line.separator && previous?.separator == true -> Unit
                // 連続する Spacer(空行) は1つに纏める
                line.spacer && previous?.spacer == true -> Unit
                // Separator の直後/直前の Spacer は区切り線で代用できるため省く
                line.spacer && (previous?.separator == true || framed.getOrNull(index + 1)?.separator == true) -> Unit
                else -> compressed += line
            }
        }
        // 先頭・末尾に残った Spacer を削除（枠線として意味をなさない空行）
        return compressed.dropWhile(RenderedLine::spacer).dropLastWhile(RenderedLine::spacer)
            .map { normalize(it.component) }
    }

    private fun renderLine(line: GuiLoreLine): RenderedLine = when (line) {
        GuiLoreLine.Spacer -> RenderedLine(Component.empty(), spacer = true)
        GuiLoreLine.Separator -> RenderedLine(LoreFormatter.separatorComponent(emptyList()), separator = true)
        is GuiLoreLine.Component -> RenderedLine(line.value)
        is GuiLoreLine.ComponentData -> RenderedLine(LoreFormatter.dataComponent(line.label, line.value, line.valueColor))
        is GuiLoreLine.StyledText -> RenderedLine(LoreFormatter.styledText(line.text, line.color, line.italic))
        is GuiLoreLine.UserText -> RenderedLine(LoreFormatter.component(line.text))
        else -> RenderedLine(LoreFormatter.component(renderFormattedLine(line)))
    }

    private fun renderFormattedLine(line: GuiLoreLine): String = when (line) {
        is GuiLoreLine.Action -> LoreFormatter.actionLine(line.operation, line.action)
        is GuiLoreLine.SingleAction -> LoreFormatter.singleActionLine(line.resolvedText)
        is GuiLoreLine.Option -> LoreFormatter.optionLine(
            line.label,
            line.selected,
            line.selectedColor,
            line.inactiveColor
        )
        is GuiLoreLine.Danger -> LoreFormatter.dangerLine(line.content)
        is GuiLoreLine.Data -> LoreFormatter.dataLine(line.label, line.value, line.valueColor)
        is GuiLoreLine.Metadata -> LoreFormatter.metadataLine(line.label, line.value)
        is GuiLoreLine.SubData -> LoreFormatter.subDataLine(line.label, line.value)
        is GuiLoreLine.Text -> LoreFormatter.textLine(line.text)
        is GuiLoreLine.Warning -> LoreFormatter.warningLine(line.content)
        GuiLoreLine.Separator,
        GuiLoreLine.Spacer,
        is GuiLoreLine.ComponentData,
        is GuiLoreLine.StyledText,
        is GuiLoreLine.UserText,
        is GuiLoreLine.Component -> error("Non-formatted lore line reached formatted renderer")
    }

    private fun normalize(component: Component): Component = component
        .colorIfAbsent(NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false)
}
