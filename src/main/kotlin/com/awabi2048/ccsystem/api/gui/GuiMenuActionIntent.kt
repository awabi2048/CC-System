package com.awabi2048.ccsystem.api.gui

/**
 * メニュー操作の意味を、クリック集合やRuntime分岐の手組みから分離する。
 *
 * [expand] はCC-System内部でのみ表示・Runtimeモデルへ変換するために使用する。
 */
sealed interface GuiMenuActionIntent {
    val enabled: Boolean

    /** A single action whose click contract is selected by semantic gesture. */
    data class GestureAction(
        val actionId: String,
        val gesture: MenuGesture,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
        val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
        val reversibleContract: MenuReversibleContract? = null,
    ) : GuiMenuActionIntent

    data class AnyClick(
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
        val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
        val reversibleContract: MenuReversibleContract? = null,
    ) : GuiMenuActionIntent {
    }

    data class LeftRight(
        val left: AnyClick,
        val right: AnyClick,
    ) : GuiMenuActionIntent {
        override val enabled: Boolean
            get() = left.enabled || right.enabled

    }

    /** Same action for left/right clicks, including Shift variants. */
    data class LeftRightSame(
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
        val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
        val reversibleContract: MenuReversibleContract? = null,
    ) : GuiMenuActionIntent

    /** Same action for plain left/right clicks only. */
    data class PlainLeftRight(
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
        val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
        val reversibleContract: MenuReversibleContract? = null,
    ) : GuiMenuActionIntent

    /** Same action for both Shift-click directions. */
    data class ShiftAny(
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
        val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
        val reversibleContract: MenuReversibleContract? = null,
    ) : GuiMenuActionIntent

    data class MiddleClick(
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
        val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
        val reversibleContract: MenuReversibleContract? = null,
    ) : GuiMenuActionIntent

    data object Back : GuiMenuActionIntent {
        override val enabled: Boolean = true
    }

    data class Confirm(
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
        val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
        val reversibleContract: MenuReversibleContract? = null,
    ) : GuiMenuActionIntent {
    }

    data class Cancel(
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
        val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
        val reversibleContract: MenuReversibleContract? = null,
    ) : GuiMenuActionIntent {
    }

    data class Page(
        val direction: Direction,
        val actionId: String,
        val label: String,
        val payload: Map<String, String> = emptyMap(),
        override val enabled: Boolean = true,
        val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
        val reversibleContract: MenuReversibleContract? = null,
    ) : GuiMenuActionIntent {
    }

    enum class Direction {
        PREVIOUS,
        NEXT,
    }
}

internal fun GuiMenuActionIntent.expand(): List<GuiMenuEntryAction> = when (this) {
    is GuiMenuActionIntent.GestureAction -> listOf(toEntry(gesture.clicks))
    is GuiMenuActionIntent.AnyClick -> listOf(
        GuiMenuEntryAction(
            actionId = actionId,
            acceptedClicks = MenuAcceptedClicks.STANDARD,
            label = label,
            payload = payload,
            enabled = enabled,
            safety = safety,
            reversibleContract = reversibleContract,
        )
    )
    is GuiMenuActionIntent.LeftRight -> buildList {
        if (left.enabled) add(left.toEntry(MenuAcceptedClicks.PLAIN_LEFT))
        if (right.enabled) add(right.toEntry(MenuAcceptedClicks.PLAIN_RIGHT))
    }
    is GuiMenuActionIntent.LeftRightSame -> listOf(toEntry(MenuAcceptedClicks.LEFT_RIGHT))
    is GuiMenuActionIntent.PlainLeftRight -> listOf(toEntry(MenuAcceptedClicks.PLAIN_LEFT_RIGHT))
    is GuiMenuActionIntent.ShiftAny -> listOf(toEntry(MenuAcceptedClicks.SHIFT_LEFT_RIGHT))
    is GuiMenuActionIntent.MiddleClick -> listOf(toEntry(MenuAcceptedClicks.MIDDLE))
    GuiMenuActionIntent.Back -> emptyList()
    is GuiMenuActionIntent.Confirm -> GuiMenuActionIntent.AnyClick(
        actionId,
        label,
        payload,
        enabled,
        safety,
        reversibleContract,
    ).expand()
    is GuiMenuActionIntent.Cancel -> GuiMenuActionIntent.AnyClick(
        actionId,
        label,
        payload,
        enabled,
        safety,
        reversibleContract,
    ).expand()
    is GuiMenuActionIntent.Page -> GuiMenuActionIntent.AnyClick(
        actionId,
        label,
        payload,
        enabled,
        safety,
        reversibleContract,
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
    safety = safety,
    reversibleContract = reversibleContract,
)

private fun GuiMenuActionIntent.GestureAction.toEntry(
    clicks: Set<org.bukkit.event.inventory.ClickType>,
) = GuiMenuEntryAction(actionId, clicks, label, payload, enabled, safety, reversibleContract)

private fun GuiMenuActionIntent.LeftRightSame.toEntry(
    clicks: Set<org.bukkit.event.inventory.ClickType>,
) = GuiMenuEntryAction(actionId, clicks, label, payload, enabled, safety, reversibleContract)

private fun GuiMenuActionIntent.PlainLeftRight.toEntry(
    clicks: Set<org.bukkit.event.inventory.ClickType>,
) = GuiMenuEntryAction(actionId, clicks, label, payload, enabled, safety, reversibleContract)

private fun GuiMenuActionIntent.ShiftAny.toEntry(
    clicks: Set<org.bukkit.event.inventory.ClickType>,
) = GuiMenuEntryAction(actionId, clicks, label, payload, enabled, safety, reversibleContract)

private fun GuiMenuActionIntent.MiddleClick.toEntry(
    clicks: Set<org.bukkit.event.inventory.ClickType>,
) = GuiMenuEntryAction(actionId, clicks, label, payload, enabled, safety, reversibleContract)

enum class MenuGesture {
    ANY,
    LEFT,
    RIGHT,
    LEFT_RIGHT,
    PLAIN_LEFT,
    PLAIN_RIGHT,
    PLAIN_LEFT_RIGHT,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    SHIFT_LEFT_RIGHT,
    MIDDLE,
    ;

    val clicks: Set<org.bukkit.event.inventory.ClickType>
        get() = when (this) {
            ANY -> MenuAcceptedClicks.STANDARD
            LEFT -> MenuAcceptedClicks.LEFT
            RIGHT -> MenuAcceptedClicks.RIGHT
            LEFT_RIGHT -> MenuAcceptedClicks.LEFT_RIGHT
            PLAIN_LEFT -> MenuAcceptedClicks.PLAIN_LEFT
            PLAIN_RIGHT -> MenuAcceptedClicks.PLAIN_RIGHT
            PLAIN_LEFT_RIGHT -> MenuAcceptedClicks.PLAIN_LEFT_RIGHT
            SHIFT_LEFT -> MenuAcceptedClicks.SHIFT_LEFT
            SHIFT_RIGHT -> MenuAcceptedClicks.SHIFT_RIGHT
            SHIFT_LEFT_RIGHT -> MenuAcceptedClicks.SHIFT_LEFT_RIGHT
            MIDDLE -> MenuAcceptedClicks.MIDDLE
        }

    companion object {
        fun fromClicks(clicks: Set<org.bukkit.event.inventory.ClickType>): MenuGesture =
            entries.firstOrNull { it.clicks == clicks }
                ?: error("Unsupported semantic menu click set: $clicks")
    }
}
