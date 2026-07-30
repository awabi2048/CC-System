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

data class MenuCapabilityActionContext(
    val player: Player,
    val click: ClickType,
    val arguments: Map<String, String> = emptyMap(),
)

fun interface MenuCapabilityActionHandler {
    fun handle(context: MenuCapabilityActionContext): MenuActionResult
}

data class MenuCapabilityDefinition(
    val owner: String,
    val id: String,
    val placement: String,
    val availability: MenuCapabilityAvailability,
    val itemRenderer: MenuCapabilityItemRenderer,
    val actionAvailability: MenuCapabilityAvailability =
        MenuCapabilityAvailability { true },
    val actionHandler: MenuCapabilityActionHandler? = null,
    val acceptedClicks: Set<ClickType> = MenuAcceptedClicks.LEFT,
) {
    val capabilityId: String
        get() = "$owner:$id"

    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        require(placement.isNotBlank()) { "placement must not be blank" }
        require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
    }
}

data class ResolvedMenuCapability(
    val capabilityId: String,
    val item: ItemStack,
    val actionable: Boolean,
    val acceptedClicks: Set<ClickType>,
)

interface MenuCapabilityService {
    fun register(definition: MenuCapabilityDefinition)

    fun unregisterOwner(owner: String)

    fun definition(capabilityId: String): MenuCapabilityDefinition?

    fun definitions(): List<MenuCapabilityDefinition>

    fun definitions(placement: String): List<MenuCapabilityDefinition>

    fun resolve(
        capabilityId: String,
        player: Player,
        arguments: Map<String, String> = emptyMap(),
    ): ResolvedMenuCapability?

    fun execute(
        capabilityId: String,
        player: Player,
        click: ClickType,
        arguments: Map<String, String> = emptyMap(),
    ): MenuActionResult
}
