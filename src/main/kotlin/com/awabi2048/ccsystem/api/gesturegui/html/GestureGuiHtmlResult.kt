package com.awabi2048.ccsystem.api.gesturegui.html

import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiDocument
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutDiagnostic

/**
 * 制限付き HTML フロントエンドの出力である文書と診断の一覧です。
 *
 * 公開 API 型です。取得は
 * [com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutFacade] を用います。
 * `core` 側の実装は直接参照しません。
 */
data class GestureGuiHtmlResult(
    val document: GestureGuiDocument,
    val diagnostics: List<GestureGuiLayoutDiagnostic>,
)
