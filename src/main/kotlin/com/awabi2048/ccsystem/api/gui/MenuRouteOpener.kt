package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player

fun interface MenuRouteOpener {
    fun open(player: Player, route: MenuRoute): Boolean
}

/** 詳細結果を返すRoute openerです。既存の[MenuRouteOpener]とは別APIとして追加します。 */
fun interface MenuRouteResultOpener {
    fun open(player: Player, route: MenuRoute): MenuRuntimeOperationResult
}
