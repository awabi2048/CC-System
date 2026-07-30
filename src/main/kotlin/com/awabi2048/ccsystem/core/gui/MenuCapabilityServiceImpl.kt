package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuCapabilityContext
import com.awabi2048.ccsystem.api.gui.MenuCapabilityDefinition
import com.awabi2048.ccsystem.api.gui.MenuCapabilityService
import com.awabi2048.ccsystem.api.gui.ResolvedMenuCapability
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player

class MenuCapabilityServiceImpl : MenuCapabilityService {
    private val definitions = ConcurrentHashMap<String, MenuCapabilityDefinition>()

    override fun register(definition: MenuCapabilityDefinition) {
        val previous = definitions.putIfAbsent(definition.capabilityId, definition)
        require(previous == null) {
            "Duplicate menu capability: ${definition.capabilityId}"
        }
    }

    override fun unregisterOwner(owner: String) {
        definitions.entries.removeIf { it.value.owner == owner }
    }

    override fun definition(capabilityId: String): MenuCapabilityDefinition? =
        definitions[capabilityId]

    override fun definitions(): List<MenuCapabilityDefinition> =
        definitions.values.sortedBy(MenuCapabilityDefinition::capabilityId)

    override fun resolve(
        capabilityId: String,
        player: Player,
        arguments: Map<String, String>,
    ): ResolvedMenuCapability? {
        val definition = definitions[capabilityId] ?: return null
        val context = MenuCapabilityContext(player, arguments)
        if (!definition.availability.isAvailable(context)) return null
        return ResolvedMenuCapability(
            capabilityId = capabilityId,
            item = definition.itemRenderer.render(context).clone(),
            targetRoute = definition.routeResolver.resolve(context),
            acceptedClicks = definition.acceptedClicks,
        )
    }
}
