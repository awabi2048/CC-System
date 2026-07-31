package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import java.util.UUID

data class MenuCapabilityContext(
    val player: Player,
    val arguments: Map<String, String> = emptyMap(),
    val attributes: Map<String, Any> = emptyMap(),
)

fun interface MenuCapabilityAvailability {
    fun isAvailable(context: MenuCapabilityContext): Boolean
}

data class MenuCapabilityPresentation(
    val item: GuiItemSpec,
    val glint: Boolean? = null,
    val playerHeadOwner: UUID? = null,
    val embeddedLoreBlocks: List<GuiLoreBlock> = emptyList(),
)

fun interface MenuCapabilityPresentationProvider {
    fun resolve(context: MenuCapabilityContext): MenuCapabilityPresentation
}

data class MenuCapabilityActionContext(
    val player: Player,
    val click: ClickType,
    val arguments: Map<String, String> = emptyMap(),
    val attributes: Map<String, Any> = emptyMap(),
)

fun interface MenuCapabilityActionHandler {
    fun handle(context: MenuCapabilityActionContext): MenuActionResult
}

fun interface MenuCapabilityActionTextProvider {
    fun resolve(context: MenuCapabilityContext): String
}

enum class MenuCapabilityTrigger(
    val clicks: Set<ClickType>,
) {
    LEFT(setOf(ClickType.LEFT)),
    RIGHT(setOf(ClickType.RIGHT)),
    SHIFT_LEFT(setOf(ClickType.SHIFT_LEFT)),
    SHIFT_RIGHT(setOf(ClickType.SHIFT_RIGHT)),
    LEFT_RIGHT(setOf(ClickType.LEFT, ClickType.RIGHT)),
    ANY(MenuAcceptedClicks.STANDARD),
}

data class MenuCapabilityAction(
    val id: String,
    val trigger: MenuCapabilityTrigger,
    val textProvider: MenuCapabilityActionTextProvider,
    val availability: MenuCapabilityAvailability =
        MenuCapabilityAvailability { true },
    val sounds: MenuActionSoundPolicy = MenuActionSoundPolicy(),
    val handler: MenuCapabilityActionHandler,
) {
    init {
        require(id.isNotBlank()) { "action id must not be blank" }
    }
}

data class MenuCapabilityDefinition(
    val owner: String,
    val id: String,
    val placement: String,
    val availability: MenuCapabilityAvailability,
    val presentationProvider: MenuCapabilityPresentationProvider,
    val actions: List<MenuCapabilityAction>,
) {
    val capabilityId: String
        get() = "$owner:$id"

    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        require(placement.isNotBlank()) { "placement must not be blank" }
        require(actions.map(MenuCapabilityAction::id).distinct().size == actions.size) {
            "capability action ids must be unique"
        }
        val duplicateClicks = actions
            .flatMap { it.trigger.clicks }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateClicks.isEmpty()) {
            "capability actions must not accept the same click: $duplicateClicks"
        }
    }
}

data class ResolvedMenuCapabilityAction(
    val id: String,
    val trigger: MenuCapabilityTrigger,
    val text: String,
)

data class ResolvedMenuCapability(
    val capabilityId: String,
    val presentation: MenuCapabilityPresentation,
    val actions: List<ResolvedMenuCapabilityAction>,
) {
    val actionable: Boolean
        get() = actions.isNotEmpty()

    val acceptedClicks: Set<ClickType>
        get() = actions.flatMapTo(linkedSetOf()) { it.trigger.clicks }
}

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
        attributes: Map<String, Any> = emptyMap(),
    ): ResolvedMenuCapability?

    fun execute(
        capabilityId: String,
        player: Player,
        click: ClickType,
        arguments: Map<String, String> = emptyMap(),
        attributes: Map<String, Any> = emptyMap(),
    ): MenuActionResult
}
