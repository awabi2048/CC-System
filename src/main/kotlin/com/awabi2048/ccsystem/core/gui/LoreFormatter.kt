package com.awabi2048.ccsystem.core.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

object LoreFormatter {
    private const val SEPARATOR_UNIT = "―"
    private val legacy = LegacyComponentSerializer.legacySection()
    private val plain = PlainTextComponentSerializer.plainText()
    private val colorCodePattern = Regex("(?i)[§&][0-9A-FK-ORX]")

    fun separator(lines: Collection<String>): String {
        val maxPixelWidth = lines.maxOfOrNull { displayPixelWidth(it) } ?: 0
        val sepPixelWidth = displayPixelWidth(SEPARATOR_UNIT).coerceAtLeast(1)
        val count = ((((maxPixelWidth + sepPixelWidth - 1) / sepPixelWidth) * 3 + 1) / 2).coerceAtLeast(1)
        return "§8§m" + SEPARATOR_UNIT.repeat(count)
    }

    fun separatorComponent(lines: Collection<Component>): Component {
        return component(separator(lines.map { plain.serialize(it) }))
    }

    fun dataLine(label: String, value: Any?, valueColor: String = "§7"): String {
        return "§f❙ §7$label $valueColor${value ?: ""}"
    }

    fun subDataLine(label: String, value: Any?): String {
        return dataLine(label, value, "§b")
    }

    fun actionLine(operation: String, action: String): String {
        return "§e❙ §e$operation §7$action"
    }

    fun warningLine(content: String): String {
        return "§c§n* $content"
    }

    fun singleActionLine(actionText: String): String {
        return "§e§n$actionText"
    }

    fun textLine(text: String): String {
        return "§7$text"
    }

    fun buildLore(lines: List<String>, closingSeparator: Boolean = true): List<Component> {
        val normalized = lines.map { it.trim() }.filter { it.isNotEmpty() }
        if (normalized.isEmpty()) return emptyList()
        val separator = separator(normalized)
        return cleanup(buildList {
            add(separator)
            addAll(normalized)
            if (closingSeparator) add(separator)
        }).map(::component)
    }

    fun component(line: String): Component {
        return legacy.deserialize(line).decoration(TextDecoration.ITALIC, false)
    }

    private fun cleanup(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        var lastWasSeparator = false
        for (line in lines) {
            val normalized = line.trim()
            if (normalized.isEmpty()) continue
            val separator = isSeparator(normalized)
            if (!separator || !lastWasSeparator) {
                result += normalized
            }
            lastWasSeparator = separator
        }
        return result
    }

    private fun displayPixelWidth(text: String): Int {
        return colorCodePattern.replace(text, "").sumOf { char ->
            when {
                char.code in 0x20..0x7E -> 6
                char.code in 0xFF61..0xFF9F -> 6
                else -> 12
            }
        }
    }

    private fun isSeparator(line: String): Boolean {
        val plainLine = colorCodePattern.replace(line, "").trim()
        return plainLine.isNotEmpty() && plainLine.all { it == '―' || it == '-' || it == '－' }
    }
}
