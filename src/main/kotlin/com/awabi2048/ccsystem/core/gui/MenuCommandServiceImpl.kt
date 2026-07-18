package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuCommandService
import com.awabi2048.ccsystem.api.gui.MenuTargetPolicy
import com.awabi2048.ccsystem.api.gui.PublicMenuDefinition
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class MenuCommandServiceImpl : MenuCommandService {
    private val definitions = ConcurrentHashMap<String, PublicMenuDefinition>()

    override fun register(definition: PublicMenuDefinition) {
        require(definition.owner.matches(Regex("[a-z0-9_.-]+"))) { "Invalid menu owner: ${definition.owner}" }
        require(definition.id.matches(Regex("[a-z0-9_.-]+"))) { "Invalid menu id: ${definition.id}" }
        val previous = definitions.putIfAbsent(definition.routeId, definition)
        require(previous == null) { "Duplicate public menu route: ${definition.routeId}" }
    }

    override fun unregisterOwner(owner: String) {
        definitions.entries.removeIf { it.value.owner == owner }
    }

    override fun definitions(): List<PublicMenuDefinition> =
        definitions.values.sortedBy(PublicMenuDefinition::routeId)

    override fun definition(routeId: String): PublicMenuDefinition? = definitions[routeId]

    override fun open(
        sender: CommandSender,
        target: Player,
        routeId: String,
        arguments: Map<String, String>
    ): Boolean {
        val definition = definitions[routeId] ?: return false
        val permission = definition.permission
        if (permission != null && !sender.hasPermission(permission) && !sender.isOp) {
            return false
        }
        if (arguments.keys.any { it !in definition.argumentKeys }) {
            return false
        }
        if (definition.targetPolicy == MenuTargetPolicy.SELF_ONLY && sender !== target) {
            return false
        }
        if (sender !is Player && definition.targetPolicy != MenuTargetPolicy.CONSOLE_AND_PLAYER_TARGET) {
            return false
        }
        return runCatching { definition.opener.open(target, arguments) }.getOrDefault(false)
    }
}
