package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

sealed interface MenuDialogInput {
    val id: String
    val label: Component

    data class Text(
        override val id: String,
        override val label: Component,
        val initial: String = "",
        val width: Int = 310,
        val maxLength: Int = 128,
    ) : MenuDialogInput

    data class BooleanInput(
        override val id: String,
        override val label: Component,
        val initial: Boolean = false,
    ) : MenuDialogInput

    data class SingleOption(
        override val id: String,
        override val label: Component,
        val options: List<Option>,
        val width: Int = 310,
    ) : MenuDialogInput {
        init {
            require(options.isNotEmpty()) { "single option input must have at least one option" }
            require(options.map { it.id }.all { it.isNotBlank() }) { "option ids must not be blank" }
            require(options.map { it.id }.distinct().size == options.size) { "option ids must be unique" }
            require(options.count { it.initial } <= 1) { "only one option can be initially selected" }
        }

        data class Option(
            val id: String,
            val label: Component,
            val initial: Boolean = false,
        )
    }
}

data class MenuDialogResponse @JvmOverloads constructor(
    val text: Map<String, String>,
    val booleans: Map<String, Boolean>,
    val selections: Map<String, String> = emptyMap(),
) {
    fun textValue(id: String): String = text[id].orEmpty()
    fun booleanValue(id: String): Boolean = booleans[id] ?: false
    fun selectedValue(id: String): String = selections[id].orEmpty()
}

fun interface MenuDialogHandler {
    fun handle(player: Player, response: MenuDialogResponse): MenuActionResult
}

data class MenuDialogButton(
    val label: Component,
    val handler: MenuDialogHandler,
    val sound: MenuSoundPolicy = MenuSoundPolicy.Default,
)

data class MenuDialogRequest(
    val owner: String,
    val id: String,
    val title: Component,
    val body: List<Component>,
    val inputs: List<MenuDialogInput> = emptyList(),
    val confirm: MenuDialogButton,
    val cancel: MenuDialogButton,
    val additionalActions: List<MenuDialogButton> = emptyList(),
    val columns: Int = 1,
    val sounds: MenuActionSoundPolicy = MenuActionSoundPolicy(),
    val canCloseWithEscape: Boolean = true,
) {
    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        require(inputs.map { it.id }.all { it.isNotBlank() }) { "dialog input ids must not be blank" }
        require(inputs.map { it.id }.distinct().size == inputs.size) { "dialog input ids must be unique" }
        require(columns > 0) { "dialog columns must be positive" }
    }

    val routeId: String
        get() = "$owner:$id"
}

interface MenuDialogService {
    fun show(player: Player, request: MenuDialogRequest)

    /**
     * 指定した所有者・IDのDialogが現在表示中の場合だけ閉じます。
     *
     * 外部画面を扱う利用側がPlayer.closeDialog()を直接呼ぶと、別機能のDialogを
     * 誤って閉じる可能性があるため、表示所有権を照合する共通経路を提供します。
     */
    fun closeIfCurrent(player: Player, owner: String, id: String): Boolean
}
