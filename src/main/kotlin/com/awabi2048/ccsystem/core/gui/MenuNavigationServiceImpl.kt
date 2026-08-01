package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicy
import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicyProvider
import com.awabi2048.ccsystem.api.gui.GuiMenuMatcher
import com.awabi2048.ccsystem.api.gui.MenuNavigationService
import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRouteOpener
import com.awabi2048.ccsystem.api.gui.MenuRouteResultOpener
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperation
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationFailureReason
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationResult
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class MenuNavigationServiceImpl : MenuNavigationService {
    private val history = MenuNavigationHistory()
    private val openers = ConcurrentHashMap<RouteKey, RegisteredOpener>()
    private val menuMatchers = ConcurrentHashMap<String, GuiMenuMatcher>()
    private val inventoryPolicies = ConcurrentHashMap<String, GuiInventoryPolicy>()
    private val inventoryInstances = Collections.synchronizedMap(
        IdentityHashMap<Inventory, InventoryRegistration>()
    )
    private val currentRoutes = ConcurrentHashMap<UUID, MenuRoute>()
    override fun registerOpener(owner: String, id: String, opener: MenuRouteOpener) {
        register(owner, id, RegisteredOpener.Legacy(opener))
    }

    override fun registerResultOpener(owner: String, id: String, opener: MenuRouteResultOpener) {
        register(owner, id, RegisteredOpener.Detailed(opener))
    }

    private fun register(owner: String, id: String, opener: RegisteredOpener) {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        openers[RouteKey(owner, id)] = opener
    }

    override fun unregisterOwner(owner: String) {
        openers.keys.removeIf { it.owner == owner }
        menuMatchers.remove(owner)
        inventoryPolicies.remove(owner)
        synchronized(inventoryInstances) {
            inventoryInstances.entries.removeIf { it.value.owner == owner }
        }
        history.removeOwner(owner)
    }

    override fun registerMenuMatcher(owner: String, matcher: GuiMenuMatcher) {
        require(owner.isNotBlank()) { "owner must not be blank" }
        menuMatchers[owner] = matcher
    }

    override fun registerInventoryPolicy(owner: String, policy: GuiInventoryPolicy) {
        require(owner.isNotBlank()) { "owner must not be blank" }
        inventoryPolicies[owner] = policy
    }

    override fun unregisterInventoryPolicy(owner: String) {
        inventoryPolicies.remove(owner)
    }

    override fun registerInventory(owner: String, inventory: Inventory, policy: GuiInventoryPolicy) {
        require(owner.isNotBlank()) { "owner must not be blank" }
        inventoryInstances[inventory] = InventoryRegistration(owner, policy)
    }

    override fun unregisterInventory(inventory: Inventory) {
        inventoryInstances.remove(inventory)
    }

    override fun inventoryPolicy(inventory: Inventory): GuiInventoryPolicy? {
        (inventory.holder as? GuiInventoryPolicyProvider)?.let { return it.guiInventoryPolicy() }
        inventoryInstances[inventory]?.let { return it.policy }
        inventoryPolicies.entries.firstOrNull { (owner, _) -> matchesOwner(owner, inventory) }?.value?.let { return it }
        // matcherだけを登録した既存メニューも、GUIアイテムをプレイヤー側へ移さない既定動作にする。
        return menuMatchers.values.firstOrNull { matcher ->
            runCatching { matcher.matches(inventory) }.getOrDefault(false)
        }?.let { GuiInventoryPolicy() }
    }

    override fun isManagedInventory(inventory: Inventory): Boolean {
        return inventoryPolicy(inventory) != null
    }

    override fun closeOwnedMenus(owner: String, players: Collection<Player>): Int {
        val matcher = menuMatchers[owner]
        return closeMatchingMenus(players) { inventory ->
            inventoryInstances[inventory]?.owner == owner ||
                matcher?.let { runCatching { it.matches(inventory) }.getOrDefault(false) } == true
        }
    }

    override fun closeAllMenus(players: Collection<Player>): Int {
        val matchers = menuMatchers.values.toList()
        return closeMatchingMenus(players) { inventory ->
            inventoryInstances.containsKey(inventory) || matchers.any { it.matches(inventory) }
        }
    }

    override fun clear(player: Player) {
        history.clear(player.uniqueId)
        currentRoutes.remove(player.uniqueId)
    }

    override fun currentRoute(player: Player): MenuRoute? {
        return currentRoutes[player.uniqueId]
    }

    override fun recordCurrentRoute(player: Player, route: MenuRoute) {
        currentRoutes[player.uniqueId] = route
    }

    override fun push(player: Player, route: MenuRoute) {
        history.push(player.uniqueId, route)
    }

    override fun open(player: Player, route: MenuRoute): Boolean = openResult(player, route).successful

    override fun openResult(player: Player, route: MenuRoute): MenuRuntimeOperationResult {
        val opener = openers[RouteKey(route.owner, route.id)] ?: return MenuRuntimeOperationResult.failed(
            MenuRuntimeOperation.OPEN,
            route,
            MenuRuntimeOperationFailureReason.MISSING_OPENER,
        )
        val result = try {
            opener.open(player, route).forOperation(MenuRuntimeOperation.OPEN).copy(route = route)
        } catch (failure: Throwable) {
            MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.OPEN,
                route,
                MenuRuntimeOperationFailureReason.OPENER_EXCEPTION,
                exceptionType = failure.javaClass.name,
            )
        }
        if (result.successful) {
            currentRoutes[player.uniqueId] = route
        }
        return result
    }

    override fun pushAndOpen(player: Player, currentRoute: MenuRoute, targetRoute: MenuRoute): Boolean =
        pushAndOpenResult(player, currentRoute, targetRoute).successful

    override fun pushAndOpenResult(
        player: Player,
        currentRoute: MenuRoute,
        targetRoute: MenuRoute,
    ): MenuRuntimeOperationResult {
        val previousHistory = history.snapshot(player.uniqueId)
        history.push(player.uniqueId, currentRoute)
        val result = openResult(player, targetRoute)
        if (result.successful) return result
        history.restore(player.uniqueId, previousHistory)
        return result
    }

    override fun openRoot(player: Player, route: MenuRoute): Boolean = openRootResult(player, route).successful

    override fun openRootResult(player: Player, route: MenuRoute): MenuRuntimeOperationResult {
        clear(player)
        return openResult(player, route)
    }

    override fun openPrevious(player: Player): Boolean = openPreviousResult(player)?.successful == true

    override fun openPreviousResult(player: Player): MenuRuntimeOperationResult? {
        var lastFailure: MenuRuntimeOperationResult? = null
        val opened = history.popPrevious(player.uniqueId) { route ->
            openResult(player, route).also { result ->
                if (!result.successful) lastFailure = result
            }.successful
        }
        return opened?.let { MenuRuntimeOperationResult.succeeded(MenuRuntimeOperation.NAVIGATE, it) }
            ?: lastFailure
    }

    override fun canGoBack(player: Player): Boolean =
        history.snapshot(player.uniqueId).isNotEmpty()

    override fun breadcrumbs(player: Player): List<MenuRoute> {
        return history.snapshot(player.uniqueId)
    }

    private fun closeMatchingMenus(
        players: Collection<Player>,
        matches: (Inventory) -> Boolean
    ): Int {
        var closed = 0
        players.forEach { player ->
            if (runCatching { matches(player.openInventory.topInventory) }.getOrDefault(false)) {
                player.closeInventory()
                clear(player)
                closed++
            }
        }
        return closed
    }

    private fun matchesOwner(owner: String, inventory: Inventory): Boolean {
        val matcher = menuMatchers[owner] ?: return false
        return runCatching { matcher.matches(inventory) }.getOrDefault(false)
    }

    private data class RouteKey(
        val owner: String,
        val id: String
    )

    private data class InventoryRegistration(
        val owner: String,
        val policy: GuiInventoryPolicy,
    )

    private sealed interface RegisteredOpener {
        fun open(player: Player, route: MenuRoute): MenuRuntimeOperationResult

        data class Legacy(val opener: MenuRouteOpener) : RegisteredOpener {
            override fun open(player: Player, route: MenuRoute): MenuRuntimeOperationResult =
                if (opener.open(player, route)) {
                    MenuRuntimeOperationResult.succeeded(MenuRuntimeOperation.OPEN, route)
                } else {
                    MenuRuntimeOperationResult.failed(
                        MenuRuntimeOperation.OPEN,
                        route,
                        MenuRuntimeOperationFailureReason.OPENER_RETURNED_FALSE,
                    )
                }
        }

        data class Detailed(val opener: MenuRouteResultOpener) : RegisteredOpener {
            override fun open(player: Player, route: MenuRoute): MenuRuntimeOperationResult =
                opener.open(player, route)
        }
    }
}
