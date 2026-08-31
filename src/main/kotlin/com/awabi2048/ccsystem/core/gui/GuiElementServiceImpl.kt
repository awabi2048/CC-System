@file:Suppress("DEPRECATION")

package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementService
import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiFrameSection
import com.awabi2048.ccsystem.api.gui.GuiFrameSpec
import com.awabi2048.ccsystem.api.gui.GuiItemSpec
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiMenuEntryAction
import com.awabi2048.ccsystem.api.gui.GuiMenuActionIntent
import com.awabi2048.ccsystem.api.gui.GuiMenuEntrySpec
import com.awabi2048.ccsystem.api.gui.GuiMenuDisplaySpec
import com.awabi2048.ccsystem.api.gui.GuiMenuCapabilityInvocationSpec
import com.awabi2048.ccsystem.api.gui.GuiStructuredMenuEntrySpec
import com.awabi2048.ccsystem.api.gui.MenuActionBranch
import com.awabi2048.ccsystem.api.gui.MenuActionSafety
import com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks
import com.awabi2048.ccsystem.api.gui.MenuElement
import com.awabi2048.ccsystem.api.gui.MenuInteraction
import com.awabi2048.ccsystem.api.gui.MenuCapabilityPresentation
import com.awabi2048.ccsystem.api.gui.ResolvedMenuCapability
import com.awabi2048.ccsystem.api.gui.GuiNameStyle
import com.awabi2048.ccsystem.api.gui.GuiNameSpec
import com.awabi2048.ccsystem.api.localization.LocalizationKey
import com.awabi2048.ccsystem.api.localization.generated.CommonKeys
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Inventory
import org.bukkit.Bukkit
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

class GuiElementServiceImpl(
    private val i18n: ((Player?, LocalizationKey<String>, Map<String, Any>) -> String)? = null,
) : GuiElementService {
    private val loreService = LoreServiceImpl(i18n)
    private val semanticsFactory = MenuPresentationSemanticsFactory(i18n)
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
        // 確認ボタンはLoreの操作案内に使う言語キーをNameへ流用する場合があるため、
        // ボタンの意味ロールを境界にして元テキストのLegacy装飾だけを除去します。
        // GuiNameSpec.Textの指定色は意味上の装飾として維持し、固定ラベル由来の装飾は
        // 確認ボタン本来の既定スタイルへ戻します。
        val normalizedSpec = if (spec.role == GuiElementRole.CONFIRM || spec.role == GuiElementRole.CANCEL) {
            spec.copy(name = confirmationName(spec.name))
        } else {
            spec
        }
        val item = ItemStack(normalizedSpec.material, normalizedSpec.amount.coerceIn(1, normalizedSpec.material.maxStackSize.coerceAtLeast(1)))
        val meta = item.itemMeta ?: return item
        meta.displayName(renderName(normalizedSpec.name))
        val lore = lore(normalizedSpec.lore)
        if (lore.isNotEmpty()) {
            meta.lore(lore)
        }
        // GUIアイコンでは素材固有の説明を表示せず、NameとLoreだけを情報源にする。
        meta.addItemFlags(
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        )
        normalizedSpec.itemModel?.let(meta::setItemModel)
        meta.isHideTooltip = normalizedSpec.role == GuiElementRole.DECORATION
        GuiItemMarker.mark(meta, normalizedSpec.role)
        item.itemMeta = meta
        return item
    }

    private fun confirmationName(name: GuiNameSpec): GuiNameSpec = when (name) {
        GuiNameSpec.Empty -> GuiNameSpec.Empty
        is GuiNameSpec.Text -> GuiNameSpec.Text(stripLegacyFormatting(name.text), name.style)
        is GuiNameSpec.FixedLabel -> GuiNameSpec.FixedLabel(
            this.name(PlainTextComponentSerializer.plainText().serialize(name.value), GuiNameStyle.DEFAULT),
        )
        is GuiNameSpec.TargetIdentity -> GuiNameSpec.FixedLabel(
            this.name(PlainTextComponentSerializer.plainText().serialize(name.value), GuiNameStyle.DEFAULT),
        )
        is GuiNameSpec.Opaque -> GuiNameSpec.FixedLabel(
            this.name(PlainTextComponentSerializer.plainText().serialize(name.value), GuiNameStyle.DEFAULT),
        )
        is GuiNameSpec.Component -> GuiNameSpec.FixedLabel(
            this.name(PlainTextComponentSerializer.plainText().serialize(name.value), GuiNameStyle.DEFAULT),
        )
    }

    private fun stripLegacyFormatting(text: String): String =
        colorCodePattern.replace(text, "")

    override fun mark(item: ItemStack, role: GuiElementRole): ItemStack {
        item.editMeta { meta -> GuiItemMarker.mark(meta, role) }
        return item
    }

    override fun isGuiItem(item: ItemStack?): Boolean = GuiItemMarker.isMarked(item)

    override fun menuEntry(player: Player?, spec: GuiMenuEntrySpec): MenuElement {
        val enabledActions = spec.expandedActions().filter { it.enabled }
        val implicitBack = spec.role == GuiElementRole.BACK && spec.actions.any { it === GuiMenuActionIntent.Back }
        val presentationActions = if (implicitBack) {
            // 戻る項目は例外的にNameだけで役割が明確なため、操作案内Loreを重ねない。
            emptyList()
        } else {
            enabledActions
        }
        val loreSpec = GuiMenuEntryLoreFactory.build(spec, presentationActions, player)
        val icon = item(
            GuiItemSpec(
                material = spec.material,
                name = spec.name,
                lore = loreSpec,
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
                    safety = action.safety,
                    reversibleContract = action.reversibleContract,
                )
            }
            else -> MenuInteraction.Branches(
                branches = enabledActions.map { action ->
                    MenuActionBranch(
                        action.actionId,
                        action.acceptedClicks,
                        action.payload,
                        action.safety,
                        action.reversibleContract,
                    )
                },
                sounds = spec.sounds,
            )
        }
        return MenuElement(
            spec.slot,
            icon,
            spec.role,
            interaction = if (enabledActions.isEmpty() && spec.role == GuiElementRole.BACK) null else interaction,
        ).withPresentationSemantics(semanticsFactory.create(
            spec.name,
            loreSpec,
            when {
                spec.name is GuiNameSpec.TargetIdentity -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.LIST_TARGET
                enabledActions.size > 1 -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.MULTI_ACTION
                spec.role in setOf(GuiElementRole.NAVIGATION, GuiElementRole.BACK) ->
                    com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.PAGE_NAVIGATION
                enabledActions.singleOrNull()?.acceptedClicks == MenuAcceptedClicks.STANDARD ->
                    com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.SINGLE_STANDARD_ACTION
                enabledActions.size == 1 -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.SINGLE_CUSTOM_ACTION
                enabledActions.isEmpty() -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.DISPLAY_ONLY
                else -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.UNKNOWN
            },
        ))
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
        ).withPresentationSemantics(semanticsFactory.create(
            spec.item.name,
            spec.item.lore,
            if (spec.item.name is GuiNameSpec.TargetIdentity) {
                com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.LIST_TARGET
            } else {
                com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.DISPLAY_ONLY
            },
        ))
    }

    override fun menuStructuredEntry(player: Player?, spec: GuiStructuredMenuEntrySpec): MenuElement {
        val enabledActions = spec.expandedActions().filter(GuiMenuEntryAction::enabled)
        val actionLines = GuiMenuEntryLoreFactory.actionLines(
            enabledActions,
            player,
            spec.interactionGuidance,
        )
        val baseItem = if (spec.embeddedLoreBlocks.isNotEmpty()) {
            spec.item.copy(lore = GuiLoreSpec.Blocks(spec.embeddedLoreBlocks))
        } else {
            spec.item
        }
        val loreSpec = if (baseItem.role == GuiElementRole.CONFIRM || baseItem.role == GuiElementRole.CANCEL) {
            // 構造化エントリ経由でも、確認操作は共通してNameのみを表示する。
            GuiLoreSpec.NameOnly
        } else {
            GuiLoreComposer.compose(baseItem.lore, actionLines)
        }
        val icon = item(baseItem.copy(lore = loreSpec)).also { item ->
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
                    action.safety,
                    reversibleContract = action.reversibleContract,
                )
            }
            else -> MenuInteraction.Branches(
                enabledActions.map { action ->
                    MenuActionBranch(
                        action.actionId,
                        action.acceptedClicks,
                        action.payload,
                        action.safety,
                        action.reversibleContract,
                    )
                },
                spec.sounds,
            )
        }
        return MenuElement(
            spec.slot,
            icon,
            spec.item.role,
            interaction = if (enabledActions.isEmpty() && spec.item.role == GuiElementRole.BACK) null else interaction,
        ).withPresentationSemantics(semanticsFactory.create(
            baseItem.name,
            loreSpec,
            when {
                baseItem.name is GuiNameSpec.TargetIdentity -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.LIST_TARGET
                enabledActions.size > 1 -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.MULTI_ACTION
                baseItem.role == GuiElementRole.NAVIGATION -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.PAGE_NAVIGATION
                enabledActions.singleOrNull()?.acceptedClicks == MenuAcceptedClicks.STANDARD ->
                    com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.SINGLE_STANDARD_ACTION
                enabledActions.size == 1 -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.SINGLE_CUSTOM_ACTION
                enabledActions.isEmpty() -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.DISPLAY_ONLY
                else -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.UNKNOWN
            },
        ))
    }

    private fun capabilityItem(player: Player?, capability: ResolvedMenuCapability): Pair<ItemStack, GuiLoreSpec> {
        require(capability.compositionMode == com.awabi2048.ccsystem.api.gui.MenuCapabilityCompositionMode.FULL_ITEM) {
            "HOST_AUGMENTATION must be composed into a host item with MenuCapabilityComposer"
        }
        val presentation = capability.presentation
        val actions = capability.actions.map { action ->
            GuiMenuEntryAction(
                actionId = action.id,
                acceptedClicks = action.trigger.clicks,
                label = action.text,
            )
        }
        val actionLines = if (
            presentation.actionGuidance == com.awabi2048.ccsystem.api.gui.MenuCapabilityActionGuidance.HIDDEN
        ) {
            emptyList()
        } else {
            GuiMenuEntryLoreFactory.actionLines(actions, player)
        }
        val baseItem = if (presentation.embeddedLoreBlocks.isNotEmpty()) {
            presentation.item.copy(lore = GuiLoreSpec.Blocks(presentation.embeddedLoreBlocks))
        } else {
            presentation.item
        }
        val itemSpec = baseItem.copy(lore = GuiLoreComposer.compose(baseItem.lore, actionLines))
        val item = item(itemSpec)
        presentation.glint?.let { enabled ->
            item.editMeta { meta -> meta.setEnchantmentGlintOverride(enabled) }
        }
        presentation.playerHeadOwner?.let { owner ->
            val meta = item.itemMeta as? SkullMeta ?: return@let
            meta.owningPlayer = Bukkit.getOfflinePlayer(owner)
            item.itemMeta = meta
        }
        return item to itemSpec.lore
    }

    override fun menuCapabilityEntry(player: Player?, spec: GuiMenuCapabilityInvocationSpec): MenuElement {
        val capability = spec.capability
        val (item, loreSpec) = capabilityItem(player, capability)
        val acceptedClicks = spec.acceptedClicks
        val element = if (capability.actionable && acceptedClicks.isNotEmpty()) {
            MenuElement(
                slot = spec.slot,
                item = item,
                role = GuiElementRole.ACTION,
                interaction = GuiMenuCapabilityInteractionFactory.create(spec),
            )
        } else if (
            capability.unavailableReason != null &&
            spec.unavailableInteraction == com.awabi2048.ccsystem.api.gui.GuiUnavailableInteraction.REJECT_WITH_REASON
        ) {
            MenuElement(
                slot = spec.slot,
                item = item,
                role = GuiElementRole.CONTENT,
                interaction = MenuInteraction.Unavailable(
                    MenuAcceptedClicks.STANDARD,
                    capability.unavailableReason!!,
                ).also { unavailable ->
                    unavailable.sourceCapability = com.awabi2048.ccsystem.api.gui.MenuCapabilitySource(
                        capability.capabilityId,
                        capability.placement,
                        spec.arguments,
                        spec.attributes,
                    )
                },
            )
        } else {
            MenuElement(
                slot = spec.slot,
                item = item,
                role = GuiElementRole.CONTENT,
                interaction = MenuInteraction.DisplayOnly,
            )
        }
        val profile = when {
            capability.unavailableReason != null -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.DISABLED
            capability.presentation.item.name is GuiNameSpec.TargetIdentity ->
                com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.LIST_TARGET
            capability.actions.size > 1 -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.MULTI_ACTION
            acceptedClicks == MenuAcceptedClicks.STANDARD ->
                com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.SINGLE_STANDARD_ACTION
            capability.actionable && capability.actions.size == 1 ->
                com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.SINGLE_CUSTOM_ACTION
            !capability.actionable -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.DISPLAY_ONLY
            else -> com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.UNKNOWN
        }
        return element.withPresentationSemantics(
            semanticsFactory.create(
                capability.presentation.item.name,
                loreSpec,
                profile,
                capability.unavailableReason,
            ).also {
                it.capabilityCompositionMode = capability.compositionMode
                it.augmentationSource = capability.augmentationSource
            },
        )
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

    override fun backgroundEntry(slot: Int, material: Material): MenuElement {
        val spec = GuiItemSpec(
            material = material,
            name = GuiNameSpec.Empty,
            lore = GuiLoreSpec.None,
            role = GuiElementRole.BACKGROUND,
            amount = 1,
        )
        return MenuElement(slot, item(spec), GuiElementRole.BACKGROUND, interaction = MenuInteraction.DisplayOnly)
            .withPresentationSemantics(semanticsFactory.create(
                spec.name,
                spec.lore,
                com.awabi2048.ccsystem.api.gui.MenuPresentationProfile.DISPLAY_ONLY,
            ))
    }

    override fun backEntry(player: Player?, slot: Int, material: Material): MenuElement {
        val label = requireI18n(player, CommonKeys.GUI_COMMON_RETURN, emptyMap())
        return menuEntry(
            player,
            GuiMenuEntrySpec(
                slot = slot,
                material = material,
                name = GuiNameSpec.FixedLabel(name(label, GuiNameStyle.MUTED)),
                role = GuiElementRole.BACK,
                actions = listOf(GuiMenuActionIntent.Back),
            ),
        )
    }

    private fun navigationAction(
        label: String,
    ): GuiMenuEntryAction = GuiMenuEntryAction(
        actionId = "back",
        acceptedClicks = MenuAcceptedClicks.STANDARD,
        label = label,
        safety = MenuActionSafety.NAVIGATION_ONLY,
    )

    override fun pageNavigationEntry(
        player: Player?,
        slot: Int,
        direction: GuiMenuActionIntent.Direction,
        actionId: String,
        label: String,
        payload: Map<String, String>,
        material: Material,
    ): MenuElement = menuEntry(
        player,
        GuiMenuEntrySpec(
            slot = slot,
            material = material,
            name = GuiNameSpec.FixedLabel(Component.text(label)),
            role = GuiElementRole.NAVIGATION,
            actions = listOf(GuiMenuActionIntent.Page(direction, actionId, label, payload)),
        ),
    )

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
        is GuiNameSpec.FixedLabel -> normalizeComponent(name.value)
        is GuiNameSpec.TargetIdentity -> normalizeComponent(name.value)
        is GuiNameSpec.Opaque -> name.value
        is GuiNameSpec.Text -> this.name(name.text, name.style)
        is GuiNameSpec.Component -> normalizeComponent(name.value)
    }

    private fun MenuElement.withPresentationSemantics(
        semantics: com.awabi2048.ccsystem.api.gui.MenuElementPresentationSemantics,
    ): MenuElement = apply { presentationSemantics = semantics }

    private fun clickLabel(player: Player?, clicks: Set<ClickType>): String {
        val key = GuiInteractionLabelResolver.languageKey(clicks)
        return requireI18n(player, key, emptyMap())
    }

    private fun requireI18n(
        player: Player?,
        key: LocalizationKey<String>,
        arguments: Map<String, Any>,
    ): String = requireNotNull(i18n) {
        "GuiElementService instance does not support translated menu entries"
    }(player, key, arguments)
}
