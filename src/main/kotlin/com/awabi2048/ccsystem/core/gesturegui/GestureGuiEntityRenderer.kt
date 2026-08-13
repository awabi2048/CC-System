package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiHoverText
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiView
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVisual
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f

/** Paper Entityの生成・差分移動・破棄をセッションランタイムから隔離します。 */
internal class GestureGuiEntityRenderer(private val plugin: Plugin) {
    private val sessionKey = NamespacedKey(plugin, "gesture_gui_session")
    private val actorKey = NamespacedKey(plugin, "gesture_gui_actor")
    private val revisionKey = NamespacedKey(plugin, "gesture_gui_revision")

    internal class ScreenHandle(
        val background: List<BlockDisplay>,
        val contents: List<Entity>,
        val visualEntities: Map<String, Entity>,
    ) {
        val all: List<Entity> get() = background + contents
    }

    internal data class CatcherHandle(val actorId: UUID, val entity: Interaction)
    internal data class HoverHandle(val actorId: UUID, val entity: TextDisplay)

    fun spawnScreen(
        world: World,
        sessionId: UUID,
        revision: Long,
        pose: GestureGuiScreenPose,
        view: GestureGuiView,
    ): ScreenHandle {
        val backgrounds = mutableListOf<BlockDisplay>()
        val contents = mutableListOf<Entity>()
        val entities = linkedMapOf<String, Entity>()
        view.visuals.sortedBy(GestureGuiVisual::layer).forEach { visual ->
            val entity = when (visual) {
                is GestureGuiVisual.Block -> spawnBlock(world, pose, visual)
                is GestureGuiVisual.Item -> spawnItem(world, pose, visual)
                is GestureGuiVisual.Text -> spawnText(world, pose, visual)
            }
            mark(entity, sessionId, revision)
            entities[visual.visualId] = entity
            if (visual is GestureGuiVisual.Block && visual.background) backgrounds += entity as BlockDisplay
            else contents += entity
        }
        // 内容は背景の展開完了まで送信せず、文字・アイコンが潰れる演出を避けます。
        contents.forEach { entity -> Bukkit.getOnlinePlayers().forEach { it.hideEntity(plugin, entity) } }
        setBackgroundSize(backgrounds, 0.1f, 0.1f, 0)
        return ScreenHandle(backgrounds, contents, entities)
    }

    fun updatePose(handle: ScreenHandle, pose: GestureGuiScreenPose, view: GestureGuiView) {
        view.visuals.forEach { visual ->
            val entity = handle.visualEntities[visual.visualId] ?: return@forEach
            val location = if (visual is GestureGuiVisual.Text) {
                textLocation(entity.world, pose, visual.x, visual.y, visual.layer)
            } else {
                visualLocation(entity.world, pose, visual.x, visual.y, visual.layer)
            }
            entity.teleport(location)
        }
    }

    fun setBackgroundSize(handle: ScreenHandle, width: Float, height: Float, interpolationTicks: Int) =
        setBackgroundSize(handle.background, width, height, interpolationTicks)

    fun showContents(handle: ScreenHandle) {
        handle.contents.forEach { entity -> Bukkit.getOnlinePlayers().forEach { it.showEntity(plugin, entity) } }
    }

    fun hideContents(handle: ScreenHandle) {
        handle.contents.forEach { entity -> Bukkit.getOnlinePlayers().forEach { it.hideEntity(plugin, entity) } }
    }

    fun remove(handle: ScreenHandle) = handle.all.forEach(Entity::remove)

    fun spawnCatcher(player: Player, sessionId: UUID, revision: Long, location: Location): CatcherHandle {
        val entity = player.world.spawn(location, Interaction::class.java) {
            it.isPersistent = false
            // 後から参加したプレイヤーにも送信されないよう、生成時点から個人表示に固定します。
            it.isVisibleByDefault = false
            it.interactionWidth = 0.18f
            it.interactionHeight = 0.18f
            it.isResponsive = false
            mark(it, sessionId, revision)
            it.persistentDataContainer.set(actorKey, PersistentDataType.STRING, player.uniqueId.toString())
        }
        // Interactionは操作者固有です。他者の照準や操作を横取りさせません。
        player.showEntity(plugin, entity)
        return CatcherHandle(player.uniqueId, entity)
    }

    fun moveCatcher(handle: CatcherHandle, location: Location) {
        if (handle.entity.world == location.world) handle.entity.teleport(location)
    }

    fun removeCatcher(handle: CatcherHandle) = handle.entity.remove()

    fun spawnHover(
        player: Player,
        sessionId: UUID,
        revision: Long,
        pose: GestureGuiScreenPose,
        hover: GestureGuiHoverText,
    ): HoverHandle {
        val entity = player.world.spawn(
            textLocation(player.world, pose, hover.x, hover.y, hover.layer),
            TextDisplay::class.java,
        ) {
            prepareTextDisplay(it, pose)
            it.isVisibleByDefault = false
            it.text(hover.text)
            it.lineWidth = hover.lineWidth
            it.isSeeThrough = false
            it.alignment = TextDisplay.TextAlignment.CENTER
            val scale = GestureGuiTextMetrics.toDisplayScale(hover.size)
            it.setTransformation(Transformation(Vector3f(), AxisAngle4f(), Vector3f(scale), AxisAngle4f()))
            mark(it, sessionId, revision)
        }
        // isVisibleByDefault=falseの実体はtracking開始後に個別表示します。同一tickのshowは
        // クライアントへspawn packetが送られる前に消費される実装差があるため、次tickへ分離します。
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (player.isOnline && entity.isValid) player.showEntity(plugin, entity)
        })
        return HoverHandle(player.uniqueId, entity)
    }

    fun updateHover(handle: HoverHandle, pose: GestureGuiScreenPose, hover: GestureGuiHoverText) {
        handle.entity.teleport(textLocation(handle.entity.world, pose, hover.x, hover.y, hover.layer))
    }

    fun removeHover(handle: HoverHandle) = handle.entity.remove()

    fun ownsCatcher(entity: Entity): Boolean = entity.persistentDataContainer.has(actorKey, PersistentDataType.STRING)

    private fun spawnBlock(world: World, pose: GestureGuiScreenPose, visual: GestureGuiVisual.Block): BlockDisplay =
        world.spawn(visualLocation(world, pose, visual.x, visual.y, visual.layer), BlockDisplay::class.java) {
            prepareDisplay(it, pose)
            it.block = visual.blockData
            it.setTransformation(blockTransform(visual.width.toFloat(), visual.height.toFloat()))
        }

    private fun spawnItem(world: World, pose: GestureGuiScreenPose, visual: GestureGuiVisual.Item): ItemDisplay =
        world.spawn(visualLocation(world, pose, visual.x, visual.y, visual.layer), ItemDisplay::class.java) {
            prepareDisplay(it, pose)
            it.setItemStack(visual.item.clone())
            it.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.GUI
            val scale = visual.scale.toFloat()
            it.setTransformation(Transformation(Vector3f(), AxisAngle4f(), Vector3f(scale), AxisAngle4f()))
        }

    private fun spawnText(world: World, pose: GestureGuiScreenPose, visual: GestureGuiVisual.Text): TextDisplay =
        world.spawn(textLocation(world, pose, visual.x, visual.y, visual.layer), TextDisplay::class.java) {
            prepareTextDisplay(it, pose)
            it.text(visual.text)
            it.lineWidth = visual.lineWidth
            it.isSeeThrough = visual.seeThrough
            it.alignment = TextDisplay.TextAlignment.CENTER
            val scale = GestureGuiTextMetrics.toDisplayScale(visual.size)
            it.setTransformation(Transformation(Vector3f(), AxisAngle4f(), Vector3f(scale), AxisAngle4f()))
        }

    private fun prepareDisplay(display: Display, pose: GestureGuiScreenPose) {
        display.isPersistent = false
        display.billboard = Display.Billboard.FIXED
        display.teleportDuration = 1
        display.interpolationDelay = 0
        display.interpolationDuration = 3
        display.setRotation(GestureGuiGeometry.displayYaw(pose), GestureGuiGeometry.displayPitch(pose))
    }

    private fun prepareTextDisplay(display: TextDisplay, pose: GestureGuiScreenPose) {
        prepareDisplay(display, pose)
        // TextDisplayだけは描画面の表法線が他Displayと逆なので、画面法線へ表側を合わせます。
        display.setRotation(GestureGuiGeometry.textDisplayYaw(pose), GestureGuiGeometry.textDisplayPitch(pose))
    }

    private fun setBackgroundSize(
        backgrounds: List<BlockDisplay>,
        width: Float,
        height: Float,
        interpolationTicks: Int,
    ) {
        backgrounds.forEach {
            it.interpolationDuration = interpolationTicks
            it.interpolationDelay = 0
            it.setTransformation(blockTransform(width, height))
        }
    }

    private fun blockTransform(width: Float, height: Float) = Transformation(
        Vector3f(-width / 2f, -height / 2f, 0f),
        AxisAngle4f(),
        Vector3f(width, height, BLOCK_NORMAL_DEPTH),
        AxisAngle4f(),
    )

    private fun mark(entity: Entity, sessionId: UUID, revision: Long) {
        entity.persistentDataContainer.set(sessionKey, PersistentDataType.STRING, sessionId.toString())
        entity.persistentDataContainer.set(revisionKey, PersistentDataType.LONG, revision)
    }

    private fun visualLocation(
        world: World,
        pose: GestureGuiScreenPose,
        x: Double,
        y: Double,
        layer: Int,
    ): Location {
        // 背景の厚みや斜め視点の深度精度に負けない距離を確保し、前景ほどプレイヤー側へ出します。
        val point = pose.center + pose.right * x + pose.up * y + pose.normal * (-layer * LAYER_DEPTH)
        return Location(
            world,
            point.x,
            point.y,
            point.z,
            GestureGuiGeometry.displayYaw(pose),
            GestureGuiGeometry.displayPitch(pose),
        )
    }

    private fun textLocation(
        world: World,
        pose: GestureGuiScreenPose,
        x: Double,
        y: Double,
        layer: Int,
    ): Location = visualLocation(world, pose, x, y, layer).apply {
        yaw = GestureGuiGeometry.textDisplayYaw(pose)
        pitch = GestureGuiGeometry.textDisplayPitch(pose)
    }

    private companion object {
        // 斜め視点の深度量子化でも隣接レイヤーが重ならないよう、従来値の5/3倍を確保します。
        const val LAYER_DEPTH = 0.005
        // パネルの画面法線方向の厚みは従来値の約2倍です。
        const val BLOCK_NORMAL_DEPTH = 0.025f
    }
}
