package com.awabi2048.ccsystem.api.gui

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

enum class MenuTargetPolicy {
    SELF_ONLY,
    PLAYER_TARGET,
    CONSOLE_AND_PLAYER_TARGET
}

fun interface PublicMenuOpener {
    fun open(player: Player, arguments: Map<String, String>): Boolean
}

data class PublicMenuDefinition(
    val owner: String,
    val id: String,
    val permission: String?,
    val targetPolicy: MenuTargetPolicy,
    val argumentKeys: Set<String>,
    val opener: PublicMenuOpener
) {
    val routeId: String
        get() = "$owner:$id"
}

interface MenuCommandService {
    fun register(definition: PublicMenuDefinition)

    fun unregisterOwner(owner: String)

    fun definitions(): List<PublicMenuDefinition>

    fun definition(routeId: String): PublicMenuDefinition?

    fun open(
        sender: CommandSender,
        target: Player,
        routeId: String,
        arguments: Map<String, String>
    ): Boolean
}
