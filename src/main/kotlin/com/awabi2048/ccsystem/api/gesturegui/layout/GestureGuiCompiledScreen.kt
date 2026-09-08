package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiView

/**
 * compiler の出力である1画面と診断の一覧です。
 *
 * 公開 API 型です。取得は [GestureGuiLayoutFacade] を用います。
 * `core` 側の実装は直接参照しません。
 */
data class GestureGuiCompiledScreen(
    val view: GestureGuiView,
    val diagnostics: List<GestureGuiLayoutDiagnostic>,
)
