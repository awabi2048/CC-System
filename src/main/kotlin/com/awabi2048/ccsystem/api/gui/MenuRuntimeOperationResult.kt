package com.awabi2048.ccsystem.api.gui

/** Runtimeが画面操作を試行した種別です。 */
enum class MenuRuntimeOperation {
    OPEN,
    REPLACE,
    NAVIGATE,
    REFRESH,
    REOPEN_CURRENT,
    OPEN_EPHEMERAL,
    RESUME_EXTERNAL,
    FINISH_EXTERNAL,
    BACK,
    INSPECT,
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
    INVALID_INSPECTION_CONTEXT,
}

/** 操作要求が受理済みでも、実処理がnext tick待ちかを区別します。 */
enum class MenuRuntimeOperationCompletionState {
    PENDING,
    TERMINAL,
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
    val completionState: MenuRuntimeOperationCompletionState = MenuRuntimeOperationCompletionState.TERMINAL,
) {
    init {
        require(
            when (completionState) {
                MenuRuntimeOperationCompletionState.PENDING -> !successful && failure == null
                MenuRuntimeOperationCompletionState.TERMINAL -> successful == (failure == null)
            },
        ) {
            "pending results must be unsuccessful without a failure; terminal results must match their failure"
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

        /** 要求は受理済みですが、実処理の成否はまだ確定していません。 */
        fun pending(
            operation: MenuRuntimeOperation,
            route: MenuRoute?,
        ): MenuRuntimeOperationResult = MenuRuntimeOperationResult(
            operation = operation,
            route = route,
            successful = false,
            completionState = MenuRuntimeOperationCompletionState.PENDING,
        )
    }

    val terminal: Boolean
        get() = completionState == MenuRuntimeOperationCompletionState.TERMINAL

    fun forOperation(operation: MenuRuntimeOperation): MenuRuntimeOperationResult =
        if (this.operation == operation) this else copy(operation = operation)
}
