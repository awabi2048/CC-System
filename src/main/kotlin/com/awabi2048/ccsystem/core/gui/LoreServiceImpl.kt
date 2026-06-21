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
    private data class LegacyAction(
        val operation: String,
        val action: String,
        val singleText: String,
    )

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
        // 旧Loreと構造化Loreが混在しても、操作案内の総数から表示形式を一意に決める。
        val actionCount = lines.count { line ->
            line is GuiLoreLine.Action ||
                line is GuiLoreLine.SingleAction ||
                (line is GuiLoreLine.Raw && parseLegacyAction(line.line) != null)
        }
        val normalizedLines = lines.map { line ->
            if (line !is GuiLoreLine.Raw) return@map line
            val action = parseLegacyAction(line.line) ?: return@map line
            if (actionCount == 1) {
                GuiLoreLine.SingleAction(action.singleText)
            } else {
                GuiLoreLine.Action(action.operation, action.action)
            }
        }
        val content = normalizedLines.map { renderLine(it, separator) }
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
        return framed.fold(mutableListOf<String>()) { result, line ->
            if (!(isSeparator(line) && result.lastOrNull()?.let(::isSeparator) == true)) {
                result += line
            }
            result
        }.map(LoreFormatter::component).map(::normalize)
    }

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

    private fun parseLegacyAction(line: String): LegacyAction? {
        if (!line.trim().startsWith("\u00A7e", ignoreCase = true)) return null

        val plain = colorCodePattern.replace(line, "").trim().trimStart('|', '\u2759').trim()
        Regex("^(.*?クリック)(?:で|して|\\s+)(.+)$").matchEntire(plain)?.let { match ->
            val operation = match.groupValues[1].trim()
            val action = match.groupValues[2].trim()
            if (operation.isNotEmpty() && action.isNotEmpty()) {
                return LegacyAction(operation, action, "${operation}して${action}")
            }
        }
        Regex("^(.*?[Cc]lick)(?:\\s+to)?\\s+(.+)$", RegexOption.IGNORE_CASE)
            .matchEntire(plain)
            ?.let { match ->
                val operation = match.groupValues[1].trim()
                val action = match.groupValues[2].trim()
                if (operation.isNotEmpty() && action.isNotEmpty()) {
                    return LegacyAction(operation, action, "$operation to $action")
                }
            }
        return null
    }

    private fun normalize(component: Component): Component = component
        .colorIfAbsent(NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false)
}
