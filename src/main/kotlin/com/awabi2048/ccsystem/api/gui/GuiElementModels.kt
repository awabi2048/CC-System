package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

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
    data class Component(val value: net.kyori.adventure.text.Component) : GuiNameSpec
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

enum class GuiStatusTone {
    COMPLETE,
    INCOMPLETE
}

sealed interface GuiLoreLine {
    data object Spacer : GuiLoreLine
    data object Separator : GuiLoreLine
    data class Data(val label: String, val value: Any?, val valueColor: String) : GuiLoreLine
    data class ComponentData(
        val label: String,
        val value: net.kyori.adventure.text.Component,
        val valueColor: String
    ) : GuiLoreLine
    data class SubData(val label: String, val value: Any?) : GuiLoreLine
    data class Metadata(val label: String, val value: Any?) : GuiLoreLine
    data class Action(val operation: String, val action: String) : GuiLoreLine
    data class SingleAction(
        val operation: String,
        val action: String,
        val resolvedText: String
    ) : GuiLoreLine
    data class Option(
        val label: String,
        val selected: Boolean,
        val selectedColor: String,
        val inactiveColor: String
    ) : GuiLoreLine
    data class Warning(val content: String) : GuiLoreLine
    data class Danger(val content: String) : GuiLoreLine
    data class Text(val text: String) : GuiLoreLine
    data class StyledText(val text: String, val color: String, val italic: Boolean) : GuiLoreLine
    /** タスク内容の先頭マーカーだけを状態色で描画するデータ行。 */
    data class StatusData(
        val label: String,
        val value: Any?,
        val valueColor: String,
        val tone: GuiStatusTone
    ) : GuiLoreLine
    data class StatusComponentData(
        val label: net.kyori.adventure.text.Component,
        val value: net.kyori.adventure.text.Component,
        val tone: GuiStatusTone
    ) : GuiLoreLine
    /**
     * 複数段階の進行状況を、等幅のPathとラベルとして2行で描画する。
     * 図形、空白、色はCC-Systemが現在位置から決定する。
     */
    data class ProgressPath(
        val labels: List<String>,
        val currentIndex: Int
    ) : GuiLoreLine {
        init {
            require(labels.isNotEmpty()) { "Progress path must contain at least one label" }
            require(currentIndex in labels.indices) { "Current progress index must reference a label" }
        }
    }
    /** プレイヤーが入力した装飾可能な本文。固定UI文言には使用しない。 */
    data class UserText(val text: String) : GuiLoreLine
    data class Component(val value: net.kyori.adventure.text.Component) : GuiLoreLine
}

sealed interface GuiLoreSpec {
    data object None : GuiLoreSpec
    /**
     * Nameだけを表示し、Capabilityが持つ操作案内もLoreへ合成しません。
     * クリック契約自体は維持されます。
     */
    data object NameOnly : GuiLoreSpec
    data class Blocks(val blocks: List<GuiLoreBlock>) : GuiLoreSpec {
        init {
            require(blocks.isNotEmpty()) { "Lore blocks must not be empty" }
        }
    }
    data class Rich(
        val lines: List<GuiLoreLine>,
        val frame: GuiLoreFrame
    ) : GuiLoreSpec
}

data class GuiLoreBlock(val lines: List<GuiLoreLine>) {
    init {
        require(lines.isNotEmpty()) { "Lore block must contain at least one line" }
        require(lines.any { it != GuiLoreLine.Spacer }) { "Lore block must not contain only spacers" }
        require(lines.none { it == GuiLoreLine.Separator }) {
            "Lore block separators are managed by CC-System"
        }
    }
}

data class GuiItemSpec(
    val material: Material,
    val name: GuiNameSpec,
    val lore: GuiLoreSpec,
    val role: GuiElementRole,
    val amount: Int
)

data class GuiMenuIconAction(
    val operation: String,
    val action: String,
    val resolvedText: String?,
    val enabled: Boolean
)

data class GuiMenuIconData(
    val label: String,
    val value: Any?,
    val valueColor: String
)

data class GuiMenuIconOption(
    val label: String,
    val selected: Boolean,
    val selectedColor: String,
    val inactiveColor: String
)

/**
 * メニュー画面は表示情報だけを渡し、区切り・Lore行・グリントはCC-Systemが組み立てる。
 */
data class GuiMenuIconSpec(
    val material: Material,
    val name: GuiNameSpec,
    val role: GuiElementRole,
    val amount: Int,
    val description: List<String>,
    val data: List<GuiMenuIconData>,
    val options: List<GuiMenuIconOption>,
    val warnings: List<String>,
    val dangers: List<String>,
    val actions: List<GuiMenuIconAction>,
    val glint: Boolean?
)

enum class GuiValueTone(val colorCode: String) {
    DEFAULT("\u00A7f"),
    MUTED("\u00A77"),
    PRIMARY("\u00A7e"),
    INFO("\u00A7b"),
    SUCCESS("\u00A7a"),
    WARNING("\u00A76"),
    DANGER("\u00A7c"),
}

data class GuiMenuEntryData(
    val label: String,
    val value: Any?,
    val tone: GuiValueTone = GuiValueTone.DEFAULT,
)

data class GuiMenuEntryOption(
    val label: String,
    val selected: Boolean,
)

/**
 * 外部システムが宣言できる操作の意味情報。
 * 操作案内、クリック受付、Runtime分岐はCC-Systemがこの宣言から同時生成する。
 */
data class GuiMenuEntryAction(
    val actionId: String,
    val acceptedClicks: Set<ClickType>,
    val label: String,
    val payload: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
) {
    init {
        require(actionId.isNotBlank()) { "actionId must not be blank" }
        require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
        require(label.isNotBlank()) { "action label must not be blank" }
    }
}

/**
 * 表示と操作を一体で宣言するメニュー要素。
 * 外部システムはItemStack、Lore、クリック案内を生成しない。
 */
data class GuiMenuEntrySpec(
    val slot: Int,
    val material: Material,
    val name: GuiNameSpec,
    val role: GuiElementRole,
    val amount: Int = 1,
    val description: List<String> = emptyList(),
    val data: List<GuiMenuEntryData> = emptyList(),
    val options: List<GuiMenuEntryOption> = emptyList(),
    val warnings: List<String> = emptyList(),
    val dangers: List<String> = emptyList(),
    val actions: List<GuiMenuEntryAction> = emptyList(),
    val glint: Boolean? = null,
    val sounds: MenuActionSoundPolicy? = null,
) {
    init {
        require(slot >= 0) { "slot must not be negative" }
        val accepted = actions.filter(GuiMenuEntryAction::enabled).flatMap(GuiMenuEntryAction::acceptedClicks)
        require(accepted.size == accepted.distinct().size) {
            "a click type cannot be assigned to multiple menu actions"
        }
        require(role != GuiElementRole.DECORATION || actions.isEmpty()) {
            "decoration entries cannot have actions"
        }
    }
}

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
