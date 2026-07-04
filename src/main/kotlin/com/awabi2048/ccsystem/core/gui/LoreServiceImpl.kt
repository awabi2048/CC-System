package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiLoreFrame
import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.LoreService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

class LoreServiceImpl : LoreService {
    private val legacy = LegacyComponentSerializer.legacySection()
    private val colorCodePattern = Regex("(?i)[\u00A7&][0-9A-FK-ORX]")
    private val dataLinePattern = Regex("^\u00A77([^:\uFF1A]+)[:\uFF1A]\\s*(.*)$")
    private val richDataPrefixPattern = Regex("^\u00A7f\u00A7l\\|\\s*")
    private val richActionPrefixPattern = Regex("^\u00A7e\u00A7l\\|\\s*")
    private val actionPrefixPattern = Regex("^\u00A7e\u2759\\s*")

    override fun render(spec: GuiLoreSpec): List<Component> {
        return when (spec) {
            GuiLoreSpec.None -> emptyList()
            is GuiLoreSpec.Blocks -> renderBlocks(spec.blocks)
            is GuiLoreSpec.Auto -> renderAuto(spec.lines, spec.frame)
            is GuiLoreSpec.Rich -> renderRich(spec.lines, spec.frame)
            is GuiLoreSpec.Simple -> spec.actions
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map(LoreFormatter::singleActionLine)
                .map(LoreFormatter::component)
        }
    }

    private fun renderBlocks(blocks: List<GuiLoreBlock>): List<Component> {
        val lines = buildList {
            add(GuiLoreLine.Separator)
            blocks.forEachIndexed { index, block ->
                if (index > 0) add(GuiLoreLine.Separator)
                addAll(block.lines)
            }
            add(GuiLoreLine.Separator)
        }
        return renderRich(lines, GuiLoreFrame.NONE)
    }

    private fun renderRich(lines: List<GuiLoreLine>, frame: GuiLoreFrame): List<Component> {
        if (lines.isEmpty()) return emptyList()
        val separator = LoreFormatter.separator(emptyList())
        val content = lines.map { renderLine(it, separator) }
        val framed = buildList {
            if ((frame == GuiLoreFrame.TOP || frame == GuiLoreFrame.BOTH) &&
                content.firstOrNull()?.let(::isSeparator).let { it != true }
            ) {
                add(separator)
            }
            addAll(content)
            if ((frame == GuiLoreFrame.BOTTOM || frame == GuiLoreFrame.BOTH) &&
                content.lastOrNull()?.let(::isSeparator).let { it != true }
            ) {
                add(separator)
            }
        }
        // 旧Loreと標準フレームが混在しても、境界に同じ線を二重表示しない。
        // また、行数が可変のLoreで余分な空行(Spacer)が連続・先頭末尾に残らないよう圧縮する。
        val compressed = mutableListOf<String>()
        framed.forEachIndexed { index, line ->
            val previous = compressed.lastOrNull()
            when {
                // 連続する Separator は1つに纏める
                isSeparator(line) && previous?.let(::isSeparator) == true -> Unit
                // 連続する Spacer(空行) は1つに纏める
                isSpacer(line) && previous?.let(::isSpacer) == true -> Unit
                // Separator の直後/直前の Spacer は区切り線で代用できるため省く
                isSpacer(line) && (previous?.let(::isSeparator) == true || isSeparatorAt(framed, index + 1)) -> Unit
                else -> compressed += line
            }
        }
        // 先頭・末尾に残った Spacer を削除（枠線として意味をなさない空行）
        return compressed.dropWhile(::isSpacer).dropLastWhile(::isSpacer)
            .map(LoreFormatter::component).map(::normalize)
    }

    private fun isSeparatorAt(lines: List<String>, index: Int): Boolean {
        if (index !in lines.indices) return false
        return isSeparator(lines[index])
    }

    private fun isSpacer(line: String): Boolean = line.isEmpty()

    private fun isSeparator(line: String): Boolean {
        val plain = colorCodePattern.replace(line, "").trim()
        return plain.isNotEmpty() && plain.all { it == '―' || it == '-' || it == '－' || it == '—' }
    }

    private fun renderAuto(lines: List<String>, frame: GuiLoreFrame): List<Component> {
        if (lines.isEmpty()) return emptyList()
        return renderRich(
            lines.map { line ->
                if (line.isBlank()) GuiLoreLine.Spacer else GuiLoreLine.Raw(richLine(line.trim()))
            },
            frame
        )
    }

    private fun renderLine(line: GuiLoreLine, separator: String): String = when (line) {
        GuiLoreLine.Spacer -> ""
        GuiLoreLine.Separator -> separator
        is GuiLoreLine.Action -> LoreFormatter.actionLine(line.operation, line.action)
        is GuiLoreLine.SingleAction -> LoreFormatter.singleActionLine(line.action)
        is GuiLoreLine.Danger -> LoreFormatter.dangerLine(line.content)
        is GuiLoreLine.Data -> LoreFormatter.dataLine(line.label, line.value, line.valueColor)
        is GuiLoreLine.Raw -> line.line
        is GuiLoreLine.SubData -> LoreFormatter.subDataLine(line.label, line.value)
        is GuiLoreLine.Text -> LoreFormatter.textLine(line.text)
        is GuiLoreLine.Warning -> LoreFormatter.warningLine(line.content)
    }

    private fun richLine(line: String): String {
        dataLinePattern.matchEntire(line)?.let { match ->
            val (label, value) = match.destructured
            return LoreFormatter.dataLine(label.trim(), value.trim(), "\u00A7f")
        }
        return line
            .replace(richDataPrefixPattern, "\u00A7f\u2759 ")
            .replace(richActionPrefixPattern, "\u00A7e\u2759 ")
            .replace(actionPrefixPattern, "\u00A7e\u2759 ")
    }

    private fun normalize(component: Component): Component = component
        .colorIfAbsent(NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false)
}
