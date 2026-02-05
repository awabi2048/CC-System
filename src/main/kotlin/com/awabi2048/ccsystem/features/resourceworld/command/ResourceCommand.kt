package com.awabi2048.ccsystem.features.resourceworld.command

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.features.resourceworld.manager.ScoreboardManager
import com.awabi2048.ccsystem.features.resourceworld.manager.WorldManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ResourceCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) return false

        when (args[0].lowercase()) {
            "generate" -> {
                if (!hasPluginPermission(sender, "ccsystem.resource.generate")) {
                    sender.sendMessage(LanguageManager.getMessage(sender as? Player, "no_permission"))
                    return true
                }

                 if (args.size < 2) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.usage.generate"))
                     return true
                 }

                 val target = args[1].split(":")
                 if (target.size != 2) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.invalid_format"))
                     return true
                 }

                val type = target[0]
                val variation = target[1]
                val borderSize = if (args.size >= 3) args[2].toIntOrNull() else null

                 sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.generation_start", "type" to type, "variation" to variation))
                 val success = WorldManager.generateResourceWorld(type, variation, borderSize)
                 if (success) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.generation_success", "type" to type, "variation" to variation))
                 } else {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.generation_failed", "type" to type, "variation" to variation))
                 }
                return true
            }

             "teleport" -> {
                 if (!hasPluginPermission(sender, "ccsystem.resource.teleport")) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "no_permission"))
                     return true
                 }

                 if (args.size < 2) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.usage.teleport"))
                     return true
                 }

                val target = args[1].split(":")
                if (target.size != 2) {
                    sender.sendMessage("§c形式が正しくありません (例: normal:a)")
                    return true
                }

                val type = target[0]
                val variation = target[1]

                val targetPlayer = if (args.size >= 3) {
                     Bukkit.getPlayer(args[2]) ?: run {
                         sender.sendMessage(LanguageManager.getMessage(sender as? Player, "player_not_found", "player" to args[2]))
                         return true
                     }
                } else {
                    if (sender is Player) {
                        sender
                     } else {
                         sender.sendMessage(LanguageManager.getMessage(sender as? Player, "player_only_command"))
                         return true
                     }
                }

                WorldManager.teleportToResourceWorld(targetPlayer, type, variation)
                return true
            }

             "reload" -> {
                 if (!hasPluginPermission(sender, "ccsystem.resource.reload")) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "no_permission"))
                     return true
                 }
                val plugin = CCSystem.instance
                plugin.saveDefaultConfig()
                plugin.reloadConfig()
                 ConfigManager.load(plugin.config)
                 sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.reload_success"))
                 return true
            }

             "pause_pregen" -> {
                 if (!hasPluginPermission(sender, "ccsystem.resource.pause_pregen")) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "no_permission"))
                     return true
                 }

                 if (args.size < 2) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.usage.pause_pregen"))
                     return true
                 }

                val target = args[1].split(":")
                if (target.size != 2) {
                    sender.sendMessage("§c形式が正しくありません (例: normal:a)")
                    return true
                }

                val type = target[0]
                val variation = target[1]

                 val success = WorldManager.pausePregeneration(type, variation)
                 if (success) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.pause_success", "type" to type, "variation" to variation))
                 } else {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.pause_failed", "type" to type, "variation" to variation))
                 }
                return true
            }

             "close" -> {
                 if (!hasPluginPermission(sender, "ccsystem.resource.close")) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "no_permission"))
                     return true
                 }

                 if (args.size < 2) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.usage.close"))
                     return true
                 }

                val target = args[1].split(":")
                if (target.size != 2) {
                    sender.sendMessage("§c形式が正しくありません (例: normal:a)")
                    return true
                }

                val type = target[0]
                val variation = target[1]

                 val success = WorldManager.closeResourceWorld(type, variation)
                 if (success) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.close_success", "type" to type, "variation" to variation))
                 } else {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "resource.close_failed", "type" to type, "variation" to variation))
                 }
                return true
            }

             "monitor" -> {
                 if (!hasPluginPermission(sender, "ccsystem.resource.monitor")) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "no_permission"))
                     return true
                 }

                 if (sender !is Player) {
                     sender.sendMessage(LanguageManager.getMessage(sender as? Player, "player_only_command"))
                     return true
                 }

                val message = ScoreboardManager.toggleMonitor(sender)
                sender.sendMessage(message)
                return true
            }
        }

        return false
    }

    private fun hasPluginPermission(sender: CommandSender, permission: String): Boolean {
        return sender.hasPermission(permission) || sender.hasPermission("ccsystem.*") || sender.isOp
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val list = mutableListOf<String>()

        if (args.size == 1) {
            val subCommands = mutableListOf<String>()
            if (hasPluginPermission(sender, "ccsystem.resource.generate")) subCommands.add("generate")
            if (hasPluginPermission(sender, "ccsystem.resource.teleport")) subCommands.add("teleport")
            if (hasPluginPermission(sender, "ccsystem.resource.reload")) subCommands.add("reload")
            if (hasPluginPermission(sender, "ccsystem.resource.pause_pregen")) subCommands.add("pause_pregen")
            if (hasPluginPermission(sender, "ccsystem.resource.close")) subCommands.add("close")
            if (hasPluginPermission(sender, "ccsystem.resource.monitor")) subCommands.add("monitor")

            list.addAll(subCommands.filter { it.startsWith(args[0].lowercase()) })
        } else if (args.size == 2) {
            val query = args[1].lowercase()
            for ((type, config) in ConfigManager.getAllResourceConfigs()) {
                for (variation in config.variations) {
                    val target = "$type:$variation"
                    if (target.startsWith(query)) {
                        list.add(target)
                    }
                }
            }
        } else if (args.size == 3 && args[0].lowercase() == "teleport") {
            if (hasPluginPermission(sender, "ccsystem.resource.teleport")) {
                list.addAll(Bukkit.getOnlinePlayers().map { it.name }.filter { it.lowercase().startsWith(args[2].lowercase()) })
            }
        }

        return list
    }
}