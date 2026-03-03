package com.awabi2048.ccsystem.features.misc.command

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.config.MessageManager
import com.awabi2048.ccsystem.core.data.PlacedBlockLedgerManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import com.awabi2048.ccsystem.core.item.CustomItemFactory
import com.awabi2048.ccsystem.features.publicsign.manager.PublicSignManager
import com.awabi2048.ccsystem.features.rentalarea.manager.RentalAreaManager
import com.awabi2048.ccsystem.features.rentalarea.storage.RemainedItemManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.time.LocalDate

/**
 * CC-System管理コマンド
 * 使用法: /cc-system <toggle|lang|reload|update-day|rental-ticket|status>
 */
class CCSystemCommand : CommandExecutor, TabCompleter {

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
                            musicListener.playMusic(player, player.world.name)
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
            "lang" -> {
                if (player == null) {
                    sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。")
                    return true
                }

                val availableLangs = listOf("ja_jp", "en_us")
                val currentLang = PlayerDataManager.getString(player.uniqueId, "lang", "ja_jp")
                    ?: "ja_jp"

                val currentIndex = availableLangs.indexOf(currentLang)
                val nextIndex = if (currentIndex >= availableLangs.size - 1) 0 else currentIndex + 1
                val nextLang = availableLangs[nextIndex]

                LanguageManager.setPlayerLang(player, nextLang)

                val displayName = getLanguageDisplayName(player, nextLang)
                val functionName = getFunctionDisplayName(player, "lang")
                player.sendActionBar(
                    LanguageManager.getMessageWithoutPrefix(
                        player,
                        "lang_updated",
                        "function" to functionName,
                        "lang" to displayName
                    )
                )
            }
            "reload" -> {
                CCSystem.instance.ensureDefaultFiles()
                CCSystem.instance.reloadConfig()
                ConfigManager.reload(CCSystem.instance.config)
                LanguageManager.load()
                MessageManager.load()
                PlacedBlockLedgerManager.load()
                if (ConfigManager.isPublicSignEnabled()) {
                    PublicSignManager.load()
                }
                if (ConfigManager.isRentalAreaEnabled()) {
                    RemainedItemManager.load()
                    RentalAreaManager.load()
                }

                // 音楽再生設定を反映させるためにタスクを更新
                if (CCSystem.instance.hasMusicListener()) {
                    CCSystem.instance.musicListener.stopAllPlayersMusic()
                }
                if (ConfigManager.isMusicEnabled() && CCSystem.instance.hasMusicListener()) {
                    CCSystem.instance.musicListener.startAllPlayersMusic()
                }

                // 動的描画距離設定を再反映
                if (CCSystem.instance.hasDynamicDistanceListener()) {
                    CCSystem.instance.dynamicDistanceListener.reload()
                }

                sender.sendMessage(LanguageManager.getMessage(player, "reload_success"))
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

                val features = listOf(
                    "resource_world" to ConfigManager.isResourceWorldEnabled(),
                    "rental_area" to ConfigManager.isRentalAreaEnabled(),
                    "public_sign" to ConfigManager.isPublicSignEnabled(),
                    "music" to ConfigManager.isMusicEnabled(),
                    "dynamic_distance" to ConfigManager.isDynamicDistanceEnabled(),
                    "shift_f_binder" to ConfigManager.isShiftFBinderEnabled(),
                    "disable_global_sound_events" to ConfigManager.isGlobalSoundEventsAutoDisable(),
                    "delay_command" to ConfigManager.isDelayCommandEnabled(),
                    "npc_message" to ConfigManager.isNpcMessageEnabled()
                )

                for ((featureKey, enabled) in features) {
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
                "lang" -> "cc-system.lang"
                "reload" -> "cc-system.reload"
                "update-day" -> "cc-system.update-day"
                "rental-ticket" -> "cc-system.rental-ticket"
                "status" -> "cc-system.status"
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

    private fun getLanguageDisplayName(player: Player?, lang: String): String {
        val key = "language.$lang"
        val localized = LanguageManager.getRawString(player, key)
        return if (localized == key) lang else localized
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            val subCommands =
                listOf("toggle", "lang", "reload", "update-day", "rental-ticket", "status").filter {
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
                    if (!ConfigManager.isMusicEnabled()) return emptyList()
                    return listOf("play_music").filter {
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
            }
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

        return emptyList()
    }
}
