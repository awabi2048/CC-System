package com.awabi2048.ccsystem.api.gui

/**
 * メニュー操作の意味を、クリック集合やRuntime分岐の手組みから分離する。
 *
 * [expand] はCC-System内部でのみ表示・Runtimeモデルへ変換するために使用する。
 */
sealed interface GuiMenuActionIntent {
    val enabled: Boolean

    data class AnyClick(
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
    ) : GuiMenuActionIntent {
    }

    data class LeftRight(
        val left: AnyClick,
        val right: AnyClick,
    ) : GuiMenuActionIntent {
        override val enabled: Boolean
            get() = left.enabled || right.enabled

    }

    data object Back : GuiMenuActionIntent {
        override val enabled: Boolean = true
    }

    data class Confirm(
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
    ) : GuiMenuActionIntent {
    }

    data class Cancel(
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
    ) : GuiMenuActionIntent {
    }

    data class Page(
        val direction: Direction,
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
    ) : GuiMenuActionIntent {
    }

    enum class Direction {
        PREVIOUS,
        NEXT,
    }
}

internal fun GuiMenuActionIntent.expand(): List<GuiMenuEntryAction> = when (this) {
    is GuiMenuActionIntent.AnyClick -> listOf(
        GuiMenuEntryAction(
            actionId = actionId,
            acceptedClicks = MenuAcceptedClicks.STANDARD,
            label = label,
            payload = payload,
            enabled = enabled,
        )
    )
    is GuiMenuActionIntent.LeftRight -> buildList {
        if (left.enabled) add(left.toEntry(MenuAcceptedClicks.PLAIN_LEFT))
        if (right.enabled) add(right.toEntry(MenuAcceptedClicks.PLAIN_RIGHT))
    }
    GuiMenuActionIntent.Back -> emptyList()
    is GuiMenuActionIntent.Confirm -> GuiMenuActionIntent.AnyClick(
        actionId,
        label,
        payload,
        enabled,
    ).expand()
    is GuiMenuActionIntent.Cancel -> GuiMenuActionIntent.AnyClick(
        actionId,
        label,
        payload,
        enabled,
    ).expand()
    is GuiMenuActionIntent.Page -> GuiMenuActionIntent.AnyClick(
        actionId,
        label,
        payload,
        enabled,
    ).expand()
}

private fun GuiMenuActionIntent.AnyClick.toEntry(
    clicks: Set<org.bukkit.event.inventory.ClickType>,
) = GuiMenuEntryAction(
    actionId = actionId,
    acceptedClicks = clicks,
    label = label,
    payload = payload,
    enabled = enabled,
)
