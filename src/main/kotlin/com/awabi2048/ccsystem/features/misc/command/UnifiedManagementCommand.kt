package com.awabi2048.ccsystem.features.misc.command

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.api.config.ConfigPreparationState
import com.awabi2048.ccsystem.api.gui.MenuTargetPolicy
import com.awabi2048.ccsystem.core.config.LanguageManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class UnifiedManagementCommand : CommandExecutor, TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(message(sender, "management.usage"))
            return true
        }
        return when (args[0].lowercase()) {
            "config" -> handleConfig(sender, args.drop(1))
            "give" -> handleGive(sender, args.drop(1))
            "menu" -> handleMenu(sender, args.drop(1))
            else -> {
                sender.sendMessage(message(sender, "management.usage"))
                true
            }
        }
    }

    private fun handleConfig(sender: CommandSender, args: List<String>): Boolean {
        if (!hasPermission(sender, "cc.command.config")) {
            sender.sendMessage(message(sender, "management.no_permission"))
            return true
        }
        val action = args.firstOrNull()?.lowercase() ?: "status"
        val owner = args.getOrNull(1)
        val service = CCSystem.getAPI().getConfigSchemaService()
        val statuses = when (action) {
            "status" -> service.status(owner)
            "check" -> service.check(owner).statuses
            "migrate" -> service.prepare(owner).statuses
            "reload" -> service.reload(owner).statuses
            else -> {
                sender.sendMessage(message(sender, "management.config.usage"))
                return true
            }
        }
        if (statuses.isEmpty()) {
            sender.sendMessage(message(sender, "management.config.none"))
            return true
        }
        statuses.forEach { status ->
            val color = when (status.state) {
                ConfigPreparationState.CURRENT -> "§a"
                ConfigPreparationState.UPDATED -> "§b"
                ConfigPreparationState.MISSING,
                ConfigPreparationState.OUTDATED,
                ConfigPreparationState.RESTART_REQUIRED -> "§e"
                ConfigPreparationState.FUTURE_VERSION,
                ConfigPreparationState.INVALID,
                ConfigPreparationState.FAILED -> "§c"
            }
            sender.sendMessage(
                message(
                    sender,
                    "management.config.line",
                    mapOf(
                        "color" to color,
                        "owner" to status.owner,
                        "file" to status.resourcePath,
                        "detected" to (status.detectedVersion?.toString() ?: "-"),
                        "required" to status.requiredVersion.toString(),
                        "state" to status.state.name,
                        "message" to (status.message ?: "")
                    )
                )
            )
        }
        return true
    }

    private fun handleGive(sender: CommandSender, args: List<String>): Boolean {
        if (!hasPermission(sender, "cc.command.give")) {
            sender.sendMessage(message(sender, "management.no_permission"))
            return true
        }
        if (args.size < 2) {
            sender.sendMessage(message(sender, "management.give.usage"))
            return true
        }
        val targets = resolveTargets(sender, args[0])
        if (targets == null || targets.isEmpty()) {
            sender.sendMessage(message(sender, "management.player_not_found", mapOf("player" to args[0])))
            return true
        }
        if (args[0].equals("@a", true) && !hasPermission(sender, "cc.command.give.all")) {
            sender.sendMessage(message(sender, "management.no_permission"))
            return true
        }
        val itemId = args[1]
        val explicitAmount = args.getOrNull(2)?.toIntOrNull()
        val amount = explicitAmount ?: 1
        val providerArguments = args.drop(if (explicitAmount == null) 2 else 3)
        val service = CCSystem.getAPI().getItemGrantService()
        targets.forEach { target ->
            val result = service.grant(sender, target, itemId, amount, providerArguments)
            val key = if (result.success) "management.give.success" else "management.give.failed"
            sender.sendMessage(
                message(
                    sender,
                    key,
                    mapOf(
                        "player" to target.name,
                        "item" to itemId,
                        "amount" to amount.toString(),
                        "granted" to result.grantedAmount.toString(),
                        "dropped" to result.droppedAmount.toString(),
                        "reason" to (result.message ?: "")
                    )
                )
            )
        }
        return true
    }

    private fun handleMenu(sender: CommandSender, args: List<String>): Boolean {
        if (!hasPermission(sender, "cc.command.menu")) {
            sender.sendMessage(message(sender, "management.no_permission"))
            return true
        }
        val routeId = args.firstOrNull()
        if (routeId == null) {
            sender.sendMessage(message(sender, "management.menu.usage"))
            return true
        }
        val definition = CCSystem.getAPI().getMenuCommandService().definition(routeId)
        if (definition == null) {
            sender.sendMessage(message(sender, "management.menu.unknown", mapOf("menu" to routeId)))
            return true
        }
        val possibleTarget = args.getOrNull(1)
        val explicitTarget = possibleTarget?.takeUnless { '=' in it }
        val target = when {
            explicitTarget == null && sender is Player -> sender
            explicitTarget == "@s" && sender is Player -> sender
            explicitTarget != null -> Bukkit.getPlayerExact(explicitTarget)
            else -> null
        }
        if (target == null) {
            sender.sendMessage(message(sender, "management.menu.target_required"))
            return true
        }
        if (definition.targetPolicy == MenuTargetPolicy.SELF_ONLY && sender !== target) {
            sender.sendMessage(message(sender, "management.menu.self_only"))
            return true
        }
        val argumentStart = if (explicitTarget == null) 1 else 2
        val values = LinkedHashMap<String, String>()
        for (argument in args.drop(argumentStart)) {
            val split = argument.split('=', limit = 2)
            if (split.size != 2 || split[0].isBlank()) {
                sender.sendMessage(message(sender, "management.menu.invalid_argument", mapOf("argument" to argument)))
                return true
            }
            values[split[0]] = split[1]
        }
        val opened = CCSystem.getAPI().getMenuCommandService().open(sender, target, routeId, values)
        sender.sendMessage(
            message(
                sender,
                if (opened) "management.menu.success" else "management.menu.failed",
                mapOf("menu" to routeId, "player" to target.name)
            )
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        val input = args.lastOrNull().orEmpty()
        if (args.size == 1) {
            return filter(listOf("config", "give", "menu"), input)
        }
        return when (args[0].lowercase()) {
            "config" -> when (args.size) {
                2 -> filter(listOf("status", "check", "migrate", "reload"), input)
                3 -> filter(CCSystem.getAPI().getConfigSchemaService().status().map { it.owner }.distinct(), input)
                else -> emptyList()
            }
            "give" -> when (args.size) {
                2 -> filter(listOf("@s", "@a") + Bukkit.getOnlinePlayers().map(Player::getName), input)
                3 -> filter(CCSystem.getAPI().getItemGrantService().definitions().map { it.id }, input)
                else -> {
                    val definition = args.getOrNull(2)?.let(CCSystem.getAPI().getItemGrantService()::definition)
                    definition?.argumentSuggestions?.suggest(args.drop(4))?.let { filter(it, input) }.orEmpty()
                }
            }
            "menu" -> when (args.size) {
                2 -> filter(CCSystem.getAPI().getMenuCommandService().definitions().map { it.routeId }, input)
                3 -> filter(listOf("@s") + Bukkit.getOnlinePlayers().map(Player::getName), input)
                else -> {
                    val definition = args.getOrNull(1)?.let(CCSystem.getAPI().getMenuCommandService()::definition)
                    filter(definition?.argumentKeys?.map { "$it=" }.orEmpty(), input)
                }
            }
            else -> emptyList()
        }
    }

    private fun resolveTargets(sender: CommandSender, raw: String): List<Player>? = when {
        raw.equals("@s", true) -> (sender as? Player)?.let(::listOf)
        raw.equals("@a", true) -> Bukkit.getOnlinePlayers().toList()
        else -> Bukkit.getPlayerExact(raw)?.let(::listOf)
    }

    private fun hasPermission(sender: CommandSender, permission: String): Boolean =
        sender.isOp || sender.hasPermission(permission) || sender.hasPermission("cc.command.*")

    private fun filter(values: Collection<String>, input: String): List<String> =
        values.filter { it.startsWith(input, ignoreCase = true) }.sorted()

    private fun message(
        sender: CommandSender,
        key: String,
        placeholders: Map<String, Any> = emptyMap()
    ): String {
        return LanguageManager.getUnified().getString(sender as? Player, key, placeholders)
    }
}
