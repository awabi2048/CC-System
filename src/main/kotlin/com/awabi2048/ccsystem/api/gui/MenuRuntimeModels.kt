package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Inventory

/** CC-Systemが管理するメニューの表示方式。 */
enum class MenuSurface {
    INVENTORY,
    DIALOG,
    FORM,
}

/** 既定音、明示的な無音、任意音を曖昧さなく表す。 */
sealed interface MenuSoundPolicy {
    data object Default : MenuSoundPolicy
    data object Silent : MenuSoundPolicy
    data class Custom(val sound: MenuSound) : MenuSoundPolicy
}

data class MenuActionSoundPolicy(
    val success: MenuSoundPolicy = MenuSoundPolicy.Default,
    val rejected: MenuSoundPolicy = MenuSoundPolicy.Default,
)

/** Inventory上の1要素。actionIdがない要素は表示専用として扱う。 */
sealed interface MenuInteraction {
    data object DisplayOnly : MenuInteraction

    data class Action(
        val actionId: String,
        val acceptedClicks: Set<ClickType> = MenuAcceptedClicks.LEFT_RIGHT,
        val payload: Map<String, String> = emptyMap(),
        val sounds: MenuActionSoundPolicy? = null,
    ) : MenuInteraction {
        init {
            require(actionId.isNotBlank()) { "actionId must not be blank" }
            require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
        }
    }

    /**
     * クリック種別ごとに異なるActionへ分岐する宣言。
     * 表示案内とクリック受付を同じBranch群から生成するために使用する。
     */
    data class Branches(
        val branches: List<MenuActionBranch>,
        val sounds: MenuActionSoundPolicy? = null,
    ) : MenuInteraction {
        init {
            require(branches.isNotEmpty()) { "branches must not be empty" }
            val accepted = branches.flatMap(MenuActionBranch::acceptedClicks)
            require(accepted.size == accepted.distinct().size) {
                "a click type cannot be assigned to multiple action branches"
            }
        }
    }

    data class Capability(
        val capabilityId: String,
        val arguments: Map<String, String> = emptyMap(),
        val attributes: Map<String, Any> = emptyMap(),
        val acceptedClicks: Set<ClickType> = MenuAcceptedClicks.LEFT_RIGHT,
        val sounds: MenuActionSoundPolicy? = null,
    ) : MenuInteraction {
        init {
            require(capabilityId.isNotBlank()) { "capabilityId must not be blank" }
            require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
        }
    }

    data class Unavailable(
        val acceptedClicks: Set<ClickType> = MenuAcceptedClicks.LEFT_RIGHT,
        val message: Component? = null,
        val sounds: MenuActionSoundPolicy? = null,
    ) : MenuInteraction {
        init {
            require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
        }
    }

    data class Back(
        val acceptedClicks: Set<ClickType> = MenuAcceptedClicks.LEFT_RIGHT,
        val sounds: MenuActionSoundPolicy? = null,
    ) : MenuInteraction {
        init {
            require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
        }
    }
}

data class MenuActionBranch(
    val actionId: String,
    val acceptedClicks: Set<ClickType>,
    val payload: Map<String, String> = emptyMap(),
) {
    init {
        require(actionId.isNotBlank()) { "actionId must not be blank" }
        require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
    }
}

object MenuAcceptedClicks {
    val STANDARD: Set<ClickType> = setOf(
        ClickType.LEFT,
        ClickType.RIGHT,
        ClickType.SHIFT_LEFT,
        ClickType.SHIFT_RIGHT,
        ClickType.MIDDLE,
    )
    val LEFT: Set<ClickType> = setOf(ClickType.LEFT, ClickType.SHIFT_LEFT)
    val RIGHT: Set<ClickType> = setOf(ClickType.RIGHT, ClickType.SHIFT_RIGHT)
    val LEFT_RIGHT: Set<ClickType> = LEFT + RIGHT
    val PLAIN_LEFT: Set<ClickType> = setOf(ClickType.LEFT)
    val PLAIN_RIGHT: Set<ClickType> = setOf(ClickType.RIGHT)
    val PLAIN_LEFT_RIGHT: Set<ClickType> = PLAIN_LEFT + PLAIN_RIGHT
    val SHIFT_LEFT: Set<ClickType> = setOf(ClickType.SHIFT_LEFT)
    val SHIFT_RIGHT: Set<ClickType> = setOf(ClickType.SHIFT_RIGHT)
    val MIDDLE: Set<ClickType> = setOf(ClickType.MIDDLE)
}

@ConsistentCopyVisibility
data class MenuElement internal constructor(
    val slot: Int,
    val item: ItemStack,
    val role: GuiElementRole,
    val actionId: String? = null,
    val actionPayload: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
    val sounds: MenuActionSoundPolicy? = null,
    val interaction: MenuInteraction? = null,
) {
    init {
        require(slot >= 0) { "slot must not be negative" }
        require(actionId?.isNotBlank() != false) { "actionId must not be blank" }
        require(actionId != null || actionPayload.isEmpty()) { "display-only elements cannot have action payload" }
        require(role != GuiElementRole.DECORATION || actionId == null) {
            "decoration elements cannot have an action"
        }
        require(
            interaction !is MenuInteraction.Action &&
                interaction !is MenuInteraction.Capability ||
                role != GuiElementRole.DECORATION
        ) {
            "decoration elements cannot have an interaction action"
        }
        require(interaction !is MenuInteraction.Back || role == GuiElementRole.BACK) {
            "back interaction requires BACK role"
        }
    }

    fun resolvedInteraction(): MenuInteraction = interaction ?: when {
        role == GuiElementRole.BACK -> MenuInteraction.Back(sounds = sounds)
        !enabled -> MenuInteraction.Unavailable(sounds = sounds)
        actionId != null -> MenuInteraction.Action(actionId, payload = actionPayload, sounds = sounds)
        else -> MenuInteraction.DisplayOnly
    }
}

data class InventoryMenuView(
    val size: Int,
    val title: Component,
    val elements: List<MenuElement>,
    val standardFrame: Boolean = true,
    val inputSlots: Set<Int> = emptySet(),
    val inputItems: Map<Int, ItemStack> = emptyMap(),
    val playerInventoryInteraction: PlayerInventoryInteraction = PlayerInventoryInteraction.INTERACTIVE,
) {
    @Deprecated(
        message = "playerInventoryInteractionで画面の入力モードを明示してください",
        replaceWith = ReplaceWith("playerInventoryInteraction != PlayerInventoryInteraction.BLOCKED"),
    )
    val allowPlayerInventoryInteraction: Boolean
        get() = playerInventoryInteraction != PlayerInventoryInteraction.BLOCKED

    constructor(
        size: Int,
        title: Component,
        elements: List<MenuElement>,
        standardFrame: Boolean,
        inputSlots: Set<Int>,
        inputItems: Map<Int, ItemStack>,
        allowPlayerInventoryInteraction: Boolean,
    ) : this(
        size = size,
        title = title,
        elements = elements,
        standardFrame = standardFrame,
        inputSlots = inputSlots,
        inputItems = inputItems,
        playerInventoryInteraction =
            if (allowPlayerInventoryInteraction) {
                PlayerInventoryInteraction.INTERACTIVE
            } else {
                PlayerInventoryInteraction.BLOCKED
            },
    )

    @Suppress("UNUSED_PARAMETER")
    constructor(
        size: Int,
        title: Component,
        elements: List<MenuElement>,
        standardFrame: Boolean,
        inputSlots: Set<Int>?,
        allowPlayerInventoryInteraction: Boolean,
        mask: Int,
        marker: kotlin.jvm.internal.DefaultConstructorMarker?,
    ) : this(
        size = size,
        title = title,
        elements = elements,
        standardFrame = if (mask and 0x08 != 0) true else standardFrame,
        inputSlots = if (mask and 0x10 != 0) emptySet() else requireNotNull(inputSlots),
        inputItems = emptyMap(),
        playerInventoryInteraction =
            if (mask and 0x20 != 0 || allowPlayerInventoryInteraction) {
                PlayerInventoryInteraction.INTERACTIVE
            } else {
                PlayerInventoryInteraction.BLOCKED
            },
    )

    @Suppress("UNUSED_PARAMETER")
    constructor(
        size: Int,
        title: Component,
        elements: List<MenuElement>,
        standardFrame: Boolean,
        inputSlots: Set<Int>?,
        inputItems: Map<Int, ItemStack>?,
        allowPlayerInventoryInteraction: Boolean,
        mask: Int,
        marker: kotlin.jvm.internal.DefaultConstructorMarker?,
    ) : this(
        size = size,
        title = title,
        elements = elements,
        standardFrame = if (mask and 0x08 != 0) true else standardFrame,
        inputSlots = if (mask and 0x10 != 0) emptySet() else requireNotNull(inputSlots),
        inputItems = if (mask and 0x20 != 0) emptyMap() else requireNotNull(inputItems),
        playerInventoryInteraction =
            if (mask and 0x40 != 0 || allowPlayerInventoryInteraction) {
                PlayerInventoryInteraction.INTERACTIVE
            } else {
                PlayerInventoryInteraction.BLOCKED
            },
    )

    init {
        require(size > 0 && size % 9 == 0) { "inventory menu size must be a positive multiple of 9" }
        require(elements.all { it.slot < size }) { "menu element slot is outside the inventory" }
        require(elements.map { it.slot }.distinct().size == elements.size) { "menu element slots must be unique" }
        require(inputSlots.all { it in 0 until size }) { "input slot is outside the inventory" }
        require(elements.none { it.slot in inputSlots }) { "input slots cannot contain rendered menu elements" }
        require(inputItems.keys.all { it in inputSlots }) { "input items must be placed in declared input slots" }
    }
}

data class MenuRenderContext(
    val player: Player,
    val route: MenuRoute,
    val canGoBack: Boolean = false,
)

fun interface InventoryMenuRenderer {
    fun render(context: MenuRenderContext): InventoryMenuView
}

data class MenuActionContext(
    val player: Player,
    val route: MenuRoute,
    val actionId: String,
    val payload: Map<String, String>,
    val click: ClickType,
    val item: ItemStack,
    val cursor: ItemStack,
)

/** Runtimeがプレイヤーインベントリ側の選択をActionへ渡す際に使用する予約Action。 */
object MenuRuntimeActions {
    const val PLAYER_INVENTORY_CLICK = "__player_inventory_click"
    const val PLAYER_INVENTORY_SLOT_PAYLOAD = "slot"
}

fun interface MenuActionHandler {
    fun handle(context: MenuActionContext): MenuActionResult
}

enum class MenuCloseReason {
    USER_DISMISSED,
    ROUTE_REPLACED,
    RUNTIME_CLOSED,
}

data class MenuCloseContext(
    val player: Player,
    val route: MenuRoute,
    val inventory: Inventory,
    val reason: MenuCloseReason,
) {
    constructor(
        player: Player,
        route: MenuRoute,
        inventory: Inventory,
    ) : this(player, route, inventory, MenuCloseReason.USER_DISMISSED)
}

fun interface MenuCloseHandler {
    fun handle(context: MenuCloseContext)
}

sealed interface MenuUpdate {
    data object None : MenuUpdate
    data object Refresh : MenuUpdate
    data object Resume : MenuUpdate
    data object Close : MenuUpdate
    data object Back : MenuUpdate
    data class Replace(val route: MenuRoute) : MenuUpdate
    data class Navigate(val route: MenuRoute) : MenuUpdate
}

enum class ManagedMenuTransition {
    AUTOMATIC,
    ROOT,
    REPLACE,
    NAVIGATE,
    PRESERVE_HISTORY,
}

data class ManagedInventoryMenuRequest(
    val route: MenuRoute,
    val inventory: Inventory,
    val transition: ManagedMenuTransition = ManagedMenuTransition.AUTOMATIC,
    val policy: GuiInventoryPolicy = GuiInventoryPolicy(),
    val openSound: MenuSoundPolicy = MenuSoundPolicy.Default,
)

object MenuRouteIds {
    fun fromInventory(inventory: Inventory): String {
        val holderName = inventory.holder?.javaClass?.simpleName
            ?.takeIf(String::isNotBlank)
            ?: return "inventory"
        return fromHolderName(holderName)
    }

    fun fromHolderName(holderName: String): String {
        require(holderName.isNotBlank()) { "holderName must not be blank" }
        val withoutSuffix = holderName
            .removeSuffix("InventoryHolder")
            .removeSuffix("Holder")
            .ifBlank { holderName }
        return withoutSuffix
            .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1_$2")
            .lowercase()
    }
}

enum class ManagedMenuInteractionOutcome {
    SUCCESS,
    REJECTED,
}

data class ManagedMenuInteraction(
    val outcome: ManagedMenuInteractionOutcome,
    val clickType: MenuClickType = MenuClickType.DEFAULT,
    val sound: MenuSoundPolicy = MenuSoundPolicy.Default,
)

sealed interface MenuActionResult {
    data class Success(
        val update: MenuUpdate = MenuUpdate.Refresh,
        val sound: MenuSoundPolicy = MenuSoundPolicy.Default,
    ) : MenuActionResult

    data class Rejected(
        val message: Component? = null,
        val sound: MenuSoundPolicy = MenuSoundPolicy.Default,
    ) : MenuActionResult

    data object Ignored : MenuActionResult
}

data class InventoryMenuDefinition(
    val owner: String,
    val id: String,
    val renderer: InventoryMenuRenderer,
    val actions: Map<String, MenuActionHandler>,
    val sounds: MenuActionSoundPolicy = MenuActionSoundPolicy(),
    val onClose: MenuCloseHandler? = null,
) {
    @Suppress("UNUSED_PARAMETER")
    constructor(
        owner: String,
        id: String,
        renderer: InventoryMenuRenderer,
        actions: Map<String, MenuActionHandler>,
        sounds: MenuActionSoundPolicy?,
        mask: Int,
        marker: kotlin.jvm.internal.DefaultConstructorMarker?,
    ) : this(
        owner = owner,
        id = id,
        renderer = renderer,
        actions = actions,
        sounds = if (mask and 0x10 != 0) MenuActionSoundPolicy() else requireNotNull(sounds),
        onClose = null,
    )

    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        require(actions.keys.all { it.isNotBlank() }) { "action ids must not be blank" }
    }

    val routeId: String
        get() = "$owner:$id"
}
