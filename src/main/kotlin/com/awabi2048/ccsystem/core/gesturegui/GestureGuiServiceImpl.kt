package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiActionContext
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiCloseMode
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiRay
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiService
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiSessionSnapshot
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiSessionState
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiView
import com.awabi2048.ccsystem.api.input.PlayerInteractionChannel
import com.awabi2048.ccsystem.api.input.PlayerInteractionClaim
import com.awabi2048.ccsystem.api.input.PlayerInteractionClaimService
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import kotlin.math.abs

class GestureGuiServiceImpl(
    private val plugin: Plugin,
    private val claimService: PlayerInteractionClaimService,
) : GestureGuiService {
    private data class ActorRuntime(
        val playerId: UUID,
        val claims: List<PlayerInteractionClaim>,
        val catcher: GestureGuiEntityRenderer.CatcherHandle,
    )

    private data class ScreenRuntime(
        val view: GestureGuiView,
        var pose: GestureGuiScreenPose,
        val render: GestureGuiEntityRenderer.ScreenHandle,
    )

    private data class Session(
        val id: UUID,
        val ownerId: UUID,
        var revision: Long,
        var state: GestureGuiSessionState,
        var retainedYaw: Float,
        var screens: List<ScreenRuntime>,
        val actors: MutableMap<UUID, ActorRuntime>,
    )

    private val renderer = GestureGuiEntityRenderer(plugin)
    private val registeredOwners = mutableSetOf<UUID>()
    private val sessions = mutableMapOf<UUID, Session>()
    private var nextRevision = 1L
    private var tickTask: BukkitTask? = Bukkit.getScheduler().runTaskTimer(plugin, Runnable(::tick), 1L, 1L)

    override fun registerOwner(ownerId: UUID) {
        registeredOwners += ownerId
    }

    override fun unregisterOwner(ownerId: UUID) {
        registeredOwners -= ownerId
        close(ownerId, GestureGuiCloseMode.IMMEDIATE)
    }

    override fun open(owner: Player, views: List<GestureGuiView>): GestureGuiSessionSnapshot {
        require(views.size in 1..3) { "gesture GUI requires one to three screens" }
        require(views.map { it.definition.screenId }.distinct().size == views.size) {
            "gesture GUI screenId must be unique within a session"
        }
        check(owner.isOnline) { "gesture GUI owner must be online" }
        close(owner.uniqueId, GestureGuiCloseMode.IMMEDIATE)
        registeredOwners += owner.uniqueId

        val revision = nextRevision++
        val id = UUID.randomUUID()
        val poses = poses(owner, owner.location.yaw, views.size)
        val screens = views.zip(poses).map { (view, pose) ->
            ScreenRuntime(view, pose, renderer.spawnScreen(owner.world, id, revision, pose, view))
        }
        val session = Session(id, owner.uniqueId, revision, GestureGuiSessionState.OPENING, owner.location.yaw, screens, mutableMapOf())
        try {
            session.actors[owner.uniqueId] = createActor(session, owner)
        } catch (failure: Throwable) {
            screens.forEach { renderer.remove(it.render) }
            throw failure
        }
        sessions[owner.uniqueId] = session
        animateOpen(session)
        return snapshot(session)
    }

    override fun refresh(ownerId: UUID, views: List<GestureGuiView>): Boolean {
        val old = sessions[ownerId] ?: return false
        val owner = Bukkit.getPlayer(ownerId)?.takeIf(Player::isOnline) ?: return false
        require(views.size in 1..3) { "gesture GUI requires one to three screens" }
        val actors = old.actors.keys.toList()
        destroy(old)
        sessions.remove(ownerId)
        val opened = open(owner, views)
        val current = sessions[ownerId] ?: return false
        actors.asSequence().filter { it != ownerId }.mapNotNull(Bukkit::getPlayer).filter(Player::isOnline).forEach {
            runCatching { current.actors[it.uniqueId] = createActor(current, it) }
        }
        return opened.ownerId == ownerId
    }

    override fun close(ownerId: UUID, mode: GestureGuiCloseMode): Boolean {
        val session = sessions[ownerId] ?: return false
        if (mode == GestureGuiCloseMode.IMMEDIATE) {
            sessions.remove(ownerId)
            destroy(session)
            return true
        }
        if (session.state == GestureGuiSessionState.CLOSING) return true
        session.state = GestureGuiSessionState.CLOSING
        session.revision = nextRevision++
        session.screens.forEach { renderer.hideContents(it.render) }
        val expected = session.revision
        later(0, session, expected) { it.screens.forEach { screen -> renderer.setBackgroundSize(screen.render, 1.5f, 0.1f, 3) } }
        later(3, session, expected) { it.screens.forEach { screen -> renderer.setBackgroundSize(screen.render, 0.1f, 0.1f, 3) } }
        later(6, session, expected) {
            sessions.remove(it.ownerId)
            destroy(it)
        }
        return true
    }

    override fun handleGesture(actor: Player, gesture: GestureGuiGesture): Boolean {
        val candidate = sessions.values.asSequence()
            .filter { it.state == GestureGuiSessionState.ACTIVE && actor.world.uid == Bukkit.getPlayer(it.ownerId)?.world?.uid }
            .mapNotNull { session -> hit(session, actor)?.let { session to it } }
            .filter { (session, hit) -> session.screens[hit.screenIndex].view.definition.canOperate(session.ownerId, actor.uniqueId) }
            .minByOrNull { (_, hit) -> hit.distance }
            ?: return false
        val (session, hit) = candidate
        val screen = session.screens[hit.screenIndex]
        val element = screen.view.definition.elements.firstOrNull { it.elementId == hit.elementId }
        // 画面内の未割当操作も吸収しますが、Actionは実行しません。
        if (element == null || gesture !in element.acceptedGestures) return true
        if (actor.uniqueId !in session.actors) {
            session.actors[actor.uniqueId] = runCatching { createActor(session, actor) }.getOrNull() ?: return true
        }
        val revision = session.revision
        if (sessions[session.ownerId] !== session || session.state != GestureGuiSessionState.ACTIVE) return true
        screen.view.onAction(
            GestureGuiActionContext(session.ownerId, actor.uniqueId, screen.view.definition.screenId, element.elementId, gesture, revision)
        )
        return true
    }

    override fun snapshot(ownerId: UUID): GestureGuiSessionSnapshot? = sessions[ownerId]?.let(::snapshot)

    override fun shutdown() {
        tickTask?.cancel()
        tickTask = null
        sessions.values.toList().forEach(::destroy)
        sessions.clear()
        registeredOwners.clear()
    }

    /** Listenerからライフサイクル終了を即時通知する内部入口です。 */
    internal fun leaveImmediately(actorId: UUID) {
        if (close(actorId, GestureGuiCloseMode.IMMEDIATE)) return
        sessions.values.filter { actorId in it.actors }.forEach { removeActor(it, actorId) }
    }

    /** Shift+Jumpでは所有者は画面全体、第三者は自身の操作参加だけを終了します。 */
    internal fun leaveOrClose(actorId: UUID): Boolean {
        if (actorId in sessions) return close(actorId)
        val session = sessions.values.firstOrNull { actorId in it.actors } ?: return false
        removeActor(session, actorId)
        return true
    }

    internal fun ownsCatcher(entity: org.bukkit.entity.Entity): Boolean = renderer.ownsCatcher(entity)

    internal fun isParticipating(playerId: UUID): Boolean =
        playerId in sessions || sessions.values.any { playerId in it.actors }

    private fun animateOpen(session: Session) {
        val revision = session.revision
        later(0, session, revision) { it.screens.forEach { screen -> renderer.setBackgroundSize(screen.render, 1.5f, 0.1f, 3) } }
        later(3, session, revision) { it.screens.forEach { screen -> renderer.setBackgroundSize(screen.render, 1.5f, 0.75f, 3) } }
        later(6, session, revision) {
            it.screens.forEach { screen -> renderer.showContents(screen.render) }
            it.state = GestureGuiSessionState.ACTIVE
        }
    }

    private fun later(delay: Long, session: Session, revision: Long, action: (Session) -> Unit) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (sessions[session.ownerId] === session && session.revision == revision) action(session)
        }, delay)
    }

    private fun tick() {
        sessions.values.toList().forEach { session ->
            val owner = Bukkit.getPlayer(session.ownerId)
            if (owner == null || !owner.isOnline) {
                close(session.ownerId, GestureGuiCloseMode.IMMEDIATE)
                return@forEach
            }
            val currentHit = hit(session, owner, margin = 0.06)
            if (currentHit == null) {
                val delta = shortestYawDelta(session.retainedYaw, owner.location.yaw)
                // 小さな揺れを無視し、大回転時も画面が瞬間移動しないよう追従量を制限します。
                if (abs(delta) > 0.35f) session.retainedYaw += delta.coerceIn(-8f, 8f)
            }
            val newPoses = poses(owner, session.retainedYaw, session.screens.size)
            session.screens.zip(newPoses).forEach { (screen, pose) ->
                screen.pose = pose
                renderer.updatePose(screen.render, pose, screen.view)
            }
            session.actors.values.toList().forEach { actor ->
                val player = Bukkit.getPlayer(actor.playerId)
                if (player == null || !player.isOnline || player.world.uid != owner.world.uid) removeActor(session, actor.playerId)
                else renderer.moveCatcher(actor.catcher, catcherLocation(player))
            }
        }
    }

    private fun createActor(session: Session, player: Player): ActorRuntime {
        val owner = "gesture-gui:${session.id}"
        val claims = mutableListOf<PlayerInteractionClaim>()
        try {
            PlayerInteractionChannel.entries.forEach { channel ->
                claims += claimService.claim(player.uniqueId, channel, owner)
                    ?: throw IllegalStateException("gesture GUI could not claim $channel for ${player.uniqueId}")
            }
        } catch (failure: Throwable) {
            claims.forEach(PlayerInteractionClaim::close)
            throw failure
        }
        return try {
            ActorRuntime(player.uniqueId, claims, renderer.spawnCatcher(player, session.id, session.revision, catcherLocation(player)))
        } catch (failure: Throwable) {
            claims.forEach(PlayerInteractionClaim::close)
            throw failure
        }
    }

    private fun removeActor(session: Session, actorId: UUID) {
        val actor = session.actors.remove(actorId) ?: return
        actor.claims.forEach(PlayerInteractionClaim::close)
        renderer.removeCatcher(actor.catcher)
    }

    private fun destroy(session: Session) {
        session.actors.keys.toList().forEach { removeActor(session, it) }
        session.screens.forEach { renderer.remove(it.render) }
    }

    private fun hit(session: Session, player: Player, margin: Double = 0.0) = GestureGuiGeometry.hitTest(
        ray(player),
        session.screens.map { screen -> screen.pose.copy(width = screen.pose.width + margin * 2, height = screen.pose.height + margin * 2) to screen.view.definition },
    )

    private fun ray(player: Player): GestureGuiRay {
        val eye = player.eyeLocation
        val direction = eye.direction
        return GestureGuiRay(
            GestureGuiVector3(eye.x, eye.y, eye.z),
            GestureGuiVector3(direction.x, direction.y, direction.z),
        )
    }

    private fun poses(player: Player, yaw: Float, count: Int) = GestureGuiGeometry.poses(
        GestureGuiVector3(player.eyeLocation.x, player.eyeLocation.y, player.eyeLocation.z), yaw.toDouble(), count
    )

    private fun catcherLocation(player: Player): Location = player.eyeLocation.clone().add(player.eyeLocation.direction.multiply(1.25))

    private fun shortestYawDelta(from: Float, to: Float): Float = ((to - from + 540f) % 360f) - 180f

    private fun snapshot(session: Session) = GestureGuiSessionSnapshot(
        session.id,
        session.ownerId,
        session.revision,
        session.state,
        session.screens.map { it.view.definition.screenId },
        session.retainedYaw,
        session.actors.keys.toSet(),
    )
}
