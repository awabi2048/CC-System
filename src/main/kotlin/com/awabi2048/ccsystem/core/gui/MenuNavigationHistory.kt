package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuRoute
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MenuNavigationHistory(private val maxSize: Int = DEFAULT_MAX_SIZE) {
    private val histories = ConcurrentHashMap<UUID, ArrayDeque<MenuRoute>>()

    fun clear(playerId: UUID) {
        histories.remove(playerId)
    }

    fun push(playerId: UUID, route: MenuRoute): Boolean {
        val history = histories.computeIfAbsent(playerId) { ArrayDeque() }
        if (history.lastOrNull()?.key() == route.key()) return false

        // 画面そのものではなく再オープン可能な経路だけを積み、DialogなどGUI外の状態遷移にも同じ戻り先を使う。
        history.addLast(route)
        while (history.size > maxSize) {
            history.removeFirst()
        }
        return true
    }

    fun restore(playerId: UUID, routes: List<MenuRoute>) {
        if (routes.isEmpty()) {
            histories.remove(playerId)
            return
        }
        histories[playerId] = ArrayDeque(routes)
    }

    fun popPrevious(playerId: UUID, opener: (MenuRoute) -> Boolean): MenuRoute? {
        val history = histories[playerId] ?: return null

        while (history.isNotEmpty()) {
            val route = history.removeLast()
            if (opener(route)) {
                if (history.isEmpty()) histories.remove(playerId)
                return route
            }
        }

        histories.remove(playerId)
        return null
    }

    fun removeOwner(owner: String) {
        val iterator = histories.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.removeAll { it.owner == owner }
            if (entry.value.isEmpty()) {
                iterator.remove()
            }
        }
    }

    fun snapshot(playerId: UUID): List<MenuRoute> {
        return histories[playerId]?.toList().orEmpty()
    }

    private companion object {
        private const val DEFAULT_MAX_SIZE = 16
    }
}
