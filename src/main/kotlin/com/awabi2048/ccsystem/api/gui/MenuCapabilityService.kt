package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

data class MenuCapabilityContext(
    val player: Player,
    val arguments: Map<String, String> = emptyMap(),
)

fun interface MenuCapabilityAvailability {
    fun isAvailable(context: MenuCapabilityContext): Boolean
}

fun interface MenuCapabilityItemRenderer {
    fun render(context: MenuCapabilityContext): ItemStack
}

fun interface MenuCapabilityRouteResolver {
    fun resolve(context: MenuCapabilityContext): MenuRoute?
}

data class MenuCapabilityDefinition(
    val owner: String,
    val id: String,
    val availability: MenuCapabilityAvailability,
    val itemRenderer: MenuCapabilityItemRenderer,
    val routeResolver: MenuCapabilityRouteResolver,
    val acceptedClicks: Set<ClickType> = MenuAcceptedClicks.LEFT,
) {
    val capabilityId: String
        get() = "$owner:$id"

    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
    }
}

data class ResolvedMenuCapability(
    val capabilityId: String,
    val item: ItemStack,
    val targetRoute: MenuRoute?,
    val acceptedClicks: Set<ClickType>,
)

interface MenuCapabilityService {
    fun register(definition: MenuCapabilityDefinition)

    fun unregisterOwner(owner: String)

    fun definition(capabilityId: String): MenuCapabilityDefinition?

    fun definitions(): List<MenuCapabilityDefinition>

    fun resolve(
        capabilityId: String,
        player: Player,
        arguments: Map<String, String> = emptyMap(),
    ): ResolvedMenuCapability?
}
