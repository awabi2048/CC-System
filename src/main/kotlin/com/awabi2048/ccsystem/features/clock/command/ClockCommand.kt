package com.awabi2048.ccsystem.features.clock.command

import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.features.clock.manager.ClockManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ClockCommand : CommandExecutor, TabCompleter {

    private val durationRegex = Regex("^(\\d+)([smhSMH])$")
    private val timeRegex = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$")
    private val maxDurationMillis = 24L * 60L * 60L * 1000L
    private val serverZoneId: ZoneId = ZoneId.systemDefault()

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
            sender.sendMessage(LanguageManager.getMessage(player, "clock.usage.main"))
            return true
        }

        when (args[0].lowercase()) {
            "timer" -> handleTimer(sender, player, args)
            "alarm" -> handleAlarm(sender, player, args)
            "list" -> handleList(sender, player)
            "cancel" -> handleCancel(sender, player, args)
            else -> sender.sendMessage(LanguageManager.getMessage(player, "clock.usage.main"))
        }
        return true
    }

    private fun handleTimer(sender: CommandSender, player: Player?, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.usage.timer"))
            return
        }

        val durationMillis = parseDurationMillis(args[1])
        if (durationMillis == null) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.invalid_duration_format"))
            return
        }

        if (durationMillis < 1000L || durationMillis >= maxDurationMillis) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.duration_out_of_range"))
            return
        }

        val titleAndForce = parseTitleAndForce(args, 2)
        if (titleAndForce == null) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.usage.timer"))
            return
        }

        val (title, forceBar) = titleAndForce
        if (title.isBlank()) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.title_required"))
            return
        }

        val durationLabel = formatDurationLabel(args[1])
        val created = ClockManager.createTimer(
            title = title,
            durationMillis = durationMillis,
            durationLabel = durationLabel,
            forceBar = forceBar,
            setterName = sender.name
        )
        if (created == null) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.title_conflict", "title" to title))
            return
        }

        sender.sendMessage(
            LanguageManager.getMessage(player, "clock.set_success", "id" to created.id)
        )
    }

    private fun handleAlarm(sender: CommandSender, player: Player?, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.usage.alarm"))
            return
        }

        val alarmTime = parseAlarmTime(args[1])
        if (alarmTime == null) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.invalid_time_format"))
            return
        }

        val now = LocalDateTime.now(serverZoneId)
        val target = now.toLocalDate().atTime(alarmTime)
        if (!target.isAfter(now)) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.alarm_not_future"))
            return
        }

        val titleAndForce = parseTitleAndForce(args, 2)
        if (titleAndForce == null) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.usage.alarm"))
            return
        }

        val (title, forceBar) = titleAndForce
        if (title.isBlank()) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.title_required"))
            return
        }

        val triggerMillis = target.atZone(serverZoneId).toInstant().toEpochMilli()
        val alarmLabel = ClockManager.formatAlarmLabelFromEpochMillis(triggerMillis)
        val created = ClockManager.createAlarm(
            title = title,
            triggerEpochMillis = triggerMillis,
            alarmLabel = alarmLabel,
            forceBar = forceBar,
            setterName = sender.name
        )
        if (created == null) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.title_conflict", "title" to title))
            return
        }

        sender.sendMessage(
            LanguageManager.getMessage(player, "clock.set_success", "id" to created.id)
        )
    }

    private fun handleList(sender: CommandSender, player: Player?) {
        val clocks = ClockManager.getActiveClocks()
        if (clocks.isEmpty()) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.list_empty"))
            return
        }

        sender.sendMessage(LanguageManager.getMessageWithoutPrefix(player, "clock.list_header"))
        for (clock in clocks) {
            val typeLabel = when (clock.type) {
                ClockManager.ClockType.TIMER -> LanguageManager.getRawString(player, "clock.type.timer")
                ClockManager.ClockType.ALARM -> LanguageManager.getRawString(player, "clock.type.alarm")
            }
            val remaining = formatRemaining(ClockManager.getRemainingMillis(clock))
            val force = if (clock.forceBar) "true" else "false"
            sender.sendMessage(
                LanguageManager.getMessageWithoutPrefix(
                    player,
                    "clock.list_line",
                    "id" to clock.id,
                    "type" to typeLabel,
                    "arg" to clock.argLabel,
                    "title" to clock.title,
                    "remaining" to remaining,
                    "force" to force
                )
            )
        }
    }

    private fun handleCancel(sender: CommandSender, player: Player?, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.usage.cancel"))
            return
        }

        val targetTitle = args.drop(1).joinToString(" ").trim()
        if (targetTitle.isEmpty()) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.usage.cancel"))
            return
        }

        val canceled = ClockManager.cancelByTitle(targetTitle)
        if (canceled == null) {
            sender.sendMessage(LanguageManager.getMessage(player, "clock.cancel_not_found", "title" to targetTitle))
            return
        }

        sender.sendMessage(LanguageManager.getMessage(player, "clock.cancel_success", "title" to canceled.title))
    }

    private fun parseDurationMillis(raw: String): Long? {
        val match = durationRegex.matchEntire(raw.trim()) ?: return null
        val value = match.groupValues[1].toLongOrNull() ?: return null
        val unit = match.groupValues[2].lowercase()
        if (value <= 0L) {
            return null
        }
        return when (unit) {
            "s" -> if (value >= maxDurationMillis / 1000L) maxDurationMillis else value * 1000L
            "m" -> if (value >= maxDurationMillis / (60L * 1000L)) maxDurationMillis else value * 60L * 1000L
            "h" -> if (value >= maxDurationMillis / (60L * 60L * 1000L)) maxDurationMillis else value * 60L * 60L * 1000L
            else -> null
        }
    }

    private fun formatDurationLabel(raw: String): String {
        val match = durationRegex.matchEntire(raw.trim()) ?: return raw
        val value = match.groupValues[1]
        val unit = match.groupValues[2].lowercase()
        return when (unit) {
            "s" -> "${value}秒"
            "m" -> "${value}分"
            "h" -> "${value}時間"
            else -> raw
        }
    }

    private fun parseAlarmTime(raw: String): LocalTime? {
        val trimmed = raw.trim()
        if (!timeRegex.matches(trimmed)) {
            return null
        }
        val parts = trimmed.split(":")
        if (parts.size != 2) {
            return null
        }
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return runCatching { LocalTime.of(hour, minute) }.getOrNull()
    }

    private fun parseTitleAndForce(args: Array<out String>, startIndex: Int): Pair<String, Boolean>? {
        if (args.size <= startIndex) {
            return null
        }

        val titleTokens = args.drop(startIndex).toMutableList()
        var forceBar = false

        val lastToken = titleTokens.lastOrNull()
        val parsedBoolean = if (titleTokens.size >= 2) {
            lastToken?.let { parseBooleanLiteral(it) }
        } else {
            null
        }
        if (parsedBoolean != null) {
            forceBar = parsedBoolean
            titleTokens.removeAt(titleTokens.lastIndex)
        }

        if (titleTokens.isEmpty()) {
            return null
        }

        val title = titleTokens.joinToString(" ").trim().take(64)
        if (title.isEmpty()) {
            return null
        }
        return Pair(title, forceBar)
    }

    private fun parseBooleanLiteral(raw: String): Boolean? {
        return when (raw.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    private fun hasPermission(sender: CommandSender): Boolean {
        return sender.hasPermission("cc-system.clock.set") ||
            sender.hasPermission("cc-system.admin") ||
            sender.hasPermission("cc-system.*") ||
            sender.isOp
    }

    private fun formatRemaining(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!hasPermission(sender)) {
            return emptyList()
        }

        if (args.size == 1) {
            return listOf("timer", "alarm", "list", "cancel")
                .filter { it.startsWith(args[0], ignoreCase = true) }
        }

        val sub = args[0].lowercase()
        if (args.size == 2) {
            return when (sub) {
                "timer" -> listOf("10s", "5m", "30m", "1h").filter { it.startsWith(args[1], ignoreCase = true) }
                "alarm" -> listOf("07:00", "12:00", "21:30").filter { it.startsWith(args[1], ignoreCase = true) }
                "cancel" -> ClockManager.getActiveClocks().map { it.title }.distinct().filter {
                    it.startsWith(args[1], ignoreCase = true)
                }

                else -> emptyList()
            }
        }

        if (sub == "cancel" && args.size >= 3) {
            val input = args.drop(1).joinToString(" ").trim().lowercase()
            return ClockManager.getActiveClocks().map { it.title }.distinct().filter {
                it.lowercase().startsWith(input)
            }
        }

        if (sub == "timer" || sub == "alarm") {
            if (args.size >= 3 && parseBooleanLiteral(args.last()) == null) {
                return listOf("true", "false").filter { it.startsWith(args.last(), ignoreCase = true) }
            }
        }

        return emptyList()
    }
}
