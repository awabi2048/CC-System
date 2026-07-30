package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Inventory
import org.bukkit.entity.Player

interface GuiElementService {
    fun title(name: GuiNameSpec): Component

    fun name(raw: String, style: GuiNameStyle = GuiNameStyle.DEFAULT): Component

    fun lore(spec: GuiLoreSpec): List<Component>

    fun item(spec: GuiItemSpec): ItemStack

    /** 直接生成したアイテムの表示を変更せず、GUI保護マーカーだけを付与する。 */
    fun mark(item: ItemStack, role: GuiElementRole = GuiElementRole.CONTENT): ItemStack

    fun menuIcon(spec: GuiMenuIconSpec): ItemStack

    /**
     * 意味情報から表示とRuntime操作を同時生成する。
     * 外部システムは生成後のItemStackやLoreを変更しない。
     */
    fun menuEntry(player: Player?, spec: GuiMenuEntrySpec): MenuElement

    fun menuCapability(presentation: MenuCapabilityPresentation): ItemStack

    fun applyFrame(inventory: Inventory, spec: GuiFrameSpec)

    fun fillEmpty(inventory: Inventory, element: GuiItemSpec)

    fun decoration(material: Material): ItemStack

    fun backItem(name: String, material: Material = Material.REDSTONE): ItemStack

    fun confirmItem(name: String, confirm: Boolean): ItemStack
}
