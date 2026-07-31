package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuCapabilityContext
import com.awabi2048.ccsystem.api.gui.MenuCapabilityActionContext
import com.awabi2048.ccsystem.api.gui.MenuCapabilityDefinition
import com.awabi2048.ccsystem.api.gui.MenuCapabilityService
import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.ResolvedMenuCapabilityAction
import com.awabi2048.ccsystem.api.gui.ResolvedMenuCapability
import com.awabi2048.ccsystem.api.gui.MenuSoundPolicy
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

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

    override fun definitions(placement: String): List<MenuCapabilityDefinition> =
        definitions.values
            .filter { it.placement == placement }
            .sortedBy(MenuCapabilityDefinition::capabilityId)

    override fun resolve(
        capabilityId: String,
        player: Player,
        arguments: Map<String, String>,
        attributes: Map<String, Any>,
    ): ResolvedMenuCapability? {
        val definition = definitions[capabilityId] ?: return null
        val context = MenuCapabilityContext(player, arguments, attributes)
        if (!definition.availability.isAvailable(context)) return null
        val resolvedActions = definition.actions.mapNotNull { action ->
            if (!action.availability.isAvailable(context)) return@mapNotNull null
            val text = action.textProvider.resolve(context)
                .replace(LEGACY_FORMATTING, "")
            require(text.isNotBlank()) {
                "Capability action text must not be blank: $capabilityId:${action.id}"
            }
            ResolvedMenuCapabilityAction(action.id, action.trigger, text)
        }
        return ResolvedMenuCapability(
            capabilityId = capabilityId,
            presentation = definition.presentationProvider.resolve(context),
            actions = resolvedActions,
        )
    }

    override fun execute(
        capabilityId: String,
        player: Player,
        click: ClickType,
        arguments: Map<String, String>,
        attributes: Map<String, Any>,
    ): MenuActionResult {
        val definition = definitions[capabilityId] ?: return MenuActionResult.Ignored
        val context = MenuCapabilityContext(player, arguments, attributes)
        if (!definition.availability.isAvailable(context)) return MenuActionResult.Ignored
        val action = definition.actions.firstOrNull {
            click in it.trigger.clicks && it.availability.isAvailable(context)
        } ?: return MenuActionResult.Ignored
        return applyActionSounds(
            action.handler.handle(MenuCapabilityActionContext(player, click, arguments, attributes)),
            action.sounds,
        )
    }

    private fun applyActionSounds(
        result: MenuActionResult,
        sounds: com.awabi2048.ccsystem.api.gui.MenuActionSoundPolicy,
    ): MenuActionResult = when (result) {
        MenuActionResult.Ignored -> result
        is MenuActionResult.Success ->
            if (result.sound == MenuSoundPolicy.Default) result.copy(sound = sounds.success) else result
        is MenuActionResult.Rejected ->
            if (result.sound == MenuSoundPolicy.Default) result.copy(sound = sounds.rejected) else result
    }

    private companion object {
        val LEGACY_FORMATTING = Regex("(?i)[§&][0-9A-FK-ORX]")
    }
}
