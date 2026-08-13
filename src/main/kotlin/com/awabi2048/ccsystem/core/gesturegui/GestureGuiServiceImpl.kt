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
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
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
        var hover: GestureGuiEntityRenderer.HoverHandle? = null,
        var hoverIdentity: String? = null,
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
        var targetYaw: Float?,
        var anchorX: Double,
        var anchorZ: Double,
        var appliedYaw: Float,
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
        val session = Session(
            id, owner.uniqueId, revision, GestureGuiSessionState.OPENING, owner.location.yaw, null,
            owner.eyeLocation.x, owner.eyeLocation.z, owner.location.yaw, screens, mutableMapOf(),
        )
        try {
            session.actors[owner.uniqueId] = createActor(session, owner)
        } catch (failure: Throwable) {
            screens.forEach { renderer.remove(it.render) }
            throw failure
        }
        sessions[owner.uniqueId] = session
        playTransitionSound(owner, opening = true)
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
        Bukkit.getPlayer(ownerId)?.let { playTransitionSound(it, opening = false) }
        session.screens.forEach { renderer.hideContents(it.render) }
        val expected = session.revision
        later(ANIMATION_INITIAL_DELAY, session, expected) {
            it.screens.forEach { screen -> renderer.setBackgroundSize(screen.render, 1.5f, 0.1f, ANIMATION_STAGE_TICKS) }
        }
        later(ANIMATION_SECOND_STAGE_DELAY, session, expected) {
            it.screens.forEach { screen -> renderer.setBackgroundSize(screen.render, 0.1f, 0.1f, ANIMATION_STAGE_TICKS) }
        }
        later(ANIMATION_COMPLETION_DELAY, session, expected) {
            sessions.remove(it.ownerId)
            destroy(it)
        }
        return true
    }

    override fun handleGesture(actor: Player, gesture: GestureGuiGesture): Boolean {
        val candidate = sessions.values.asSequence()
            .filter { it.state == GestureGuiSessionState.ACTIVE && actor.world.uid == Bukkit.getPlayer(it.ownerId)?.world?.uid }
            .mapNotNull { session -> accessibleHit(session, actor)?.let { session to it } }
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
        // 初期scaleを最低1 tickクライアントへ送った後、各補間の完了した次tickに次段階へ進みます。
        later(ANIMATION_INITIAL_DELAY, session, revision) {
            it.screens.forEach { screen -> renderer.setBackgroundSize(screen.render, 1.5f, 0.1f, ANIMATION_STAGE_TICKS) }
        }
        later(ANIMATION_SECOND_STAGE_DELAY, session, revision) {
            it.screens.forEach { screen -> renderer.setBackgroundSize(screen.render, 1.5f, 0.75f, ANIMATION_STAGE_TICKS) }
        }
        later(ANIMATION_COMPLETION_DELAY, session, revision) {
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
            val insideScreenArea = if (session.screens.size == 1) {
                hit(session, owner, margin = 0.06) != null
            } else {
                GestureGuiGeometry.containsScreenEnvelope(
                    ray(owner).direction,
                    session.retainedYaw.toDouble(),
                    session.screens.size,
                )
            }
            if (!insideScreenArea && session.targetYaw == null) {
                // 画面外へ出た瞬間の移動先を固定します。画面が途中で視線内へ戻っても、
                // このyawが画面中央へ来るまでは追従を中断しません。
                session.targetYaw = owner.location.yaw
            }
            session.targetYaw?.let { targetYaw ->
                val delta = shortestYawDelta(session.retainedYaw, targetYaw)
                if (abs(delta) <= YAW_TARGET_EPSILON) {
                    session.retainedYaw = targetYaw
                    session.targetYaw = null
                } else {
                    session.retainedYaw += delta.coerceIn(-MAX_YAW_STEP, MAX_YAW_STEP)
                }
            }
            val eye = owner.eyeLocation
            val horizontalMoved = eye.x != session.anchorX || eye.z != session.anchorZ
            val yawMoved = abs(shortestYawDelta(session.appliedYaw, session.retainedYaw)) > 1.0e-4f
            // XZが不変なら上下動だけでは画面を移動しません。yaw追従が必要な場合だけ姿勢を更新します。
            if (horizontalMoved || yawMoved) {
                val newPoses = poses(owner, session.retainedYaw, session.screens.size)
                session.screens.zip(newPoses).forEach { (screen, pose) ->
                    screen.pose = pose
                    renderer.updatePose(screen.render, pose, screen.view)
                }
                session.anchorX = eye.x
                session.anchorZ = eye.z
                session.appliedYaw = session.retainedYaw
            }
            session.actors[session.ownerId]?.let { actor ->
                renderer.moveCatcher(actor.catcher, catcherLocation(owner))
                // 開閉アニメーション中は内容より先にホバーだけが現れないよう、操作可能になってから表示します。
                val hoverHit = if (session.state == GestureGuiSessionState.ACTIVE) hit(session, owner) else null
                updateHover(session, actor, owner, hoverHit)
            }
        }
        reconcileExternalActors()
    }

    /**
     * PUBLIC/ALLOWLIST画面は最初のクリックより前にInteractionを用意します。
     * 複数画面が重なる場合も、一人の入力は最寄りの一セッションだけが所有します。
     */
    private fun reconcileExternalActors() {
        val activeSessions = sessions.values.filter { it.state == GestureGuiSessionState.ACTIVE }
        Bukkit.getOnlinePlayers().forEach { player ->
            if (player.uniqueId in sessions) return@forEach
            val desired = activeSessions.mapNotNull { session ->
                accessibleHit(session, player)?.let { session to it }
            }.minByOrNull { (_, hit) -> hit.distance }
            activeSessions.forEach { session ->
                if (session !== desired?.first && player.uniqueId in session.actors) removeActor(session, player.uniqueId)
            }
            val (session, hit) = desired ?: return@forEach
            val actor = session.actors[player.uniqueId] ?: runCatching { createActor(session, player) }.getOrNull() ?: return@forEach
            renderer.moveCatcher(actor.catcher, catcherLocation(player, hit.distance))
            updateHover(session, actor, player, hit)
        }
        activeSessions.forEach { session ->
            session.actors.keys.filter { actorId ->
                actorId != session.ownerId && Bukkit.getPlayer(actorId)?.isOnline != true
            }.forEach { removeActor(session, it) }
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
        actor.hover?.let(renderer::removeHover)
        renderer.removeCatcher(actor.catcher)
    }

    private fun updateHover(
        session: Session,
        actor: ActorRuntime,
        player: Player,
        hit: com.awabi2048.ccsystem.api.gesturegui.GestureGuiHit?,
    ) {
        val screen = hit?.let { session.screens.getOrNull(it.screenIndex) }
        val element = screen?.view?.definition?.elements?.firstOrNull { it.elementId == hit.elementId }
        val hoverText = element?.hoverText
        val identity = if (screen != null && element != null && hoverText != null) {
            "${screen.view.definition.screenId}:${element.elementId}"
        } else null
        if (identity == null) {
            actor.hover?.let(renderer::removeHover)
            actor.hover = null
            actor.hoverIdentity = null
            return
        }
        if (actor.hoverIdentity != identity || actor.hover == null) {
            actor.hover?.let(renderer::removeHover)
            actor.hover = renderer.spawnHover(player, session.id, session.revision, screen!!.pose, hoverText!!)
            actor.hoverIdentity = identity
        } else {
            renderer.updateHover(actor.hover!!, screen!!.pose, hoverText!!)
        }
    }

    private fun destroy(session: Session) {
        session.actors.keys.toList().forEach { removeActor(session, it) }
        session.screens.forEach { renderer.remove(it.render) }
    }

    private fun hit(session: Session, player: Player, margin: Double = 0.0) = GestureGuiGeometry.hitTest(
        ray(player),
        session.screens.map { screen -> screen.pose.copy(width = screen.pose.width + margin * 2, height = screen.pose.height + margin * 2) to screen.view.definition },
    )

    private fun accessibleHit(session: Session, player: Player): com.awabi2048.ccsystem.api.gesturegui.GestureGuiHit? {
        val hit = hit(session, player) ?: return null
        val screen = session.screens.getOrNull(hit.screenIndex) ?: return null
        if (!screen.view.definition.canOperate(session.ownerId, player.uniqueId)) return null
        val maximum = (player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.value ?: 3.0).coerceAtLeast(1.0)
        if (hit.distance > maximum) return null
        val eye = player.eyeLocation
        val block = player.world.rayTraceBlocks(
            eye,
            eye.direction,
            hit.distance,
            FluidCollisionMode.NEVER,
            true,
        )
        val blockDistance = block?.hitPosition?.distance(eye.toVector())
        return hit.takeIf { blockDistance == null || blockDistance + 0.01 >= hit.distance }
    }

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

    private fun catcherLocation(player: Player, hitDistance: Double = 1.25): Location =
        player.eyeLocation.clone().add(player.eyeLocation.direction.multiply((hitDistance - 0.05).coerceAtLeast(0.1)))

    private fun shortestYawDelta(from: Float, to: Float): Float = ((to - from + 540f) % 360f) - 180f

    private fun playTransitionSound(player: Player, opening: Boolean) {
        player.world.playSound(
            player.location,
            if (opening) Sound.BLOCK_ENDER_CHEST_OPEN else Sound.BLOCK_ENDER_CHEST_CLOSE,
            1.0f,
            2.0f,
        )
        player.world.playSound(player.location, Sound.BLOCK_PISTON_EXTEND, 1.0f, 2.0f)
    }

    private fun snapshot(session: Session) = GestureGuiSessionSnapshot(
        session.id,
        session.ownerId,
        session.revision,
        session.state,
        session.screens.map { it.view.definition.screenId },
        session.retainedYaw,
        session.actors.keys.toSet(),
    )

    private companion object {
        const val ANIMATION_STAGE_TICKS = 3
        const val ANIMATION_INITIAL_DELAY = 1L
        const val ANIMATION_SECOND_STAGE_DELAY = ANIMATION_INITIAL_DELAY + ANIMATION_STAGE_TICKS + 1L
        const val ANIMATION_COMPLETION_DELAY = ANIMATION_SECOND_STAGE_DELAY + ANIMATION_STAGE_TICKS + 1L
        const val MAX_YAW_STEP = 8.0f
        const val YAW_TARGET_EPSILON = 0.05f
    }
}
