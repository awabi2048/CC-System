package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

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
    val allowPlayerInventoryInteraction: Boolean = false,
) {
    init {
        require(size > 0 && size % 9 == 0) { "inventory menu size must be a positive multiple of 9" }
        require(elements.all { it.slot < size }) { "menu element slot is outside the inventory" }
        require(elements.map { it.slot }.distinct().size == elements.size) { "menu element slots must be unique" }
        require(inputSlots.all { it in 0 until size }) { "input slot is outside the inventory" }
        require(elements.none { it.slot in inputSlots }) { "input slots cannot contain rendered menu elements" }
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
)

fun interface MenuActionHandler {
    fun handle(context: MenuActionContext): MenuActionResult
}

sealed interface MenuUpdate {
    data object None : MenuUpdate
    data object Refresh : MenuUpdate
    data object Close : MenuUpdate
    data object Back : MenuUpdate
    data class Replace(val route: MenuRoute) : MenuUpdate
    data class Navigate(val route: MenuRoute) : MenuUpdate
}

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
) {
    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        require(actions.keys.all { it.isNotBlank() }) { "action ids must not be blank" }
    }

    val routeId: String
        get() = "$owner:$id"
}
