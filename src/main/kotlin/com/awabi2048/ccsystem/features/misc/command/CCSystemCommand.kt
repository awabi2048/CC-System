package com.awabi2048.ccsystem.features.misc.command

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.config.MessageManager
import com.awabi2048.ccsystem.core.data.PlacedBlockLedgerManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import com.awabi2048.ccsystem.core.item.CustomItemFactory
import com.awabi2048.ccsystem.features.clock.manager.ClockManager
import com.awabi2048.ccsystem.features.publicsign.manager.PublicSignManager
import com.awabi2048.ccsystem.features.rentalarea.manager.RentalAreaManager
import com.awabi2048.ccsystem.features.announce.manager.AnnouncementManager
import com.awabi2048.ccsystem.features.lwcx.command.LwcExpansionCommand
import com.awabi2048.ccsystem.features.misc.inputmacro.InputMacroCommandHandler
import com.awabi2048.ccsystem.features.misc.inputmacro.InputMacroMode
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.time.LocalDate

/**
 * CC-System管理コマンド
 * 使用法: /cc-system <toggle|reload|update-day|rental-ticket|status|enable|disable>
 */
class CCSystemCommand : CommandExecutor, TabCompleter {

    private val lwcExpansionCommand = LwcExpansionCommand()
    private val inputMacroCommandHandler = InputMacroCommandHandler()

    private val manageableFeatures = listOf(
        "resource_world",
        "rental_area",
        "public_sign",
        "music",
        "dynamic_distance",
        "debug",
        "shift_f_binder",
        "player_left_click_binder",
        "disable_global_sound_events",
        "delay_command",
        "npc_message"
    )

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val player = sender as? Player

        if (args.isEmpty()) {
            sender.sendMessage(LanguageManager.getMessage(player, "usage"))
            return true
        }

        val subCommand = args[0].lowercase()
        if (!hasPermissionForSubCommand(sender, subCommand)) {
            sender.sendMessage(LanguageManager.getMessage(player, "no_permission"))
            return true
        }

        when (subCommand) {
            "toggle" -> {
                if (args.size < 2) {
                    sender.sendMessage(LanguageManager.getMessage(player, "usage"))
                    return true
                }

                val toggleFunction = args[1].lowercase()

                when (toggleFunction) {
                    "play_music" -> {
                        if (!ConfigManager.isMusicEnabled()) {
                            sender.sendMessage(
                                LanguageManager.getMessage(
                                    player,
                                    "feature_disabled",
                                    "feature" to getFunctionDisplayName(player, "play_music")
                                )
                            )
                            return true
                        }
                        if (!CCSystem.instance.hasMusicListener()) {
                            sender.sendMessage(
                                LanguageManager.getMessage(
                                    player,
                                    "feature_disabled",
                                    "feature" to getFunctionDisplayName(player, "play_music")
                                )
                            )
                            return true
                        }
                        if (player == null) {
                            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。")
                            return true
                        }
                        val current = PlayerDataManager.getBoolean(player.uniqueId, "play_music", true)
                        val newValue = !current
                        PlayerDataManager.set(player.uniqueId, "play_music", newValue)

                        // プレイヤーの音楽を即座に更新
                        val musicListener = CCSystem.instance.musicListener
                        if (newValue) {
                            musicListener.playMusic(player, player.world.key.toString())
                        } else {
                            musicListener.stopMusic(player)
                        }

                        val statusKey = if (newValue) "enabled" else "disabled"
                        val statusString = LanguageManager.getRawString(player, statusKey)
                        val functionName = getFunctionDisplayName(player, "play_music")
                        player.sendActionBar(
                            LanguageManager.getMessageWithoutPrefix(
                                player,
                                "toggle_success",
                                "function" to functionName,
                                "status" to statusString
                            )
                        )
                    }
                    "clock_bar" -> {
                        if (player == null) {
                            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。")
                            return true
                        }

                        val current = PlayerDataManager.getBoolean(player.uniqueId, ClockManager.PLAYER_DATA_CLOCK_BAR, true)
                        val newValue = !current
                        PlayerDataManager.set(player.uniqueId, ClockManager.PLAYER_DATA_CLOCK_BAR, newValue)

                        val statusKey = if (newValue) "enabled" else "disabled"
                        val statusString = LanguageManager.getRawString(player, statusKey)
                        val functionName = getFunctionDisplayName(player, "clock_bar")
                        player.sendActionBar(
                            LanguageManager.getMessageWithoutPrefix(
                                player,
                                "toggle_success",
                                "function" to functionName,
                                "status" to statusString
                            )
                        )
                    }
                    else -> {
                        sender.sendMessage(
                            LanguageManager.getMessage(
                                player,
                                "function_not_found",
                                "function" to args[1]
                            )
                        )
                    }
                }
            }
            "reload" -> {
                performReload(player, sender)
            }
            "update-day" -> {
                val resetCount = if (ConfigManager.isPublicSignEnabled()) {
                    PublicSignManager.updateDay(LocalDate.now())
                } else {
                    0
                }
                val rentalExpiredCount = if (ConfigManager.isRentalAreaEnabled()) {
                    RentalAreaManager.updateDay(LocalDate.now())
                } else {
                    0
                }
                sender.sendMessage(
                    LanguageManager.getMessage(
                        player,
                        "update_day_done",
                        "public_sign_count" to resetCount.toString(),
                        "rental_area_count" to rentalExpiredCount.toString()
                    )
                )
            }
            "rental-ticket" -> {
                if (!ConfigManager.isRentalAreaEnabled()) {
                    sender.sendMessage(
                        LanguageManager.getMessage(
                            player,
                            "feature_disabled",
                            "feature" to "rental_area"
                        )
                    )
                    return true
                }

                if (args.size < 3) {
                    sender.sendMessage(LanguageManager.getMessage(player, "rental_ticket_usage"))
                    return true
                }

                val target = Bukkit.getPlayerExact(args[1])
                if (target == null) {
                    sender.sendMessage(
                        LanguageManager.getMessage(
                            player,
                            "player_not_found",
                            "player" to args[1]
                        )
                    )
                    return true
                }

                val days = args[2].toIntOrNull()
                if (days == null || days <= 0) {
                    sender.sendMessage(LanguageManager.getMessage(player, "rental_ticket_invalid_days"))
                    return true
                }

                val amount = args.getOrNull(3)?.toIntOrNull()?.coerceIn(1, 64) ?: 1
                val ticket = CustomItemFactory.createRentalTicket(target, days, amount)
                val leftovers = target.inventory.addItem(ticket)
                leftovers.values.forEach { overflow ->
                    target.world.dropItemNaturally(target.location, overflow)
                }

                sender.sendMessage(
                    LanguageManager.getMessage(
                        player,
                        "rental_ticket_give_success",
                        "player" to target.name,
                        "days" to days.toString(),
                        "amount" to amount.toString()
                    )
                )

                if (sender != target) {
                    target.sendMessage(
                        LanguageManager.getMessage(
                            target,
                            "rental_ticket_received",
                            "days" to days.toString(),
                            "amount" to amount.toString()
                        )
                    )
                }
            }
            "status" -> {
                sender.sendMessage(LanguageManager.getMessageWithoutPrefix(player, "status_header"))

                for (featureKey in manageableFeatures) {
                    val enabled = isFeatureEnabled(featureKey)
                    val statusLabel = LanguageManager.getRawString(player, if (enabled) "enabled" else "disabled")
                    val statusText = if (enabled) "§a$statusLabel" else "§c$statusLabel"
                    val featureName = LanguageManager.getRawString(player, "feature.$featureKey")
                    sender.sendMessage(
                        LanguageManager.getMessageWithoutPrefix(
                            player,
                            "status_line",
                            "feature" to featureName,
                            "status" to statusText
                        )
                    )
                }
            }
            "lwc-expansion" -> {
                lwcExpansionCommand.execute(sender, args.drop(1).toTypedArray())
            }
            "input_macro" -> {
                val player = sender as? Player
                if (player == null) {
                    sender.sendMessage(LanguageManager.getMessage(null, "input_macro.player_only"))
                    return true
                }
                inputMacroCommandHandler.execute(player, args.drop(1))
            }
            "enable", "disable" -> {
                if (args.size < 2) {
                    sender.sendMessage(LanguageManager.getMessage(player, "feature_toggle_usage"))
                    return true
                }

                val featureKey = resolveFeatureKey(args[1])
                if (featureKey == null) {
                    sender.sendMessage(
                        LanguageManager.getMessage(
                            player,
                            "function_not_found",
                            "function" to args[1]
                        )
                    )
                    return true
                }

                val targetEnabled = subCommand == "enable"
                val currentEnabled = isFeatureEnabled(featureKey)
                val featureName = LanguageManager.getRawString(player, "feature.$featureKey")
                val targetStatus = LanguageManager.getRawString(player, if (targetEnabled) "enabled" else "disabled")

                if (currentEnabled == targetEnabled) {
                    sender.sendMessage(
                        LanguageManager.getMessage(
                            player,
                            "feature_already_set",
                            "feature" to featureName,
                            "status" to targetStatus
                        )
                    )
                    return true
                }

                val saved = when (featureKey) {
                    "debug" -> ConfigManager.setDebugEnabled(targetEnabled)
                    else -> ConfigManager.setFeatureEnabled(featureKey, targetEnabled)
                }

                if (!saved) {
                    sender.sendMessage("§c機能設定の保存に失敗しました。")
                    return true
                }

                sender.sendMessage(
                    LanguageManager.getMessage(
                        player,
                        "feature_set_success",
                        "feature" to featureName,
                        "status" to targetStatus
                    )
                )

                performReload(player, sender)
            }
            else -> {
                sender.sendMessage(LanguageManager.getMessage(player, "usage"))
            }
        }

        return true
    }

    private fun hasPermissionForSubCommand(sender: CommandSender, subCommand: String): Boolean {
        val permission =
            when (subCommand) {
                "toggle" -> "cc-system.toggle"
                "reload" -> "cc-system.reload"
                "update-day" -> "cc-system.update-day"
                "rental-ticket" -> "cc-system.rental-ticket"
                "status" -> "cc-system.status"
                "enable" -> "cc-system.enable"
                "disable" -> "cc-system.disable"
                "lwc-expansion" -> "cc-system.lwc.expansion"
                "input_macro" -> return InputMacroMode.entries.any { hasPluginPermission(sender, it.permission) }
                else -> return true
            }
        return hasPluginPermission(sender, permission)
    }

    private fun hasPluginPermission(sender: CommandSender, permission: String): Boolean {
        return sender.hasPermission(permission) ||
            sender.hasPermission("cc-system.admin") ||
            sender.hasPermission("cc-system.*") ||
            sender.isOp
    }

    private fun getFunctionDisplayName(player: Player?, function: String): String {
        val key = "function.$function"
        val localized = LanguageManager.getRawString(player, key)
        return if (localized == key) function else localized
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            val subCommands =
                listOf("toggle", "reload", "update-day", "rental-ticket", "status", "enable", "disable", "lwc-expansion", "input_macro").filter {
                    hasPermissionForSubCommand(sender, it) &&
                        when (it) {
                            "rental-ticket" -> ConfigManager.isRentalAreaEnabled()
                            else -> true
                        }
                }
            return subCommands.filter {
                it.startsWith(args[0], ignoreCase = true)
            }
        }

        if (args.size == 2) {
            when (args[0].lowercase()) {
                "toggle" -> {
                    if (!hasPermissionForSubCommand(sender, "toggle")) return emptyList()
                    val toggleTargets = mutableListOf("clock_bar")
                    if (ConfigManager.isMusicEnabled()) {
                        toggleTargets.add("play_music")
                    }
                    return toggleTargets.filter {
                        it.startsWith(args[1], ignoreCase = true)
                    }
                }
                "rental-ticket" -> {
                    if (!hasPermissionForSubCommand(sender, "rental-ticket")) return emptyList()
                    if (!ConfigManager.isRentalAreaEnabled()) return emptyList()
                    return Bukkit.getOnlinePlayers().map { it.name }.filter {
                        it.startsWith(args[1], ignoreCase = true)
                    }
                }
                "enable", "disable" -> {
                    if (!hasPermissionForSubCommand(sender, args[0].lowercase())) return emptyList()

                    val targetEnabled = args[0].equals("enable", ignoreCase = true)
                    return manageableFeatures
                        .filter { isFeatureEnabled(it) != targetEnabled }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                }
                "lwc-expansion" -> {
                    if (!hasPermissionForSubCommand(sender, "lwc-expansion")) return emptyList()
                    return listOf("world_cleanup", "remained_info_lookup", "status").filter {
                        it.startsWith(args[1], ignoreCase = true)
                    }
                }
            }
        }

        if (
            args.size == 3 &&
            args[0].equals("lwc-expansion", ignoreCase = true) &&
            args[1].equals("world_cleanup", ignoreCase = true) &&
            hasPermissionForSubCommand(sender, "lwc-expansion")
        ) {
            return Bukkit.getWorlds().map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
        }

        if (
            args.size == 4 &&
            args[0].equals("lwc-expansion", ignoreCase = true) &&
            args[1].equals("world_cleanup", ignoreCase = true) &&
            hasPermissionForSubCommand(sender, "lwc-expansion")
        ) {
            return listOf("confirm").filter { it.startsWith(args[3], ignoreCase = true) }
        }

        if (
                args.size == 3 &&
                args[0].equals("rental-ticket", ignoreCase = true) &&
                ConfigManager.isRentalAreaEnabled() &&
                hasPermissionForSubCommand(sender, "rental-ticket")
        ) {
            return listOf("7", "14", "30").filter {
                it.startsWith(args[2], ignoreCase = true)
            }
        }

        if (
                args.size == 4 &&
                args[0].equals("rental-ticket", ignoreCase = true) &&
                ConfigManager.isRentalAreaEnabled() &&
                hasPermissionForSubCommand(sender, "rental-ticket")
        ) {
            return listOf("1", "16", "64").filter {
                it.startsWith(args[3], ignoreCase = true)
            }
        }

        if (args[0].equals("input_macro", ignoreCase = true)) {
            return InputMacroMode.entries
                .filter { hasPluginPermission(sender, it.permission) }
                .map { it.label }
                .filter { it.startsWith(args.last(), ignoreCase = true) }
        }

        return emptyList()
    }

    private fun resolveFeatureKey(raw: String): String? {
        val normalized = raw.lowercase().replace('-', '_')
        return manageableFeatures.firstOrNull { it == normalized }
    }

    private fun isFeatureEnabled(featureKey: String): Boolean {
        return when (featureKey) {
            "resource_world" -> ConfigManager.isResourceWorldEnabled()
            "rental_area" -> ConfigManager.isRentalAreaEnabled()
            "public_sign" -> ConfigManager.isPublicSignEnabled()
            "music" -> ConfigManager.isMusicEnabled()
            "dynamic_distance" -> ConfigManager.isDynamicDistanceEnabled()
            "debug" -> ConfigManager.isDebug()
            "shift_f_binder" -> ConfigManager.isShiftFBinderEnabled()
            "player_left_click_binder" -> ConfigManager.isPlayerLeftClickBinderEnabled()
            "disable_global_sound_events" -> ConfigManager.isGlobalSoundEventsAutoDisable()
            "delay_command" -> ConfigManager.isDelayCommandEnabled()
            "npc_message" -> ConfigManager.isNpcMessageEnabled()
            else -> false
        }
    }

    private fun performReload(player: Player?, sender: CommandSender) {
        CCSystem.instance.ensureDefaultFiles()
        ConfigManager.reload()
        LanguageManager.load()
        MessageManager.load()
        PlacedBlockLedgerManager.load()
        AnnouncementManager.load()
        CCSystem.instance.syncFeatureRuntime()

        sender.sendMessage(LanguageManager.getMessage(player, "reload_success"))
    }
}
