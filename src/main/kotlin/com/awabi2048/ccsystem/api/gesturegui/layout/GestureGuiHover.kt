package com.awabi2048.ccsystem.api.gesturegui.layout

import net.kyori.adventure.text.Component
import org.bukkit.block.data.BlockData

/** ホバーの文字位置です。自動配置は操作ノードの解決済みborder boundsを基準にします。 */
sealed interface GestureGuiHoverPosition {
    data class Auto(val anchor: Anchor = Anchor.ABOVE, val gap: Double = 0.02) : GestureGuiHoverPosition {
        init { require(gap.isFinite() && gap >= 0.0) { "hover gap must be finite and non-negative" } }
    }
    /** 画面中央原点の固定座標です。共通の説明スロットへ表示を集約する場合に使用します。 */
    data class Fixed(val x: Double, val y: Double) : GestureGuiHoverPosition {
        init { require(x.isFinite() && y.isFinite()) { "hover position must be finite" } }
    }
    enum class Anchor { ABOVE, BELOW, CENTER }
}

/** 置換する表示の指定です。Nodeはコンパイル前の宣言ノードIDを指定します。 */
sealed interface GestureGuiHoverTarget {
    data object None : GestureGuiHoverTarget
    /** 自身の直接visualです。visualを持たない容器では明示的なNode指定を使用します。 */
    data object Self : GestureGuiHoverTarget
    data class Node(val nodeId: String) : GestureGuiHoverTarget {
        init { require(nodeId.isNotBlank()) { "hover node reference must not be blank" } }
    }
}

/** ホバー中だけBlockの素材を置換します。文字の置換対象とは独立した指定です。 */
data class GestureGuiHoverBlock(
    val blockData: BlockData,
    val target: GestureGuiHoverTarget = GestureGuiHoverTarget.Self,
)

/**
 * 宣言ノードのホバー情報です。配置済みノードを基準にcompilerが低レベルのhoverTextへ変換します。
 * tooltipは意図的に親のclip外へ表示でき、元ノードの描画・操作面の切り抜きとは分離します。
 */
data class GestureGuiHover(
    val text: Component,
    val size: Double = 0.006,
    val lineWidth: Int = 160,
    /** 自動配置時の論理表示高さです。複数行の文言では呼び出し側で十分な高さを指定します。 */
    val height: Double = size * 3.0,
    val position: GestureGuiHoverPosition = GestureGuiHoverPosition.Auto(),
    val replacement: GestureGuiHoverTarget = GestureGuiHoverTarget.None,
    val blockReplacement: GestureGuiHoverBlock? = null,
) {
    init {
        require(size.isFinite() && size > 0.0) { "hover size must be positive and finite" }
        require(height.isFinite() && height > 0.0) { "hover height must be positive and finite" }
        require(lineWidth > 0) { "hover lineWidth must be positive" }
    }
}
