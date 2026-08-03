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
    CLICK_BRANCHES,
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
    CANCEL,
    BACK,
    REPLACE,
    NAVIGATE,
}

/** 宣言済みの更新が実際に適用されなかった理由です。 */
enum class MenuRuntimeUpdateFailureReason {
    NONE,
    PENDING,
    OPEN_FAILED,
    UPDATE_FAILED,
    MISSING_OPENER,
    MISSING_DEFINITION,
    RENDER_FAILED,
    CONTRACT_INVALID,
    INVENTORY_OPEN_FAILED,
    OPENER_EXCEPTION,
    OPENER_RETURNED_FALSE,
    NO_ACTIVE_SESSION,
    ROUTE_MISMATCH,
    NO_HISTORY,
    STALE_REVISION,
    NOT_APPLICABLE,
    EXCEPTION,
}

/** 宣言済みupdateの適用が未試行・予約中・完了済みのどれかを表します。 */
enum class MenuRuntimeUpdateApplicationState {
    NOT_ATTEMPTED,
    PENDING,
    TERMINAL,
}

/** payloadをキー順で複写したRouteの診断表現です。 */
class MenuRuntimeRouteSnapshot(
    val owner: String,
    val id: String,
    payload: Map<String, String>,
) {
    val payload: Map<String, String> = MenuImmutableCollections.strings(payload)

    fun copy(
        owner: String = this.owner,
        id: String = this.id,
        payload: Map<String, String> = this.payload,
    ): MenuRuntimeRouteSnapshot = MenuRuntimeRouteSnapshot(owner, id, payload)

    operator fun component1(): String = owner
    operator fun component2(): String = id
    operator fun component3(): Map<String, String> = payload
    override fun equals(other: Any?): Boolean =
        other is MenuRuntimeRouteSnapshot && owner == other.owner && id == other.id && payload == other.payload
    override fun hashCode(): Int = 31 * (31 * owner.hashCode() + id.hashCode()) + payload.hashCode()
    override fun toString(): String = "MenuRuntimeRouteSnapshot(owner=$owner, id=$id, payload=$payload)"
}

/** 復元 provider の診断情報です。provider が返した不透明状態や token 本体は含みません。 */
class MenuRuntimeReversibleContractSnapshot(
    val providerId: String,
    arguments: Map<String, String>,
) {
    val arguments: Map<String, String> = MenuImmutableCollections.strings(arguments)
    fun copy(
        providerId: String = this.providerId,
        arguments: Map<String, String> = this.arguments,
    ): MenuRuntimeReversibleContractSnapshot = MenuRuntimeReversibleContractSnapshot(providerId, arguments)
    operator fun component1(): String = providerId
    operator fun component2(): Map<String, String> = arguments
    override fun equals(other: Any?): Boolean =
        other is MenuRuntimeReversibleContractSnapshot && providerId == other.providerId && arguments == other.arguments
    override fun hashCode(): Int = 31 * providerId.hashCode() + arguments.hashCode()
    override fun toString(): String =
        "MenuRuntimeReversibleContractSnapshot(providerId=$providerId, arguments=$arguments)"
}

data class MenuRuntimeBranchSnapshot(
    val actionId: String,
    val acceptedClicks: Set<ClickType>,
    val payload: Map<String, String>,
    val safety: MenuActionSafety,
    val reversibleContract: MenuRuntimeReversibleContractSnapshot? = null,
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
    /** ClickBranchesを含む最終interactionまで保持する共通snapshotです。 */
    val interaction: MenuRuntimeInspectionInteractionSnapshot? = null,
    val reversibleContractsByClick: Map<ClickType, MenuRuntimeReversibleContractSnapshot> = emptyMap(),
)
{
    var presentationSemantics: MenuElementPresentationSemantics = MenuElementPresentationSemantics.opaque()
        internal set
    var capabilityComposition: MenuCapabilityCompositionSnapshot? = null
        internal set
}

fun MenuRuntimeSlotSnapshot.copyWithPresentationSemantics(): MenuRuntimeSlotSnapshot =
    copy().also {
        it.presentationSemantics = presentationSemantics
        it.capabilityComposition = capabilityComposition
    }

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
                MenuUpdate.Cancel -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.CANCEL)
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
 * MenuUpdateの宣言と、Runtimeが実際に適用できた結果を分けて記録します。
 *
 * [kind] と [expectedRoute] は宣言された更新、[observedRoute] と [afterRevision] は
 * traceを記録した時点のRuntime観測値です。更新のないclickは[attempted]がfalseです。
 */
data class MenuRuntimeUpdateApplication(
    val attempted: Boolean,
    val applied: Boolean,
    val kind: MenuRuntimeUpdateKind?,
    val expectedRoute: MenuRuntimeRouteSnapshot?,
    val observedRoute: MenuRuntimeRouteSnapshot?,
    val beforeRevision: Long?,
    val afterRevision: Long?,
    val failureReason: MenuRuntimeUpdateFailureReason,
    /** Boolean更新APIでは失われる、open/refresh失敗の詳細です。 */
    val operationResult: MenuRuntimeOperationResult? = null,
    /** PENDINGの間は[applied]を真とみなしてはいけません。 */
    val state: MenuRuntimeUpdateApplicationState =
        if (attempted) MenuRuntimeUpdateApplicationState.TERMINAL else MenuRuntimeUpdateApplicationState.NOT_ATTEMPTED,
) {
    /** PENDING以外のtraceは、更新の有無を問わず終端状態です。 */
    val terminal: Boolean
        get() = state != MenuRuntimeUpdateApplicationState.PENDING

    /** 既存のJava/Kotlin呼出しバイナリを維持する8引数コンストラクタです。 */
    constructor(
        attempted: Boolean,
        applied: Boolean,
        kind: MenuRuntimeUpdateKind?,
        expectedRoute: MenuRuntimeRouteSnapshot?,
        observedRoute: MenuRuntimeRouteSnapshot?,
        beforeRevision: Long?,
        afterRevision: Long?,
        failureReason: MenuRuntimeUpdateFailureReason,
    ) : this(
        attempted,
        applied,
        kind,
        expectedRoute,
        observedRoute,
        beforeRevision,
        afterRevision,
        failureReason,
        null,
        if (attempted) MenuRuntimeUpdateApplicationState.TERMINAL else MenuRuntimeUpdateApplicationState.NOT_ATTEMPTED,
    )

    companion object {
        fun notAttempted(
            kind: MenuRuntimeUpdateKind? = null,
            expectedRoute: MenuRuntimeRouteSnapshot? = null,
            beforeRevision: Long? = null,
            failureReason: MenuRuntimeUpdateFailureReason = MenuRuntimeUpdateFailureReason.NOT_APPLICABLE,
        ): MenuRuntimeUpdateApplication = MenuRuntimeUpdateApplication(
            attempted = false,
            applied = false,
            kind = kind,
            expectedRoute = expectedRoute,
            observedRoute = null,
            beforeRevision = beforeRevision,
            afterRevision = null,
            failureReason = failureReason,
        )

        fun pending(
            kind: MenuRuntimeUpdateKind,
            expectedRoute: MenuRuntimeRouteSnapshot?,
            beforeRevision: Long?,
            operationResult: MenuRuntimeOperationResult,
        ): MenuRuntimeUpdateApplication = MenuRuntimeUpdateApplication(
            attempted = true,
            applied = false,
            kind = kind,
            expectedRoute = expectedRoute,
            observedRoute = null,
            beforeRevision = beforeRevision,
            afterRevision = null,
            failureReason = MenuRuntimeUpdateFailureReason.PENDING,
            operationResult = operationResult,
            state = MenuRuntimeUpdateApplicationState.PENDING,
        )
    }
}

/** click trace待機要求が、現在保持されていない識別子を参照した理由です。 */
enum class MenuRuntimeClickTraceAwaitFailureReason {
    UNKNOWN_RUN,
    UNKNOWN_SEQUENCE,
}

/** [MenuRuntimeService.awaitTerminalClickTrace] の即時失敗を識別する例外です。 */
class MenuRuntimeClickTraceAwaitException(
    val reason: MenuRuntimeClickTraceAwaitFailureReason,
) : IllegalStateException(
    when (reason) {
        MenuRuntimeClickTraceAwaitFailureReason.UNKNOWN_RUN -> "click trace run is not active"
        MenuRuntimeClickTraceAwaitFailureReason.UNKNOWN_SEQUENCE -> "click trace sequence is not retained"
    },
)

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
    /** Handlerが返した宣言値です。既存のupdate APIとの互換性を維持します。 */
    val update: MenuRuntimeUpdateSnapshot?,
    val exceptionType: String?,
    val afterRevision: Long?,
    val afterRoute: MenuRuntimeRouteSnapshot?,
    val application: MenuRuntimeUpdateApplication = MenuRuntimeUpdateApplication.notAttempted(),
    val reversibleContract: MenuRuntimeReversibleContractSnapshot? = null,
) {
    /** [update]を明示した名称です。 */
    val declaredUpdate: MenuRuntimeUpdateSnapshot?
        get() = update
}
