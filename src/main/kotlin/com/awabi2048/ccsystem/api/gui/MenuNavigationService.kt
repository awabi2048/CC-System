package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

fun interface GuiMenuMatcher {
    fun matches(inventory: Inventory): Boolean
}

interface MenuNavigationService {
    fun registerOpener(owner: String, id: String, opener: MenuRouteOpener)

    fun unregisterOwner(owner: String)

    fun registerMenuMatcher(owner: String, matcher: GuiMenuMatcher)

    fun registerInventoryPolicy(owner: String, policy: GuiInventoryPolicy)

    fun unregisterInventoryPolicy(owner: String)

    fun registerInventory(owner: String, inventory: Inventory, policy: GuiInventoryPolicy)

    fun unregisterInventory(inventory: Inventory)

    fun inventoryPolicy(inventory: Inventory): GuiInventoryPolicy?

    fun isManagedInventory(inventory: Inventory): Boolean

    fun closeOwnedMenus(owner: String, players: Collection<Player>): Int

    fun closeAllMenus(players: Collection<Player>): Int

    fun clear(player: Player)

    fun currentRoute(player: Player): MenuRoute?

    fun recordCurrentRoute(player: Player, route: MenuRoute)

    fun persistCurrentRoute(player: Player)

    fun persistCurrentRoutes(players: Collection<Player>)

    fun resume(player: Player): MenuResumeResult

    fun push(player: Player, route: MenuRoute)

    fun open(player: Player, route: MenuRoute): Boolean

    fun pushAndOpen(player: Player, currentRoute: MenuRoute, targetRoute: MenuRoute): Boolean

    fun openRoot(player: Player, route: MenuRoute): Boolean

    fun openPrevious(player: Player): Boolean

    /** 現在の画面から戻れる再生成可能なRouteが存在するかを返す。 */
    fun canGoBack(player: Player): Boolean

    /**
     * GUI、Dialog、入力待ちをまたぐ戻り先を、CC-System共通のパンくずとして参照する。
     * 戻り処理では末尾から開き直すため、呼び出し側はInventory実体ではなく再生成可能なMenuRouteだけを積む。
     */
    fun breadcrumbs(player: Player): List<MenuRoute>
}

enum class MenuResumeResult {
    NONE,
    OPENED,
    UNAVAILABLE
}
