package com.awabi2048.ccsystem.features.misc.listener

import org.bukkit.entity.Player

object BinderCommandExecutor {

    fun execute(player: Player, commands: List<String>): Boolean {
        var executed = false
        for (command in commands) {
            if (command.isBlank()) continue

            val processedCommand = command
                .replace("%player_name%", player.name)
                .replace("%player_uuid%", player.uniqueId.toString())

            player.performCommand(processedCommand)
            executed = true
        }
        return executed
    }
}
