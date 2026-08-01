package com.awabi2048.ccsystem.api.gui

/** Runtimeが画面操作を試行した種別です。 */
enum class MenuRuntimeOperation {
    OPEN,
    REPLACE,
    NAVIGATE,
    REFRESH,
}

/** 画面操作が完了しなかった詳細な理由です。 */
enum class MenuRuntimeOperationFailureReason {
    MISSING_OPENER,
    MISSING_DEFINITION,
    RENDER_FAILED,
    CONTRACT_INVALID,
    INVENTORY_OPEN_FAILED,
    OPENER_RETURNED_FALSE,
    OPENER_EXCEPTION,
    NO_ACTIVE_SESSION,
    ROUTE_MISMATCH,
    NO_HISTORY,
}

/** 例外本体を公開せず、診断に必要な構造化情報だけを保持します。 */
data class MenuRuntimeOperationFailure(
    val reason: MenuRuntimeOperationFailureReason,
    val contractViolations: List<MenuContractViolation> = emptyList(),
    val exceptionType: String? = null,
)

/** Boolean互換APIの失敗を失わずに返す、Runtime画面操作の結果です。 */
data class MenuRuntimeOperationResult(
    val operation: MenuRuntimeOperation,
    val route: MenuRoute?,
    val successful: Boolean,
    val failure: MenuRuntimeOperationFailure? = null,
) {
    init {
        require(successful == (failure == null)) {
            "successful result must not have a failure and failed result must have one"
        }
    }

    companion object {
        fun succeeded(
            operation: MenuRuntimeOperation,
            route: MenuRoute?,
        ): MenuRuntimeOperationResult = MenuRuntimeOperationResult(
            operation = operation,
            route = route,
            successful = true,
        )

        fun failed(
            operation: MenuRuntimeOperation,
            route: MenuRoute?,
            reason: MenuRuntimeOperationFailureReason,
            contractViolations: List<MenuContractViolation> = emptyList(),
            exceptionType: String? = null,
        ): MenuRuntimeOperationResult = MenuRuntimeOperationResult(
            operation = operation,
            route = route,
            successful = false,
            failure = MenuRuntimeOperationFailure(reason, contractViolations, exceptionType),
        )
    }

    fun forOperation(operation: MenuRuntimeOperation): MenuRuntimeOperationResult =
        if (this.operation == operation) this else copy(operation = operation)
}
