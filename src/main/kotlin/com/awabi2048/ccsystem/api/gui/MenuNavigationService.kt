package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player

interface MenuNavigationService {
    fun registerOpener(owner: String, id: String, opener: MenuRouteOpener)

    fun unregisterOwner(owner: String)

    fun clear(player: Player)

    fun push(player: Player, route: MenuRoute)

    fun open(player: Player, route: MenuRoute): Boolean

    fun pushAndOpen(player: Player, currentRoute: MenuRoute, targetRoute: MenuRoute): Boolean

    fun openRoot(player: Player, route: MenuRoute): Boolean

    fun openPrevious(player: Player): Boolean
}
