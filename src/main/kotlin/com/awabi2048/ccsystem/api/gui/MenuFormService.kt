package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player

data class MenuFormButton(
    val id: String,
    val label: String,
    val imagePath: String? = null,
    val enabled: Boolean = true,
    val sound: MenuActionSoundPolicy = MenuActionSoundPolicy()
)

sealed interface MenuFormInput {
    val id: String

    data class Text(
        override val id: String,
        val label: String,
        val placeholder: String = "",
        val defaultValue: String = ""
    ) : MenuFormInput

    data class Toggle(
        override val id: String,
        val label: String,
        val defaultValue: Boolean = false
    ) : MenuFormInput
}

data class MenuFormResponse(
    val text: Map<String, String> = emptyMap(),
    val toggles: Map<String, Boolean> = emptyMap()
) {
    fun textValue(id: String): String = text[id].orEmpty()
    fun toggleValue(id: String): Boolean = toggles[id] == true
}

fun interface MenuFormHandler {
    fun handle(player: Player, response: MenuFormResponse): MenuActionResult
}

data class MenuSimpleFormRequest(
    val owner: String,
    val id: String,
    val title: String,
    val content: String,
    val buttons: List<MenuFormButton>,
    val handler: MenuFormHandler,
    val onClosed: MenuFormHandler? = null,
    val sounds: MenuActionSoundPolicy = MenuActionSoundPolicy()
)

data class MenuCustomFormRequest(
    val owner: String,
    val id: String,
    val title: String,
    val inputs: List<MenuFormInput>,
    val handler: MenuFormHandler,
    val onClosed: MenuFormHandler? = null,
    val sounds: MenuActionSoundPolicy = MenuActionSoundPolicy()
)

interface MenuFormService {
    fun isAvailable(player: Player): Boolean
    fun show(player: Player, request: MenuSimpleFormRequest): Boolean
    fun show(player: Player, request: MenuCustomFormRequest): Boolean
}
