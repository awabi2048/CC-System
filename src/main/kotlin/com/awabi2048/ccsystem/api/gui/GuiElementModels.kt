package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.Material

enum class GuiNameStyle(val colorCode: String) {
    DEFAULT("\u00A7f"),
    PRIMARY("\u00A7e"),
    MUTED("\u00A77"),
    SUCCESS("\u00A7a"),
    WARNING("\u00A76"),
    DANGER("\u00A7c")
}

sealed interface GuiNameSpec {
    data object Empty : GuiNameSpec
    data class Text(val text: String, val style: GuiNameStyle) : GuiNameSpec
}

enum class GuiElementRole {
    CONTENT,
    ACTION,
    BACK,
    CONFIRM,
    CANCEL,
    NAVIGATION,
    DECORATION
}

enum class GuiLoreFrame {
    NONE,
    TOP,
    BOTTOM,
    BOTH
}

sealed interface GuiLoreLine {
    data object Spacer : GuiLoreLine
    data object Separator : GuiLoreLine
    data class Data(val label: String, val value: Any?, val valueColor: String) : GuiLoreLine
    data class SubData(val label: String, val value: Any?) : GuiLoreLine
    data class Action(val operation: String, val action: String) : GuiLoreLine
    data class SingleAction(val action: String) : GuiLoreLine
    data class Warning(val content: String) : GuiLoreLine
    data class Danger(val content: String) : GuiLoreLine
    data class Text(val text: String) : GuiLoreLine
    data class Raw(val line: String) : GuiLoreLine
}

sealed interface GuiLoreSpec {
    data object None : GuiLoreSpec
    data class Blocks(val blocks: List<GuiLoreBlock>) : GuiLoreSpec {
        init {
            require(blocks.isNotEmpty()) { "Lore blocks must not be empty" }
        }
    }
    data class Auto(
        val lines: List<String>,
        val frame: GuiLoreFrame
    ) : GuiLoreSpec
    data class Rich(
        val lines: List<GuiLoreLine>,
        val frame: GuiLoreFrame
    ) : GuiLoreSpec
    data class Simple(
        val actions: List<String>
    ) : GuiLoreSpec
}

data class GuiLoreBlock(val lines: List<GuiLoreLine>) {
    init {
        require(lines.isNotEmpty()) { "Lore block must contain at least one line" }
        require(lines.any { it != GuiLoreLine.Spacer }) { "Lore block must not contain only spacers" }
        require(lines.none(::isSeparatorLine)) {
            "Lore block separators are managed by CC-System"
        }
    }

    private fun isSeparatorLine(line: GuiLoreLine): Boolean {
        if (line == GuiLoreLine.Separator) return true
        val raw = (line as? GuiLoreLine.Raw)?.line ?: return false
        val plain = raw.replace(Regex("(?i)[§&][0-9A-FK-ORX]"), "").trim()
        return plain.isNotEmpty() && plain.all { it == '―' || it == '-' || it == '－' || it == '—' }
    }
}

data class GuiItemSpec(
    val material: Material,
    val name: GuiNameSpec,
    val lore: GuiLoreSpec,
    val role: GuiElementRole,
    val amount: Int
)

sealed interface GuiFrameSection {
    data object None : GuiFrameSection
    data class Row(val element: GuiItemSpec) : GuiFrameSection
    data class Slots(val slots: Set<Int>, val element: GuiItemSpec) : GuiFrameSection
}

data class GuiFrameSpec(
    val header: GuiFrameSection,
    val footer: GuiFrameSection,
    val emptySlot: GuiItemSpec?
)
