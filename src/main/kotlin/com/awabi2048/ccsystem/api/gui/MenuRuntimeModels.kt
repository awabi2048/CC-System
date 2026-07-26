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
data class MenuElement(
    val slot: Int,
    val item: ItemStack,
    val role: GuiElementRole,
    val actionId: String? = null,
    val actionPayload: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
    val sounds: MenuActionSoundPolicy? = null,
) {
    init {
        require(slot >= 0) { "slot must not be negative" }
        require(actionId?.isNotBlank() != false) { "actionId must not be blank" }
        require(actionId != null || actionPayload.isEmpty()) { "display-only elements cannot have action payload" }
        require(role != GuiElementRole.DECORATION || actionId == null) {
            "decoration elements cannot have an action"
        }
    }
}

data class InventoryMenuView(
    val size: Int,
    val title: Component,
    val elements: List<MenuElement>,
    val standardFrame: Boolean = true,
    val inputSlots: Set<Int> = emptySet(),
    val inputItems: Map<Int, ItemStack> = emptyMap(),
    val allowPlayerInventoryInteraction: Boolean = false,
) {
    @Suppress("UNUSED_PARAMETER")
    constructor(
        size: Int,
        title: Component,
        elements: List<MenuElement>,
        standardFrame: Boolean,
        inputSlots: Set<Int>,
        allowPlayerInventoryInteraction: Boolean,
        mask: Int,
        marker: kotlin.jvm.internal.DefaultConstructorMarker?,
    ) : this(
        size = size,
        title = title,
        elements = elements,
        standardFrame = if (mask and 0x08 != 0) true else standardFrame,
        inputSlots = if (mask and 0x10 != 0) emptySet() else inputSlots,
        inputItems = emptyMap(),
        allowPlayerInventoryInteraction =
            if (mask and 0x20 != 0) false else allowPlayerInventoryInteraction,
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

data class MenuCloseContext(
    val player: Player,
    val route: MenuRoute,
    val inventory: Inventory,
)

fun interface MenuCloseHandler {
    fun handle(context: MenuCloseContext)
}

sealed interface MenuUpdate {
    data object None : MenuUpdate
    data object Refresh : MenuUpdate
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
        sounds: MenuActionSoundPolicy,
        mask: Int,
        marker: kotlin.jvm.internal.DefaultConstructorMarker?,
    ) : this(
        owner = owner,
        id = id,
        renderer = renderer,
        actions = actions,
        sounds = if (mask and 0x10 != 0) MenuActionSoundPolicy() else sounds,
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
