package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuCapabilityContext
import com.awabi2048.ccsystem.api.gui.MenuCapabilityActionContext
import com.awabi2048.ccsystem.api.gui.MenuCapabilityDefinition
import com.awabi2048.ccsystem.api.gui.MenuCapabilityService
import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.ResolvedMenuCapabilityAction
import com.awabi2048.ccsystem.api.gui.ResolvedMenuCapability
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
        val actions = definition.actions.mapNotNull { action ->
            if (!action.availability.isAvailable(context)) return@mapNotNull null
            val loreLines = action.presentationProvider.resolve(context)
            require(loreLines.isNotEmpty()) {
                "Capability action presentation must not be empty: $capabilityId:${action.id}"
            }
            ResolvedMenuCapabilityAction(action.id, action.clicks, loreLines)
        }
        return ResolvedMenuCapability(
            capabilityId = capabilityId,
            presentation = withActionLore(
                definition.presentationProvider.resolve(context),
                actions.flatMap(ResolvedMenuCapabilityAction::loreLines),
            ),
            actions = actions,
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
            click in it.clicks && it.availability.isAvailable(context)
        } ?: return MenuActionResult.Ignored
        return action.handler.handle(MenuCapabilityActionContext(player, click, arguments, attributes))
    }

    private fun withActionLore(
        presentation: com.awabi2048.ccsystem.api.gui.MenuCapabilityPresentation,
        actionLines: List<GuiLoreLine>,
    ): com.awabi2048.ccsystem.api.gui.MenuCapabilityPresentation {
        if (actionLines.isEmpty()) return presentation
        val actionBlock = GuiLoreBlock(actionLines)
        val item = presentation.item
        val lore = when (val current = item.lore) {
            GuiLoreSpec.None -> GuiLoreSpec.Blocks(listOf(actionBlock))
            is GuiLoreSpec.Blocks -> GuiLoreSpec.Blocks(current.blocks + actionBlock)
            is GuiLoreSpec.Rich -> GuiLoreSpec.Rich(
                current.lines + GuiLoreLine.Separator + actionLines,
                current.frame,
            )
        }
        return presentation.copy(
            item = item.copy(lore = lore),
            embeddedLoreBlocks = if (presentation.embeddedLoreBlocks.isEmpty()) {
                presentation.embeddedLoreBlocks
            } else {
                presentation.embeddedLoreBlocks + actionBlock
            },
        )
    }
}
