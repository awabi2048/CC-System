package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuActionBranch
import com.awabi2048.ccsystem.api.gui.MenuActionSafety
import com.awabi2048.ccsystem.api.gui.MenuInteraction
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionInteractionBranchSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionInteractionSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInteractionKind
import org.bukkit.event.inventory.ClickType

/**
 * 画面を開かずに interaction 契約を診断値へ写す純粋ファクトリです。
 *
 * Capability の attributes は UI 表示用の文字列へ変換せず、そのまま診断値として保持します。
 */
internal object MenuRuntimeInspectionInteractionSnapshotFactory {
    fun create(interaction: MenuInteraction): MenuRuntimeInspectionInteractionSnapshot = when (interaction) {
        MenuInteraction.DisplayOnly -> MenuRuntimeInspectionInteractionSnapshot(
            MenuRuntimeInteractionKind.DISPLAY_ONLY,
        )
        is MenuInteraction.Action -> MenuRuntimeInspectionInteractionSnapshot(
            MenuRuntimeInteractionKind.ACTION,
            actionId = interaction.actionId,
            capabilityId = interaction.capabilityId,
            arguments = interaction.payload.toSortedMap(),
            acceptedClicks = interaction.acceptedClicks.toSet(),
            safety = interaction.safety,
            safetyByClick = interaction.safetyByClick.toSortedMap(compareBy(ClickType::name)),
        )
        is MenuInteraction.Branches -> MenuRuntimeInspectionInteractionSnapshot(
            MenuRuntimeInteractionKind.BRANCHES,
            acceptedClicks = interaction.branches.flatMapTo(linkedSetOf()) { it.acceptedClicks },
            safety = interaction.branches.map(MenuActionBranch::safety).distinct().singleOrNull()
                ?: MenuActionSafety.UNSPECIFIED,
            safetyByClick = buildMap {
                interaction.branches.forEach { branch ->
                    branch.acceptedClicks.forEach { click -> put(click, branch.safety) }
                }
            }.toSortedMap(compareBy(ClickType::name)),
            branches = interaction.branches.map { branch ->
                MenuRuntimeInspectionInteractionBranchSnapshot(
                    branch.acceptedClicks.toSet(),
                    create(
                        MenuInteraction.Action(
                            branch.actionId,
                            branch.acceptedClicks,
                            branch.payload,
                            safety = branch.safety,
                        ),
                    ),
                )
            },
        )
        is MenuInteraction.ClickBranches -> MenuRuntimeInspectionInteractionSnapshot(
            MenuRuntimeInteractionKind.CLICK_BRANCHES,
            acceptedClicks = interaction.branches.flatMapTo(linkedSetOf()) { it.acceptedClicks },
            safety = interaction.branches.map { it.interaction.safetyForSnapshot() }.distinct().singleOrNull()
                ?: MenuActionSafety.UNSPECIFIED,
            safetyByClick = buildMap {
                interaction.branches.forEach { branch ->
                    branch.acceptedClicks.forEach { click ->
                        put(click, branch.interaction.safetyForSnapshot(click))
                    }
                }
            }.toSortedMap(compareBy(ClickType::name)),
            branches = interaction.branches.map { branch ->
                MenuRuntimeInspectionInteractionBranchSnapshot(
                    branch.acceptedClicks.toSet(),
                    create(branch.interaction),
                )
            },
        )
        is MenuInteraction.Capability -> MenuRuntimeInspectionInteractionSnapshot(
            MenuRuntimeInteractionKind.CAPABILITY,
            capabilityId = interaction.capabilityId,
            arguments = interaction.arguments.toSortedMap(),
            attributes = interaction.attributes.toMap(),
            acceptedClicks = interaction.acceptedClicks.toSet(),
            safety = interaction.safety,
            safetyByClick = interaction.safetyByClick.toSortedMap(compareBy(ClickType::name)),
        )
        is MenuInteraction.Unavailable -> MenuRuntimeInspectionInteractionSnapshot(
            MenuRuntimeInteractionKind.UNAVAILABLE,
            acceptedClicks = interaction.acceptedClicks.toSet(),
        )
        is MenuInteraction.Back -> MenuRuntimeInspectionInteractionSnapshot(
            MenuRuntimeInteractionKind.BACK,
            acceptedClicks = interaction.acceptedClicks.toSet(),
            safety = MenuActionSafety.NAVIGATION_ONLY,
        )
    }

    private fun MenuInteraction.safetyForSnapshot(): MenuActionSafety = when (this) {
        MenuInteraction.DisplayOnly,
        is MenuInteraction.Unavailable -> MenuActionSafety.UNSPECIFIED
        is MenuInteraction.Action -> safety
        is MenuInteraction.Branches,
        is MenuInteraction.ClickBranches -> MenuActionSafety.UNSPECIFIED
        is MenuInteraction.Capability -> safety
        is MenuInteraction.Back -> MenuActionSafety.NAVIGATION_ONLY
    }

    private fun MenuInteraction.safetyForSnapshot(click: ClickType): MenuActionSafety = when (this) {
        MenuInteraction.DisplayOnly,
        is MenuInteraction.Unavailable -> MenuActionSafety.UNSPECIFIED
        is MenuInteraction.Action -> safetyFor(click)
        is MenuInteraction.Branches,
        is MenuInteraction.ClickBranches -> MenuActionSafety.UNSPECIFIED
        is MenuInteraction.Capability -> safetyFor(click)
        is MenuInteraction.Back -> MenuActionSafety.NAVIGATION_ONLY
    }
}
