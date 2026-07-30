package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementService
import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiFrameSection
import com.awabi2048.ccsystem.api.gui.GuiFrameSpec
import com.awabi2048.ccsystem.api.gui.GuiItemSpec
import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiMenuEntryAction
import com.awabi2048.ccsystem.api.gui.GuiMenuEntrySpec
import com.awabi2048.ccsystem.api.gui.GuiMenuDisplaySpec
import com.awabi2048.ccsystem.api.gui.GuiMenuCapabilitySpec
import com.awabi2048.ccsystem.api.gui.GuiStructuredMenuEntrySpec
import com.awabi2048.ccsystem.api.gui.MenuActionBranch
import com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks
import com.awabi2048.ccsystem.api.gui.MenuElement
import com.awabi2048.ccsystem.api.gui.MenuInteraction
import com.awabi2048.ccsystem.api.gui.MenuCapabilityPresentation
import com.awabi2048.ccsystem.api.gui.ResolvedMenuCapability
import com.awabi2048.ccsystem.api.gui.GuiNameStyle
import com.awabi2048.ccsystem.api.gui.GuiNameSpec
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Inventory
import org.bukkit.Bukkit
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

class GuiElementServiceImpl(
    private val i18n: ((Player?, String, Map<String, Any>) -> String)? = null,
) : GuiElementService {
    private val loreService = LoreServiceImpl(i18n)
    private val legacy = LegacyComponentSerializer.legacySection()
    private val colorCodePattern = Regex("(?i)[\u00A7&][0-9A-FK-ORX]")

    override fun title(name: GuiNameSpec): Component {
        return renderName(name)
    }

    override fun name(raw: String, style: GuiNameStyle): Component {
        val normalized = raw.trim()
        val styled = if (colorCodePattern.containsMatchIn(normalized)) normalized else style.colorCode + normalized
        return normalizeComponent(legacy.deserialize(styled))
    }

    override fun lore(spec: GuiLoreSpec): List<Component> {
        return loreService.render(spec)
    }

    override fun item(spec: GuiItemSpec): ItemStack {
        val item = ItemStack(spec.material, spec.amount.coerceIn(1, spec.material.maxStackSize.coerceAtLeast(1)))
        val meta = item.itemMeta ?: return item
        meta.displayName(renderName(spec.name))
        val nameOnlyRole = spec.role in setOf(
            GuiElementRole.BACK,
            GuiElementRole.CONFIRM,
            GuiElementRole.CANCEL,
            GuiElementRole.NAVIGATION,
        )
        val lore = if (nameOnlyRole) emptyList() else lore(spec.lore)
        if (lore.isNotEmpty()) {
            meta.lore(lore)
        }
        // GUIアイコンでは素材固有の説明を表示せず、NameとLoreだけを情報源にする。
        meta.addItemFlags(
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        )
        meta.isHideTooltip = spec.role == GuiElementRole.DECORATION
        GuiItemMarker.mark(meta, spec.role)
        item.itemMeta = meta
        return item
    }

    override fun mark(item: ItemStack, role: GuiElementRole): ItemStack {
        item.editMeta { meta -> GuiItemMarker.mark(meta, role) }
        return item
    }

    override fun menuEntry(player: Player?, spec: GuiMenuEntrySpec): MenuElement {
        val enabledActions = spec.actions.filter { it.enabled }
        val icon = item(
            GuiItemSpec(
                material = spec.material,
                name = spec.name,
                lore = GuiMenuEntryLoreFactory.build(
                    spec,
                    enabledActions,
                    player,
                ),
                role = spec.role,
                amount = spec.amount,
            ),
        ).also { item ->
            spec.glint?.let { enabled ->
                item.editMeta { meta -> meta.setEnchantmentGlintOverride(enabled) }
            }
            spec.playerHeadOwner?.let { owner ->
                val meta = item.itemMeta as? SkullMeta
                    ?: error("playerHeadOwner requires a player head material")
                meta.owningPlayer = Bukkit.getOfflinePlayer(owner)
                item.itemMeta = meta
            }
        }
        val interaction = when {
            enabledActions.isEmpty() -> MenuInteraction.DisplayOnly
            enabledActions.size == 1 -> enabledActions.single().let { action ->
                MenuInteraction.Action(
                    actionId = action.actionId,
                    acceptedClicks = action.acceptedClicks,
                    payload = action.payload,
                    sounds = spec.sounds,
                )
            }
            else -> MenuInteraction.Branches(
                branches = enabledActions.map { action ->
                    MenuActionBranch(action.actionId, action.acceptedClicks, action.payload)
                },
                sounds = spec.sounds,
            )
        }
        return MenuElement(spec.slot, icon, spec.role, interaction = interaction)
    }

    override fun menuDisplay(spec: GuiMenuDisplaySpec): MenuElement {
        val icon = item(spec.item).also { item ->
            spec.glint?.let { enabled ->
                item.editMeta { meta -> meta.setEnchantmentGlintOverride(enabled) }
            }
            spec.playerHeadOwner?.let { owner ->
                val meta = item.itemMeta as? SkullMeta
                    ?: error("playerHeadOwner requires a player head material")
                meta.owningPlayer = Bukkit.getOfflinePlayer(owner)
                item.itemMeta = meta
            }
        }
        return MenuElement(
            slot = spec.slot,
            item = icon,
            role = spec.item.role,
            interaction = MenuInteraction.DisplayOnly,
        )
    }

    override fun menuStructuredEntry(player: Player?, spec: GuiStructuredMenuEntrySpec): MenuElement {
        val enabledActions = spec.actions.filter(GuiMenuEntryAction::enabled)
        val actionLines = GuiMenuEntryLoreFactory.actionLines(enabledActions, player)
        val icon = item(
            spec.item.copy(lore = appendActionLore(spec.item.lore, actionLines)),
        ).also { item ->
            spec.glint?.let { enabled -> item.editMeta { meta -> meta.setEnchantmentGlintOverride(enabled) } }
            spec.playerHeadOwner?.let { owner ->
                val meta = item.itemMeta as? SkullMeta
                    ?: error("playerHeadOwner requires a player head material")
                meta.owningPlayer = Bukkit.getOfflinePlayer(owner)
                item.itemMeta = meta
            }
        }
        val interaction = when {
            enabledActions.isEmpty() -> MenuInteraction.DisplayOnly
            enabledActions.size == 1 -> enabledActions.single().let { action ->
                MenuInteraction.Action(
                    action.actionId,
                    action.acceptedClicks,
                    action.payload,
                    spec.sounds,
                )
            }
            else -> MenuInteraction.Branches(
                enabledActions.map { action ->
                    MenuActionBranch(action.actionId, action.acceptedClicks, action.payload)
                },
                spec.sounds,
            )
        }
        return MenuElement(spec.slot, icon, spec.item.role, interaction = interaction)
    }

    override fun menuCapability(player: Player?, capability: ResolvedMenuCapability): ItemStack {
        val presentation = capability.presentation
        val actions = capability.actions.map { action ->
            GuiMenuEntryAction(
                actionId = action.id,
                acceptedClicks = action.trigger.clicks,
                label = action.text,
            )
        }
        val actionLines = GuiMenuEntryLoreFactory.actionLines(
            actions,
            player,
        )
        val itemSpec = presentation.item.copy(
            lore = appendActionLore(presentation.item.lore, actionLines),
        )
        val item = item(itemSpec)
        presentation.glint?.let { enabled ->
            item.editMeta { meta -> meta.setEnchantmentGlintOverride(enabled) }
        }
        presentation.playerHeadOwner?.let { owner ->
            val meta = item.itemMeta as? SkullMeta ?: return@let
            meta.owningPlayer = Bukkit.getOfflinePlayer(owner)
            item.itemMeta = meta
        }
        return item
    }

    override fun menuCapabilityEntry(player: Player?, spec: GuiMenuCapabilitySpec): MenuElement {
        val capability = spec.capability
        val item = menuCapability(player, capability)
        val acceptedClicks = capability.acceptedClicks
        return if (capability.actionable && acceptedClicks.isNotEmpty()) {
            MenuElement(
                slot = spec.slot,
                item = item,
                role = GuiElementRole.ACTION,
                interaction = MenuInteraction.Action(
                    actionId = spec.actionId,
                    acceptedClicks = acceptedClicks,
                    payload = spec.actionPayload,
                ),
            )
        } else {
            MenuElement(
                slot = spec.slot,
                item = item,
                role = GuiElementRole.CONTENT,
                interaction = MenuInteraction.DisplayOnly,
            )
        }
    }

    private fun appendActionLore(base: GuiLoreSpec, actionLines: List<GuiLoreLine>): GuiLoreSpec {
        if (actionLines.isEmpty() || base == GuiLoreSpec.NameOnly) return base
        val actionBlock = GuiLoreBlock(actionLines)
        return when (base) {
            GuiLoreSpec.None -> GuiLoreSpec.Blocks(listOf(actionBlock))
            GuiLoreSpec.NameOnly -> GuiLoreSpec.NameOnly
            is GuiLoreSpec.Blocks -> GuiLoreSpec.Blocks(base.blocks + actionBlock)
            is GuiLoreSpec.Rich -> GuiLoreSpec.Rich(
                base.lines + GuiLoreLine.Spacer + actionLines,
                base.frame,
            )
        }
    }

    override fun applyFrame(inventory: Inventory, spec: GuiFrameSpec) {
        applySection(inventory, spec.header, 0 until minOf(9, inventory.size))
        val footerStart = (inventory.size - 9).coerceAtLeast(0)
        applySection(inventory, spec.footer, footerStart until inventory.size)
        spec.emptySlot?.let { fillEmpty(inventory, it) }
    }

    override fun fillEmpty(inventory: Inventory, element: GuiItemSpec) {
        for (slot in 0 until inventory.size) {
            if (inventory.getItem(slot) == null) inventory.setItem(slot, item(element))
        }
    }

    private fun applySection(inventory: Inventory, section: GuiFrameSection, rowSlots: IntRange) {
        val slots = when (section) {
            GuiFrameSection.None -> return
            is GuiFrameSection.Row -> rowSlots
            is GuiFrameSection.Slots -> section.slots
        }
        val element = when (section) {
            is GuiFrameSection.Row -> section.element
            is GuiFrameSection.Slots -> section.element
            GuiFrameSection.None -> return
        }
        slots.filter { it in 0 until inventory.size }.forEach { inventory.setItem(it, item(element)) }
    }

    override fun decoration(material: Material): ItemStack {
        return item(
            GuiItemSpec(
                material = material,
                name = GuiNameSpec.Empty,
                lore = GuiLoreSpec.None,
                role = GuiElementRole.DECORATION,
                amount = 1
            )
        )
    }

    override fun backItem(name: String, material: Material): ItemStack {
        return item(
            GuiItemSpec(
                material = material,
                name = GuiNameSpec.Text(name, GuiNameStyle.MUTED),
                lore = GuiLoreSpec.None,
                role = GuiElementRole.BACK,
                amount = 1
            )
        )
    }

    override fun confirmItem(name: String, confirm: Boolean): ItemStack {
        return item(
            GuiItemSpec(
                material = if (confirm) Material.LIME_CONCRETE else Material.RED_CONCRETE,
                name = GuiNameSpec.Text(name, if (confirm) GuiNameStyle.SUCCESS else GuiNameStyle.DANGER),
                lore = GuiLoreSpec.None,
                role = if (confirm) GuiElementRole.CONFIRM else GuiElementRole.CANCEL,
                amount = 1
            )
        )
    }

    private fun normalizeComponents(lines: List<Component>): List<Component> {
        return lines
            .map(LoreFormatter::normalizeSeparator)
            .map(::normalizeComponent)
    }

    private fun normalizeComponent(component: Component): Component {
        return component
            .colorIfAbsent(NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false)
    }

    private fun renderName(name: GuiNameSpec): Component = when (name) {
        GuiNameSpec.Empty -> Component.empty()
        is GuiNameSpec.Text -> this.name(name.text, name.style)
        is GuiNameSpec.Component -> normalizeComponent(name.value)
    }

    private fun clickLabel(player: Player?, clicks: Set<ClickType>): String {
        val key = GuiInteractionLabelResolver.languageKey(clicks)
        return requireI18n(player, key, emptyMap())
    }

    private fun requireI18n(
        player: Player?,
        key: String,
        arguments: Map<String, Any>,
    ): String = requireNotNull(i18n) {
        "GuiElementService instance does not support translated menu entries"
    }(player, key, arguments)
}
