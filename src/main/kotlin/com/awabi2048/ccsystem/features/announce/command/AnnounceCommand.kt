package com.awabi2048.ccsystem.features.announce.command

import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.features.announce.listener.AnnounceListener
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class AnnounceCommand : CommandExecutor, TabCompleter {
    companion object {
        const val USE_PERMISSION = "cc-system.announce.use"
        const val MANAGE_PERMISSION = "cc-system.announce.manage"
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!hasPluginPermission(sender, USE_PERMISSION)) {
            sender.sendMessage(LanguageManager.getMessage(sender as? Player, "no_permission"))
            return true
        }

        val player = sender as? Player
        if (player == null) {
            sender.sendMessage(LanguageManager.getMessage(null, "player_only_command"))
            return true
        }

        if (args.isEmpty()) {
            AnnounceListener.openAnnouncementMenu(player, openedFromMenuArgument = false)
            return true
        }

        if (args.size == 1 && args[0].equals("-menu", ignoreCase = true)) {
            AnnounceListener.openAnnouncementMenu(player, openedFromMenuArgument = true)
            return true
        }

        player.sendMessage(LanguageManager.getMessage(player, "announce_usage"))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!hasPluginPermission(sender, USE_PERMISSION)) {
            return emptyList()
        }

        if (args.size == 1) {
            return listOf("-menu").filter { it.startsWith(args[0], ignoreCase = true) }
        }

        return emptyList()
    }

    private fun hasPluginPermission(sender: CommandSender, permission: String): Boolean {
        return sender.hasPermission(permission) ||
            sender.hasPermission("cc-system.admin") ||
            sender.hasPermission("cc-system.*") ||
            sender.isOp
    }
}
