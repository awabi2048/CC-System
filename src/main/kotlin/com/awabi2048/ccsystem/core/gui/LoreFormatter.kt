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
        val count = 30
        return "§8§m" + SEPARATOR_UNIT.repeat(count)
    }

    fun separatorComponent(lines: Collection<Component>): Component {
        return component(separator(lines.map { plain.serialize(it) }))
    }

    fun dataLine(label: String, value: Any?, valueColor: String = "§7"): String {
        return "§f❙ §7$label $valueColor${value ?: ""}"
    }

    fun dataComponent(label: String, value: Component, valueColor: String = "§7"): Component {
        return component("§f❙ §7$label $valueColor").append(value)
    }

    fun subDataLine(label: String, value: Any?): String {
        return dataLine(label, value, "§b")
    }

    fun metadataLine(label: String, value: Any?): String {
        return "§8$label: ${value ?: ""}"
    }

    fun actionLine(operation: String, action: String): String {
        return "§e❙ §e$operation §7$action"
    }

    fun warningLine(content: String): String {
        return "§c※ $content"
    }

    fun dangerLine(content: String): String {
        return "§4§l※ $content"
    }

    fun singleActionLine(actionText: String): String {
        return "§e§n$actionText"
    }

    fun optionLine(
        label: String,
        selected: Boolean,
        selectedColor: String,
        inactiveColor: String
    ): String {
        val marker = if (selected) "§a\u00BB" else "§8\u30FB"
        val color = if (selected) selectedColor else inactiveColor
        return "$marker $color$label"
    }

    fun textLine(text: String): String {
        return "§7$text"
    }

    fun styledText(text: String, color: String, italic: Boolean): Component {
        return component("$color$text").decoration(TextDecoration.ITALIC, italic)
    }

    fun component(line: String): Component {
        return legacy.deserialize(line).decoration(TextDecoration.ITALIC, false)
    }

    fun normalizeSeparator(component: Component): Component {
        return if (isSeparator(plain.serialize(component))) {
            separatorComponent(emptyList())
        } else {
            component
        }
    }

    private fun isSeparator(line: String): Boolean {
        val plainLine = colorCodePattern.replace(line, "").trim()
        return plainLine.isNotEmpty() && plainLine.all { it == '―' || it == '-' || it == '－' }
    }
}
