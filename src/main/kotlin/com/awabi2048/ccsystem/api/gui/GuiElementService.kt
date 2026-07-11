package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Inventory

interface GuiElementService {
    fun component(raw: String): Component

    fun name(raw: String, style: GuiNameStyle = GuiNameStyle.DEFAULT): Component

    fun lore(spec: GuiLoreSpec): List<Component>

    fun autoLore(lines: List<String>, closingSeparator: Boolean = true): List<Component>

    fun item(spec: GuiItemSpec): ItemStack

    fun menuIcon(spec: GuiMenuIconSpec): ItemStack

    fun applyFrame(inventory: Inventory, spec: GuiFrameSpec)

    fun fillEmpty(inventory: Inventory, element: GuiItemSpec)

    fun decoration(material: Material): ItemStack

    fun backItem(name: String, material: Material = Material.REDSTONE): ItemStack

    fun confirmItem(name: String, confirm: Boolean): ItemStack
}
