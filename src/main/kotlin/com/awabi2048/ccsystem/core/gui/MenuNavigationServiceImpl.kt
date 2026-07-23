package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicy
import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicyProvider
import com.awabi2048.ccsystem.api.gui.GuiMenuMatcher
import com.awabi2048.ccsystem.api.gui.MenuNavigationService
import com.awabi2048.ccsystem.api.gui.MenuResumeResult
import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRouteOpener
import java.io.File
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class MenuNavigationServiceImpl(dataFile: File? = null) : MenuNavigationService {
    private val history = MenuNavigationHistory()
    private val openers = ConcurrentHashMap<RouteKey, MenuRouteOpener>()
    private val menuMatchers = ConcurrentHashMap<String, GuiMenuMatcher>()
    private val inventoryPolicies = ConcurrentHashMap<String, GuiInventoryPolicy>()
    private val inventoryInstances = Collections.synchronizedMap(
        IdentityHashMap<Inventory, InventoryRegistration>()
    )
    private val currentRoutes = ConcurrentHashMap<UUID, MenuRoute>()
    private val pendingRoutes = ConcurrentHashMap<UUID, MenuRoute>()
    private val stateStore = dataFile?.let(::MenuRouteStateStore)

    init {
        pendingRoutes.putAll(stateStore?.load().orEmpty())
    }

    override fun registerOpener(owner: String, id: String, opener: MenuRouteOpener) {
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
        if (pendingRoutes.remove(player.uniqueId) != null) savePendingRoutes()
    }

    override fun currentRoute(player: Player): MenuRoute? {
        return currentRoutes[player.uniqueId]
    }

    override fun recordCurrentRoute(player: Player, route: MenuRoute) {
        currentRoutes[player.uniqueId] = route
    }

    override fun persistCurrentRoute(player: Player) {
        currentRoutes.remove(player.uniqueId)?.let { route ->
            history.clear(player.uniqueId)
            pendingRoutes[player.uniqueId] = route
            savePendingRoutes()
        }
    }

    override fun persistCurrentRoutes(players: Collection<Player>) {
        players.forEach { player ->
            currentRoutes.remove(player.uniqueId)?.let { route ->
                history.clear(player.uniqueId)
                pendingRoutes[player.uniqueId] = route
            }
        }
        savePendingRoutes()
    }

    override fun resume(player: Player): MenuResumeResult {
        val route = pendingRoutes.remove(player.uniqueId) ?: return MenuResumeResult.NONE
        savePendingRoutes()
        return if (runCatching { open(player, route) }.getOrDefault(false)) {
            MenuResumeResult.OPENED
        } else {
            pendingRoutes[player.uniqueId] = route
            savePendingRoutes()
            MenuResumeResult.UNAVAILABLE
        }
    }

    override fun push(player: Player, route: MenuRoute) {
        history.push(player.uniqueId, route)
    }

    override fun open(player: Player, route: MenuRoute): Boolean {
        val opener = openers[RouteKey(route.owner, route.id)] ?: return false
        val opened = runCatching { opener.open(player, route) }.getOrDefault(false)
        if (opened) {
            currentRoutes[player.uniqueId] = route
            if (pendingRoutes.remove(player.uniqueId) != null) savePendingRoutes()
        }
        return opened
    }

    override fun pushAndOpen(player: Player, currentRoute: MenuRoute, targetRoute: MenuRoute): Boolean {
        if (!open(player, targetRoute)) return false
        push(player, currentRoute)
        return true
    }

    override fun openRoot(player: Player, route: MenuRoute): Boolean {
        clear(player)
        return open(player, route)
    }

    override fun openPrevious(player: Player): Boolean {
        return history.popPrevious(player.uniqueId) { route -> open(player, route) } != null
    }

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

    private fun savePendingRoutes() {
        stateStore?.save(pendingRoutes.toMap())
    }

    private data class RouteKey(
        val owner: String,
        val id: String
    )

    private data class InventoryRegistration(
        val owner: String,
        val policy: GuiInventoryPolicy,
    )
}
