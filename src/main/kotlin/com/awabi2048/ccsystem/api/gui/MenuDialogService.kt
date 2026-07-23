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
}

data class MenuDialogResponse(
    val text: Map<String, String>,
    val booleans: Map<String, Boolean>,
) {
    fun textValue(id: String): String = text[id].orEmpty()
    fun booleanValue(id: String): Boolean = booleans[id] ?: false
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
    val sounds: MenuActionSoundPolicy = MenuActionSoundPolicy(),
) {
    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        require(inputs.map { it.id }.all { it.isNotBlank() }) { "dialog input ids must not be blank" }
        require(inputs.map { it.id }.distinct().size == inputs.size) { "dialog input ids must be unique" }
    }

    val routeId: String
        get() = "$owner:$id"
}

interface MenuDialogService {
    fun show(player: Player, request: MenuDialogRequest)
}
