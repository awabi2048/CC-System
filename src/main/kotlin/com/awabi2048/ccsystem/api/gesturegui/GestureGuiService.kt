package com.awabi2048.ccsystem.api.gesturegui

import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * BlockDisplayへ重ねる矩形の内側枠です。
 *
 * 縁取りはDisplay Entityの後処理ではなく、四辺の薄いBlockDisplayとして描画します。
 * そのため、画面の向きが反転しても枠の上下左右が崩れないよう、rendererは対象と同じ
 * ローカル座標系・回転を使って各辺を配置します。
 */
data class GestureGuiOutline(
    val blockData: BlockData,
    /** 対象矩形の短辺に対する枠の太さです。0.10なら短辺の10%になります。 */
    val thicknessRatio: Double = 0.10,
) {
    init {
        require(thicknessRatio.isFinite() && thicknessRatio > 0.0 && thicknessRatio < 0.5) {
            "gesture GUI outline thicknessRatio must be finite and between 0 and 0.5"
        }
    }
}

/** ジェスチャーGUIに描画する要素です。座標は画面中央基準、寸法はブロック単位です。 */
sealed interface GestureGuiVisual {
    val visualId: String
    val x: Double
    val y: Double
    val layer: Int

    data class Block(
        override val visualId: String,
        override val x: Double,
        override val y: Double,
        val width: Double,
        val height: Double,
        val blockData: BlockData,
        override val layer: Int = 4,
        /** 選択ハイライト等に用いるglowの色(ARGB)。nullならglowなし。Geyser非対応時の背景色変更と併用 */
        val glowColor: Int? = null,
        /** ボタン矩形の内側へ追加する四辺の枠。nullなら枠なし。 */
        val outline: GestureGuiOutline? = null,
    ) : GestureGuiVisual {
        init {
            require(visualId.isNotBlank()) { "gesture GUI visualId must not be blank" }
            require(x.isFinite() && y.isFinite()) { "gesture GUI visual position must be finite" }
            require(width > 0.0 && height > 0.0) { "gesture GUI block visual size must be positive" }
            require(layer in 1..40) { "gesture GUI visual layer must be between 1 and 40" }
        }
    }

    data class Text(
        override val visualId: String,
        override val x: Double,
        override val y: Double,
        val text: Component,
        /** 画面上の論理文字サイズです。rendererがTextDisplay用の倍率へ変換します。 */
        val size: Double = 0.0125,
        val lineWidth: Int = 160,
        override val layer: Int = 20,
        val seeThrough: Boolean = false,
    ) : GestureGuiVisual {
        init {
            require(visualId.isNotBlank()) { "gesture GUI visualId must not be blank" }
            require(x.isFinite() && y.isFinite()) { "gesture GUI visual position must be finite" }
            require(size > 0.0) { "gesture GUI text size must be positive" }
            require(lineWidth > 0) { "gesture GUI text lineWidth must be positive" }
            require(layer in 1..40) { "gesture GUI visual layer must be between 1 and 40" }
        }
    }

    data class Item(
        override val visualId: String,
        override val x: Double,
        override val y: Double,
        val item: ItemStack,
        val scale: Double = 0.22,
        override val layer: Int = 10,
        /** 選択ハイライト等に用いるglowの色(ARGB)。nullならglowなし。Geyser非対応時の背景色変更と併用 */
        val glowColor: Int? = null,
    ) : GestureGuiVisual {
        init {
            require(visualId.isNotBlank()) { "gesture GUI visualId must not be blank" }
            require(x.isFinite() && y.isFinite()) { "gesture GUI visual position must be finite" }
            require(scale > 0.0) { "gesture GUI item scale must be positive" }
            require(layer in 1..40) { "gesture GUI visual layer must be between 1 and 40" }
        }
    }
}

data class GestureGuiActionContext(
    val ownerId: UUID,
    val actorId: UUID,
    val screenId: String,
    val elementId: String,
    val gesture: GestureGuiGesture,
    val revision: Long,
)

/** 一枚の画面について、意味上の当たり判定・表示・Actionをまとめた不変viewです。 */
data class GestureGuiView(
    val definition: GestureGuiScreenDefinition,
    val visuals: List<GestureGuiVisual>,
    val panel: GestureGuiPanel = GestureGuiPanel(),
    val onAction: (GestureGuiActionContext) -> Unit,
) {
    init {
        require(visuals.map(GestureGuiVisual::visualId).distinct().size == visuals.size) {
            "gesture GUI visualId must be unique within a screen"
        }
        val visualIds = visuals.mapTo(hashSetOf(), GestureGuiVisual::visualId)
        require(definition.elements.mapNotNull { it.targetVisualId }.all { it in visualIds }) {
            "gesture GUI element targetVisualId must reference a visual in the same screen"
        }
        require(definition.elements.mapNotNull { it.hoverText?.replacesVisualId }.all { it in visualIds }) {
            "gesture GUI hover replacesVisualId must reference a visual in the same screen"
        }
    }
}

enum class GestureGuiCloseMode { ANIMATED, IMMEDIATE }

enum class GestureGuiSessionState { OPENING, ACTIVE, CLOSING }

data class GestureGuiSessionSnapshot(
    val sessionId: UUID,
    val ownerId: UUID,
    val revision: Long,
    val state: GestureGuiSessionState,
    val screenIds: List<String>,
    val retainedYaw: Float,
    val actorIds: Set<UUID>,
    val childScreenIds: List<String>,
    val verticalSlots: List<GestureGuiVerticalSlot>? = null,
)

/** Display Entityの詳細を外部へ漏らさず、セッション単位で操作する公開サービスです。 */
interface GestureGuiService {
    fun registerOwner(ownerId: UUID)
    fun unregisterOwner(ownerId: UUID)
    fun open(owner: Player, views: List<GestureGuiView>): GestureGuiSessionSnapshot
    /** 固定位置モードで開きます。anchorを指定すると画面をワールド固定し、プレイヤー追従しなくなります。 */
    fun open(owner: Player, views: List<GestureGuiView>, options: GestureGuiOpenOptions): GestureGuiSessionSnapshot
    fun refresh(ownerId: UUID, views: List<GestureGuiView>): Boolean
    /** 指定した画面のみを即座に差し替えます(アニメーションなし)。下部パネル切替や選択反映に用います。 */
    fun updateScreen(ownerId: UUID, view: GestureGuiView): Boolean
    fun openChild(ownerId: UUID, view: GestureGuiView, options: GestureGuiChildOptions): Boolean
    fun closeChild(ownerId: UUID, screenId: String): Boolean
    fun close(ownerId: UUID, mode: GestureGuiCloseMode = GestureGuiCloseMode.ANIMATED): Boolean
    /**
     * 現在のGesture GUIセッションに一度でも参加したプレイヤーのうち、所有者・ID一致の
     * 外部Dialogだけを閉じます。セッションIDも照合するため、古いエディターの終了処理が
     * 別機能のDialogを閉じません。権限剥奪後のDialogを後始末する用途にも使います。
     */
    fun closeExternalDialogIfCurrent(
        ownerId: UUID,
        sessionId: UUID,
        player: Player,
        dialogOwner: String,
        dialogId: String,
    ): Boolean
    fun handleGesture(actor: Player, gesture: GestureGuiGesture): Boolean
    /**
     * 所有者ならセッション全体を閉じ、第三者ならその第三者だけを参加解除します。
     * 共有画面の「閉じる」操作を、第三者が所有者の画面ごと閉じないための入口です。
     */
    fun leave(actorId: UUID): Boolean
    fun snapshot(ownerId: UUID): GestureGuiSessionSnapshot?
    fun shutdown()
}
