package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Inventory
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

interface GuiElementService {
    fun title(name: GuiNameSpec): Component

    fun name(raw: String, style: GuiNameStyle = GuiNameStyle.DEFAULT): Component

    fun lore(spec: GuiLoreSpec): List<Component>

    fun item(spec: GuiItemSpec): ItemStack

    /** 直接生成したアイテムの表示を変更せず、GUI保護マーカーだけを付与する。 */
    fun mark(item: ItemStack, role: GuiElementRole = GuiElementRole.CONTENT): ItemStack

    /** GUI経路で生成されたアイテムかを判定し、入力アイテムとの混同を防ぎます。 */
    fun isGuiItem(item: ItemStack?): Boolean

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

    /** 標準フレームが所有する背景要素です。任意のraw ItemStackにはこの意味を推論しません。 */
    fun backgroundEntry(slot: Int, material: Material): MenuElement

    fun backItem(name: String, material: Material = Material.REDSTONE): ItemStack

    /** 共通の戻る項目。表示、STANDARD受付、BACK role、履歴戻りを一体で生成する。 */
    fun backEntry(player: Player?, slot: Int, material: Material = Material.REDSTONE): MenuElement

    fun pageNavigationEntry(
        player: Player?,
        slot: Int,
        direction: GuiMenuActionIntent.Direction,
        actionId: String,
        label: String,
        payload: Map<String, String> = emptyMap(),
        material: Material,
    ): MenuElement

    fun confirmItem(name: String, confirm: Boolean): ItemStack
}

/** 明示的に無効な要素を、表示専用要素と区別して生成します。 */
fun GuiElementService.menuUnavailable(
    player: Player?,
    spec: GuiMenuEntrySpec,
    reason: Component,
    acceptedClicks: Set<ClickType> = MenuAcceptedClicks.STANDARD,
): MenuElement {
    require(spec.expandedActions().none(GuiMenuEntryAction::enabled)) {
        "unavailable menu entries cannot contain enabled actions"
    }
    val base = menuEntry(player, spec)
    return MenuElement(
        slot = base.slot,
        item = base.item,
        role = base.role,
        sounds = base.sounds,
        interaction = MenuInteraction.Unavailable(acceptedClicks.toSet(), reason, base.sounds),
    ).also { element ->
        element.presentationSemantics = base.presentationSemantics.copy(
            profile = MenuPresentationProfile.DISABLED,
            disabledReason = reason,
        )
    }
}
