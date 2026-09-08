package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiAccess
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiActionContext
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.html.GestureGuiHtmlEnvironment
import com.awabi2048.ccsystem.api.gesturegui.html.GestureGuiHtmlResult
import com.awabi2048.ccsystem.core.gesturegui.html.GestureGuiHtml
import com.awabi2048.ccsystem.core.gesturegui.layout.GestureGuiLayoutCompiler
import com.awabi2048.ccsystem.core.gesturegui.layout.GestureGuiLayoutEngine
import java.util.UUID

/**
 * 宣言レイアウト基盤の公開入口です（HTML 解析→解決→コンパイル）。
 *
 * モジュール外からはこの facade のみを用い、`core` 側の実装は直接参照しません。
 * 契約版（レイアウト版・HTML プロファイル版）の確認には
 * `CCSystemAPI.gestureGuiLayoutContractVersion` /
 * `CCSystemAPI.gestureHtmlProfileVersion` を用います。
 */
object GestureGuiLayoutFacade {
    /**
     * HTML 文字列を文書へ変換します。
     *
     * @param documentName 診断用の発生源名です（例 "editor.html"）。
     */
    fun parseHtml(
        html: String,
        environment: GestureGuiHtmlEnvironment,
        documentName: String? = null,
        panel: GestureGuiPanel = GestureGuiPanel(),
    ): GestureGuiHtmlResult = GestureGuiHtml.parse(html, environment, documentName, panel)

    /**
     * 宣言的文書を解決済みレイアウトへ変換します。
     *
     * @param knownActionIds 既知の action ID 集合です。null の場合は action 参照検証を省略します。
     * @param source 診断用の発生源表記です（例 "editor.html:42"、直接構築時は null）。
     */
    fun layout(
        document: GestureGuiDocument,
        knownActionIds: Set<String>? = null,
        source: String? = null,
    ): ResolvedGestureGui = GestureGuiLayoutEngine.layout(document, knownActionIds, source)

    /**
     * 解決済み文書を1画面へ変換します。
     *
     * @param actionHandlers action ID から処理への対応表です。HTML の `action="..."`
     * 属性や宣言ノードの actionId はここへ接続します。
     * @param customRenderers rendererId から Custom renderer への対応表です。
     * @param source 診断用の発生源表記です（例 "editor.html:42"、直接構築時は null）。
     */
    fun compile(
        resolved: ResolvedGestureGui,
        screenId: String,
        actionHandlers: Map<String, (GestureGuiActionContext) -> Unit> = emptyMap(),
        customRenderers: Map<String, GestureGuiCustomRenderer> = emptyMap(),
        access: GestureGuiAccess = GestureGuiAccess.OWNER_ONLY,
        allowlist: Set<UUID> = emptySet(),
        source: String? = null,
    ): GestureGuiCompiledScreen = GestureGuiLayoutCompiler.compile(
        resolved,
        screenId,
        actionHandlers,
        customRenderers,
        access,
        allowlist,
        source,
    )
}
