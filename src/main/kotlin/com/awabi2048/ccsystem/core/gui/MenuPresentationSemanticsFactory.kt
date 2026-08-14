package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.*
import com.awabi2048.ccsystem.api.localization.LocalizationKey
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

internal class MenuPresentationSemanticsFactory(
    private val i18n: ((Player?, LocalizationKey<String>, Map<String, Any>) -> String)?,
) {
    fun create(
        name: GuiNameSpec,
        lore: GuiLoreSpec,
        profile: MenuPresentationProfile,
        disabledReason: Component? = null,
    ) = MenuElementPresentationSemantics(nameKind(name), loreSemantics(lore), profile, disabledReason)

    private fun nameKind(name: GuiNameSpec): MenuNameSemantic = when (name) {
        GuiNameSpec.Empty -> MenuNameSemantic.EMPTY
        is GuiNameSpec.FixedLabel -> MenuNameSemantic.FIXED_LABEL
        is GuiNameSpec.TargetIdentity -> MenuNameSemantic.TARGET_IDENTITY
        is GuiNameSpec.Opaque, is GuiNameSpec.Text, is GuiNameSpec.Component -> MenuNameSemantic.OPAQUE
    }

    private fun loreSemantics(spec: GuiLoreSpec): MenuLoreSemantics {
        val (frame, blocks) = when (spec) {
            GuiLoreSpec.None, GuiLoreSpec.NameOnly -> GuiLoreFrame.NONE to emptyList()
            is GuiLoreSpec.Blocks -> GuiLoreFrame.BOTH to spec.blocks
            is GuiLoreSpec.FramedBlocks -> spec.frame to spec.blocks
            is GuiLoreSpec.Rich -> spec.frame to spec.lines.takeIf { it.isNotEmpty() }
                ?.let { listOf(GuiLoreBlock(it)) }.orEmpty()
            is GuiLoreSpec.Opaque -> spec.frame to listOf(GuiLoreBlock(spec.lines.map(GuiLoreLine::Opaque)))
            is GuiLoreSpec.WithActions -> withActionsBlocks(spec)
        }
        val source = if (spec.containsOpaque()) MenuLoreSemanticSource.OPAQUE else MenuLoreSemanticSource.STRUCTURED
        return MenuLoreSemantics(source, frame, blocks.map { block ->
            MenuLoreBlockSemantics(block.lines.map(::lineSemantics))
        })
    }

    private fun withActionsBlocks(spec: GuiLoreSpec.WithActions): Pair<GuiLoreFrame, List<GuiLoreBlock>> {
        val actions = GuiLoreComposer.actionBlock(spec.actions)
        return when (val base = spec.base) {
            GuiLoreSpec.None -> GuiLoreFrame.BOTH to listOf(actions)
            GuiLoreSpec.NameOnly -> GuiLoreFrame.NONE to emptyList()
            is GuiLoreSpec.Blocks -> GuiLoreFrame.BOTH to (base.blocks + actions)
            is GuiLoreSpec.FramedBlocks -> base.frame to (base.blocks + actions)
            is GuiLoreSpec.Rich -> base.frame to listOf(GuiLoreBlock(base.lines + GuiLoreLine.Spacer + actions.lines))
            is GuiLoreSpec.Opaque -> base.frame to listOf(
                GuiLoreBlock(base.lines.map(GuiLoreLine::Opaque) + GuiLoreLine.Spacer + actions.lines),
            )
            is GuiLoreSpec.WithActions -> error("Nested Lore actions are not allowed")
        }
    }

    private fun lineSemantics(line: GuiLoreLine): MenuLoreLineSemantics = when (line) {
        GuiLoreLine.Spacer -> MenuLoreLineSemantics(MenuLoreLineKind.SPACER)
        GuiLoreLine.Separator -> MenuLoreLineSemantics(MenuLoreLineKind.SEPARATOR)
        is GuiLoreLine.Text, is GuiLoreLine.StyledText, is GuiLoreLine.UserText, is GuiLoreLine.Component ->
            MenuLoreLineSemantics(MenuLoreLineKind.DESCRIPTION)
        is GuiLoreLine.Data, is GuiLoreLine.ComponentData, is GuiLoreLine.SubData, is GuiLoreLine.Metadata,
        is GuiLoreLine.StatusData, is GuiLoreLine.StatusComponentData, is GuiLoreLine.ProgressPath ->
            MenuLoreLineSemantics(MenuLoreLineKind.DATA)
        is GuiLoreLine.Option, is GuiLoreLine.Checkbox -> MenuLoreLineSemantics(MenuLoreLineKind.CHOICE)
        is GuiLoreLine.Warning -> MenuLoreLineSemantics(MenuLoreLineKind.WARNING)
        is GuiLoreLine.Danger -> MenuLoreLineSemantics(MenuLoreLineKind.DANGER)
        is GuiLoreLine.Interaction -> MenuLoreLineSemantics(
            MenuLoreLineKind.ACTION,
            MenuLoreActionSemantics(
                line.gesture.defensiveCopy(),
                (line.gesture as? GuiInputGesture.MenuClicks)?.acceptedClicks.orEmpty(),
                operationLabel(line),
                line.label,
            ),
        )
        is GuiLoreLine.Opaque -> MenuLoreLineSemantics(MenuLoreLineKind.UNKNOWN)
    }

    private fun operationLabel(line: GuiLoreLine.Interaction): String {
        return line.operationLabelKey?.let { key ->
            requireNotNull(i18n) {
                "Gui interaction semantics require translated operation labels"
            }(line.viewer, key, emptyMap())
        } ?: when (val gesture = line.gesture) {
            is GuiInputGesture.MenuClicks -> requireNotNull(i18n) {
                "Gui interaction semantics require translated operation labels"
            }(
                line.viewer,
                GuiInteractionLabelResolver.languageKey(gesture.acceptedClicks),
                emptyMap(),
            )
            is GuiInputGesture.Described -> gesture.operationLabel
        }
    }

    private fun GuiInputGesture.defensiveCopy(): GuiInputGesture = when (this) {
        is GuiInputGesture.MenuClicks -> GuiInputGesture.MenuClicks(acceptedClicks.toSet())
        is GuiInputGesture.Described -> copy()
    }

    private fun GuiLoreSpec.containsOpaque(): Boolean = when (this) {
        is GuiLoreSpec.Opaque -> true
        is GuiLoreSpec.WithActions -> base.containsOpaque()
        is GuiLoreSpec.Blocks -> blocks.any { it.lines.any { line -> line is GuiLoreLine.Opaque } }
        is GuiLoreSpec.FramedBlocks -> blocks.any { it.lines.any { line -> line is GuiLoreLine.Opaque } }
        is GuiLoreSpec.Rich -> lines.any { it is GuiLoreLine.Opaque }
        GuiLoreSpec.None, GuiLoreSpec.NameOnly -> false
    }
}
