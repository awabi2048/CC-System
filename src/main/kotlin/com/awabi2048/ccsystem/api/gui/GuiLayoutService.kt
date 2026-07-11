package com.awabi2048.ccsystem.api.gui

import org.bukkit.inventory.Inventory

/** Common inventory layout constants and frames shared by CC feature plugins. */
interface GuiLayoutService {
    fun size45(): Int

    fun size54(): Int

    fun backSlot45(): Int

    fun footerLeftSlot54(): Int

    fun footerCenterSlot54(): Int

    fun backSlot54(): Int

    fun confirmation45(): GuiConfirmationLayout

    fun pagedList54(): GuiPagedListLayout

    fun sevenColumnList(itemCount: Int): GuiSevenColumnListLayout

    fun sevenColumnPage(totalItemCount: Int, requestedPage: Int): GuiSevenColumnPage

    fun settings54(): GuiSettingsLayout

    fun threeChoice45(): GuiThreeChoiceLayout

    fun free45(): GuiFreeLayout

    fun free54(): GuiFreeLayout

    fun applyStandardFrame(inventory: Inventory)
}
