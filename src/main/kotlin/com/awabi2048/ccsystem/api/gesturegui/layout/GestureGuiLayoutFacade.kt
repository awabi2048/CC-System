package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiAccess
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiActionContext
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.html.GestureGuiHtmlEnvironment
import com.awabi2048.ccsystem.api.gesturegui.html.GestureGuiHtmlResult
import com.awabi2048.ccsystem.api.gesturegui.theme.GestureGuiThemes
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
     * @param baseStyle 基底スタイルシートです。文書の `<style>`・`style` 属性より
     *   低い優先度で適用します（cc-system 同梱語彙を使う場合は
     *   [GestureGuiThemes.baseStyleSheet] を渡します）。
     */
    fun parseHtml(
        html: String,
        environment: GestureGuiHtmlEnvironment,
        documentName: String? = null,
        panel: GestureGuiPanel = GestureGuiPanel(),
        baseStyle: String? = null,
    ): GestureGuiHtmlResult = GestureGuiHtml.parse(html, environment, documentName, panel, baseStyle)

    /**
     * クラスパス上の `.html` リソースを文書へ変換します。
     *
     * リソースは呼び出し側モジュールの JAR 同梱を前提とし、[loader] にはその
     * モジュールのクラスローダを渡します（例 `MyPlugin::class.java.classLoader`）。
     * 既定で cc-system 同梱の基底スタイルシートを適用するため、マークアップ側は
     * 語彙クラスと構造だけを記述します。同梱語彙を使わない場合は [baseStyle] に
     * null または独自シートを渡します。
     *
     * @param resourcePath クラスパス上のリソース位置です（例 `"gesture-gui/setting.html"`）。
     */
    fun parseHtmlResource(
        resourcePath: String,
        loader: ClassLoader,
        environment: GestureGuiHtmlEnvironment,
        documentName: String? = resourcePath.substringAfterLast('/'),
        panel: GestureGuiPanel = GestureGuiPanel(),
        baseStyle: String? = GestureGuiThemes.baseStyleSheet(),
    ): GestureGuiHtmlResult {
        val html = loader.getResourceAsStream(resourcePath)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IllegalArgumentException("Gesture GUI の HTML リソースが見つかりません: $resourcePath")
        return GestureGuiHtml.parse(html, environment, documentName, panel, baseStyle)
    }

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
