package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiBounds
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiHoverText
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVisual
import org.bukkit.entity.Player

/**
 * Custom ノード用 renderer が返す操作面です。
 *
 * elementId は Custom スコープ内で一意にし、compiler がノード ID 接頭辞を付けて
 * 画面全体の一意性を保証します。bounds は画面中央原点の Low-level 座標です。
 */
data class GestureGuiCustomElement(
    val elementId: String,
    val bounds: GestureGuiBounds,
    val acceptedGestures: Set<GestureGuiGesture> = setOf(GestureGuiGesture.PRIMARY),
    /**
     * この操作面に対応する action ID です。
     * null の場合は Custom ノード自体の actionId へフォールバックします。
     */
    val actionId: String? = null,
    val targetVisualId: String? = null,
    val hoverText: GestureGuiHoverText? = null,
    /** 通常ノードと同じく、低レベルAPIへ渡して入力時に判定します。 */
    val gestureGuard: ((Player, GestureGuiGesture) -> Boolean)? = null,
    /** actionなしのバリア等を生成します。受付gestureはcompilerで空集合へ正規化します。 */
    val consumeInput: Boolean = false,
) {
    init {
        require(elementId.isNotBlank()) { "custom elementId must not be blank" }
        require(actionId == null || actionId.isNotBlank()) { "custom action id must not be blank" }
        require(targetVisualId == null || targetVisualId.isNotBlank()) {
            "custom targetVisualId must not be blank"
        }
    }
}

/**
 * Custom ノード用 renderer の出力です。
 *
 * visuals の visualId は Custom スコープ内で一意にし、compiler が接頭辞を付けて
 * 画面全体の一意性を保証します。viewport / clip は CC-System が担うため、
 * renderer は [ResolvedGestureGuiNode.contentBounds] を基準に画面中央原点へ座標を換算し、
 * Text / Item の表示矩形を申告します。切り抜きと操作面の同期はcompilerが行います。
 */
data class GestureGuiCustomRenderResult(
    val visuals: List<GestureGuiVisual>,
    val elements: List<GestureGuiCustomElement> = emptyList(),
    /**
     * Text / Item の表示を囲む矩形です（visualId → 画面中央原点の矩形）。
     * CLIP 内では必須です。素材やフォントの実寸は renderer が指定し、切り抜き判断は
     * CC-System が行います。Block は width / height から直接求めるため指定不要です。
     * 部分表示できない Text / Item は矩形全体が収まる場合だけ描画・操作可能になります。
     */
    val visualBounds: Map<String, GestureGuiBounds> = emptyMap(),
)

/**
 * 特殊描画（Graph 等）向けの escape hatch です。
 *
 * 責務分担は以下です。
 * - viewport / clip: CC-System（解決済み clip を返却visualと操作面へ適用します）
 * - graph semantics / 配置: 呼び出し側 renderer
 * - Display Entity 描画: CC-System（返された Low-level visual を描画します）
 *
 * layout 引数には解決済みノード（border / content / effectiveClip 付き）を渡すため、
 * renderer は Kantan 側の独自 clipping を再実装せずに済みます。
 */
fun interface GestureGuiCustomRenderer {
    fun render(node: GestureGuiCustom, layout: ResolvedGestureGuiNode): GestureGuiCustomRenderResult
}
