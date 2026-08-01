package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.ClickType

enum class MenuNameSemantic {
    FIXED_LABEL,
    TARGET_IDENTITY,
    EMPTY,
    OPAQUE,
}

enum class MenuLoreSemanticSource {
    STRUCTURED,
    OPAQUE,
}

enum class MenuLoreLineKind {
    DESCRIPTION,
    DATA,
    CHOICE,
    WARNING,
    DANGER,
    ACTION,
    SPACER,
    SEPARATOR,
    UNKNOWN,
}

enum class MenuPresentationProfile {
    SINGLE_STANDARD_ACTION,
    MULTI_ACTION,
    LIST_TARGET,
    PAGE_NAVIGATION,
    DISABLED,
    DISPLAY_ONLY,
    UNKNOWN,
}

data class MenuLoreActionSemantics(
    val gesture: GuiInputGesture,
    private val sourceAcceptedClicks: Set<ClickType>,
    val operationLabel: String,
    val actionLabel: String,
) {
    val acceptedClicks: Set<ClickType> = MenuImmutableCollections.orderedSet(
        sourceAcceptedClicks,
        compareBy(ClickType::name),
    )

    init {
        require(operationLabel.isNotBlank()) { "operationLabel must not be blank" }
        require(actionLabel.isNotBlank()) { "actionLabel must not be blank" }
    }
}

data class MenuLoreLineSemantics(
    val kind: MenuLoreLineKind,
    val action: MenuLoreActionSemantics? = null,
) {
    init {
        require((kind == MenuLoreLineKind.ACTION) == (action != null)) {
            "only ACTION lore lines may contain action semantics"
        }
    }
}

data class MenuLoreBlockSemantics(
    private val sourceLines: List<MenuLoreLineSemantics>,
) {
    val lines: List<MenuLoreLineSemantics> = sourceLines.toList()

    init {
        require(sourceLines.isNotEmpty()) { "semantic lore block must not be empty" }
    }
}

data class MenuLoreSemantics(
    val source: MenuLoreSemanticSource,
    val frame: GuiLoreFrame,
    private val sourceBlocks: List<MenuLoreBlockSemantics>,
) {
    val blocks: List<MenuLoreBlockSemantics> = sourceBlocks.toList()
}

data class MenuElementPresentationSemantics(
    val name: MenuNameSemantic,
    val lore: MenuLoreSemantics,
    val profile: MenuPresentationProfile,
    val disabledReason: Component? = null,
) {
    companion object {
        @JvmStatic
        fun opaque(): MenuElementPresentationSemantics = MenuElementPresentationSemantics(
            MenuNameSemantic.OPAQUE,
            MenuLoreSemantics(MenuLoreSemanticSource.OPAQUE, GuiLoreFrame.NONE, emptyList()),
            MenuPresentationProfile.UNKNOWN,
        )
    }
}

sealed interface MenuAvailabilityResult {
    data object Available : MenuAvailabilityResult
    data class Unavailable(val reason: Component) : MenuAvailabilityResult
}

fun interface MenuAvailabilityProvider {
    fun resolve(): MenuAvailabilityResult
}

object MenuPresentationSemanticsValidator {
    @JvmStatic
    fun violations(element: MenuElement): List<String> = buildList {
        addAll(violations(element.presentationSemantics))
        val interaction = element.resolvedInteraction()
        when (element.presentationSemantics.profile) {
            MenuPresentationProfile.SINGLE_STANDARD_ACTION -> if (
                interaction !is MenuInteraction.Action || interaction.acceptedClicks != MenuAcceptedClicks.STANDARD
            ) add("PROFILE_INTERACTION_MISMATCH")
            MenuPresentationProfile.MULTI_ACTION -> if (
                interaction !is MenuInteraction.Branches && interaction !is MenuInteraction.ClickBranches
            ) add("PROFILE_INTERACTION_MISMATCH")
            MenuPresentationProfile.PAGE_NAVIGATION -> if (element.role != GuiElementRole.NAVIGATION) {
                add("PROFILE_ROLE_MISMATCH")
            }
            MenuPresentationProfile.DISABLED -> if (interaction !is MenuInteraction.Unavailable) {
                add("PROFILE_INTERACTION_MISMATCH")
            }
            MenuPresentationProfile.DISPLAY_ONLY -> if (interaction != MenuInteraction.DisplayOnly) {
                add("PROFILE_INTERACTION_MISMATCH")
            }
            MenuPresentationProfile.LIST_TARGET,
            MenuPresentationProfile.UNKNOWN -> Unit
        }
    }

    @JvmStatic
    fun violations(semantics: MenuElementPresentationSemantics): List<String> = buildList {
        val lines = semantics.lore.blocks.flatMap(MenuLoreBlockSemantics::lines)
        val content = lines.filter { it.kind !in setOf(MenuLoreLineKind.SPACER, MenuLoreLineKind.SEPARATOR) }
        val ranks = mapOf(
            MenuLoreLineKind.DESCRIPTION to 0,
            MenuLoreLineKind.DATA to 1,
            MenuLoreLineKind.CHOICE to 2,
            MenuLoreLineKind.WARNING to 3,
            MenuLoreLineKind.DANGER to 4,
            MenuLoreLineKind.ACTION to 5,
        )
        val ranked = content.mapNotNull { ranks[it.kind] }
        if (ranked.zipWithNext().any { (left, right) -> left > right }) add("LORE_KIND_ORDER")
        val actions = content.withIndex().filter { it.value.kind == MenuLoreLineKind.ACTION }
        if (actions.isNotEmpty() && actions.last().index != content.lastIndex) add("ACTION_NOT_LAST")
        if (semantics.profile == MenuPresentationProfile.SINGLE_STANDARD_ACTION) {
            val action = actions.singleOrNull()?.value?.action
            if (action == null || action.acceptedClicks != MenuAcceptedClicks.STANDARD) add("STANDARD_ACTION_CLICKS")
        }
        if (semantics.profile == MenuPresentationProfile.DISABLED && semantics.disabledReason == null) {
            add("DISABLED_REASON_MISSING")
        }
        if (semantics.profile == MenuPresentationProfile.DISABLED && actions.isNotEmpty()) add("DISABLED_HAS_ACTION")
        if (content.all { it.kind == MenuLoreLineKind.ACTION } && lines.firstOrNull()?.kind == MenuLoreLineKind.SPACER) {
            add("ACTION_ONLY_LEADING_SPACER")
        }
    }
}
