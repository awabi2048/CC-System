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
        return buildList {
            if (frame == GuiLoreFrame.TOP || frame == GuiLoreFrame.BOTH) add(LoreFormatter.component(separator))
            lines.forEach { line -> add(LoreFormatter.component(renderLine(line, separator))) }
            if (frame == GuiLoreFrame.BOTTOM || frame == GuiLoreFrame.BOTH) add(LoreFormatter.component(separator))
        }.map(::normalize)
    }

    private fun renderAuto(lines: List<String>, frame: GuiLoreFrame): List<Component> {
        if (lines.isEmpty()) return emptyList()
        val nonBlank = lines.filter { it.isNotBlank() }
        val actions = nonBlank.mapNotNull(::simpleAction)
        if (nonBlank.isNotEmpty() && actions.size == nonBlank.size && nonBlank.size == lines.size && frame == GuiLoreFrame.NONE) {
            return actions.map(LoreFormatter::singleActionLine).map(LoreFormatter::component)
        }
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

    private fun simpleAction(line: String): String? {
        val plain = colorCodePattern.replace(line, "").trim().trimStart('|', '\u2759').trim()
        return when {
            plain.startsWith("クリックで") -> "クリックして${plain.removePrefix("クリックで").trim()}"
            plain.startsWith("クリック ") -> "クリックして${plain.removePrefix("クリック").trim()}"
            plain.startsWith("左クリック ") -> "左クリックして${plain.removePrefix("左クリック").trim()}"
            plain.startsWith("右クリック ") -> "右クリックして${plain.removePrefix("右クリック").trim()}"
            plain.startsWith("Shift + クリック ") -> "Shift + クリックして${plain.removePrefix("Shift + クリック").trim()}"
            plain.startsWith("Shift + 左クリック ") -> "Shift + 左クリックして${plain.removePrefix("Shift + 左クリック").trim()}"
            plain.startsWith("Shift + 右クリック ") -> "Shift + 右クリックして${plain.removePrefix("Shift + 右クリック").trim()}"
            plain.startsWith("ホイールクリック ") -> "ホイールクリックして${plain.removePrefix("ホイールクリック").trim()}"
            plain.startsWith("Click to ", ignoreCase = true) -> plain
            plain.startsWith("Left-Click ", ignoreCase = true) -> "Left-Click to ${plain.substringAfter(' ').trim()}"
            plain.startsWith("Left click ", ignoreCase = true) -> "Left-Click to ${plain.substringAfter(' ').trim()}"
            plain.startsWith("Right-Click ", ignoreCase = true) -> "Right-Click to ${plain.substringAfter(' ').trim()}"
            plain.startsWith("Right click ", ignoreCase = true) -> "Right-Click to ${plain.substringAfter(' ').trim()}"
            plain.startsWith("Click ", ignoreCase = true) -> "Click to ${plain.substringAfter(' ').trim()}"
            else -> null
        }?.takeIf { it.isNotBlank() }
    }

    private fun normalize(component: Component): Component = component
        .colorIfAbsent(NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false)
}
