package com.awabi2048.ccsystem.features.misc.command

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.config.MessageManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import com.awabi2048.ccsystem.features.publicsign.manager.PublicSignManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.time.LocalDate

/**
 * CC-System管理コマンド
 * 使用法: /cc-system <toggle|lang|reload|update-day>
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
                PublicSignManager.load()

                // 音楽再生設定を反映させるためにタスクを更新
                CCSystem.instance.musicListener.stopAllPlayersMusic()
                if (ConfigManager.isMusicEnabled()) {
                    CCSystem.instance.musicListener.startAllPlayersMusic()
                }

                sender.sendMessage(LanguageManager.getMessage(player, "reload_success"))
            }
            "update-day" -> {
                val resetCount = PublicSignManager.updateDay(LocalDate.now())
                sender.sendMessage(
                    LanguageManager.getMessage(
                        player,
                        "public_sign_update_day_done",
                        "count" to resetCount.toString()
                    )
                )
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
            return listOf("toggle", "lang", "reload", "update-day").filter {
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
            }
        }

        return emptyList()
    }
}
