package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiActionService
import com.awabi2048.ccsystem.api.gui.GuiClickLabel
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import org.bukkit.entity.Player

class GuiActionServiceImpl(
    private val i18n: (Player?, String, Map<String, Any>) -> String,
) : GuiActionService {
    override fun singleClick(
        player: Player?,
        action: String,
    ): GuiLoreLine.SingleAction =
        single(player, clickLabel(player, GuiClickLabel.ANY), action)

    override fun single(
        player: Player?,
        operation: String,
        action: String,
    ): GuiLoreLine.SingleAction {
        require(operation.isNotBlank()) { "Single action operation must not be blank" }
        require(action.isNotBlank()) { "Single action content must not be blank" }
        return GuiLoreLine.SingleAction(
            operation = operation,
            action = action,
            resolvedText = i18n(
                player,
                "lore.action_single_with_operation",
                mapOf("operation" to operation, "action" to action),
            ),
        )
    }

    override fun cycle(player: Player?): GuiLoreLine.Action =
        GuiLoreLine.Action(
            clickLabel(player, GuiClickLabel.LEFT_RIGHT),
            i18n(player, "gui.common.action.cycle", emptyMap()),
        )

    override fun clickLabel(
        player: Player?,
        click: GuiClickLabel,
    ): String = i18n(player, click.i18nKey, emptyMap())
}
