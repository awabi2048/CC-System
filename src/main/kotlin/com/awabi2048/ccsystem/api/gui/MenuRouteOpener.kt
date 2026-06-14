package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player

fun interface MenuRouteOpener {
    fun open(player: Player, route: MenuRoute): Boolean
}
