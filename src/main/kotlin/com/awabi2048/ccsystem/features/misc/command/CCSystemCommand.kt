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
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.time.LocalDate

/**
 * CC-System管理コマンド
 * 使用法: /cc-system <toggle|lang|reload|update-day|rental-ticket>
 */
class CCSystemCommand : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val player = sender as? Player

        if (!hasPermission(sender)) {
            sender.sendMessage(LanguageManager.getMessage(player, "no_permission"))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(LanguageManager.getMessage(player, "usage"))
            return true
        }

        when (args[0].lowercase()) {
            "toggle" -> {
                if (args.size < 2) {
                    sender.sendMessage(LanguageManager.getMessage(player, "usage"))
                    return true
                }

                when (args[1].lowercase()) {
                    "play_music" -> {
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
                        sender.sendMessage(
                            LanguageManager.getMessage(
                                player,
                                "toggle_success",
                                "function" to "play_music",
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

                val displayName = getLanguageDisplayName(nextLang)
                sender.sendMessage(
                    LanguageManager.getMessage(
                        player,
                        "lang_updated",
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
                PublicSignManager.load()
                RentalAreaManager.load()

                // 音楽再生設定を反映させるためにタスクを更新
                CCSystem.instance.musicListener.stopAllPlayersMusic()
                if (ConfigManager.isMusicEnabled()) {
                    CCSystem.instance.musicListener.startAllPlayersMusic()
                }

                sender.sendMessage(LanguageManager.getMessage(player, "reload_success"))
            }
            "update-day" -> {
                val resetCount = PublicSignManager.updateDay(LocalDate.now())
                val rentalExpiredCount = RentalAreaManager.updateDay(LocalDate.now())
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
            else -> {
                sender.sendMessage(LanguageManager.getMessage(player, "usage"))
            }
        }

        return true
    }

    private fun hasPermission(sender: CommandSender): Boolean {
        return sender.hasPermission("cc-system.admin") ||
               sender.hasPermission("cc-system.*") ||
               sender.isOp
    }

    private fun getLanguageDisplayName(lang: String): String {
        return when (lang) {
            "ja_jp" -> "日本語"
            "en_us" -> "English"
            else -> lang
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (!hasPermission(sender)) return emptyList()

        if (args.size == 1) {
            return listOf("toggle", "lang", "reload", "update-day", "rental-ticket").filter {
                it.startsWith(args[0], ignoreCase = true)
            }
        }

        if (args.size == 2) {
            when (args[0].lowercase()) {
                "toggle" -> {
                    return listOf("play_music").filter {
                        it.startsWith(args[1], ignoreCase = true)
                    }
                }
                "rental-ticket" -> {
                    return Bukkit.getOnlinePlayers().map { it.name }.filter {
                        it.startsWith(args[1], ignoreCase = true)
                    }
                }
            }
        }

        if (args.size == 3 && args[0].equals("rental-ticket", ignoreCase = true)) {
            return listOf("7", "14", "30").filter {
                it.startsWith(args[2], ignoreCase = true)
            }
        }

        if (args.size == 4 && args[0].equals("rental-ticket", ignoreCase = true)) {
            return listOf("1", "16", "64").filter {
                it.startsWith(args[3], ignoreCase = true)
            }
        }

        return emptyList()
    }
}
