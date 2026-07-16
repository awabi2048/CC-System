package com.awabi2048.ccsystem.features.lwcx.command

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.features.lwcx.service.LwcExpansionService
import com.awabi2048.ccsystem.features.lwcx.service.LwcStatusReport
import com.awabi2048.ccsystem.features.lwcx.service.LwcWorldSummary
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LwcExpansionCommand {
    private val service by lazy { LwcExpansionService(CCSystem.instance) }
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player
        when (args.firstOrNull()?.lowercase()) {
            "world_cleanup" -> cleanup(sender, player, args.drop(1))
            "remained_info_lookup" -> lookup(sender, player, args.drop(1))
            "status" -> status(sender, player, args.drop(1))
            else -> sender.sendMessage(message(player, "lwc_expansion.usage"))
        }
        return true
    }

    private fun cleanup(sender: CommandSender, player: Player?, args: List<String>) {
        val worldName = args.getOrNull(0)?.takeIf { it.isNotBlank() }
        if (worldName == null || !args.getOrNull(1).equals("confirm", ignoreCase = true)) {
            sender.sendMessage(message(player, "lwc_expansion.cleanup.usage"))
            return
        }
        if (!service.isAvailable()) {
            sender.sendMessage(message(player, "lwc_expansion.unavailable"))
            return
        }
        sender.sendMessage(message(player, "lwc_expansion.cleanup.started", "world" to worldName))
        service.cleanupWorld(worldName) { count, error ->
            if (error != null) {
                sender.sendMessage(message(player, "lwc_expansion.cleanup.failed", "reason" to error))
            } else {
                sender.sendMessage(message(player, "lwc_expansion.cleanup.complete", "world" to worldName, "count" to count.toString()))
            }
        }
    }

    private fun lookup(sender: CommandSender, player: Player?, args: List<String>) {
        if (args.isNotEmpty()) {
            sender.sendMessage(message(player, "lwc_expansion.usage"))
            return
        }
        if (!service.isAvailable()) {
            sender.sendMessage(message(player, "lwc_expansion.unavailable"))
            return
        }
        service.loadRemainedInfo { worlds, error ->
            if (error != null) {
                sender.sendMessage(message(player, "lwc_expansion.lookup.failed", "reason" to error))
                return@loadRemainedInfo
            }
            sender.sendMessage(message(player, "lwc_expansion.lookup.header"))
            if (worlds.isNullOrEmpty()) {
                sender.sendMessage(message(player, "lwc_expansion.lookup.empty"))
                return@loadRemainedInfo
            }
            worlds.forEach { summary ->
                sender.sendMessage(summaryLine(player, "lwc_expansion.lookup.line", summary, includeWorld = true))
            }
        }
    }

    private fun status(sender: CommandSender, player: Player?, args: List<String>) {
        if (args.isNotEmpty()) {
            sender.sendMessage(message(player, "lwc_expansion.usage"))
            return
        }
        if (!service.isAvailable()) {
            sender.sendMessage(message(player, "lwc_expansion.unavailable"))
            return
        }
        service.loadStatus { report, error ->
            if (error != null) {
                sender.sendMessage(message(player, "lwc_expansion.status.failed", "reason" to error))
                return@loadStatus
            }
            report?.let { renderStatus(sender, player, it) }
        }
    }

    private fun renderStatus(sender: CommandSender, player: Player?, report: LwcStatusReport) {
        sender.sendMessage(message(player, "lwc_expansion.status.header"))
        sender.sendMessage(summaryLine(player, "lwc_expansion.status.existing", report.existing, includeWorld = false))
        sender.sendMessage(summaryLine(player, "lwc_expansion.status.missing", report.missing, includeWorld = false))
        sender.sendMessage(
            message(
                player,
                "lwc_expansion.status.delta",
                "total" to formatDelta(report.totalDelta),
                "existing" to formatDelta(report.existingDelta),
                "missing" to formatDelta(report.missingDelta)
            )
        )
        sender.sendMessage(message(player, "lwc_expansion.status.top_header"))
        if (report.topMyWorlds.isEmpty()) {
            sender.sendMessage(message(player, "lwc_expansion.status.top_empty"))
        } else {
            report.topMyWorlds.forEach { summary ->
                sender.sendMessage(summaryLine(player, "lwc_expansion.status.top_line", summary, includeWorld = true))
            }
        }
    }

    private fun summaryLine(player: Player?, key: String, summary: LwcWorldSummary, includeWorld: Boolean): Component {
        val base = message(
            player,
            key,
            "world" to summary.world,
            "count" to summary.count.toString()
        )
        val hoverLines = mutableListOf<Component>()
        summary.typeCounts.forEach { (type, count) ->
            hoverLines += message(player, "lwc_expansion.hover.type", "type" to type, "count" to count.toString())
        }
        if (includeWorld) {
            summary.latestCreation?.let { hoverLines += message(player, "lwc_expansion.hover.created", "value" to dateFormatter.format(it)) }
            if (summary.latestChangedAt > 0L) {
                hoverLines += message(player, "lwc_expansion.hover.changed", "value" to dateFormatter.format(Instant.ofEpochMilli(summary.latestChangedAt)))
            }
        }
        return base.hoverEvent(HoverEvent.showText(joinLines(hoverLines)))
    }

    private fun joinLines(lines: List<Component>): Component {
        var result: Component = Component.empty()
        lines.forEachIndexed { index, line ->
            result = if (index == 0) line else result.append(Component.newline()).append(line)
        }
        return result
    }

    private fun formatDelta(value: Int?): String = value?.let { if (it > 0) "+$it" else it.toString() } ?: "-"

    private fun message(player: Player?, key: String, vararg placeholders: Pair<String, String>): Component {
        return LanguageManager.getMessageWithoutPrefix(player, key, *placeholders)
    }
}
