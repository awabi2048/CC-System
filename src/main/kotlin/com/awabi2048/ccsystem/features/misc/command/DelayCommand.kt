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

        val timeArg = args[0]
        val (timeValue, unit) = parseTime(timeArg)

        if (timeValue <= 0L) {
            sender.sendMessage("§c時間は0より大きい値を指定してください。")
            return true
        }

        // 残りの引数をコマンドとして結合
        val commandToExecute = args.drop(1).joinToString(" ")

        // tickに変換（sの場合は秒→tick）
        val delayTicks = if (unit == 's') timeValue * 20L else timeValue

        // 遅延実行
        Bukkit.getScheduler().scheduleSyncDelayedTask(CCSystem.instance, Runnable {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute)
        }, delayTicks)

        sender.sendMessage("§a${timeArg}後にコマンドを実行します: $commandToExecute")
        return true
    }

    private fun parseTime(timeArg: String): Pair<Long, Char> {
        val regex = Regex("""(\d+)([st])""")
        val match = regex.find(timeArg)
        
        if (match != null) {
            val (value, unit) = match.destructured
            return Pair(value.toLong(), unit[0].lowercaseChar())
        }
        
        // デフォルト: 秒として解釈（単位なし）
        return try {
            Pair(timeArg.toLong(), 's')
        } catch (e: NumberFormatException) {
            Pair(0L, 's')
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