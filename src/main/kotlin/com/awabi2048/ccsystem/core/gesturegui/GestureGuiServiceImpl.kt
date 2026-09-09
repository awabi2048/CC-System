package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.entity.SystemEntityRegistry
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiActionContext
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiCloseMode
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiCloseReason
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
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVisual
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
    private val systemEntityRegistry: SystemEntityRegistry,
) : GestureGuiService {
    private data class ActorRuntime(
        val playerId: UUID,
        val claims: List<PlayerInteractionClaim>,
        val catcher: GestureGuiEntityRenderer.CatcherHandle,
        var hoverIdentity: String? = null,
        var hoverScreenKey: String? = null,
    )

    private data class ScreenRuntime(
        val view: GestureGuiView,
        var pose: GestureGuiScreenPose,
    )

    private data class ChildRuntime(
        val view: GestureGuiView,
        val options: GestureGuiChildOptions,
        var pose: GestureGuiScreenPose,
        /** モーダル遮蔽面の素材。null なら遮蔽面なし。 */
        val overlayMaterial: Material?,
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
        /** 視線が画面外にある状態の連続tick数。短い視線移動では画面を回転させません。 */
        var gazeOutsideTicks: Int,
        /**
         * 最後に移動を検出したサービスtick番号です。停止確定
         * ([GestureGuiFollowPolicy.STOP_SETTLE_TICKS]無移動継続)の基準に使います。
         * 一度も動いていないセッションは-1のままにし、再召喚しません。
         */
        var lastMotionTick: Long = -1L,
        /**
         * 前tickに観測した所有者の目位置です。移動判定は適用済み位置ではなく、
         * 必ずこの毎tick更新値との差で求めます。
         * 適用済み位置を基準にすると、一度動いた後に静止しても差が残り続けて
         * 永久に移動扱いになり、停止確定へ到達できなくなります。
         */
        var lastEyeX: Double,
        var lastEyeY: Double,
        var lastEyeZ: Double,
        /**
         * 前回確定した追従基準位置です。停止確定時の再召喚は、この位置からの
         * 変位が[GestureGuiFollowPolicy.RESUMMON_MIN_DISTANCE]以上の場合だけ
         * 実体を作り直します。未満の場合は基準位置の更新に留め、ちらつきを防ぎます。
         */
        var lastAppliedX: Double,
        var lastAppliedY: Double,
        var lastAppliedZ: Double,
        /**
         * 凍結中に確定待ちの移動があるかです。停止確定時に一度だけ再召喚し、
         * 直後にfalseへ戻すことで、停止中の再召喚の繰り返しを防ぎます。
         */
        var followDirty: Boolean = false,
        var screens: List<ScreenRuntime>,
        val children: MutableList<ChildRuntime>,
        val actors: MutableMap<UUID, ActorRuntime>,
        /** セッション中に一度でも参加したUUID。Dialog掃除を権限剥奪後も可能にします。 */
        val knownActorIds: MutableSet<UUID>,
        /** 固定位置モードのアンカー。nullならプレイヤー追従 */
        var fixedAnchor: Location? = null,
        /** 固定後の画面pose。refresh時もプレイヤー位置から再計算せず、この姿勢を引き継ぎます。 */
        var fixedPoseSnapshot: List<GestureGuiScreenPose>? = null,
        /** 利用側のローカル状態を終了通知へ接続するリスナー */
        val sessionListener: GestureGuiSessionListener? = null,
        /** close通知を一度だけ送るための状態 */
        var closeNotified: Boolean = false,
        /** 主要画面の並び方向。pose再計算（updateScreen等）でも同じ配置を維持します */
        val layout: GestureGuiScreenLayout = GestureGuiScreenLayout.VERTICAL,
        /** 縦配置で使用する実体スロット。欠けたスロットは生成しません。 */
        val verticalSlots: List<GestureGuiVerticalSlot>? = null,
        /** Inventory GUIとは独立したInteraction入力の右クリック許可設定です。 */
        val secondaryInputEnabled: Boolean = true,
        /** 画面外を含むワールド左右クリックの外部操作を吸収する設定です。 */
        val suppressWorldClicks: Boolean = false,
        /** 追従pose全体へ加算するY方向の補正です(ブロック単位)。 */
        val verticalOffset: Double = 0.0,
        /** 画面の上下傾き倍率です。1.0で従来配置、小さくするほど垂直に近づきます。 */
        val tiltScale: Double = 1.0,
        /** 追従表示で目位置から画面中心までの距離です(ブロック単位)。 */
        val screenDistance: Double,
        /**
         * 移動中に本体を隠してダミーパネルで追従しているかです。
         * 本体の view・pose は保持したまま描画だけを差し替え、停止確定後に
         * 再召喚またはそのまま復帰させます。ダミーは viewer 状態側で管理します。
         */
        var dummyActive: Boolean = false,
    )

    private val renderer = GestureGuiEntityRenderer(plugin, systemEntityRegistry)
    /**
     * 仮想描画 backend 群です。Bukkit Entity を描画に使わず、viewer ごとに
     * Spawn/Metadata/Destroy packet だけを送ります。catcher だけは Bukkit のままです。
     */
    private val virtualBackend = GestureGuiProtocolLibBackend(plugin)
    private val virtualScreens = GestureGuiVirtualScreens(virtualBackend)
    /** (sessionId, viewerId) ごとの描画済み状態です。transition 時のみ packet を送ります。 */
    private val viewerStates = mutableMapOf<Pair<UUID, UUID>, GestureViewerRenderState>()
    private val registeredOwners = mutableSetOf<UUID>()
    private val sessions = mutableMapOf<UUID, Session>()
    private var nextRevision = 1L
    /** 追従の約10Hz間引きの基準になるサービスtick番号です。 */
    private var tickIndex: Long = 0L
    private var tickTask: BukkitTask? = Bukkit.getScheduler().runTaskTimer(plugin, Runnable(::tick), 1L, 1L)

    override fun registerOwner(ownerId: UUID) {
        registeredOwners += ownerId
    }
    /**
     * 視線依存処理（catcher 追従・hover・外部 actor 照合）の実行間隔です。
     * 毎 tick ではなく 5Hz（4 tick ごと）を基準とし、state transition 時は
     * [requestGazeUpdate] で待ちなしに即時実行します。
     */
    private var gazeDirty: Boolean = true

    override fun unregisterOwner(ownerId: UUID) {
        registeredOwners -= ownerId
        close(ownerId, GestureGuiCloseMode.IMMEDIATE)
    }

    override fun open(owner: Player, views: List<GestureGuiView>): GestureGuiSessionSnapshot =
        open(owner, views, GestureGuiOpenOptions())

    override fun open(owner: Player, views: List<GestureGuiView>, options: GestureGuiOpenOptions): GestureGuiSessionSnapshot =
        openInternal(owner, views, options)

    /** 公開openとrefreshで、固定画面の現在poseを引き継ぐための内部入口です。 */
    private fun openInternal(
        owner: Player,
        views: List<GestureGuiView>,
        options: GestureGuiOpenOptions,
        fixedPoseOverride: List<GestureGuiScreenPose>? = null,
    ): GestureGuiSessionSnapshot {
        require(views.size in 1..3) { "gesture GUI requires one to three screens" }
        require(views.map { it.definition.screenId }.distinct().size == views.size) {
            "gesture GUI screenId must be unique within a session"
        }
        require(options.verticalSlots == null || options.verticalSlots.size == views.size) {
            "gesture GUI vertical slot count must match screen count"
        }
        check(owner.isOnline) { "gesture GUI owner must be online" }
        close(owner.uniqueId, GestureGuiCloseMode.IMMEDIATE)
        // 公開Gesture画面の第三者として参加中の場合、その画面の入力claimを
        // 持ったまま新しい所有者画面を開くとPRIMARYが競合します。所有者画面は
        // 閉じても別セッションのactorまでは閉じないため、開くプレイヤーだけを
        // 他セッションから離脱させ、公開画面自体は維持します。
        sessions.values.toList()
            .filter { it.ownerId != owner.uniqueId && owner.uniqueId in it.actors }
            .forEach { removeActor(it, owner.uniqueId) }
        registeredOwners += owner.uniqueId

        val revision = nextRevision++
        val id = UUID.randomUUID()
        val anchor = options.anchor
        // 内容更新時のpose維持と合わせ、再生成でも旧poseを引き継げるようにします。
        // 移動はtick再召喚だけが担い、明示操作・更新で画面が逃げないようにします。
        val poseOverride = fixedPoseOverride
            ?.takeIf { it.size == views.size }
            ?.mapIndexed { index, pose ->
                // 更新後のパネル寸法だけは新しいviewへ反映し、world上の中心・向きは
                // 引き継ぎ元のposeから維持します。
                pose.copy(width = views[index].panel.width, height = views[index].panel.height)
            }
        val poses = if (anchor != null) {
            poseOverride
                ?: fixedPoses(anchor, owner.eyeLocation, views, options.layout, options.verticalSlots)
        } else {
            poseOverride
                ?: poses(owner, owner.location.yaw, views, options.layout, options.verticalSlots, options.verticalOffset, options.tiltScale, options.screenDistance)
        }
        val screens = mutableListOf<ScreenRuntime>()
        var session: Session? = null
        try {
            // 仮想描画では open 時に Bukkit Entity を作りません。論理 view・pose だけを保持し、
            // 実 packet は次の gaze 通過で viewer 状態ごとに生成します。
            views.zip(poses).forEach { (view, pose) ->
                screens += ScreenRuntime(view, pose)
            }
            session = Session(
                id = id,
                ownerId = owner.uniqueId,
                revision = revision,
                state = GestureGuiSessionState.OPENING,
                retainedYaw = owner.location.yaw,
                targetYaw = null,
                gazeOutsideTicks = 0,
                lastEyeX = owner.eyeLocation.x,
                lastEyeY = owner.eyeLocation.y,
                lastEyeZ = owner.eyeLocation.z,
                lastAppliedX = owner.eyeLocation.x,
                lastAppliedY = owner.eyeLocation.y,
                lastAppliedZ = owner.eyeLocation.z,
                screens = screens.toList(),
                children = mutableListOf(),
                actors = mutableMapOf(),
                knownActorIds = mutableSetOf(owner.uniqueId),
                fixedAnchor = anchor,
                fixedPoseSnapshot = anchor?.let { poses.toList() },
                sessionListener = options.sessionListener,
                layout = options.layout,
                verticalSlots = options.verticalSlots,
                secondaryInputEnabled = options.secondaryInputEnabled,
                suppressWorldClicks = options.suppressWorldClicks,
                verticalOffset = options.verticalOffset,
                tiltScale = options.tiltScale,
                screenDistance = options.screenDistance,
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
            }
            throw failure
        }
        val openedSession = requireNotNull(session)
        // 開いた直後の 5Hz 待ちで入力入口が遅れないよう、次 tick で gaze を即時実行します。
        requestGazeUpdate()
        return snapshot(openedSession)
    }

    override fun updateScreen(ownerId: UUID, view: GestureGuiView): Boolean {
        val session = sessions[ownerId]?.takeIf { it.state == GestureGuiSessionState.ACTIVE } ?: return false
        Bukkit.getPlayer(ownerId)?.takeIf(Player::isOnline) ?: return false
        // ダミー表示中の内容変更は本体へ戻してから適用し、ダミー形状との不整合を残しません。
        // 移動が継続していれば次tickのゲート判定で必要に応じてダミーへ戻ります。
        if (session.dummyActive) restoreMainFromDummy(session)
        val targetIndex = session.screens.indexOfFirst { it.view.definition.screenId == view.definition.screenId }
        if (targetIndex >= 0) {
            val oldScreens = session.screens
            val newViews = oldScreens.mapIndexed { index, screen -> if (index == targetIndex) view else screen.view }
            // 内容更新ではposeを動かしません。移動はtick再召喚だけに一本化し、
            // 画面内注視＋接近後のクリックで画面が逃げる挙動を防ぎます。
            // 画面外クリック時も即時再配置せず、停止確定後のtickに委ねます。
            // 寸法変更時は中心・向きを維持したまま幅・高さだけ反映します。
            val newPoses = oldScreens.mapIndexed { index, screen ->
                screen.pose.copy(width = newViews[index].panel.width, height = newViews[index].panel.height)
            }
            session.revision = nextRevision++
            session.screens = oldScreens.mapIndexed { index, screen ->
                screen.copy(view = newViews[index], pose = newPoses[index])
            }
            if (session.fixedAnchor != null) {
                // 固定後の画面を更新しても、次回refreshの基準は更新後の現在poseです。
                session.fixedPoseSnapshot = session.screens.map(ScreenRuntime::pose)
            }
            repositionChildren(session)
            GestureGuiRenderMetrics.logicalViewUpdates.incrementAndGet()
            requestGazeUpdate()
            return true
        }
        val targetChild = session.children.firstOrNull { it.view.definition.screenId == view.definition.screenId }
        if (targetChild != null) {
            session.revision = nextRevision++
            val pose = targetChild.pose.copy(width = view.panel.width, height = view.panel.height)
            val idx = session.children.indexOf(targetChild)
            session.children[idx] = targetChild.copy(view = view, pose = pose)
            // 子画面自身の寸法が変わると、その前面に積まれた確認子画面や
            // モーダルオーバーレイの基準位置も変わります。親画面更新時と同じ
            // 再配置経路を通し、孫画面まで古いposeを残さないようにします。
            repositionChildren(session)
            GestureGuiRenderMetrics.logicalViewUpdates.incrementAndGet()
            requestGazeUpdate()
            return true
        }
        return false
    }

    override fun pinToCurrentPosition(ownerId: UUID): Boolean {
        val session = sessions[ownerId]?.takeIf { it.state == GestureGuiSessionState.ACTIVE } ?: return false
        if (!GestureGuiClipTogglePolicy.canPin(session.state, session.fixedAnchor)) return false
        val owner = Bukkit.getPlayer(ownerId)?.takeIf(Player::isOnline) ?: return false
        // ダミー表示中の固定は本体へ戻してから行い、固定poseがダミーを指さないようにします。
        if (session.dummyActive) restoreMainFromDummy(session)
        // 既存のscreen.poseを変更せず、次のtickから追従分岐だけを止めます。
        // fixedPoses()で再計算しないため、ボタンを押した瞬間の表示位置を正確に保持できます。
        session.fixedAnchor = owner.eyeLocation.clone()
        session.fixedPoseSnapshot = session.screens.map(ScreenRuntime::pose)
        session.targetYaw = null
        session.gazeOutsideTicks = 0
        requestGazeUpdate()
        return true
    }

    override fun unpinToFollow(ownerId: UUID): Boolean {
        val session = sessions[ownerId]?.takeIf { it.state == GestureGuiSessionState.ACTIVE } ?: return false
        if (!GestureGuiClipTogglePolicy.canUnpin(session.state, session.fixedAnchor)) return false
        val owner = Bukkit.getPlayer(ownerId)?.takeIf(Player::isOnline) ?: return false
        // pin側と対称に、解除前にダミーを本体へ戻します。通常フローでは
        // 固定中にダミーは開始されないため到達しませんが、不変条件が崩れた
        // 場合にダミー残留＋dirty解消の組合せを作らないための防御です。
        if (session.dummyActive) {
            restoreMainFromDummy(session)
        }
        session.fixedAnchor = null
        session.fixedPoseSnapshot = null
        session.targetYaw = null
        session.gazeOutsideTicks = 0
        // 固定中は姿勢(rotYaw)も固定なので、解除ではyawを変えずに現在の目位置へ
        // poseを即座に再計算します。回転ジャンプを起こさず、画面だけが追従位置へ
        // 復帰します。以降のtickは通常の追従分岐へ戻ります。
        val eye = owner.eyeLocation
        session.lastEyeX = eye.x
        session.lastEyeY = eye.y
        session.lastEyeZ = eye.z
        session.lastAppliedX = eye.x
        session.lastAppliedY = eye.y
        session.lastAppliedZ = eye.z
        val newPoses = parentPoses(session, owner, session.screens.map(ScreenRuntime::view))
        session.screens.zip(newPoses).forEach { (screen, pose) ->
            screen.pose = pose
        }
        repositionChildren(session)
        // 解除直後の追従状態を確定済みとして扱い、次tickの停止判定へ引き継ぎます。
        session.lastMotionTick = tickIndex
        session.followDirty = false
        requestGazeUpdate()
        return true
    }

    /**
     * 停止確定時に、論理 pose をその場へ確定させます。
     *
     * 仮想描画では実体の作り直しが不要なため、pose オブジェクトの更新だけを行い、
     * viewer ごとの destroy+spawn は次の gaze 通過の差分検出に委ねます。
     * 向きは現在の視線yawへ正対させます。成功時のみtrueを返します。
     */
    private fun resummonFollowScreens(session: Session, owner: Player): Boolean {
        // 現在の視線へ正対させ、以降は動くまで完全に固定します。
        session.retainedYaw = owner.location.yaw
        session.targetYaw = null
        session.gazeOutsideTicks = 0
        session.revision = nextRevision++
        // ダミー表示中の再召喚では、ダミーを先に破棄して本体の作り直しへ一本化します。
        if (session.dummyActive) {
            session.dummyActive = false
        }
        val eye = owner.eyeLocation
        val newPoses = try {
            parentPoses(session, owner, session.screens.map(ScreenRuntime::view))
        } catch (failure: Throwable) {
            plugin.logger.log(Level.WARNING, "Gesture GUI停止時再召喚のpose計算に失敗しました", failure)
            return false
        }
        try {
            val newScreens = session.screens.mapIndexed { index, screen ->
                screen.copy(pose = newPoses[index])
            }
            val newChildren = session.children.mapIndexed { index, child ->
                // 親が見つからない子は旧来のrepositionChildrenと同様に維持します。
                val parent = newScreens.firstOrNull {
                    it.view.definition.screenId == child.options.parentScreenId
                } ?: return@mapIndexed child
                val newPose = childPose(
                    parent.pose,
                    child.view,
                    child.options,
                    newScreens.size + index,
                    index,
                )
                child.copy(pose = newPose)
            }
            session.screens = newScreens
            newChildren.forEachIndexed { index, child -> session.children[index] = child }
            // ホバーは古いposeに対応するため捨て、次の gaze 通過で新poseへ作り直させます。
            session.actors.values.forEach { actor ->
                actor.hoverIdentity = null
                actor.hoverScreenKey = null
            }
            // viewer 状態の hover 除去は gaze 通過の destroyHover 経路へ委ねます。
            session.lastEyeX = eye.x
            session.lastEyeY = eye.y
            session.lastEyeZ = eye.z
            session.lastAppliedX = eye.x
            session.lastAppliedY = eye.y
            session.lastAppliedZ = eye.z
            session.lastMotionTick = tickIndex
            session.followDirty = false
            requestGazeUpdate()
            return true
        } catch (failure: Throwable) {
            plugin.logger.log(Level.WARNING, "Gesture GUI停止時再召喚に失敗しました", failure)
            return false
        }
    }

    /**
     * 移動中に本体表示をダミーパネルへ切り替えます。
     *
     * 本体の view・pose は保持したまま描画だけを差し替え、同じ外形の空背景を
     * 次の gaze 通過で viewer ごとに送ります。以降の移動中はダミーだけが 5Hz で
     * 追従し、本体への入力は targetHit 関門で無効化されます。
     * 開始には停止時再召喚と同一のゲート通過を要求し、小さな揺れやセクタ内の間は
     * 従来どおり凍結を維持します。
     */
    private fun maybeStartDummyFollow(session: Session, owner: Player, eye: Location) {
        if (session.dummyActive) return
        if (session.state != GestureGuiSessionState.ACTIVE || session.fixedAnchor != null) return
        if (session.screens.isEmpty()) return
        if (!GestureGuiFollowPolicy.shouldStartDummyFollow(
                isGazeInsideScreen(session, owner),
                eye.x - session.lastAppliedX,
                eye.y - session.lastAppliedY,
                eye.z - session.lastAppliedZ,
            )
        ) return
        // ホバーは古いposeに対応するため捨て、次の gaze 通過で作り直させます。
        session.actors.values.forEach { actor ->
            actor.hoverIdentity = null
            actor.hoverScreenKey = null
        }
        session.dummyActive = true
        requestGazeUpdate()
    }

    /**
     * 移動中のダミーパネル追従は gaze 通過の描画同期へ委ねます。
     *
     * 仮想ダミーは viewer 状態の差分検出で 5Hz 更新されるため、ここでは何もしません。
     * 向きは停止時の再召喚（現在yawへ正対）で確定します。
     */
    private fun updateDummyFollow(session: Session, owner: Player) {
        if (!session.dummyActive || session.fixedAnchor != null) return
        // pose は gaze 通過時に求め直すため、ここでは保持しません。
    }

    /**
     * ダミーを破棄して本体の表示を復帰させます。
     *
     * 本体は view・pose を保持したままのため、フラグを戻すだけで復帰します。
     * viewer 側のダミー破棄・本体再送は次の gaze 通過の差分検出に委ねます。
     */
    private fun restoreMainFromDummy(session: Session) {
        if (!session.dummyActive) return
        session.dummyActive = false
        requestGazeUpdate()
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
            secondaryInputEnabled = old.secondaryInputEnabled,
            suppressWorldClicks = old.suppressWorldClicks,
            verticalOffset = old.verticalOffset,
            tiltScale = old.tiltScale,
            screenDistance = old.screenDistance,
        )
        notifyClosed(old)
        if (sessions[ownerId] === old) sessions.remove(ownerId)
        destroy(old)
        val opened = openInternal(
            owner,
            views,
            options,
            // 追従中も旧poseを引き継ぎます。移動はtick再召喚に委ねます。
            fixedPoseOverride = old.fixedAnchor?.let {
                old.fixedPoseSnapshot ?: old.screens.map(ScreenRuntime::pose)
            } ?: old.screens.map(ScreenRuntime::pose),
        )
        val current = sessions[ownerId] ?: return false
        actors.asSequence()
            .filter { it != ownerId }
            .mapNotNull(Bukkit::getPlayer)
            .filter(Player::isOnline)
            .filter { actor -> views.any { view -> view.definition.canOperate(ownerId, actor.uniqueId) } }
            .forEach { actor ->
                runCatching {
                    current.actors[actor.uniqueId] = createActor(current, actor)
                    current.knownActorIds += actor.uniqueId
                }
        }
        // refresh は openInternal 経由で gaze 更新を予約済みですが、actor 再生成後も
        // 次 tick で照合が走るよう明示します。
        requestGazeUpdate()
        return opened.ownerId == ownerId
    }

    override fun openChild(ownerId: UUID, view: GestureGuiView, options: GestureGuiChildOptions): Boolean {
        val session = sessions[ownerId]?.takeIf { it.state == GestureGuiSessionState.ACTIVE } ?: return false
        val owner = Bukkit.getPlayer(ownerId)?.takeIf(Player::isOnline) ?: return false
        // ダミー表示中の子画面追加は本体へ戻してから行い、親pose基準のずれを防ぎます。
        if (session.dummyActive) restoreMainFromDummy(session)
        if (session.children.size >= MAX_CHILD_DEPTH) return false
        val parent = parentRuntime(session, options.parentScreenId) ?: return false
        require(session.screens.none { it.view.definition.screenId == view.definition.screenId } &&
            session.children.none { it.view.definition.screenId == view.definition.screenId }) {
            "gesture GUI screenId must be unique within a session"
        }
        val childIndex = session.children.size
        session.revision = nextRevision++
        val pose = childPose(parent.pose, view, options, session.screens.size + childIndex, childIndex)
        // 仮想描画では子画面も論理 view・pose だけを保持し、遮蔽面の素材だけ記録します。
        // 実 packet は次の gaze 通過で viewer ごとに生成します。
        val child = ChildRuntime(
            view, options, pose,
            if (!options.allowParentInteraction) options.overlayMaterial ?: Material.GRAY_STAINED_GLASS else null,
            if (options.animated) GestureGuiSessionState.OPENING else GestureGuiSessionState.ACTIVE,
        )
        session.children += child
        if (options.animated) {
            animateChildOpen(session, child)
        }
        requestGazeUpdate()
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
                destroyChild(session, child)
            }
        }
        requestGazeUpdate()
        return true
    }

    override fun close(ownerId: UUID, mode: GestureGuiCloseMode): Boolean {
        val session = sessions[ownerId] ?: return false
        // 仮想描画では閉じるアニメーションを行わず、即時に破棄します。
        // viewer 側の virtual entity 破棄は destroy 経路で行い、残留させません。
        // mode 引数は API 互換のために残し、動作は統一します。
        if (session.dummyActive) {
            session.dummyActive = false
        }
        notifyClosed(session)
        if (sessions[ownerId] === session) sessions.remove(ownerId)
        destroy(session)
        requestGazeUpdate()
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
        if (session.id != sessionId || player.uniqueId !in session.knownActorIds) return false
        return closeExternalDialog(player, dialogOwner, dialogId)
    }

    override fun handleGesture(actor: Player, gesture: GestureGuiGesture): Boolean =
        dispatchGesture(actor, gesture).consumed

    override fun leave(actorId: UUID): Boolean = leaveOrClose(actorId)

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
        if (!session.secondaryInputEnabled &&
            gesture in setOf(GestureGuiGesture.SECONDARY, GestureGuiGesture.SHIFT_SECONDARY)
        ) {
            // 右クリックは入力として吸収しますが、Inventory GUIのクリック仕様とは無関係に
            // Interaction由来のActionだけを無効化します。
            return GestureGuiDispatchResult.CONSUMED
        }
        if (target.blocked) return GestureGuiDispatchResult.CONSUMED
        val view = target.view ?: return GestureGuiDispatchResult.CONSUMED
        val element = view.definition.elements.firstOrNull { it.elementId == target.hit.elementId }
        // 画面内の未割当操作も吸収しますが、Actionは実行しません。
        // ただし同一パケットの後続InteractでEntity位置が確定する可能性があるため、
        // この分岐だけは入力重複抑止の対象にしません。
        if (element == null || !element.acceptsGesture(actor, gesture)) {
            return GestureGuiDispatchResult.RETRYABLE_CONSUMED
        }
        if (actor.uniqueId !in session.actors) {
            // claim取得に失敗した場合は、このGUIが入力を所有していません。
            // trueを返すとListenerが元イベントをキャンセルし、claimを所有する
            // 別機能へ入力が届かなくなるため、失敗時は未処理として返します。
            val actorRuntime = runCatching { createActor(session, actor) }.getOrNull()
                ?: return GestureGuiDispatchResult.UNHANDLED
            session.actors[actor.uniqueId] = actorRuntime
            session.knownActorIds += actor.uniqueId
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
        // Action 実行で内容が変わるため、次 tick で gaze（hover・差分）を即時更新します。
        requestGazeUpdate()
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
        requestGazeUpdate()
    }

    /** Shift+Jumpでは所有者は画面全体、第三者は自身の操作参加だけを終了します。 */
    internal fun leaveOrClose(actorId: UUID): Boolean {
        if (actorId in sessions) return close(actorId)
        val session = sessions.values.firstOrNull { actorId in it.actors } ?: return false
        removeActor(session, actorId)
        requestGazeUpdate()
        return true
    }

    /** 画面を閉じる入力の直前に、利用側の実行状態を停止できる通知を送ります。 */
    internal fun notifyCloseRequested(actorId: UUID, reason: GestureGuiCloseReason): Boolean {
        val session = sessions.values.firstOrNull {
            actorId == it.ownerId || actorId in it.actors
        } ?: return false
        session.sessionListener?.let { listener ->
            runCatching {
                listener.onCloseRequested(session.ownerId, session.id, actorId, reason)
            }.onFailure { failure ->
                plugin.logger.log(
                    Level.WARNING,
                    "Gesture GUI終了要求通知に失敗しました: owner=${session.ownerId} session=${session.id}",
                    failure,
                )
            }
        }
        return true
    }

    internal fun ownsCatcher(entity: org.bukkit.entity.Entity, playerId: UUID? = null): Boolean =
        renderer.ownsCatcher(entity, playerId)

    /** KantanCommander等がInteractionの右クリックを無効化するためのセッション単位判定です。 */
    internal fun isSecondaryInputDisabled(player: Player): Boolean =
        sessions.values.any { session ->
            !session.secondaryInputEnabled && isScreenInputActive(session, player)
        }

    /**
     * 外部ブロック／エンティティ操作を漏らさないためのセッション単位の入力遮断です。
     * 開幕アニメーション中もcatcherを生成済みなので、ACTIVE待ちの1 tickを無防備にしません。
     */
    internal fun isWorldClickSuppressed(player: Player): Boolean =
        sessions.values.any { session ->
            session.suppressWorldClicks && isScreenInputActive(session, player)
        }

    /**
     * スニーク右クリックだけは視線判定を待たずに消費します。
     *
     * 追従中に画面とのray交差が一瞬外れても、Kantanの編集画面を開いている間は
     * スニーク右クリックをバニラ配置や外部プラグインへ渡さない契約を維持します。
     */
    internal fun isSneakRightClickSuppressed(player: Player): Boolean =
        sessions.values.any { session ->
            val participating = player.uniqueId == session.ownerId || player.uniqueId in session.actors
            val sameWorldAsOwner = Bukkit.getPlayer(session.ownerId)?.world?.uid == player.world.uid
            sameWorldAsOwner && GestureGuiInputCapturePolicy.isSneakSecondarySuppressed(
                state = session.state,
                participating = participating,
                sneaking = player.isSneaking,
                secondaryInputEnabled = session.secondaryInputEnabled,
            )
        }

    /**
     * Shift+Jumpは画面を見失っていても、参加中のセッションの終了操作として扱います。
     * 通常クリックの視線条件は維持し、終了専用の入力だけを別契約にします。
     */
    internal fun isCloseGestureActive(player: Player): Boolean =
        sessions.values.any { session ->
            val participating = player.uniqueId == session.ownerId || player.uniqueId in session.actors
            val sameWorldAsOwner = Bukkit.getPlayer(session.ownerId)?.world?.uid == player.world.uid
            sameWorldAsOwner && GestureGuiInputCapturePolicy.isCloseGestureActive(session.state, participating)
        }

    /**
     * 現在の入力をGesture GUIへ渡せるかを返します。
     *
     * セッションに参加しているだけでは入力を奪いません。ACTIVE中は同じ視線判定を
     * dispatchGestureと共有し、画面から視線を外したクリックを通常ワールドへ戻します。
     */
    internal fun isScreenInputActive(player: Player): Boolean =
        sessions.values.any { session -> isScreenInputActive(session, player) }

    private fun isScreenInputActive(session: Session, player: Player): Boolean {
        val participating = player.uniqueId == session.ownerId || player.uniqueId in session.actors
        val sameWorldAsOwner = Bukkit.getPlayer(session.ownerId)?.world?.uid == player.world.uid
        val lookingAtScreen = session.state == GestureGuiSessionState.ACTIVE &&
            sameWorldAsOwner &&
            accessibleTarget(session, player) != null
        return GestureGuiInputCapturePolicy.isActive(session.state, participating, lookingAtScreen)
    }

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

    /**
     * 開幕状態を ACTIVE へ遷移させます。
     *
     * 仮想描画では scale アニメーションを行わず、gaze 通過で実寸のまま送ります。
     * 入力入口の維持期間だけ OPENING 状態を保ちます。
     */
    private fun animateOpen(session: Session) {
        val revision = session.revision
        later(GestureGuiAnimationTimeline.OPEN_COMPLETE_DELAY, session, revision) {
            it.state = GestureGuiSessionState.ACTIVE
            requestGazeUpdate()
        }
    }

    private fun animateChildOpen(session: Session, child: ChildRuntime) {
        laterChild(GestureGuiAnimationTimeline.OPEN_COMPLETE_DELAY, session, child, GestureGuiSessionState.OPENING) {
            it.state = GestureGuiSessionState.ACTIVE
            requestGazeUpdate()
        }
    }

    private fun animateChildClose(session: Session, child: ChildRuntime) {
        if (child.state == GestureGuiSessionState.CLOSING) return
        child.state = GestureGuiSessionState.CLOSING
        laterChild(GestureGuiAnimationTimeline.CLOSE_COMPLETE_DELAY, session, child, GestureGuiSessionState.CLOSING) {
            session.children.remove(it)
            destroyChild(session, it)
            requestGazeUpdate()
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

    /**
     * 次のサービス tick で視線依存処理を即時実行します。
     *
     * session open/close・world 変化・teleport・GUI 有効/無効切替・操作権限喪失等の
     * 明確な state transition 用であり、通常の視線追跡・hover 更新は 5Hz 周期に従います。
     */
    private fun requestGazeUpdate() {
        gazeDirty = true
    }

    private fun tick() {
        tickIndex++
        GestureGuiRenderMetrics.tickCount.incrementAndGet()
        sessions.values.toList().forEach { session -> tickFollow(session) }
        // 視線依存処理は 5Hz（4 tick ごと）を基準とし、transition 時のみ即時実行します。
        // player×session の全量 visibility 照合・catcher/teleport・hover 更新を
        // 毎 tick 行わないことで、session 数の増加に対する負荷拡大を抑えます。
        if (gazeDirty || tickIndex % GAZE_INTERVAL_TICKS == 0L) {
            gazeDirty = false
            // 1 session の失敗で他 session の gaze を止めないよう分離します。
            runCatching(::gazePass).onFailure { failure ->
                plugin.logger.log(Level.WARNING, "Gesture GUI の gaze 通過に失敗しました", failure)
            }
        }
    }

    /** 追従判定だけを毎 tick 行います。描画・入力の entity 操作は含みません。 */
    private fun tickFollow(session: Session) {
        val owner = Bukkit.getPlayer(session.ownerId)
        if (owner == null || !owner.isOnline) {
            close(session.ownerId, GestureGuiCloseMode.IMMEDIATE)
            return
        }
            // 固定位置モードではプレイヤー追従せず、open時のposeを維持します。
            // 固定中にダミーが残存するはずはありませんが、残っていた場合は
            // 本体へ戻して表示不整合を自己回復させます。
            if (session.fixedAnchor != null) {
                if (session.dummyActive) {
                    restoreMainFromDummy(session)
                }
            } else {
                // 視線再調整は停止時再召喚へ統合したため無効化しています。
                // 移動中はposeを一切更新せず凍結し、停止確定後に再召喚で確定させます。
                // 移動中の毎tick teleportが背景と内容物の適用時刻差でティアを招くため、
                // 追従の滑らかさより停止時の位置正確さを優先します。
                val eye = owner.eyeLocation
                // 移動判定はPosition専用です。前tick観測値との差で求め、
                // 適用済み位置を基準にすると静止後も差が残り続けるため使いません。
                // 再召喚時にはその時の目線へ正対させるため、向きは別途扱います。
                val moved = GestureGuiFollowPolicy.isSignificantMotion(
                    eye.x - session.lastEyeX,
                    eye.y - session.lastEyeY,
                    eye.z - session.lastEyeZ,
                )
                session.lastEyeX = eye.x
                session.lastEyeY = eye.y
                session.lastEyeZ = eye.z
                when (GestureGuiFollowPolicy.decideFollowMotion(tickIndex, session.lastMotionTick, moved)) {
                    GestureGuiFollowPolicy.FollowMotionState.MOVING -> {
                        session.lastMotionTick = tickIndex
                        session.followDirty = true
                        // ゲートを通過した場合だけ本体をダミーへ切り替えます。
                        // 小さな揺れやセクタ内の間は従来どおり凍結を維持します。
                        maybeStartDummyFollow(session, owner, eye)
                        if (session.dummyActive) updateDummyFollow(session, owner)
                    }
                    GestureGuiFollowPolicy.FollowMotionState.SETTLING -> {
                        // 停止確定前は何もしません。次tickへ判定を持ち越します。
                    }
                    GestureGuiFollowPolicy.FollowMotionState.STOPPED -> {
                        if (session.followDirty && session.state == GestureGuiSessionState.ACTIVE) {
                            // ダミー表示中は旧セクタを破棄し、変位だけで再召喚／復元を
                            // 決めます。旧セクタは移動前の凍結poseを指すため、戻って
                            // 停止した際にセクタ内凍結へ吸い込まれるとダミーのまま
                            // 残留します。非ダミー時のみセクタ内凍結を維持します。
                            // 明示操作・内容変更・ホバーはこの制限の対象外です。
                            when (
                                GestureGuiFollowPolicy.decideStopAction(
                                    session.dummyActive,
                                    isGazeInsideScreen(session, owner),
                                    eye.x - session.lastAppliedX,
                                    eye.y - session.lastAppliedY,
                                    eye.z - session.lastAppliedZ,
                                )
                            ) {
                                GestureGuiFollowPolicy.StopAction.FREEZE -> {
                                    // 操作者が球面セクタ内にいる間は一切の再描画を行わず、
                                    // 位置を固定します。dirtyは維持するため、セクタ外へ出た
                                    // 時点で再召喚が走ります。
                                }
                                GestureGuiFollowPolicy.StopAction.RESUMMON -> {
                                    resummonFollowScreens(session, owner)
                                }
                                GestureGuiFollowPolicy.StopAction.RESTORE -> {
                                    // 前回確定位置からの変位が閾値未満の場合は実体を
                                    // 作り直さず、基準位置だけを更新して確定済みにします。
                                    // ダミー表示中（一旦遠ざかって戻った場合を含む）は本体へ戻します。
                                    if (session.dummyActive) {
                                        restoreMainFromDummy(session)
                                    }
                                    session.lastAppliedX = eye.x
                                    session.lastAppliedY = eye.y
                                    session.lastAppliedZ = eye.z
                                    session.followDirty = false
                                }
                            }
                        }
                    }
                }
            }
    }

    /**
     * 視線依存処理（所有者 actor・hover・外部 actor 照合）を 5Hz で行います。
     *
     * 毎 tick の全量 visibility 照合・catcher teleport・hover 更新をやめ、
     * gaze 周期または state transition 時のみ実行します。hit-test 対象は
     * この pass 内に限定し、tick 駆動の無条件走査を行いません。
     */
    private fun gazePass() {
        sessions.values.toList().forEach { session ->
            val owner = Bukkit.getPlayer(session.ownerId) ?: return@forEach
            if (!owner.isOnline) return@forEach
            if (session.state == GestureGuiSessionState.ACTIVE) {
                if (session.dummyActive) {
                    // ダミー表示中は本体の古いposeへの誤操作を防ぐため、入力を
                    // 無効化します。targetHit 関門でも遮断するため、ここでは
                    // 所有者の catcher を確実に除去するだけに留めます。
                    removeActor(session, session.ownerId)
                } else {
                    // 視線・距離・遮蔽のいずれかで操作対象でなくなった間はInteraction自体を
                    // 削除します。残したまま視点だけを追従させると、画面を見ていない、または
                    // 遠すぎるクリックまでEntity操作として扱われ、外部エンティティの操作経路と
                    // 競合します。条件が戻ったtickで再生成します。画面の表示可否は別途
                    // GestureGuiVisibilityPolicyで判定するため、遠距離でも画面自体は残ります。
                    val ownerHit = accessibleTarget(session, owner)
                    if (ownerHit == null) {
                        removeActor(session, session.ownerId)
                    } else {
                        val actor = getOrCreateActor(session, owner)
                        actor?.let {
                            renderer.moveCatcher(it.catcher, catcherLocation(owner))
                            GestureGuiRenderMetrics.catcherMoves.incrementAndGet()
                            updateHover(session, it, owner, ownerHit)
                        }
                    }
                }
            } else {
                // 開閉アニメーション中は入口を維持し、ワールド操作が漏れないようにします。
                session.actors[session.ownerId]?.let { actor ->
                    renderer.moveCatcher(actor.catcher, catcherLocation(owner))
                    GestureGuiRenderMetrics.catcherMoves.incrementAndGet()
                    updateHover(session, actor, owner, null)
                }
            }
        }
        reconcileExternalActors()
    }

    /**
     * PUBLIC/ALLOWLIST画面は最初のクリックより前にInteractionを用意します。
     * 複数画面が重なる場合も、一人の入力は最寄りの一セッションだけが所有します。
     *
     * 描画の可視性は入力対象の有無から独立させ、(session, viewer) の LOD 状態で
     * transition 時のみ packet を送ります。hit-test は actor 解決に限定し、
     * 全 session の無条件走査は行いません。
     */
    private fun reconcileExternalActors() {
        val activeSessions = sessions.values.filter { it.state == GestureGuiSessionState.ACTIVE }
        Bukkit.getOnlinePlayers().forEach { player ->
            activeSessions.forEach { session ->
                // 候補評価数を計測し、player×session の走査規模を可視化します。
                GestureGuiRenderMetrics.candidateEvaluations.incrementAndGet()
                syncSessionForViewer(session, player)
            }
            // 自分が別のGestureセッションを所有している場合は、同じ入力claimを
            // 二つのセッションへ同時に渡さず、ここから先のactor選択だけを省略します。
            // 画面の可視性は上で個別に反映済みです。
            if (player.uniqueId in sessions) return@forEach
            val desired = activeSessions.mapNotNull { session ->
                // ダミー表示中のセッションは入力を受け付けないため、claim 対象から外します。
                if (session.dummyActive) null
                else accessibleTarget(session, player)?.let { session to it }
            }.minByOrNull { (_, hit) -> hit.hit.distance }
            activeSessions.forEach { session ->
                if (session !== desired?.first && player.uniqueId in session.actors) removeActor(session, player.uniqueId)
            }
            val (session, hit) = desired ?: return@forEach
            val actor = getOrCreateActor(session, player) ?: return@forEach
            renderer.moveCatcher(actor.catcher, catcherLocation(player))
            GestureGuiRenderMetrics.catcherMoves.incrementAndGet()
            updateHover(session, actor, player, hit)
        }
        activeSessions.forEach { session ->
            session.actors.keys.filter { actorId ->
                actorId != session.ownerId && Bukkit.getPlayer(actorId)?.isOnline != true
            }.forEach { removeActor(session, it) }
        }
        // 切断済み viewer の追跡は捨てます。client 側は切断で破棄されるため送信不要です。
        viewerStates.keys.filter { (_, viewerId) ->
            Bukkit.getPlayer(viewerId)?.isOnline != true
        }.forEach { viewerStates.remove(it) }
    }

    /**
     * 1 セッション分の viewer 表示を LOD に合わせて同期します。
     *
     * LOD は (session, viewer) 単位で、最近画面までの距離二乗・操作可否・可視性から求めます。
     * 無変化では packet を送らず、遷移時は不足分・過剰分だけを spawn/destroy します。
     */
    private fun syncSessionForViewer(session: Session, player: Player) {
        // backend 利用不可時は送信を止めます（初回に一度だけ SEVERE を出します）。
        if (!virtualBackend.checkAvailable(player)) return
        val state = viewerStates.getOrPut(session.id to player.uniqueId) { GestureViewerRenderState() }
        val owner = Bukkit.getPlayer(session.ownerId)
        if (owner == null || !owner.isOnline || player.world.uid != owner.world.uid) {
            if (state.visibilityLevel != GestureViewerLod.HIDDEN) {
                GestureGuiRenderMetrics.lodTransitions.incrementAndGet()
            }
            state.visibilityLevel = GestureViewerLod.HIDDEN
            destroySessionScreens(player, state, session)
            return
        }
        val eye = ray(player).origin
        var minDistanceSquared = Double.POSITIVE_INFINITY
        var anyVisible = false
        var anyOperable = false
        session.screens.forEach { screen ->
            minDistanceSquared = minOf(minDistanceSquared, distanceSquared(eye, screen.pose.center))
            if (screen.view.definition.canView(session.ownerId, player.uniqueId)) anyVisible = true
            if (screen.view.definition.canOperate(session.ownerId, player.uniqueId)) anyOperable = true
        }
        session.children.forEach { child ->
            minDistanceSquared = minOf(minDistanceSquared, distanceSquared(eye, child.pose.center))
            if (child.view.definition.canView(session.ownerId, player.uniqueId)) anyVisible = true
            if (child.view.definition.canOperate(session.ownerId, player.uniqueId)) anyOperable = true
        }
        val lod = GestureViewerLodPolicy.resolve(state.visibilityLevel, minDistanceSquared, anyOperable, anyVisible)
        if (lod != state.visibilityLevel) {
            GestureGuiRenderMetrics.lodTransitions.incrementAndGet()
            state.visibilityLevel = lod
        }
        if (lod == GestureViewerLod.HIDDEN) {
            destroySessionScreens(player, state, session)
            return
        }
        if (session.dummyActive) {
            // ダミー追従中は本体を送らず、現在位置の背景だけ送ります。子画面は隠します。
            val dummyPoses = livePoses(session, owner) ?: return
            session.screens.forEachIndexed { index, screen ->
                val pose = dummyPoses.getOrNull(index) ?: screen.pose
                virtualScreens.syncScreen(
                    player, session.id, state, screenKey(screen), screen.view, pose, lod, null,
                    GestureGuiVirtualScreens.DummyRequest(screen.view.panel.width, screen.view.panel.height),
                )
            }
            session.children.forEach { child ->
                val key = screenKey(child)
                virtualScreens.destroyScreenKeys(player, state, key)
                virtualScreens.destroyHover(player, state, key)
                destroyOverlayBlock(player, state, key)
            }
        } else {
            session.screens.forEach { screen ->
                virtualScreens.syncScreen(
                    player, session.id, state, screenKey(screen), screen.view, screen.pose, lod, null, null,
                )
            }
            session.children.forEachIndexed { index, child ->
                val overlayMaterial = child.overlayMaterial?.takeIf { !child.options.allowParentInteraction }
                if (overlayMaterial != null) {
                    val parent = parentRuntime(session, child.options.parentScreenId)
                    val overlayPose = if (parent != null) modalOverlayPose(parent.pose, index) else child.pose
                    syncOverlayBlock(player, state, screenKey(child), overlayMaterial, overlayPose)
                } else {
                    destroyOverlayBlock(player, state, screenKey(child))
                }
                virtualScreens.syncScreen(
                    player, session.id, state, screenKey(child), child.view, child.pose, lod, null, null,
                )
            }
        }
        state.screenRevision = session.revision
    }

    /** セッション配下の全画面キー・hover を viewer から破棄します。 */
    private fun destroySessionScreens(player: Player, state: GestureViewerRenderState, session: Session) {
        session.screens.forEach { screen ->
            val key = screenKey(screen)
            virtualScreens.destroyScreenKeys(player, state, key)
            virtualScreens.destroyHover(player, state, key)
        }
        session.children.forEach { child ->
            val key = screenKey(child)
            virtualScreens.destroyScreenKeys(player, state, key)
            virtualScreens.destroyHover(player, state, key)
            destroyOverlayBlock(player, state, key)
        }
    }

    /** モーダル遮蔽面の単一 Block を同期します。画面キーとは別キーで追跡します。 */
    private fun syncOverlayBlock(
        player: Player,
        state: GestureViewerRenderState,
        childKey: String,
        material: Material,
        pose: GestureGuiScreenPose,
    ) {
        virtualScreens.syncSingleBlock(
            player, state, "$childKey/overlay", pose,
            0.0, 0.0, GestureGuiVirtualScreens.MODAL_OVERLAY_LAYER,
            pose.width, pose.height, material,
        )
    }

    private fun destroyOverlayBlock(player: Player, state: GestureViewerRenderState, childKey: String) {
        virtualScreens.destroySingleBlock(player, state, "$childKey/overlay")
    }

    /** ダミー追従用の現在 pose 群です。計算失敗時は null を返し、凍結を維持します。 */
    private fun livePoses(session: Session, owner: Player): List<GestureGuiScreenPose>? {
        val views = session.screens.map(ScreenRuntime::view)
        if (views.isEmpty()) return null
        return runCatching {
            poses(
                owner, owner.location.yaw, views, session.layout, session.verticalSlots,
                session.verticalOffset, session.tiltScale, session.screenDistance,
            )
        }.onFailure { failure ->
            plugin.logger.log(Level.WARNING, "Gesture GUIダミーposeの計算に失敗しました", failure)
        }.getOrNull()
    }

    private fun distanceSquared(a: GestureGuiVector3, b: GestureGuiVector3): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return dx * dx + dy * dy + dz * dz
    }

    private fun createActor(session: Session, player: Player): ActorRuntime {
        val owner = "gesture-gui:${session.id}"
        val claims = mutableListOf<PlayerInteractionClaim>()
        try {
            PlayerInteractionChannel.entries.forEach { channel ->
                claims += claimInputChannel(player.uniqueId, channel, owner)
                    ?: throw IllegalStateException(
                        "gesture GUI could not claim $channel for ${player.uniqueId}; " +
                            "currentOwner=${claimService.ownerOf(player.uniqueId, channel)}",
                    )
            }
        } catch (failure: Throwable) {
            claims.forEach(PlayerInteractionClaim::close)
            throw failure
        }
        return try {
            ActorRuntime(
                player.uniqueId,
                claims,
                renderer.spawnCatcher(
                    player,
                    session.id,
                    session.revision,
                    catcherLocation(player),
                    responsive = session.secondaryInputEnabled,
                ),
            )
        } catch (failure: Throwable) {
            claims.forEach(PlayerInteractionClaim::close)
            throw failure
        }
    }

    /**
     * Actorを生成したら必ずセッションへ登録します。
     *
     * 公開画面の第三者と、所有者が視線を画面外から戻した場合は毎tickこの経路を
     * 通ります。登録を呼び出し側へ委ねると、生成済みのclaim・catcher・hoverが
     * Session.actorsから漏れ、次tickに重複生成されて終了時にも回収できません。
     */
    private fun getOrCreateActor(session: Session, player: Player): ActorRuntime? {
        session.actors[player.uniqueId]?.let { return it }
        val created = runCatching { createActor(session, player) }.getOrNull() ?: return null
        session.actors[player.uniqueId] = created
        session.knownActorIds += player.uniqueId
        return created
    }

    /**
     * 入力claimの取得を一箇所へ集約します。
     *
     * Gesture GUIはセッション終了時にclaimを解放しますが、プラグインの途中再起動や
     * 生成途中の失敗では、セッションMapに存在しない `gesture-gui:<UUID>` だけが残る
     * 可能性があります。その所有者を無条件に奪うと公開画面の第三者入力を壊すため、
     * 現在のセッションMapに実体がないUUIDだけを所有者指定で回収して一度だけ再試行します。
     */
    private fun claimInputChannel(
        playerId: UUID,
        channel: PlayerInteractionChannel,
        owner: String,
    ): PlayerInteractionClaim? {
        claimService.claim(playerId, channel, owner)?.let { return it }
        val currentOwner = claimService.ownerOf(playerId, channel) ?: return null
        if (!currentOwner.startsWith(GESTURE_OWNER_PREFIX)) return null
        val sessionId = currentOwner.removePrefix(GESTURE_OWNER_PREFIX)
            .let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return null
        val liveSession = sessions.values.any { session ->
            session.id == sessionId &&
                session.state != GestureGuiSessionState.CLOSING &&
                (session.ownerId == playerId || playerId in session.actors)
        }
        if (liveSession) return null
        claimService.releaseAll(playerId, currentOwner)
        return claimService.claim(playerId, channel, owner)
    }

    private fun removeActor(session: Session, actorId: UUID) {
        val actor = session.actors.remove(actorId) ?: return
        actor.claims.forEach(PlayerInteractionClaim::close)
        // hover 表示の除去と置換非表示の復元は viewer 状態側で行います。
        // 公開画面の閲覧だけが残る場合、次 gaze 通過で隠れていた内容を再送します。
        Bukkit.getPlayer(actorId)?.let { player ->
            viewerStates[session.id to actorId]?.let { state ->
                session.screens.forEach { screen ->
                    virtualScreens.destroyHover(player, state, screenKey(screen))
                }
                session.children.forEach { child ->
                    virtualScreens.destroyHover(player, state, screenKey(child))
                }
                state.hiddenVisualIds.clear()
                state.hiddenVisualBodyIds.clear()
            }
        }
        renderer.removeCatcher(actor.catcher)
        requestGazeUpdate()
    }

    private fun updateHover(
        session: Session,
        actor: ActorRuntime,
        player: Player,
        target: TargetHit?,
    ) {
        val state = viewerStates.getOrPut(session.id to player.uniqueId) { GestureViewerRenderState() }
        val view = target?.view
        val pose = target?.pose
        val element = view?.definition?.elements?.firstOrNull { it.elementId == target.hit.elementId }
        val hoverText = element?.hoverText
        val identity = if (view != null && element != null && hoverText != null) {
            "${view.definition.screenId}:${element.elementId}"
        } else null
        val screenKey = target?.child?.let(::screenKey) ?: target?.screen?.let(::screenKey)
        if (identity == null || screenKey == null || pose == null) {
            // hover 対象なし：前回の hover 表示を捨て、置換非表示を復元します。
            // 内容の再送は次 gaze 通過の画面同期に委ねます。
            actor.hoverScreenKey?.let { virtualScreens.destroyHover(player, state, it) }
            if (state.hiddenVisualIds.isNotEmpty() || state.hiddenVisualBodyIds.isNotEmpty()) {
                state.hiddenVisualIds.clear()
                state.hiddenVisualBodyIds.clear()
                requestGazeUpdate()
            }
            actor.hoverIdentity = null
            actor.hoverScreenKey = null
            return
        }
        if (actor.hoverScreenKey != null && actor.hoverScreenKey != screenKey) {
            virtualScreens.destroyHover(player, state, actor.hoverScreenKey!!)
        }
        // backend 利用不可時は hover を出さず、置換も行いません。
        if (!virtualBackend.checkAvailable(player)) {
            actor.hoverIdentity = null
            actor.hoverScreenKey = null
            return
        }
        // identity が非 null の場合、hoverText も非 null が確定しています。
        val confirmedHover = requireNotNull(hoverText)
        val descriptionVisualIds = buildSet<String> {
            confirmedHover.replacesVisualId?.let(::add)
        }
        val bodyVisualIds = buildSet<String> {
            confirmedHover.hoverBlockVisualId?.let(::add)
        } - descriptionVisualIds
        // 置換対象の変化は画面同期の desired 集合を変えるため、次通過で差分配信します。
        if (state.hiddenVisualIds != descriptionVisualIds || state.hiddenVisualBodyIds != bodyVisualIds) {
            state.hiddenVisualIds.clear()
            state.hiddenVisualIds += descriptionVisualIds
            state.hiddenVisualBodyIds.clear()
            state.hiddenVisualBodyIds += bodyVisualIds
            requestGazeUpdate()
        }
        // 置換対象の通常visualと深さを揃えます。対象を操作者へ隠したうえで
        // 0.02ブロックだけ前面へ浮かせることで、ホバー中の法線方向の跳ねを最小化し、
        // 層の重なり規則（前面に浮く）も維持します。対象を解決できない場合は
        // 従来どおりhover.layerを使います。
        val effectiveHover = confirmedHover.let { currentHover ->
            val descriptionVisualId = currentHover.replacesVisualId
            if (descriptionVisualId != null) {
                view.visuals.firstOrNull { it.visualId == descriptionVisualId }
                    ?.let { base -> currentHover.copy(layer = GestureGuiVirtualScreens.hoverReplaceLayer(base.layer)) }
                    ?: currentHover
            } else {
                currentHover
            }
        }
        val hoverVisual = confirmedHover.hoverBlockVisualId?.let { visualId ->
            view.visuals.firstOrNull { it.visualId == visualId } as? GestureGuiVisual.Block
        }
        val hoverBlockData = confirmedHover.hoverBlockData
        val hoverBlock = if (hoverVisual != null && hoverBlockData != null) hoverVisual to hoverBlockData else null
        virtualScreens.syncHover(player, state, screenKey, pose, effectiveHover, hoverBlock, identity)
        actor.hoverIdentity = identity
        actor.hoverScreenKey = screenKey
    }

    private fun destroy(session: Session) {
        session.actors.keys.toList().forEach { removeActor(session, it) }
        session.dummyActive = false
        // viewer 状態に残る virtual entity を破棄し、追跡も捨てます。
        // 切断済み viewer の client 側は破棄済みのため、online のみ送信します。
        viewerStates.keys.filter { it.first == session.id }.toList().forEach { key ->
            Bukkit.getPlayer(key.second)?.let { player ->
                viewerStates[key]?.let { virtualScreens.destroyAll(player, it) }
            }
            viewerStates.remove(key)
        }
        // catcher の登録漏れ等があっても、セッションID単位で最後に掃除します。
        renderer.removeSessionEntities(session.id)
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

    /**
     * 子画面の viewer 側表示を破棄します。
     *
     * 論理リストからの除去は呼び出し側で行い、ここでは全 viewer の
     * 当該画面キー・hover を破棄します。
     */
    private fun destroyChild(session: Session, child: ChildRuntime) {
        val key = screenKey(child)
        viewerStates.filter { it.key.first == session.id }.forEach { (viewerKey, state) ->
            Bukkit.getPlayer(viewerKey.second)?.let { player ->
                virtualScreens.destroyScreenKeys(player, state, key)
                virtualScreens.destroyHover(player, state, key)
            }
        }
    }

    /**
     * 操作者が画面正面の球面セクタ内にいるかを返します。
     *
     * 停止確定後の再召喚ゲートとダミー開始ゲートで共有します。頂点＝凍結中心・
     * 軸＝操作者側（−normal）・半角60度・半径＝操作可能距離のセクタに
     * いずれかの親画面で入っていれば内側とします。視線方向は使わないため、
     * 目の前まで近づいて大きく下を向いても画面は動きません。
     * 当たり判定・包絡は位置更新条件に含めません。
     * ホバー・キャッチャー・明示操作の判定には影響しません。
     */
    private fun isGazeInsideScreen(session: Session, owner: Player): Boolean {
        val eye = ray(owner).origin
        val range = operableRange(owner)
        return session.screens.any { screen ->
            val offset = eye - screen.pose.center
            val distance = offset.length()
            if (distance > range) return@any false
            // normalは視点から画面へ向く定義のため、操作者側の軸は反転します。
            val axis = screen.pose.normal * -1.0
            GestureGuiFollowPolicy.isInsideCone(
                offset.x, offset.y, offset.z, axis.x, axis.y, axis.z,
            )
        }
    }

    private fun targetHit(session: Session, player: Player, margin: Double = 0.0): TargetHit? {
        // ダミー表示中は本体の古いposeへの入力を無効化します。dispatch・視線・
        // 距離の全経路がこの関門を通るため、操作・ホバー・catcher を一括で止められます。
        if (session.dummyActive) return null
        GestureGuiRenderMetrics.hitTests.incrementAndGet()
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
        val maximum = operableRange(player)
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

    /**
     * プレイヤーが画面を操作できる距離です(ブロック単位)。
     * 入力到達判定と視線ゲートで同じ ENTITY_INTERACTION_RANGE 基準を使い、
     * 表示と入力の距離条件を一致させます。
     */
    private fun operableRange(player: Player): Double =
        (player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.value ?: 3.0).coerceAtLeast(1.0)

    private fun ray(player: Player): GestureGuiRay {        val eye = player.eyeLocation
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
        verticalOffset: Double = 0.0,
        tiltScale: Double = 1.0,
        screenDistance: Double = GestureGuiOpenOptions.DEFAULT_SCREEN_DISTANCE,
    ) = GestureGuiGeometry.poses(
        GestureGuiVector3(player.eyeLocation.x, player.eyeLocation.y, player.eyeLocation.z),
        yaw.toDouble(),
        views.size,
        views.map { it.panel.width to it.panel.height },
        layout,
        verticalSlots,
        tiltScale,
        screenDistance,
    ).map { pose ->
        if (verticalOffset == 0.0) pose
        else pose.copy(center = pose.center + GestureGuiVector3(0.0, verticalOffset, 0.0))
    }

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
        poses(
            owner,
            session.retainedYaw,
            views,
            session.layout,
            session.verticalSlots,
            session.verticalOffset,
            session.tiltScale,
            session.screenDistance,
        )
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
        }
    }

    /** 画面の安定キーです。viewer 状態の virtual ID 追跡に使います。 */
    private fun screenKey(screen: ScreenRuntime): String = "s:${screen.view.definition.screenId}"

    /** 子画面の安定キーです。viewer 状態の virtual ID 追跡に使います。 */
    private fun screenKey(child: ChildRuntime): String = "c:${child.view.definition.screenId}"

    /**
     * 固定アンカーから画面poseを生成します。
     *
     * 固定配置の画面は、開いた瞬間のブロック基準高さを維持する必要があります。
     * そのため、プレイヤーの目とアンカーのY差を画面法線へ含めず、XZ平面の方向だけで
     * 向きを決めます。上下方向の視線に依存すると、同じブロックを別の高さから開いた
     * ときに画面全体が上下へ移動し、表示高さの契約が崩れます。
     */
    private fun fixedPoses(
        anchor: Location,
        eye: Location,
        views: List<GestureGuiView>,
        layout: GestureGuiScreenLayout = GestureGuiScreenLayout.VERTICAL,
        verticalSlots: List<GestureGuiVerticalSlot>? = null,
    ): List<GestureGuiScreenPose> {
        val anchorVec = GestureGuiVector3(anchor.x, anchor.y, anchor.z)
        val eyeVec = GestureGuiVector3(eye.x, eye.y, eye.z)
        val horizontal = GestureGuiVector3(anchorVec.x - eyeVec.x, 0.0, anchorVec.z - eyeVec.z)
        val normal = if (horizontal.length() > 1.0e-9) {
            horizontal.normalized()
        } else {
            // 真上／真下から開いた場合はXZ方向が定まらないため、開いた時の
            // プレイヤーyawをフォールバックとして使い、姿勢の不定性をなくします。
            val yaw = Math.toRadians(eye.yaw.toDouble())
            GestureGuiVector3(-kotlin.math.sin(yaw), 0.0, kotlin.math.cos(yaw))
        }
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

    /** InteractionのLocationは底面基準なので、ヒットボックス中央が目位置へ来るよう補正します。 */
    private fun catcherLocation(player: Player): Location =
        player.eyeLocation.clone().subtract(0.0, GESTURE_CATCHER_SIZE / 2.0, 0.0)


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
        const val GESTURE_OWNER_PREFIX = "gesture-gui:"
        const val CHILD_SCREEN_DEPTH = 0.25
        // 通常要素の最大40 layer（0.2 block）より広く取り、次の遮蔽が必ず前面へ来るようにします。
        const val CHILD_STACK_DEPTH = 0.25
        const val MAX_CHILD_DEPTH = 3
        /** 視線依存処理の実行間隔（tick）です。4 tick = 約5Hz です。 */
        const val GAZE_INTERVAL_TICKS: Long = 4L
        /** 固定位置モードで、アンカーから画面中心までの距離 */
        const val FIXED_SCREEN_DISTANCE: Double = 1.2
        /** 固定位置モードで、画面をアンカーからどれだけ持ち上げるか */
        const val FIXED_SCREEN_LIFT: Double = 0.4
    }
}
