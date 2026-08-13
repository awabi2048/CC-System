package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
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
            entity.teleport(visualLocation(entity.world, pose, visual.x, visual.y, visual.layer))
            entity.setRotation((-poseYaw(pose)).toFloat(), 0f)
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
            it.interactionWidth = 0.18f
            it.interactionHeight = 0.18f
            it.isResponsive = false
            mark(it, sessionId, revision)
            it.persistentDataContainer.set(actorKey, PersistentDataType.STRING, player.uniqueId.toString())
        }
        // Interactionは操作者固有です。他者の照準や操作を横取りさせません。
        Bukkit.getOnlinePlayers().forEach { viewer -> viewer.hideEntity(plugin, entity) }
        player.showEntity(plugin, entity)
        return CatcherHandle(player.uniqueId, entity)
    }

    fun moveCatcher(handle: CatcherHandle, location: Location) {
        if (handle.entity.world == location.world) handle.entity.teleport(location)
    }

    fun removeCatcher(handle: CatcherHandle) = handle.entity.remove()

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
        world.spawn(visualLocation(world, pose, visual.x, visual.y, visual.layer), TextDisplay::class.java) {
            prepareDisplay(it, pose)
            it.text(visual.text)
            it.lineWidth = visual.lineWidth
            it.isSeeThrough = visual.seeThrough
            it.alignment = TextDisplay.TextAlignment.CENTER
            val scale = visual.scale.toFloat()
            it.setTransformation(Transformation(Vector3f(), AxisAngle4f(), Vector3f(scale), AxisAngle4f()))
        }

    private fun prepareDisplay(display: Display, pose: GestureGuiScreenPose) {
        display.isPersistent = false
        display.billboard = Display.Billboard.FIXED
        display.teleportDuration = 1
        display.interpolationDelay = 0
        display.interpolationDuration = 3
        display.setRotation((-poseYaw(pose)).toFloat(), 0f)
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
        Vector3f(width, height, 0.0125f),
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
        val point = pose.center + pose.right * x + pose.up * y + pose.normal * (-layer * 0.0005)
        return Location(world, point.x, point.y, point.z)
    }

    private fun poseYaw(pose: GestureGuiScreenPose): Double = Math.toDegrees(kotlin.math.atan2(-pose.normal.x, pose.normal.z))
}

private fun GestureGuiVector3.toLocation(world: World): Location = Location(world, x, y, z)
