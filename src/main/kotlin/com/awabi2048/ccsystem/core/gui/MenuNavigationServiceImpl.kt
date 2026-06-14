package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuNavigationService
import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRouteOpener
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player

class MenuNavigationServiceImpl : MenuNavigationService {
    private val history = MenuNavigationHistory()
    private val openers = ConcurrentHashMap<RouteKey, MenuRouteOpener>()

    override fun registerOpener(owner: String, id: String, opener: MenuRouteOpener) {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        openers[RouteKey(owner, id)] = opener
    }

    override fun unregisterOwner(owner: String) {
        openers.keys.removeIf { it.owner == owner }
        history.removeOwner(owner)
    }

    override fun clear(player: Player) {
        history.clear(player.uniqueId)
    }

    override fun push(player: Player, route: MenuRoute) {
        history.push(player.uniqueId, route)
    }

    override fun open(player: Player, route: MenuRoute): Boolean {
        val opener = openers[RouteKey(route.owner, route.id)] ?: return false
        return opener.open(player, route)
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

    private data class RouteKey(
        val owner: String,
        val id: String
    )
}
