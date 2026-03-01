package com.awabi2048.ccsystem.features.misc.command

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.LanguageManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * 遅延コマンド実行
 * 使用法: /delay <time with s(sec)|t(tick)> <command(console execution)>
 */
class DelayCommand : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!hasPermission(sender)) {
            sender.sendMessage(LanguageManager.getMessage(null, "no_permission"))
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§c使用法: /delay <time with s(sec)|t(tick)> <command>")
            return true
        }

        val timeArg = args[0].trim()
        val (timeValue, unit) = parseTime(timeArg)

        if (timeValue <= 0L) {
            sender.sendMessage("§c時間は0より大きい値を指定してください。")
            return true
        }

        // 残りの引数をコマンドとして結合し、先頭と末尾の空白、および先頭のスラッシュを除去
        val commandToExecute = args.drop(1).joinToString(" ").trim().removePrefix("/")

        // tickに変換（sの場合は秒→tick）
        val delayTicks = if (unit == 's') timeValue * 20L else timeValue
        val delayLabel = formatDelayLabel(timeValue, unit, delayTicks)

        CCSystem.instance.logger.info("遅延コマンドを予約しました: delay=$delayLabel, command=$commandToExecute")

        // 遅延実行
        Bukkit.getScheduler().scheduleSyncDelayedTask(CCSystem.instance, Runnable {
            val success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute)
            if (!success) {
                CCSystem.instance.logger.warning("遅延コマンドの実行に失敗しました: $commandToExecute")
            }
        }, delayTicks)

        sender.sendMessage("§a${delayLabel}後にコマンドを実行します: $commandToExecute")
        return true
    }

    private fun parseTime(timeArg: String): Pair<Long, Char> {
        val regex = Regex("""(?i)^(\d+)([st])$""")
        val match = regex.matchEntire(timeArg)
        
        if (match != null) {
            val (value, unit) = match.destructured
            return Pair(value.toLong(), unit[0].lowercaseChar())
        }
        
        // デフォルト: tickとして解釈（単位なし）
        return try {
            Pair(timeArg.toLong(), 't')
        } catch (e: NumberFormatException) {
            Pair(0L, 't')
        }
    }

    private fun formatDelayLabel(timeValue: Long, unit: Char, delayTicks: Long): String {
        return if (unit == 's') {
            "${timeValue}秒"
        } else {
            "${delayTicks}tick"
        }
    }

    private fun hasPermission(sender: CommandSender): Boolean {
        return sender.hasPermission("cc-system.delay.use") ||
               sender.hasPermission("cc-system.*") ||
               sender.isOp
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (!hasPermission(sender)) return emptyList()

        if (args.size == 1) {
            // 時間の例を提案
            return listOf("10s", "30s", "60s", "20t", "40t", "100t")
                .filter { it.startsWith(args[0], ignoreCase = true) }
        }

        // 2つ目以降の引数では何も提案しない（自由なコマンド入力）
        return emptyList()
    }
}
