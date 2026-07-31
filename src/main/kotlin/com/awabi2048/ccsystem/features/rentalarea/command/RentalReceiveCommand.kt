package com.awabi2048.ccsystem.features.rentalarea.command

import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.features.rentalarea.storage.RemainedItemManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class RentalReceiveCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!ConfigManager.isRentalAreaEnabled()) {
            sender.sendMessage(
                LanguageManager.getMessage(sender as? Player, "feature_disabled", "feature" to "rental_area")
            )
            return true
        }

        if (sender !is Player) {
            sender.sendMessage(LanguageManager.getMessage(null, "command_player_only"))
            return true
        }

        val player = sender
        val areaIds = RemainedItemManager.getRemainedAreaIds(player.uniqueId)

        if (areaIds.isEmpty()) {
            player.sendMessage(LanguageManager.getMessage(player, "rental_area_no_remained_items"))
            return true
        }

        val areaId = if (args.isNotEmpty() && args[0] in areaIds) {
            args[0]
        } else {
            areaIds.first()
        }

        val opened = RemainedItemManager.openStorage(player, areaId)
        if (!opened) {
            player.sendMessage(LanguageManager.getMessage(player, "rental_area_no_remained_items"))
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return emptyList()
        }

        if (sender !is Player) {
            return emptyList()
        }

        if (args.size == 1) {
            val areaIds = RemainedItemManager.getRemainedAreaIds(sender.uniqueId)
            return areaIds.filter { it.startsWith(args[0], ignoreCase = true) }
        }

        return emptyList()
    }
}
