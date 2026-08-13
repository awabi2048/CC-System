package com.awabi2048.ccsystem.api.gesturegui

import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

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
        override val layer: Int = 0,
        /** trueの要素だけが画面の開閉アニメーションに使われます。 */
        val background: Boolean = false,
    ) : GestureGuiVisual {
        init {
            require(visualId.isNotBlank()) { "gesture GUI visualId must not be blank" }
            require(x.isFinite() && y.isFinite()) { "gesture GUI visual position must be finite" }
            require(width > 0.0 && height > 0.0) { "gesture GUI block visual size must be positive" }
        }
    }

    data class Text(
        override val visualId: String,
        override val x: Double,
        override val y: Double,
        val text: Component,
        val scale: Double = 0.0125,
        val lineWidth: Int = 160,
        override val layer: Int = 20,
        val seeThrough: Boolean = false,
    ) : GestureGuiVisual {
        init {
            require(visualId.isNotBlank()) { "gesture GUI visualId must not be blank" }
            require(x.isFinite() && y.isFinite()) { "gesture GUI visual position must be finite" }
            require(scale > 0.0) { "gesture GUI text scale must be positive" }
            require(lineWidth > 0) { "gesture GUI text lineWidth must be positive" }
        }
    }

    data class Item(
        override val visualId: String,
        override val x: Double,
        override val y: Double,
        val item: ItemStack,
        val scale: Double = 0.22,
        override val layer: Int = 10,
    ) : GestureGuiVisual {
        init {
            require(visualId.isNotBlank()) { "gesture GUI visualId must not be blank" }
            require(x.isFinite() && y.isFinite()) { "gesture GUI visual position must be finite" }
            require(scale > 0.0) { "gesture GUI item scale must be positive" }
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
)

/** Display Entityの詳細を外部へ漏らさず、セッション単位で操作する公開サービスです。 */
interface GestureGuiService {
    fun registerOwner(ownerId: UUID)
    fun unregisterOwner(ownerId: UUID)
    fun open(owner: Player, views: List<GestureGuiView>): GestureGuiSessionSnapshot
    fun refresh(ownerId: UUID, views: List<GestureGuiView>): Boolean
    fun close(ownerId: UUID, mode: GestureGuiCloseMode = GestureGuiCloseMode.ANIMATED): Boolean
    fun handleGesture(actor: Player, gesture: GestureGuiGesture): Boolean
    fun snapshot(ownerId: UUID): GestureGuiSessionSnapshot?
    fun shutdown()
}
