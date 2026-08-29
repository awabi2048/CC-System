package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiActionContext
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiCloseMode
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiChildOptions
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiOpenOptions
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiRay
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenLayout
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiService
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiSessionSnapshot
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiSessionState
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiSessionListener
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiView
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVerticalSlot
import com.awabi2048.ccsystem.api.input.PlayerInteractionChannel
import com.awabi2048.ccsystem.api.input.PlayerInteractionClaim
import com.awabi2048.ccsystem.api.input.PlayerInteractionClaimService
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.logging.Level
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2

/** Listenerが同一tickの後続イベントを抑制するかまで含めた入力処理結果です。 */
internal enum class GestureGuiDispatchResult(
    val consumed: Boolean,
    val deduplicate: Boolean,
) {
    UNHANDLED(consumed = false, deduplicate = false),
    CONSUMED(consumed = true, deduplicate = true),
    /** 視線はGUI領域内だが要素が未確定なため、同一tickに再判定できます。 */
    RETRYABLE_CONSUMED(consumed = true, deduplicate = false),
    ACTION_HANDLED(consumed = true, deduplicate = true),
}

class GestureGuiServiceImpl(
    private val plugin: Plugin,
    private val claimService: PlayerInteractionClaimService,
    private val closeExternalDialog: (Player, String, String) -> Boolean,
) : GestureGuiService {
    private data class HoverReplacement(
        val render: GestureGuiEntityRenderer.ScreenHandle,
        val visualId: String,
    )

    private data class ActorRuntime(
        val playerId: UUID,
        val claims: List<PlayerInteractionClaim>,
        val catcher: GestureGuiEntityRenderer.CatcherHandle,
        var hover: GestureGuiEntityRenderer.HoverHandle? = null,
        var hoverIdentity: String? = null,
        var hoverReplacement: HoverReplacement? = null,
    )

    private data class ScreenRuntime(
        val view: GestureGuiView,
        var pose: GestureGuiScreenPose,
        val render: GestureGuiEntityRenderer.ScreenHandle,
    )

    private data class ChildRuntime(
        val view: GestureGuiView,
        val options: GestureGuiChildOptions,
        var pose: GestureGuiScreenPose,
        val render: GestureGuiEntityRenderer.ScreenHandle,
        val overlay: org.bukkit.entity.BlockDisplay?,
        var state: GestureGuiSessionState,
    )

    private data class TargetHit(
        val screen: ScreenRuntime?,
        val child: ChildRuntime?,
        val hit: com.awabi2048.ccsystem.api.gesturegui.GestureGuiHit,
        val blocked: Boolean = false,
    ) {
        val view: GestureGuiView? get() = child?.view ?: screen?.view
        val pose: GestureGuiScreenPose? get() = child?.pose ?: screen?.pose
    }

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
        val children: MutableList<ChildRuntime>,
        val actors: MutableMap<UUID, ActorRuntime>,
        /** 固定位置モードのアンカー。nullならプレイヤー追従 */
        val fixedAnchor: Location? = null,
        /** 利用側のローカル状態を終了通知へ接続するリスナー */
        val sessionListener: GestureGuiSessionListener? = null,
        /** close通知を一度だけ送るための状態 */
        var closeNotified: Boolean = false,
        /** 主要画面の並び方向。pose再計算（updateScreen等）でも同じ配置を維持します */
        val layout: GestureGuiScreenLayout = GestureGuiScreenLayout.VERTICAL,
        /** 縦配置で使用する実体スロット。欠けたスロットは生成しません。 */
        val verticalSlots: List<GestureGuiVerticalSlot>? = null,
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

    override fun open(owner: Player, views: List<GestureGuiView>): GestureGuiSessionSnapshot =
        open(owner, views, GestureGuiOpenOptions())

    override fun open(owner: Player, views: List<GestureGuiView>, options: GestureGuiOpenOptions): GestureGuiSessionSnapshot {
        require(views.size in 1..3) { "gesture GUI requires one to three screens" }
        require(views.map { it.definition.screenId }.distinct().size == views.size) {
            "gesture GUI screenId must be unique within a session"
        }
        require(options.verticalSlots == null || options.verticalSlots.size == views.size) {
            "gesture GUI vertical slot count must match screen count"
        }
        check(owner.isOnline) { "gesture GUI owner must be online" }
        close(owner.uniqueId, GestureGuiCloseMode.IMMEDIATE)
        registeredOwners += owner.uniqueId

        val revision = nextRevision++
        val id = UUID.randomUUID()
        val anchor = options.anchor
        val poses = if (anchor != null) {
            fixedPoses(anchor, owner.eyeLocation, views, options.layout, options.verticalSlots)
        } else {
            poses(owner, owner.location.yaw, views, options.layout, options.verticalSlots)
        }
        val screens = mutableListOf<ScreenRuntime>()
        var session: Session? = null
        try {
            views.zip(poses).forEach { (view, pose) ->
                // 画面Entityは一つでも生成に失敗すると操作不能になります。生成途中の
                // 実体を必ず同じtry境界で管理し、背景だけ残る部分生成を防ぎます。
                screens += ScreenRuntime(view, pose, renderer.spawnScreen(owner, id, revision, pose, view))
            }
            session = Session(
                id = id,
                ownerId = owner.uniqueId,
                revision = revision,
                state = GestureGuiSessionState.OPENING,
                retainedYaw = owner.location.yaw,
                targetYaw = null,
                anchorX = owner.eyeLocation.x,
                anchorZ = owner.eyeLocation.z,
                appliedYaw = owner.location.yaw,
                screens = screens.toList(),
                children = mutableListOf(),
                actors = mutableMapOf(),
                fixedAnchor = anchor,
                sessionListener = options.sessionListener,
                layout = options.layout,
                verticalSlots = options.verticalSlots,
            )
            session.actors[owner.uniqueId] = createActor(session, owner)
            sessions[owner.uniqueId] = session
            playTransitionSound(owner, opening = true)
            animateOpen(session)
        } catch (failure: Throwable) {
            session?.let { failedSession ->
                // Session登録後の効果音・アニメーション失敗も含め、同一実体だけを
                // 条件付きでMapから除去します。ownerIdだけで削除すると、失敗経路へ
                // 再入して作られた別セッションを巻き込む可能性があります。
                if (sessions[owner.uniqueId] === failedSession) {
                    sessions.remove(owner.uniqueId)
                }
                // open()が呼び出し元へ戻る前の失敗でも、登録済みリスナーには
                // セッション終了を一度だけ通知します。通常はFacade未登録ですが、
                // 再入や将来の利用側変更で通知先が存在しても状態を取りこぼしません。
                notifyClosed(failedSession)
                destroy(failedSession)
            } ?: screens.forEach { renderer.remove(it.render) }
            throw failure
        }
        val openedSession = requireNotNull(session)
        return snapshot(openedSession)
    }

    override fun updateScreen(ownerId: UUID, view: GestureGuiView): Boolean {
        val session = sessions[ownerId]?.takeIf { it.state == GestureGuiSessionState.ACTIVE } ?: return false
        val owner = Bukkit.getPlayer(ownerId)?.takeIf(Player::isOnline) ?: return false
        val targetIndex = session.screens.indexOfFirst { it.view.definition.screenId == view.definition.screenId }
        if (targetIndex >= 0) {
            val oldScreens = session.screens
            val newViews = oldScreens.mapIndexed { index, screen -> if (index == targetIndex) view else screen.view }
            val newPoses = parentPoses(session, owner, newViews)
            session.revision = nextRevision++
            session.screens = oldScreens.mapIndexed { index, screen ->
                val newView = newViews[index]
                val newPose = newPoses[index]
                renderer.updatePose(screen.render, newPose, newView)
                renderer.updateScreenDiff(screen.render, session.id, session.revision, newPose, screen.view, newView)
                renderer.updateAccess(screen.render, newView)
                renderer.showImmediately(screen.render, newView.panel)
                screen.copy(view = newView, pose = newPose)
            }
            repositionChildren(session)
            return true
        }
        val targetChild = session.children.firstOrNull { it.view.definition.screenId == view.definition.screenId }
        if (targetChild != null) {
            session.revision = nextRevision++
            val pose = targetChild.pose.copy(width = view.panel.width, height = view.panel.height)
            renderer.updatePose(targetChild.render, pose, view)
            renderer.updateScreenDiff(targetChild.render, session.id, session.revision, pose, targetChild.view, view)
            renderer.updateAccess(targetChild.render, view)
            renderer.showImmediately(targetChild.render, view.panel)
            val idx = session.children.indexOf(targetChild)
            session.children[idx] = targetChild.copy(view = view, pose = pose)
            // 子画面自身の寸法が変わると、その前面に積まれた確認子画面や
            // モーダルオーバーレイの基準位置も変わります。親画面更新時と同じ
            // 再配置経路を通し、孫画面まで古いposeを残さないようにします。
            repositionChildren(session)
            return true
        }
        return false
    }

    override fun refresh(ownerId: UUID, views: List<GestureGuiView>): Boolean {
        val old = sessions[ownerId] ?: return false
        val owner = Bukkit.getPlayer(ownerId)?.takeIf(Player::isOnline) ?: return false
        require(views.size in 1..3) { "gesture GUI requires one to three screens" }
        val actors = old.actors.keys.toList()
        val options = GestureGuiOpenOptions(
            anchor = old.fixedAnchor,
            sessionListener = old.sessionListener,
            layout = old.layout,
            verticalSlots = old.verticalSlots,
        )
        notifyClosed(old)
        if (sessions[ownerId] === old) sessions.remove(ownerId)
        destroy(old)
        val opened = open(owner, views, options)
        val current = sessions[ownerId] ?: return false
        actors.asSequence().filter { it != ownerId }.mapNotNull(Bukkit::getPlayer).filter(Player::isOnline).forEach {
            runCatching { current.actors[it.uniqueId] = createActor(current, it) }
        }
        return opened.ownerId == ownerId
    }

    override fun openChild(ownerId: UUID, view: GestureGuiView, options: GestureGuiChildOptions): Boolean {
        val session = sessions[ownerId]?.takeIf { it.state == GestureGuiSessionState.ACTIVE } ?: return false
        val owner = Bukkit.getPlayer(ownerId)?.takeIf(Player::isOnline) ?: return false
        if (session.children.size >= MAX_CHILD_DEPTH) return false
        val parent = parentRuntime(session, options.parentScreenId) ?: return false
        require(session.screens.none { it.view.definition.screenId == view.definition.screenId } &&
            session.children.none { it.view.definition.screenId == view.definition.screenId }) {
            "gesture GUI screenId must be unique within a session"
        }
        val childIndex = session.children.size
        session.revision = nextRevision++
        val pose = childPose(parent.pose, view, options, session.screens.size + childIndex, childIndex)
        var overlay: org.bukkit.entity.BlockDisplay? = null
        val render = try {
            if (!options.allowParentInteraction) {
                overlay = renderer.spawnModalOverlay(
                    owner,
                    session.id,
                    session.revision,
                    modalOverlayPose(parent.pose, childIndex),
                    options.overlayMaterial ?: Material.GRAY_STAINED_GLASS,
                )
            }
            renderer.spawnScreen(owner, session.id, session.revision, pose, view)
        } catch (failure: Throwable) {
            overlay?.remove()
            throw failure
        }
        val child = ChildRuntime(
            view, options, pose, render, overlay,
            if (options.animated) GestureGuiSessionState.OPENING else GestureGuiSessionState.ACTIVE,
        )
        session.children += child
        if (options.animated) {
            animateChildOpen(session, child)
        } else {
            renderer.setBackgroundSize(render, view.panel.width.toFloat(), view.panel.height.toFloat(), 0)
            renderer.showContents(render)
            // spawnと同tickのshowEntityはクライアントのentity tracking登録前に
            // 落ちて内容だけ表示されないことがあるため、翌tickに再送します。
            // revision条件を付けないのは、openChild直後のupdateScreen等でrevisionが
            // 進んでも再送が欠けると表示が確定しないためです。
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                if (sessions[session.ownerId] === session && session.children.contains(child)) {
                    renderer.showContents(render)
                }
            }, 1L)
        }
        return true
    }

    override fun closeChild(ownerId: UUID, screenId: String): Boolean {
        val session = sessions[ownerId] ?: return false
        val index = session.children.indexOfLast { it.view.definition.screenId == screenId }
        if (index < 0) return false
        // 子の上へ積まれたダイアログも同時に閉じ、親子関係のない孤立画面を残しません。
        val targets = session.children.subList(index, session.children.size).toList()
        if (targets.all { it.state == GestureGuiSessionState.CLOSING }) return true
        session.revision = nextRevision++
        Bukkit.getPlayer(ownerId)?.let { playTransitionSound(it, opening = false) }
        targets.forEach { child ->
            if (child.options.animated) animateChildClose(session, child)
            else {
                session.children.remove(child)
                destroyChild(child)
            }
        }
        return true
    }

    override fun close(ownerId: UUID, mode: GestureGuiCloseMode): Boolean {
        val session = sessions[ownerId] ?: return false
        if (mode == GestureGuiCloseMode.IMMEDIATE) {
            notifyClosed(session)
            if (sessions[ownerId] === session) sessions.remove(ownerId)
            destroy(session)
            return true
        }
        if (session.state == GestureGuiSessionState.CLOSING) return true
        session.state = GestureGuiSessionState.CLOSING
        session.revision = nextRevision++
        // 見た目のアニメーション完了ではなく、入力受付を止めた時点で利用側へ通知します。
        // 通知先が同じセッションを閉じ直しても、状態がCLOSINGなので再入しません。
        notifyClosed(session)
        // 見た目の閉じるアニメーション中も入力claimを保持すると、Fキー・
        // チャット等の外部入力が終了済み画面へ届きます。Entityは残しても、
        // 操作主体だけは直ちに無効化します。
        session.actors.keys.toList().forEach { removeActor(session, it) }
        Bukkit.getPlayer(ownerId)?.let { playTransitionSound(it, opening = false) }
        session.children.toList().forEach { child ->
            if (child.options.animated) animateChildClose(session, child)
            else {
                session.children.remove(child)
                destroyChild(child)
            }
        }
        session.screens.forEach { renderer.hideContents(it.render) }
        val expected = session.revision
        session.screens.forEach { screen ->
            renderer.setBackgroundSize(
                screen.render,
                screen.view.panel.width.toFloat(),
                0.1f,
                GestureGuiAnimationTimeline.TRANSITION_TICKS,
            )
        }
        later(GestureGuiAnimationTimeline.CLOSE_TO_POINT_DELAY, session, expected) {
            it.screens.forEach { screen ->
                renderer.setBackgroundSize(
                    screen.render,
                    0.1f,
                    0.1f,
                    GestureGuiAnimationTimeline.TRANSITION_TICKS,
                )
            }
        }
        later(GestureGuiAnimationTimeline.CLOSE_TO_ZERO_DELAY, session, expected) {
            it.screens.forEach { screen ->
                renderer.setBackgroundScaleZero(screen.render, GestureGuiAnimationTimeline.TRANSITION_TICKS)
            }
        }
        later(GestureGuiAnimationTimeline.CLOSE_COMPLETE_DELAY, session, expected) {
            // 閉じる処理の途中で同じ所有者が再オープンされても、旧セッションの
            // 遅延処理が新セッションを削除しないよう、実体を照合してから除去します。
            if (sessions[it.ownerId] === it) sessions.remove(it.ownerId)
            destroy(it)
        }
        return true
    }

    override fun closeExternalDialogIfCurrent(
        ownerId: UUID,
        sessionId: UUID,
        player: Player,
        dialogOwner: String,
        dialogId: String,
    ): Boolean {
        val session = sessions[ownerId] ?: return false
        if (session.id != sessionId || player.uniqueId != ownerId) return false
        return closeExternalDialog(player, dialogOwner, dialogId)
    }

    override fun handleGesture(actor: Player, gesture: GestureGuiGesture): Boolean =
        dispatchGesture(actor, gesture).consumed

    /**
     * 入力イベントの消費とAction実行済みを分離します。
     * ARM_SWINGの時点で視線rayが画面内へ入っていても、表示Entityの更新前などで
     * elementIdが未解決になることがあります。そのイベントは通常操作へ漏らさず、
     * 同じtickのPlayerInteractで再判定できる状態として返します。
     */
    internal fun dispatchGesture(actor: Player, gesture: GestureGuiGesture): GestureGuiDispatchResult {
        val candidate = sessions.values.asSequence()
            .filter { it.state == GestureGuiSessionState.ACTIVE && actor.world.uid == Bukkit.getPlayer(it.ownerId)?.world?.uid }
            .mapNotNull { session -> accessibleTarget(session, actor)?.let { session to it } }
            .minByOrNull { (_, target) -> target.hit.distance }
            ?: return GestureGuiDispatchResult.UNHANDLED
        val (session, target) = candidate
        if (target.blocked) return GestureGuiDispatchResult.CONSUMED
        val view = target.view ?: return GestureGuiDispatchResult.CONSUMED
        val element = view.definition.elements.firstOrNull { it.elementId == target.hit.elementId }
        // 画面内の未割当操作も吸収しますが、Actionは実行しません。
        // ただし同一パケットの後続InteractでEntity位置が確定する可能性があるため、
        // この分岐だけは入力重複抑止の対象にしません。
        if (element == null || gesture !in element.acceptedGestures) {
            return GestureGuiDispatchResult.RETRYABLE_CONSUMED
        }
        if (actor.uniqueId !in session.actors) {
            // claim取得に失敗した場合は、このGUIが入力を所有していません。
            // trueを返すとListenerが元イベントをキャンセルし、claimを所有する
            // 別機能へ入力が届かなくなるため、失敗時は未処理として返します。
            val actorRuntime = runCatching { createActor(session, actor) }.getOrNull()
                ?: return GestureGuiDispatchResult.UNHANDLED
            session.actors[actor.uniqueId] = actorRuntime
        }
        val revision = session.revision
        if (sessions[session.ownerId] !== session || session.state != GestureGuiSessionState.ACTIVE) {
            return GestureGuiDispatchResult.CONSUMED
        }
        // 余白は選択解除用の透過的な入力面であり、ボタン操作音を鳴らしません。
        // 子画面の余白には入力面を置かず、明示的な戻るボタンだけをActionにします。
        if (element.elementId != "viewport-empty") {
            actor.playSound(actor.location, Sound.UI_BUTTON_CLICK, 0.7f, 2.0f)
        }
        view.onAction(
            GestureGuiActionContext(session.ownerId, actor.uniqueId, view.definition.screenId, element.elementId, gesture, revision)
        )
        return GestureGuiDispatchResult.ACTION_HANDLED
    }

    override fun snapshot(ownerId: UUID): GestureGuiSessionSnapshot? = sessions[ownerId]?.let(::snapshot)

    override fun shutdown() {
        tickTask?.cancel()
        tickTask = null
        sessions.values.toList().forEach { session ->
            notifyClosed(session)
            if (sessions[session.ownerId] === session) sessions.remove(session.ownerId)
            destroy(session)
        }
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

    /**
     * Fキーを画面操作として消費できる状態かを、現在の視線で判定します。
     *
     * セッションの存在や入力claimだけでは判定せず、当該プレイヤーのrayが
     * 操作可能な親／子画面へ実際に交差し、到達距離も満たす場合に限定します。
     * これにより、画面を見ていない間は通常のオフハンド切替を妨げません。
     */
    internal fun isLookingAtScreen(player: Player): Boolean =
        sessions.values.asSequence()
            .filter { session ->
                session.state == GestureGuiSessionState.ACTIVE &&
                    player.world.uid == Bukkit.getPlayer(session.ownerId)?.world?.uid
            }
            .any { session -> accessibleTarget(session, player) != null }

    private fun animateOpen(session: Session) {
        val revision = session.revision
        // scale 0をクライアントへ送ってから点へ展開し、点を3 tick保持した後に横・縦の順で広げます。
        later(GestureGuiAnimationTimeline.OPEN_TO_POINT_DELAY, session, revision) {
            it.screens.forEach { screen ->
                renderer.setBackgroundSize(
                    screen.render,
                    0.1f,
                    0.1f,
                    GestureGuiAnimationTimeline.TRANSITION_TICKS,
                )
            }
        }
        later(GestureGuiAnimationTimeline.OPEN_TO_LINE_DELAY, session, revision) {
            it.screens.forEach { screen ->
                renderer.setBackgroundSize(
                    screen.render,
                    screen.view.panel.width.toFloat(),
                    0.1f,
                    GestureGuiAnimationTimeline.TRANSITION_TICKS,
                )
            }
        }
        later(GestureGuiAnimationTimeline.OPEN_TO_FULL_DELAY, session, revision) {
            it.screens.forEach { screen ->
                renderer.setBackgroundSize(
                    screen.render,
                    screen.view.panel.width.toFloat(),
                    screen.view.panel.height.toFloat(),
                    GestureGuiAnimationTimeline.TRANSITION_TICKS,
                )
            }
        }
        later(GestureGuiAnimationTimeline.OPEN_COMPLETE_DELAY, session, revision) {
            it.screens.forEach { screen -> renderer.showContents(screen.render) }
            it.state = GestureGuiSessionState.ACTIVE
        }
    }

    private fun animateChildOpen(session: Session, child: ChildRuntime) {
        laterChild(GestureGuiAnimationTimeline.OPEN_TO_POINT_DELAY, session, child, GestureGuiSessionState.OPENING) {
            renderer.setBackgroundSize(it.render, 0.1f, 0.1f, GestureGuiAnimationTimeline.TRANSITION_TICKS)
        }
        laterChild(GestureGuiAnimationTimeline.OPEN_TO_LINE_DELAY, session, child, GestureGuiSessionState.OPENING) {
            renderer.setBackgroundSize(
                it.render,
                it.view.panel.width.toFloat(),
                0.1f,
                GestureGuiAnimationTimeline.TRANSITION_TICKS,
            )
        }
        laterChild(GestureGuiAnimationTimeline.OPEN_TO_FULL_DELAY, session, child, GestureGuiSessionState.OPENING) {
            renderer.setBackgroundSize(
                it.render,
                it.view.panel.width.toFloat(),
                it.view.panel.height.toFloat(),
                GestureGuiAnimationTimeline.TRANSITION_TICKS,
            )
        }
        laterChild(GestureGuiAnimationTimeline.OPEN_COMPLETE_DELAY, session, child, GestureGuiSessionState.OPENING) {
            renderer.showContents(it.render)
            it.state = GestureGuiSessionState.ACTIVE
        }
    }

    private fun animateChildClose(session: Session, child: ChildRuntime) {
        if (child.state == GestureGuiSessionState.CLOSING) return
        child.state = GestureGuiSessionState.CLOSING
        renderer.hideContents(child.render)
        renderer.setBackgroundSize(
            child.render,
            child.view.panel.width.toFloat(),
            0.1f,
            GestureGuiAnimationTimeline.TRANSITION_TICKS,
        )
        laterChild(GestureGuiAnimationTimeline.CLOSE_TO_POINT_DELAY, session, child, GestureGuiSessionState.CLOSING) {
            renderer.setBackgroundSize(it.render, 0.1f, 0.1f, GestureGuiAnimationTimeline.TRANSITION_TICKS)
        }
        laterChild(GestureGuiAnimationTimeline.CLOSE_TO_ZERO_DELAY, session, child, GestureGuiSessionState.CLOSING) {
            renderer.setBackgroundScaleZero(it.render, GestureGuiAnimationTimeline.TRANSITION_TICKS)
        }
        laterChild(GestureGuiAnimationTimeline.CLOSE_COMPLETE_DELAY, session, child, GestureGuiSessionState.CLOSING) {
            session.children.remove(it)
            destroyChild(it)
        }
    }

    private fun later(delay: Long, session: Session, revision: Long, action: (Session) -> Unit) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (sessions[session.ownerId] === session && session.revision == revision) action(session)
        }, delay)
    }

    private fun laterChild(
        delay: Long,
        session: Session,
        child: ChildRuntime,
        expectedState: GestureGuiSessionState,
        action: (ChildRuntime) -> Unit,
    ) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (
                sessions[session.ownerId] === session &&
                child in session.children &&
                child.state == expectedState
            ) action(child)
        }, delay)
    }

    private fun tick() {
        sessions.values.toList().forEach { session ->
            val owner = Bukkit.getPlayer(session.ownerId)
            if (owner == null || !owner.isOnline) {
                close(session.ownerId, GestureGuiCloseMode.IMMEDIATE)
                return@forEach
            }
            // 固定位置モードではプレイヤー追従せず、open時のposeを維持します。
            if (session.fixedAnchor == null) {
            val insideScreenArea = if (session.screens.size == 1) {
                hit(session, owner, margin = 0.06) != null
            } else {
                GestureGuiGeometry.containsScreenEnvelope(
                    ray(owner).direction,
                    session.retainedYaw.toDouble(),
                    session.screens.size,
                    session.screens.map { it.view.panel.width to it.view.panel.height },
                    session.layout,
                    session.verticalSlots,
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
                val newPoses = parentPoses(session, owner, session.screens.map(ScreenRuntime::view))
                session.screens.zip(newPoses).forEach { (screen, pose) ->
                    screen.pose = pose
                    renderer.updatePose(screen.render, pose, screen.view)
                }
                repositionChildren(session)
                session.anchorX = eye.x
                session.anchorZ = eye.z
                session.appliedYaw = session.retainedYaw
            }
            }
            session.actors[session.ownerId]?.let { actor ->
                // 画面外ではInteractionを視線上に置かない。これにより、パネル外の攻撃が
                // ジェスチャー入力として先取りされることを防ぎます。
                val ownerHit = if (session.state == GestureGuiSessionState.ACTIVE) targetHit(session, owner) else null
                renderer.moveCatcher(actor.catcher, catcherLocation(owner, ownerHit?.hit?.distance ?: 100.0))
                // 開閉アニメーション中は内容より先にホバーだけが現れないよう、操作可能になってから表示します。
                val hoverHit = ownerHit
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
            // 描画の可視性は入力対象の有無から独立させます。従来は視線が画面に
            // 当たったときだけshowToしていたため、PUBLIC画面でも視界へ戻った際に
            // 内容が欠けたり、アクセス変更後の可視状態がtrackingに戻されました。
            // 毎tickのshowToはPaper側で既表示Entityを再送せず、参加・再追跡時だけを
            // 補完するための冪等操作です。
            activeSessions.forEach { session ->
                session.screens
                    .filter { it.view.definition.canOperate(session.ownerId, player.uniqueId) }
                    .forEach { renderer.showTo(it.render, player) }
                session.children
                    .filter { it.view.definition.canOperate(session.ownerId, player.uniqueId) }
                    .forEach { renderer.showTo(it.render, player) }
            }
            val desired = activeSessions.mapNotNull { session ->
                accessibleTarget(session, player)?.let { session to it }
            }.minByOrNull { (_, hit) -> hit.hit.distance }
            activeSessions.forEach { session ->
                if (session !== desired?.first && player.uniqueId in session.actors) removeActor(session, player.uniqueId)
            }
            val (session, hit) = desired ?: return@forEach
            val actor = session.actors[player.uniqueId] ?: runCatching { createActor(session, player) }.getOrNull() ?: return@forEach
            session.screens.filter {
                it.view.definition.canOperate(session.ownerId, player.uniqueId)
            }.forEach { renderer.showTo(it.render, player) }
            session.children.filter {
                it.view.definition.canOperate(session.ownerId, player.uniqueId)
            }.forEach { renderer.showTo(it.render, player) }
            renderer.moveCatcher(actor.catcher, catcherLocation(player, hit.hit.distance))
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
        actor.hoverReplacement?.let { replacement ->
            Bukkit.getPlayer(actorId)?.let { player ->
                renderer.setVisualVisible(replacement.render, replacement.visualId, player, visible = true)
            }
        }
        actor.hover?.let(renderer::removeHover)
        renderer.removeCatcher(actor.catcher)
    }

    private fun updateHover(
        session: Session,
        actor: ActorRuntime,
        player: Player,
        target: TargetHit?,
    ) {
        val view = target?.view
        val pose = target?.pose
        val element = view?.definition?.elements?.firstOrNull { it.elementId == target.hit.elementId }
        val hoverText = element?.hoverText
        val identity = if (view != null && element != null && hoverText != null) {
            "${view.definition.screenId}:${element.elementId}"
        } else null
        if (identity == null) {
            actor.hoverReplacement?.let { replacement ->
                renderer.setVisualVisible(replacement.render, replacement.visualId, player, visible = true)
            }
            actor.hoverReplacement = null
            actor.hover?.let(renderer::removeHover)
            actor.hover = null
            actor.hoverIdentity = null
            return
        }
        val render = target?.child?.render ?: target?.screen?.render
        val desiredReplacement = hoverText?.replacesVisualId?.let { visualId ->
            render?.let { HoverReplacement(it, visualId) }
        }
        if (actor.hoverReplacement != desiredReplacement) {
            actor.hoverReplacement?.let { replacement ->
                renderer.setVisualVisible(replacement.render, replacement.visualId, player, visible = true)
            }
            actor.hoverReplacement = null
            desiredReplacement?.let { replacement ->
                if (renderer.setVisualVisible(replacement.render, replacement.visualId, player, visible = false)) {
                    actor.hoverReplacement = replacement
                }
            }
        }
        if (actor.hoverIdentity != identity || actor.hover == null) {
            actor.hover?.let(renderer::removeHover)
            actor.hover = renderer.spawnHover(player, session.id, session.revision, pose!!, hoverText!!)
            actor.hoverIdentity = identity
        } else {
            renderer.updateHover(actor.hover!!, pose!!, hoverText!!)
        }
    }

    private fun destroy(session: Session) {
        session.actors.keys.toList().forEach { removeActor(session, it) }
        session.screens.forEach { renderer.remove(it.render) }
        session.children.forEach(::destroyChild)
    }

    /**
     * 利用側の状態をCC-Systemのセッション終了へ接続します。
     * コールバックの例外でEntity・claimの解放を中断しないよう、ここで隔離します。
     */
    private fun notifyClosed(session: Session) {
        if (session.closeNotified) return
        session.closeNotified = true
        session.sessionListener?.let { listener ->
            runCatching { listener.onClosed(session.ownerId, session.id) }
                .onFailure { failure ->
                    plugin.logger.log(
                        Level.WARNING,
                        "Gesture GUI終了通知に失敗しました: owner=${session.ownerId} session=${session.id}",
                        failure,
                    )
                }
        }
    }

    private fun destroyChild(child: ChildRuntime) {
        renderer.remove(child.render)
        child.overlay?.remove()
    }

    private fun hit(session: Session, player: Player, margin: Double = 0.0): TargetHit? =
        targetHit(session, player, margin)

    private fun targetHit(session: Session, player: Player, margin: Double = 0.0): TargetHit? {
        val ray = ray(player)
        fun childHit(child: ChildRuntime): TargetHit? = child.takeIf {
            it.state == GestureGuiSessionState.ACTIVE
        }?.let { activeChild -> GestureGuiGeometry.hitTest(
            ray,
            listOf(activeChild.pose.copy(width = activeChild.pose.width + margin * 2, height = activeChild.pose.height + margin * 2) to activeChild.view.definition),
        )?.let { TargetHit(null, activeChild, it) } }
        fun parentHit(): TargetHit? = session.screens.mapNotNull { screen ->
            GestureGuiGeometry.hitTest(
                ray,
                listOf(screen.pose.copy(width = screen.pose.width + margin * 2, height = screen.pose.height + margin * 2) to screen.view.definition),
            )?.let { TargetHit(screen, null, it) }
        }.minByOrNull { it.hit.distance }

        session.children.asReversed().forEachIndexed { reversedIndex, child ->
            childHit(child)?.let { return it }
            if (!child.options.allowParentInteraction) {
                val lowerChildren = session.children.dropLast(reversedIndex + 1).asReversed()
                val covered = lowerChildren.firstNotNullOfOrNull(::childHit) ?: parentHit()
                if (covered != null) return covered.copy(blocked = true)
                return null
            }
        }
        return parentHit()
    }

    private fun accessibleTarget(session: Session, player: Player): TargetHit? {
        val target = targetHit(session, player) ?: return null
        val targetAllowed = target.view?.definition?.canOperate(session.ownerId, player.uniqueId) == true
        if (target.blocked) {
            // 遮蔽対象（親または下位子画面）を操作できない第三者の入力までは奪いません。
            if (!targetAllowed) return null
        } else if (!targetAllowed) {
            val child = target.child ?: return null
            if (child.options.allowParentInteraction) return null
            val parentAllowed = parentRuntime(session, child.options.parentScreenId)
                ?.view?.definition?.canOperate(session.ownerId, player.uniqueId) == true
            if (!parentAllowed) return null
            return validateReach(session, player, target.copy(blocked = true))
        }
        return validateReach(session, player, target)
    }

    private fun validateReach(session: Session, player: Player, target: TargetHit): TargetHit? {
        val maximum = (player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.value ?: 3.0).coerceAtLeast(1.0)
        if (target.hit.distance > maximum) return null
        val eye = player.eyeLocation
        val block = player.world.rayTraceBlocks(
            eye,
            eye.direction,
            target.hit.distance,
            FluidCollisionMode.NEVER,
            true,
        )
        val blockDistance = block?.hitPosition?.distance(eye.toVector())
        return target.takeIf { blockDistance == null || blockDistance + 0.01 >= target.hit.distance }
    }

    private fun ray(player: Player): GestureGuiRay {
        val eye = player.eyeLocation
        val direction = eye.direction
        return GestureGuiRay(
            GestureGuiVector3(eye.x, eye.y, eye.z),
            GestureGuiVector3(direction.x, direction.y, direction.z),
        )
    }

    private fun poses(
        player: Player,
        yaw: Float,
        views: List<GestureGuiView>,
        layout: GestureGuiScreenLayout = GestureGuiScreenLayout.VERTICAL,
        verticalSlots: List<GestureGuiVerticalSlot>? = null,
    ) = GestureGuiGeometry.poses(
        GestureGuiVector3(player.eyeLocation.x, player.eyeLocation.y, player.eyeLocation.z),
        yaw.toDouble(),
        views.size,
        views.map { it.panel.width to it.panel.height },
        layout,
        verticalSlots,
    )

    /** 子画面を含む親解決結果です。親は必ず直近の同一IDを一つだけ返します。 */
    private data class ParentRuntime(
        val view: GestureGuiView,
        val pose: GestureGuiScreenPose,
    )

    private fun parentRuntime(session: Session, screenId: String): ParentRuntime? =
        session.children.asReversed().firstOrNull { it.view.definition.screenId == screenId }?.let {
            ParentRuntime(it.view, it.pose)
        } ?: session.screens.firstOrNull { it.view.definition.screenId == screenId }?.let {
            ParentRuntime(it.view, it.pose)
        }

    /** 親画面の寸法変更時に、動的画面は全体の上下配置も再計算します。 */
    private fun parentPoses(
        session: Session,
        owner: Player,
        views: List<GestureGuiView>,
    ): List<GestureGuiScreenPose> = if (session.fixedAnchor == null) {
        poses(owner, session.retainedYaw, views, session.layout, session.verticalSlots)
    } else {
        // 固定位置画面はワールド向きの正面向中心を保持し、パネル寸法変更のみ反映します。
        session.screens.mapIndexed { index, screen ->
            screen.pose.copy(width = views[index].panel.width, height = views[index].panel.height)
        }
    }

    private fun repositionChildren(session: Session) {
        session.children.forEachIndexed { index, child ->
            val parent = parentRuntime(session, child.options.parentScreenId) ?: return@forEachIndexed
            child.pose = childPose(parent.pose, child.view, child.options, session.screens.size + index, index)
            renderer.updatePose(child.render, child.pose, child.view)
            child.overlay?.let { renderer.updateModalOverlay(it, modalOverlayPose(parent.pose, index)) }
        }
    }

    /** 固定アンカーからプレイヤー目線方向を向く画面poseを生成します。CC-System APIのみで配置します。 */
    private fun fixedPoses(
        anchor: Location,
        eye: Location,
        views: List<GestureGuiView>,
        layout: GestureGuiScreenLayout = GestureGuiScreenLayout.VERTICAL,
        verticalSlots: List<GestureGuiVerticalSlot>? = null,
    ): List<GestureGuiScreenPose> {
        val anchorVec = GestureGuiVector3(anchor.x, anchor.y, anchor.z)
        val eyeVec = GestureGuiVector3(eye.x, eye.y, eye.z)
        // eye -> anchor 逆向き法線とします（eyeから見て正面）。
        // 水平法線はI選び直しAPI側で固定されるため、姿勢はこのワールドyawから算出して上下に積みます。
        val normal = (anchorVec - eyeVec).normalized()
        val yaw = Math.toDegrees(atan2(-normal.x, normal.z))
        val syntheticEye = anchorVec - normal * FIXED_SCREEN_DISTANCE
        val poses = GestureGuiGeometry.poses(
            syntheticEye,
            yaw,
            views.size,
            views.map { it.panel.width to it.panel.height },
            layout,
            verticalSlots,
        )
        // ブロックの上方へ持ち上げる（全画面共通）
        if (FIXED_SCREEN_LIFT == 0.0) return poses
        val liftUp = poses.firstOrNull()?.up ?: GestureGuiVector3(0.0, 1.0, 0.0)
        return poses.map { it.copy(center = it.center + liftUp * FIXED_SCREEN_LIFT) }
    }

    private fun childPose(
        parent: GestureGuiScreenPose,
        view: GestureGuiView,
        options: GestureGuiChildOptions,
        stackIndex: Int,
        childIndex: Int,
    ): GestureGuiScreenPose = parent.copy(
        screenIndex = stackIndex,
        center = parent.center + parent.right * options.offsetX + parent.up * options.offsetY -
            parent.normal * (CHILD_SCREEN_DEPTH + childIndex * CHILD_STACK_DEPTH),
        width = view.panel.width,
        height = view.panel.height,
    )

    private fun modalOverlayPose(parent: GestureGuiScreenPose, childIndex: Int): GestureGuiScreenPose = parent.copy(
        center = parent.center - parent.normal * (childIndex * CHILD_STACK_DEPTH),
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
        session.children.map { it.view.definition.screenId },
        session.verticalSlots,
    )

    private companion object {
        const val MAX_YAW_STEP = 8.0f
        const val YAW_TARGET_EPSILON = 0.05f
        const val CHILD_SCREEN_DEPTH = 0.25
        // 通常要素の最大40 layer（0.2 block）より広く取り、次の遮蔽が必ず前面へ来るようにします。
        const val CHILD_STACK_DEPTH = 0.25
        const val MAX_CHILD_DEPTH = 3
        /** 固定位置モードで、アンカーから画面中心までの距離 */
        const val FIXED_SCREEN_DISTANCE: Double = 1.2
        /** 固定位置モードで、画面をアンカーからどれだけ持ち上げるか */
        const val FIXED_SCREEN_LIFT: Double = 0.4
    }
}
