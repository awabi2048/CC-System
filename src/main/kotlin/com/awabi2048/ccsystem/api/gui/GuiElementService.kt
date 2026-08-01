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

    /**
     * 意味情報から表示とRuntime操作を同時生成する。
     * 外部システムは生成後のItemStackやLoreを変更しない。
     */
    fun menuEntry(player: Player?, spec: GuiMenuEntrySpec): MenuElement

    fun menuDisplay(spec: GuiMenuDisplaySpec): MenuElement

    fun menuStructuredEntry(player: Player?, spec: GuiStructuredMenuEntrySpec): MenuElement

    /** 解決済みCapabilityを文字列Actionへ変換せず、Runtimeの共通Capability経路へ配置します。 */
    fun menuCapabilityEntry(player: Player?, spec: GuiMenuCapabilityInvocationSpec): MenuElement

    fun applyFrame(inventory: Inventory, spec: GuiFrameSpec)

    fun fillEmpty(inventory: Inventory, element: GuiItemSpec)

    fun decoration(material: Material): ItemStack

    fun backItem(name: String, material: Material = Material.REDSTONE): ItemStack

    /** 共通の戻る項目。表示、STANDARD受付、BACK role、履歴戻りを一体で生成する。 */
    fun backEntry(player: Player?, slot: Int, material: Material = Material.REDSTONE): MenuElement

    fun confirmItem(name: String, confirm: Boolean): ItemStack
}
