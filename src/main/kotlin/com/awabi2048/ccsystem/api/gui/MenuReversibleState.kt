package com.awabi2048.ccsystem.api.gui

import java.time.Duration
import java.util.UUID
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * [MenuActionSafety.REVERSIBLE] の操作を監査で復元するための、表示・実行契約とは別の宣言です。
 * arguments は通常の action payload と混在させず、復元 provider だけが解釈します。
 */
data class MenuReversibleContract(
    val providerId: String,
    val arguments: Map<String, String> = emptyMap(),
) {
    init {
        require(providerId.isNotBlank()) { "reversible provider id must not be blank" }
        require(arguments.keys.none { it.isBlank() }) { "reversible argument keys must not be blank" }
    }

    fun diagnosticArguments(): Map<String, String> = arguments.toSortedMap()
}

/** Provider が保持する復元専用の不透明状態です。Runtime は内容を診断へ出力しません。 */
interface MenuReversibleProviderState

/** Runtime が発行する一回限りの不透明トークンです。 */
class MenuReversibleStateToken internal constructor(
    internal val value: UUID,
) {
    override fun equals(other: Any?): Boolean =
        other is MenuReversibleStateToken && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "MenuReversibleStateToken(redacted)"
}

data class MenuReversibleInteractionContext(
    val slot: Int,
    val click: ClickType,
    val actionId: String?,
    val capabilityId: String?,
    val contract: MenuReversibleContract,
    val revision: Long,
    /** 最終 Action の payload、または Capability invocation の arguments です。 */
    val arguments: Map<String, String> = emptyMap(),
    /** Capability invocation が持つ型付き runtime attributes です。診断へは公開しません。 */
    val attributes: Map<String, Any> = emptyMap(),
    /** capture 時点の route payload です。 */
    val routePayload: Map<String, String> = emptyMap(),
)

/** provider callback は Runtime の registry/token lock を保持しない状態で呼ばれます。 */
data class MenuReversibleStateCaptureContext(
    val player: Player,
    val route: MenuRoute,
    val runId: String,
    val interaction: MenuReversibleInteractionContext,
)

data class MenuReversibleStateRestoreContext(
    val player: Player,
    val route: MenuRoute,
    val runId: String,
    val interaction: MenuReversibleInteractionContext,
    val state: MenuReversibleProviderState,
)

sealed interface MenuReversibleProviderCaptureResult {
    data class Captured(val state: MenuReversibleProviderState) : MenuReversibleProviderCaptureResult

    data class Rejected(val reason: String) : MenuReversibleProviderCaptureResult {
        init {
            require(reason.isNotBlank()) { "capture rejection reason must not be blank" }
        }
    }
}

sealed interface MenuReversibleProviderRestoreResult {
    data object Restored : MenuReversibleProviderRestoreResult

    data class Rejected(val reason: String) : MenuReversibleProviderRestoreResult {
        init {
            require(reason.isNotBlank()) { "restore rejection reason must not be blank" }
        }
    }
}

/**
 * 監査だけが呼ぶ状態操作 API です。通常の render / inspect / click 実行経路からは呼ばれません。
 */
interface MenuReversibleStateProvider {
    fun capture(context: MenuReversibleStateCaptureContext): MenuReversibleProviderCaptureResult

    fun restore(context: MenuReversibleStateRestoreContext): MenuReversibleProviderRestoreResult
}

data class MenuReversibleStateProviderDefinition(
    val owner: String,
    val id: String,
    val provider: MenuReversibleStateProvider,
) {
    val providerId: String
        get() = "$owner:$id"

    init {
        require(owner.isNotBlank()) { "reversible provider owner must not be blank" }
        require(id.isNotBlank()) { "reversible provider id must not be blank" }
    }
}

/** registry が一度の register ごとに発行する provider 世代です。 */
data class MenuReversibleStateProviderRegistration(
    val definition: MenuReversibleStateProviderDefinition,
    val generation: UUID,
)

/** owner 付き provider registry。重複 ID は常に失敗し、既存登録を置換しません。 */
interface MenuReversibleStateProviderRegistry {
    fun register(definition: MenuReversibleStateProviderDefinition)

    fun unregister(owner: String, id: String)

    fun unregisterOwner(owner: String)

    fun definition(providerId: String): MenuReversibleStateProviderDefinition?

    /** provider と、その登録インスタンスを識別する世代を返します。 */
    fun registration(providerId: String): MenuReversibleStateProviderRegistration?

    /**
     * provider 世代の解除を購読します。listener は registry の内部ロック外で同期的に呼ばれます。
     * Runtime は発行済み token を直ちに無効化するために使用します。
     */
    fun addInvalidationListener(listener: (MenuReversibleStateProviderRegistration) -> Unit): AutoCloseable

    fun definitions(): List<MenuReversibleStateProviderDefinition>
}

enum class MenuReversibleStateFailureReason {
    NO_ACTIVE_SESSION,
    ROUTE_MISMATCH,
    SLOT_NOT_ACTIONABLE,
    CLICK_UNACCEPTED,
    NOT_REVERSIBLE,
    MISSING_CONTRACT,
    UNKNOWN_PROVIDER,
    PROVIDER_GENERATION_MISMATCH,
    NO_ACTIVE_RUN,
    RUN_MISMATCH,
    ROUTE_REVISION_MISMATCH,
    CAPTURE_REJECTED,
    CAPTURE_EXCEPTION,
    TOKEN_UNKNOWN,
    TOKEN_EXPIRED,
    TOKEN_ALREADY_USED,
    TOKEN_UNBOUND,
    TOKEN_ALREADY_BOUND,
    TRACE_NOT_TERMINAL,
    TRACE_MISMATCH,
    TOKEN_WRONG_PLAYER,
    PLAYER_OFFLINE,
    PROVIDER_UNREGISTERED,
    RESTORE_REJECTED,
    RESTORE_EXCEPTION,
}

data class MenuReversibleStateFailure(
    val reason: MenuReversibleStateFailureReason,
    val message: String? = null,
    val exceptionType: String? = null,
)

sealed interface MenuReversibleStateCaptureResult {
    data class Captured(
        val token: MenuReversibleStateToken,
        val providerId: String,
        val route: MenuRuntimeRouteSnapshot,
        val revision: Long,
    ) : MenuReversibleStateCaptureResult

    data class Failed(val failure: MenuReversibleStateFailure) : MenuReversibleStateCaptureResult
}

sealed interface MenuReversibleStateRestoreResult {
    data class Restored(
        val providerId: String,
        val route: MenuRuntimeRouteSnapshot,
        val revision: Long,
    ) : MenuReversibleStateRestoreResult

    data class Failed(val failure: MenuReversibleStateFailure) : MenuReversibleStateRestoreResult
}

sealed interface MenuReversibleStateTraceBindingResult {
    data class Bound(
        val providerId: String,
        val route: MenuRuntimeRouteSnapshot,
        val revision: Long,
        val runId: String,
        val sequence: Long,
    ) : MenuReversibleStateTraceBindingResult

    data class Failed(val failure: MenuReversibleStateFailure) : MenuReversibleStateTraceBindingResult
}

/** Runtime の token 保持期間です。実装は容量超過時にも最古 token を破棄します。 */
data class MenuReversibleStateRetention(
    val ttl: Duration = Duration.ofMinutes(5),
    val capacity: Int = 256,
) {
    init {
        require(!ttl.isNegative && !ttl.isZero) { "reversible state ttl must be positive" }
        require(capacity > 0) { "reversible state capacity must be positive" }
    }
}
