package com.awabi2048.ccsystem.api.gui

/**
 * Runtimeの境界で診断結果へ変換できる失敗を決める共通方針です。
 *
 * ExceptionとLinkageErrorを含む通常の連携失敗は操作結果へ変換します。一方で、VMの継続を
 * 危険にする[VirtualMachineError]とスレッド停止要求の[ThreadDeath]は絶対に握り潰しません。
 */
@Suppress("DEPRECATION")
internal fun Throwable.rethrowIfUnrecoverableMenuRuntimeFailure() {
    when (this) {
        is VirtualMachineError,
        is ThreadDeath -> throw this
    }
}
