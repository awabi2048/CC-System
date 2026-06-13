package com.awabi2048.ccsystem.core.gui

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player

class MenuRouteHistory {
    private val histories = ConcurrentHashMap<UUID, ArrayDeque<MenuRoute>>()

    fun clear(player: Player) {
        histories.remove(player.uniqueId)
    }

    fun push(player: Player, key: String, opener: MenuRouteOpener) {
        val history = histories.computeIfAbsent(player.uniqueId) { ArrayDeque() }
        if (history.lastOrNull()?.key == key) return
        history.addLast(MenuRoute(key, opener))
        while (history.size > MAX_HISTORY_SIZE) {
            history.removeFirst()
        }
    }

    fun openPrevious(player: Player): Boolean {
        val history = histories[player.uniqueId] ?: return false
        while (history.isNotEmpty()) {
            val route = history.removeLast()
            if (route.opener.open(player)) {
                if (history.isEmpty()) histories.remove(player.uniqueId)
                return true
            }
        }
        histories.remove(player.uniqueId)
        return false
    }

    fun interface MenuRouteOpener {
        fun open(player: Player): Boolean
    }

    private data class MenuRoute(
        val key: String,
        val opener: MenuRouteOpener
    )

    private companion object {
        private const val MAX_HISTORY_SIZE = 16
    }
}
