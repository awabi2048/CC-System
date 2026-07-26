package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuSurface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import org.bukkit.entity.Player

internal class MenuPresentationTracker {
    private val sequence = AtomicLong()
    private val presentations = ConcurrentHashMap<UUID, Presentation>()

    fun markOpened(player: Player, surface: MenuSurface, owner: String?, id: String?): Long {
        val revision = sequence.incrementAndGet()
        presentations[player.uniqueId] = Presentation(revision, surface, owner, id)
        return revision
    }

    fun markClosed(player: Player): Long {
        val revision = sequence.incrementAndGet()
        presentations.remove(player.uniqueId)
        return revision
    }

    fun current(player: Player): Presentation? = presentations[player.uniqueId]

    data class Presentation(
        val revision: Long,
        val surface: MenuSurface,
        val owner: String?,
        val id: String?,
    )
}
