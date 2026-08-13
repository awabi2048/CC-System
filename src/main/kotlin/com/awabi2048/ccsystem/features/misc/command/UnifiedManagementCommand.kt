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
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.DisplayParticleLimitType
import com.awabi2048.ccsystem.features.misc.displayparticle.DisplayParticleBookTestController
import com.awabi2048.ccsystem.features.misc.gesturegui.GestureGuiDemoController
import com.awabi2048.ccsystem.api.localization.generated.GestureGuiKeys

internal class UnifiedManagementCommand(
    private val displayParticleCountProvider: () -> Int,
    private val displayParticleBookTestController: DisplayParticleBookTestController,
    private val gestureGuiDemoController: GestureGuiDemoController,
) : CommandExecutor, TabCompleter {
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
            "particle" -> handleParticle(sender, args.drop(1))
            "debug" -> handleDebug(sender, args.drop(1))
            "gesture-gui" -> handleGestureGui(sender, args.drop(1))
            else -> {
                sender.sendMessage(message(sender, "management.usage"))
                true
            }
        }
    }

    private fun handleGestureGui(sender: CommandSender, args: List<String>): Boolean {
        if (!hasPermission(sender, "cc.command.gesture-gui")) {
            sender.sendMessage(message(sender, "management.no_permission"))
            return true
        }
        val player = sender as? Player
        val operation = args.firstOrNull()?.lowercase()
        if (player == null || operation != "demo") {
            sender.sendMessage(CCSystem.getAPI().getLocalized(sender as? Player, GestureGuiKeys.GESTURE_GUI_DEMO_USAGE))
            return true
        }
        if (args.getOrNull(1).equals("close", true)) {
            gestureGuiDemoController.close(player)
            player.sendMessage(CCSystem.getAPI().getLocalized(player, GestureGuiKeys.GESTURE_GUI_DEMO_CLOSED))
            return true
        }
        val count = args.getOrNull(1)?.toIntOrNull() ?: 1
        if (count !in 1..3) {
            player.sendMessage(CCSystem.getAPI().getLocalized(player, GestureGuiKeys.GESTURE_GUI_DEMO_USAGE))
            return true
        }
        gestureGuiDemoController.open(player, count)
        player.sendMessage(
            CCSystem.getAPI().getLocalized(player, GestureGuiKeys.GESTURE_GUI_DEMO_OPENED, mapOf("screens" to count))
        )
        return true
    }

    private fun handleDebug(sender: CommandSender, args: List<String>): Boolean {
        if (!hasPermission(sender, "cc.command.debug")) {
            sender.sendMessage(message(sender, "management.no_permission"))
            return true
        }
        val player = sender as? Player
        if (player == null || args.isEmpty()) {
            sender.sendMessage(message(sender, "management.debug.usage"))
            return true
        }
        when (args[0].lowercase()) {
            "toggle_particle_test" -> displayParticleBookTestController.toggle(player)
            "particle_test_guide" -> displayParticleBookTestController.showGuide(player, args.getOrNull(1))
            "apply_particle_book_fix" -> args.getOrNull(1)?.let { displayParticleBookTestController.applyPendingFix(player, it) }
                ?: sender.sendMessage(message(sender, "management.debug.usage"))
            else -> sender.sendMessage(message(sender, "management.debug.usage"))
        }
        return true
    }

    private fun handleParticle(sender: CommandSender, args: List<String>): Boolean {
        if (!hasPermission(sender, "cc.command.particle.limit")) {
            sender.sendMessage(message(sender, "management.no_permission"))
            return true
        }
        if (args.firstOrNull()?.lowercase() != "limit") {
            sender.sendMessage(message(sender, "management.particle.usage"))
            return true
        }
        if (args.size == 1) {
            val used = displayParticleCountProvider()
            sender.sendMessage(message(sender, "management.particle.status_header", mapOf("used" to used)))
            DisplayParticleLimitType.entries.forEach { type -> sender.sendMessage(limitStatus(sender, type)) }
            return true
        }
        val type = DisplayParticleLimitType.fromCommandName(args[1])
        if (type == null) {
            sender.sendMessage(message(sender, "management.particle.usage"))
            return true
        }
        if (args.size == 2) {
            sender.sendMessage(limitStatus(sender, type))
            return true
        }
        if (args.size != 4 || !args[2].equals("set", true)) {
            sender.sendMessage(message(sender, "management.particle.usage"))
            return true
        }
        val limit = args[3].toIntOrNull()
        if (limit == null || limit !in type.allowedRange) {
            sender.sendMessage(message(sender, "management.particle.invalid_limit", limitPlaceholders(sender, type)))
            return true
        }
        if (!ConfigManager.setDisplayParticleLimit(type, limit)) {
            sender.sendMessage(message(sender, "management.particle.save_failed"))
            return true
        }
        sender.sendMessage(message(sender, "management.particle.changed", limitPlaceholders(sender, type) + ("limit" to limit)))
        return true
    }

    private fun limitStatus(sender: CommandSender, type: DisplayParticleLimitType): String = message(
        sender,
        "management.particle.status_line",
        limitPlaceholders(sender, type) + ("limit" to ConfigManager.getDisplayParticleLimit(type))
    )

    private fun limitPlaceholders(sender: CommandSender, type: DisplayParticleLimitType): Map<String, Any> = mapOf(
        "type" to message(sender, "management.particle.type.${type.commandName}"),
        "minimum" to type.allowedRange.first,
        "maximum" to type.allowedRange.last
    )

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
            return filter(listOf("config", "debug", "gesture-gui", "give", "menu", "particle"), input)
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
            "particle" -> when (args.size) {
                2 -> filter(listOf("limit"), input)
                3 -> if (args.getOrNull(1).equals("limit", true)) filter(DisplayParticleLimitType.entries.map { it.commandName }, input) else emptyList()
                4 -> if (DisplayParticleLimitType.fromCommandName(args.getOrNull(2).orEmpty()) != null) filter(listOf("set"), input) else emptyList()
                5 -> if (args.getOrNull(3).equals("set", true)) {
                    filter(limitSuggestions(DisplayParticleLimitType.fromCommandName(args.getOrNull(2).orEmpty())), input)
                } else emptyList()
                else -> emptyList()
            }
            "debug" -> when (args.size) {
                2 -> filter(listOf("toggle_particle_test", "particle_test_guide"), input)
                3 -> if (args.getOrNull(1).equals("particle_test_guide", true)) {
                    filter(listOf("textures", "scale", "rotation", "lifetime", "motion", "collision", "emission"), input)
                } else emptyList()
                else -> emptyList()
            }
            "gesture-gui" -> when (args.size) {
                2 -> filter(listOf("demo"), input)
                3 -> filter(listOf("1", "2", "3", "close"), input)
                else -> emptyList()
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

    private fun limitSuggestions(type: DisplayParticleLimitType?): List<String> = when (type) {
        DisplayParticleLimitType.GLOBAL -> listOf("128", "256", "512", "1024")
        DisplayParticleLimitType.OWNER -> listOf("32", "64", "128", "256")
        DisplayParticleLimitType.PER_TICK -> listOf("16", "32", "64", "128")
        DisplayParticleLimitType.EMISSION -> listOf("1", "8", "16", "32")
        null -> emptyList()
    }

    private fun message(
        sender: CommandSender,
        key: String,
        placeholders: Map<String, Any> = emptyMap()
    ): String {
        return LanguageManager.getUnified().getString(sender as? Player, key, placeholders)
    }
}
