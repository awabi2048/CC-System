package com.awabi2048.ccsystem.api.gui

import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

/** Runtimeが実際に表示しているtop inventory slotの分類です。 */
enum class MenuRuntimeSlotKind {
    EMPTY,
    FRAME,
    INPUT,
    DISPLAY_ONLY,
    ACTION,
    BACK,
    UNAVAILABLE,
}

/** Slotに解決されたRuntime interactionの種類です。 */
enum class MenuRuntimeInteractionKind {
    DISPLAY_ONLY,
    ACTION,
    BRANCHES,
    CAPABILITY,
    UNAVAILABLE,
    BACK,
    PLAYER_INVENTORY,
}

/** 実clickがRuntime内でどの段階まで処理されたかを表します。 */
enum class MenuRuntimeClickDisposition {
    OWNER_MISMATCH,
    PLAYER_INVENTORY_BLOCKED,
    PLAYER_INVENTORY_INTERACTIVE,
    PLAYER_INVENTORY_UNACCEPTED,
    INPUT,
    NO_SESSION,
    ROUTE_MISMATCH,
    EMPTY,
    FRAME,
    UNSUPPORTED_CLICK,
    DISPLAY_ONLY,
    UNAVAILABLE,
    BACK,
    UNACCEPTED,
    EXECUTING,
    MISSING_HANDLER,
    HANDLED,
    EXCEPTION,
}

enum class MenuRuntimeActionResultKind {
    SUCCESS,
    IGNORED,
    REJECTED,
    ;

    companion object {
        fun from(result: MenuActionResult): MenuRuntimeActionResultKind = when (result) {
            MenuActionResult.Ignored -> IGNORED
            is MenuActionResult.Rejected -> REJECTED
            is MenuActionResult.Success -> SUCCESS
        }
    }
}

enum class MenuRuntimeUpdateKind {
    NONE,
    REFRESH,
    RESUME,
    CLOSE,
    BACK,
    REPLACE,
    NAVIGATE,
}

/** payloadをキー順で複写したRouteの診断表現です。 */
data class MenuRuntimeRouteSnapshot(
    val owner: String,
    val id: String,
    val payload: Map<String, String>,
)

data class MenuRuntimeBranchSnapshot(
    val actionId: String,
    val acceptedClicks: Set<ClickType>,
    val payload: Map<String, String>,
    val safety: MenuActionSafety,
)

/** ItemStackを保持せず、表示上必要な値だけを複写したslotの診断情報です。 */
data class MenuRuntimeSlotSnapshot(
    val slot: Int,
    val kind: MenuRuntimeSlotKind,
    val material: Material?,
    val amount: Int,
    val name: Component?,
    val lore: List<Component>,
    val glint: Boolean,
    val role: GuiElementRole?,
    val interactionKind: MenuRuntimeInteractionKind?,
    val actionId: String?,
    val capabilityId: String?,
    val acceptedClicks: Set<ClickType>,
    val payload: Map<String, String>,
    val enabled: Boolean,
    val safety: MenuActionSafety,
    val safetyByClick: Map<ClickType, MenuActionSafety>,
    val branches: List<MenuRuntimeBranchSnapshot>,
)

/** inventory surfaceでは全slotを、Dialog/Formでは空のslot一覧を返します。 */
data class MenuRuntimeSnapshot(
    val playerId: UUID,
    val route: MenuRuntimeRouteSnapshot,
    val breadcrumbs: List<MenuRuntimeRouteSnapshot>,
    val canGoBack: Boolean,
    val surface: MenuSurface,
    val title: Component,
    val size: Int,
    val revision: Long,
    val slots: List<MenuRuntimeSlotSnapshot>,
)

data class MenuRuntimeUpdateSnapshot(
    val kind: MenuRuntimeUpdateKind,
    val route: MenuRuntimeRouteSnapshot? = null,
) {
    companion object {
        fun from(result: MenuActionResult): MenuRuntimeUpdateSnapshot? = when (result) {
            MenuActionResult.Ignored,
            is MenuActionResult.Rejected -> null
            is MenuActionResult.Success -> when (val update = result.update) {
                MenuUpdate.None -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.NONE)
                MenuUpdate.Refresh -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.REFRESH)
                MenuUpdate.Resume -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.RESUME)
                MenuUpdate.Close -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.CLOSE)
                MenuUpdate.Back -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.BACK)
                is MenuUpdate.Replace -> MenuRuntimeUpdateSnapshot(
                    MenuRuntimeUpdateKind.REPLACE,
                    update.route.snapshot(),
                )
                is MenuUpdate.Navigate -> MenuRuntimeUpdateSnapshot(
                    MenuRuntimeUpdateKind.NAVIGATE,
                    update.route.snapshot(),
                )
            }
        }

        private fun MenuRoute.snapshot(): MenuRuntimeRouteSnapshot =
            MenuRuntimeRouteSnapshot(owner, id, payload.toSortedMap())
    }
}

/**
 * Runtime listenerが実際に受け取ったinventory clickの処理記録です。
 *
 * traceの保管はplayerごとに有界で、GUIの状態やactionの実行可否を変更しません。
 */
data class MenuRuntimeClickTrace(
    val runId: String,
    val sequence: Long,
    val playerId: UUID,
    val beforeRevision: Long?,
    val beforeRoute: MenuRuntimeRouteSnapshot?,
    val slot: Int,
    val click: ClickType,
    val cancelled: Boolean,
    val accepted: Boolean,
    val disposition: MenuRuntimeClickDisposition,
    val interactionKind: MenuRuntimeInteractionKind?,
    val actionId: String?,
    val capabilityId: String?,
    val payload: Map<String, String>,
    val safety: MenuActionSafety,
    val result: MenuRuntimeActionResultKind?,
    val update: MenuRuntimeUpdateSnapshot?,
    val exceptionType: String?,
    val afterRevision: Long?,
    val afterRoute: MenuRuntimeRouteSnapshot?,
)
