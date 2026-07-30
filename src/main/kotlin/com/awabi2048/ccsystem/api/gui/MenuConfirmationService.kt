package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class MenuConfirmationDraft(
    val owner: String,
    val menuId: String,
    val title: Component,
    val previewItem: ItemStack,
    val confirmItem: ItemStack,
    val cancelItem: ItemStack,
    val onConfirm: () -> MenuActionResult,
    val onCancel: () -> MenuActionResult = {
        MenuActionResult.Success(MenuUpdate.Back)
    },
    val onAbandon: () -> Unit = {},
    val confirmSound: MenuSoundPolicy = MenuSoundPolicy.Default,
    val cancelSound: MenuSoundPolicy = MenuSoundPolicy.Default,
)

interface MenuConfirmationService {
    fun prepare(player: Player, draft: MenuConfirmationDraft): MenuRoute

    fun open(player: Player, draft: MenuConfirmationDraft): Boolean

    fun clearOwner(owner: String)
}
