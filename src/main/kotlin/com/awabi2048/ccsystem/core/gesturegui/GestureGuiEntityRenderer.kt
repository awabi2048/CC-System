package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.entity.SystemEntityRegistry
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiAccess
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiAccessPolicy
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiHoverText
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVisibilityPolicy
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiView
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVisual
import com.moulberry.axiom.paperapi.AxiomEntityAPI
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.BlockDisplay
import org.bukkit.Color
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
internal class GestureGuiEntityRenderer(
    private val plugin: Plugin,
    private val systemEntityRegistry: SystemEntityRegistry,
) {
    private val sessionKey = NamespacedKey(plugin, "gesture_gui_session")
    private val actorKey = NamespacedKey(plugin, "gesture_gui_actor")
    private val revisionKey = NamespacedKey(plugin, "gesture_gui_revision")
    private val visualKey = NamespacedKey(plugin, "gesture_gui_visual")
    private val axiomEnabled = plugin.server.pluginManager.isPluginEnabled("AxiomPaper")
    private var axiomWarningLogged = false

    internal class ScreenHandle(
        val background: MutableList<BlockDisplay>,
        val contents: MutableList<Entity>,
        val visualEntities: MutableMap<String, Entity>,
        /** 主Visualごとの縦内側・横外側枠を構成するDisplay Entity群です。 */
        val visualOutlineEntities: MutableMap<String, MutableList<BlockDisplay>>,
        val ownerId: UUID,
        var access: GestureGuiAccess,
        var allowlist: Set<UUID>,
        var accessPolicy: GestureGuiAccessPolicy?,
        var visibilityPolicy: GestureGuiVisibilityPolicy?,
        /**
         * 背景へ最後に適用したパネル実寸です。追従tickでは寸法不変のまま
         * メタデータを送ると背景だけ適用時刻がずれ、内容物とのティアを招くため、
         * 変化時のみ送る判定に使います。
         */
        var lastPanelWidth: Double = Double.NaN,
        var lastPanelHeight: Double = Double.NaN,
    ) {
        val all: List<Entity> get() = background + contents

        /** ホバー説明で一時的に隠している通常visualを操作者単位で保持します。 */
        val hiddenVisualIds: MutableMap<UUID, MutableSet<String>> = mutableMapOf()

        /** ホバー置換の深さ解決に使う、visualIdごとの層です。 */
        val visualLayers: MutableMap<String, Int> = mutableMapOf()

        /** BlockDisplay本体だけをホバー差し替えする際の操作者別非表示状態です。 */
        val hiddenVisualBodyIds: MutableMap<UUID, MutableSet<String>> = mutableMapOf()
    }

    internal data class CatcherHandle(val actorId: UUID, val entity: Interaction)
    internal data class HoverHandle(
        val actorId: UUID,
        val entity: TextDisplay,
        /** ホバー中だけ操作者へ配布する背景差し替えBlockDisplayです。 */
        var blockEntity: BlockDisplay? = null,
    )

    fun spawnScreen(
        owner: Player,
        sessionId: UUID,
        revision: Long,
        pose: GestureGuiScreenPose,
        view: GestureGuiView,
    ): ScreenHandle {
        val world = owner.world
        // 可視性はPaperのtracking既定値へ任せず、必ずこのハンドルのアクセス定義から
        // 明示的に配布します。PUBLICを既定可視にすると、後からOWNER_ONLYへ変更した
        // ときにtracking再開で非許可者へ再表示され、逆方向では新規参加者へ届きません。
        // 常にfalseで生成し、showBackground/showContents/showToを唯一の配布経路にします。
        val visibleByDefault = false
        val backgrounds = mutableListOf<BlockDisplay>()
        val contents = mutableListOf<Entity>()
        val entities = linkedMapOf<String, Entity>()
        val outlineEntities = linkedMapOf<String, MutableList<BlockDisplay>>()
        val panel = view.panel
        try {
            // 全素材を先に解決し、枠素材の不正で背景だけが残る部分生成を防ぎます。
            val backgroundData = Bukkit.createBlockData(panel.backgroundMaterial)
            val frameData = Bukkit.createBlockData(panel.frameMaterial)
            val background = spawnPanelBlock(
                world, pose, backgroundData, 0.0, 0.0, panel.width, panel.height, PANEL_BACKGROUND_LAYER,
                initiallyScaleZero = true,
                visibleByDefault = visibleByDefault,
            )
            backgrounds += background
            mark(background, sessionId, revision)
            val innerHeight = panel.height - panel.frameWidth * 2.0
            listOf(
                PanelPart(0.0, (panel.height - panel.frameWidth) / 2.0, panel.width, panel.frameWidth),
                PanelPart(0.0, -(panel.height - panel.frameWidth) / 2.0, panel.width, panel.frameWidth),
                PanelPart((panel.width - panel.frameWidth) / 2.0, 0.0, panel.frameWidth, innerHeight),
                PanelPart(-(panel.width - panel.frameWidth) / 2.0, 0.0, panel.frameWidth, innerHeight),
            ).forEachIndexed { index, part ->
                val frame = spawnPanelBlock(
                    world, pose, frameData, part.x, part.y, part.width, part.height, PANEL_FRAME_LAYER,
                    visibleByDefault = visibleByDefault,
                )
                entities["__panel_frame_$index"] = frame
                contents += frame
                mark(frame, sessionId, revision)
            }
            view.visuals.sortedBy(GestureGuiVisual::layer).forEach { visual ->
                val entity = when (visual) {
                    is GestureGuiVisual.Block -> spawnBlock(world, pose, visual, visibleByDefault)
                    is GestureGuiVisual.Item -> spawnItem(world, pose, visual, visibleByDefault)
                    is GestureGuiVisual.Text -> spawnText(world, pose, visual, visibleByDefault)
                }
                entities[visual.visualId] = entity
                contents += entity
                mark(entity, sessionId, revision, visual.visualId)
                val outlines = createOutlineEntities(world, pose, visual, visibleByDefault)
                if (outlines.isNotEmpty()) {
                    outlines.forEach { outline ->
                        contents += outline
                        mark(outline, sessionId, revision, visual.visualId)
                    }
                    outlineEntities[visual.visualId] = outlines
                }
            }
            val handle = ScreenHandle(
                backgrounds,
                contents,
                entities,
                outlineEntities,
                owner.uniqueId,
                view.definition.access,
                view.definition.allowlist,
                view.definition.accessPolicy,
                view.definition.visibilityPolicy,
            )
            // ホバー置換が置換対象と同じ深さへ解決できるよう、層を記録します。
            view.visuals.forEach { visual -> handle.visualLayers[visual.visualId] = visual.layer }
            handle.lastPanelWidth = panel.width
            handle.lastPanelHeight = panel.height
            // PUBLICを含め、初期表示対象をアクセス定義に基づいて背景だけ明示します。
            // contentsの表示は呼び出し側（アニメーション完了時／非アニメーション経路の
            // 翌tick表示）へ委ねます。spawnと同tickにcontentsを表示すると、アニメーション
            // 完了前に中身が露出し、tracking未了時は逆に表示が失われるためです。
            // 後から参加したプレイヤーはGestureGuiServiceImplのreconcileExternalActors
            // からshowToされるため、可視性の経路が常に一つになります。
            showBackground(handle)
            return handle
        } catch (failure: Throwable) {
            // open/openChild側では失敗したScreenHandleを受け取れないため、
            // 生成途中のEntityはこのメソッド自身で必ず回収します。
            backgrounds.forEach(Entity::remove)
            contents.forEach(Entity::remove)
            throw failure
        }
    }

    /**
     * 追従・パン・子画面再配置時に画面全体を新poseへ移動し、teleportした体数を返します。
     *
     * 背景の実寸確定は寸法変化時のみ行い、追従中の冗長メタデータで背景だけが
     * 先行適用されるティアを抑えます。戻り値は追従計測の teleport 母数に使います。
     */
    fun updatePose(handle: ScreenHandle, pose: GestureGuiScreenPose, view: GestureGuiView): Int {
        val panel = view.panel
        // パネル寸法の変更を座標移動だけで済ませると、旧サイズの背景が残り、
        // 子画面やズーム更新時に入力面と見た目がずれます。変化時のみ実寸へ確定します。
        if (handle.lastPanelWidth != panel.width || handle.lastPanelHeight != panel.height) {
            setBackgroundSize(handle, panel.width.toFloat(), panel.height.toFloat(), interpolationTicks = 0)
        } else {
            GestureGuiFollowMetrics.recordBackgroundResizeSkipped()
        }
        handle.background.forEach { it.teleport(visualLocation(it.world, pose, 0.0, 0.0, PANEL_BACKGROUND_LAYER)) }
        var teleported = handle.background.size
        val innerHeight = panel.height - panel.frameWidth * 2.0
        val frameParts = listOf(
            PanelPart(0.0, (panel.height - panel.frameWidth) / 2.0, panel.width, panel.frameWidth),
            PanelPart(0.0, -(panel.height - panel.frameWidth) / 2.0, panel.width, panel.frameWidth),
            PanelPart((panel.width - panel.frameWidth) / 2.0, 0.0, panel.frameWidth, innerHeight),
            PanelPart(-(panel.width - panel.frameWidth) / 2.0, 0.0, panel.frameWidth, innerHeight),
        )
        frameParts.forEachIndexed { index, part ->
            handle.visualEntities["__panel_frame_$index"]?.let {
                it.teleport(
                    visualLocation(handle.background.first().world, pose, part.x, part.y, PANEL_FRAME_LAYER),
                )
                teleported++
            }
        }
        view.visuals.forEach { visual ->
            val entity = handle.visualEntities[visual.visualId] ?: return@forEach
            val location = if (visual is GestureGuiVisual.Text) {
                textLocation(entity.world, pose, visual.x, visual.y, visual.layer)
            } else {
                visualLocation(entity.world, pose, visual.x, visual.y, visual.layer)
            }
            entity.teleport(location)
            teleported++
            // 枠も主Visualと同じposeへ移動するため、teleport母数へ含めます。
            teleported += handle.visualOutlineEntities[visual.visualId]?.size ?: 0
            updateOutlinePose(handle, pose, visual)
        }
        return teleported
    }

    fun setBackgroundSize(handle: ScreenHandle, width: Float, height: Float, interpolationTicks: Int) {
        setBackgroundSize(handle.background, width, height, interpolationTicks)
        handle.lastPanelWidth = width.toDouble()
        handle.lastPanelHeight = height.toDouble()
    }

    fun setBackgroundScaleZero(handle: ScreenHandle, interpolationTicks: Int) =
        setBackgroundScaleZero(handle.background, interpolationTicks)

    fun showContents(handle: ScreenHandle) {
        val viewers = viewers(handle).toList()
        handle.contents.forEach { entity ->
            viewers.filter { isVisualVisible(handle, entity, it.uniqueId) }
                .forEach { it.showEntity(plugin, entity) }
        }
    }

    fun updateAccess(handle: ScreenHandle, view: GestureGuiView) {
        val nextAccess = view.definition.access
        val nextAllowlist = view.definition.allowlist
        val nextAccessPolicy = view.definition.accessPolicy
        val nextVisibilityPolicy = view.definition.visibilityPolicy
        if (handle.access != nextAccess ||
            handle.allowlist != nextAllowlist ||
            handle.accessPolicy !== nextAccessPolicy ||
            handle.visibilityPolicy !== nextVisibilityPolicy
        ) {
            // 公開→非公開、またはallowlist変更時に既存クライアントへ残った
            // Entityを明示的に隠し、アクセス定義と可視状態を同一tickで揃えます。
            handle.all.forEach { entity ->
                Bukkit.getOnlinePlayers()
                    .filterNot { player ->
                        canView(
                            handle.ownerId,
                            nextAccess,
                            nextAllowlist,
                            nextAccessPolicy,
                            nextVisibilityPolicy,
                            player.uniqueId,
                        )
                    }
                    .forEach { player -> player.hideEntity(plugin, entity) }
            }
        }
        handle.access = nextAccess
        handle.allowlist = nextAllowlist
        handle.accessPolicy = nextAccessPolicy
        handle.visibilityPolicy = nextVisibilityPolicy
    }

    private fun showBackground(handle: ScreenHandle) {
        val viewers = viewers(handle).toList()
        handle.background.forEach { entity -> viewers.forEach { it.showEntity(plugin, entity) } }
    }

    /** 新しい外部参加者へ、その画面の可視コンテンツだけを送信します。 */
    fun showTo(handle: ScreenHandle, player: Player) {
        if (isViewer(handle, player.uniqueId)) {
            handle.background.forEach { player.showEntity(plugin, it) }
            handle.contents.filter { isVisualVisible(handle, it, player.uniqueId) }
                .forEach { player.showEntity(plugin, it) }
        }
    }

    /** 動的権限を失ったプレイヤーへ、既に配布済みの画面を残さないために隠します。 */
    fun hideFrom(handle: ScreenHandle, player: Player) {
        handle.all.forEach { entity -> player.hideEntity(plugin, entity) }
    }

    /**
     * 通常visualの表示状態を操作者単位で切り替えます。
     *
     * PaperのEntity可視性はプレイヤーごとに管理できるため、PUBLIC画面でも
     * ホバーしている操作者だけへ既定説明の非表示を適用できます。画面更新時に
     * showContents/showToが呼ばれても同じ状態を維持できるよう、表示抑制をハンドル
     * 内へ記録します。
     */
    fun setVisualVisible(handle: ScreenHandle, visualId: String, player: Player, visible: Boolean): Boolean {
        if (handle.visualEntities[visualId] == null) return false
        val entities = visualEntities(handle, visualId)
        val hidden = handle.hiddenVisualIds.getOrPut(player.uniqueId) { mutableSetOf() }
        if (visible) {
            hidden.remove(visualId)
            if (hidden.isEmpty()) handle.hiddenVisualIds.remove(player.uniqueId)
            handle.hiddenVisualBodyIds[player.uniqueId]?.remove(visualId)
            handle.hiddenVisualBodyIds.entries.removeIf { it.value.isEmpty() }
            entities.forEach { entity -> player.showEntity(plugin, entity) }
        } else {
            hidden += visualId
            entities.forEach { entity -> player.hideEntity(plugin, entity) }
        }
        return true
    }

    /** 通常visualのBlockDisplay本体だけを操作者単位で切り替えます。 */
    fun setVisualBodyVisible(handle: ScreenHandle, visualId: String, player: Player, visible: Boolean): Boolean {
        val entity = handle.visualEntities[visualId] ?: return false
        if (entity !is BlockDisplay) return false
        val hiddenWhole = handle.hiddenVisualIds[player.uniqueId].orEmpty()
        val hiddenBody = handle.hiddenVisualBodyIds.getOrPut(player.uniqueId) { mutableSetOf() }
        if (visible) {
            hiddenBody.remove(visualId)
            if (hiddenBody.isEmpty()) handle.hiddenVisualBodyIds.remove(player.uniqueId)
            if (visualId !in hiddenWhole) player.showEntity(plugin, entity)
        } else {
            hiddenBody += visualId
            player.hideEntity(plugin, entity)
        }
        return true
    }

    /** ホバー置換の深さ解決のため、通常visualの層を参照します。 */
    fun visualLayer(handle: ScreenHandle, visualId: String): Int? = handle.visualLayers[visualId]

    /**
     * 置換対象の層より0.02ブロック（LAYER_DEPTH×HOVER_FLOAT_LAYERS）だけ前面の層です。
     * 置換対象を操作者へ隠したうえで近い深さに載せることで、ホバー中の法線方向の
     * 跳ねを最小にしつつ、層の重なり規則（前面に浮く）を維持します。
     */
    fun hoverReplaceLayer(baseLayer: Int): Int = (baseLayer + HOVER_FLOAT_LAYERS).coerceAtMost(MAX_LAYER)

    private fun viewers(handle: ScreenHandle): Sequence<Player> = Bukkit.getOnlinePlayers()
        .asSequence()
        .filter { isViewer(handle, it.uniqueId) }

    private fun isViewer(handle: ScreenHandle, playerId: UUID): Boolean =
        canView(
            handle.ownerId,
            handle.access,
            handle.allowlist,
            handle.accessPolicy,
            handle.visibilityPolicy,
            playerId,
        )

    private fun canView(
        ownerId: UUID,
        access: GestureGuiAccess,
        allowlist: Set<UUID>,
        accessPolicy: GestureGuiAccessPolicy?,
        visibilityPolicy: GestureGuiVisibilityPolicy?,
        playerId: UUID,
    ): Boolean {
        val staticallyAllowed = when (access) {
            GestureGuiAccess.PUBLIC -> true
            GestureGuiAccess.OWNER_ONLY -> playerId == ownerId
            GestureGuiAccess.ALLOWLIST -> playerId == ownerId || playerId in allowlist
        }
        val dynamicallyVisible = visibilityPolicy?.canView(ownerId, playerId)
            ?: accessPolicy?.canOperate(ownerId, playerId)
            ?: true
        return staticallyAllowed && dynamicallyVisible
    }

    /** 同一visualIdの実体を再利用し、追加・変更・削除だけを反映します。 */
    fun updateScreenDiff(
        handle: ScreenHandle,
        sessionId: UUID,
        revision: Long,
        pose: GestureGuiScreenPose,
        oldView: GestureGuiView,
        newView: GestureGuiView,
    ) {
        val newIds = newView.visuals.mapTo(HashSet(), GestureGuiVisual::visualId)
        // oldViewは利用側の状態更新が失敗した場合に実体Mapより古くなることが
        // あります。旧viewだけを走査すると、その間に作られたTextDisplayが画面へ
        // 残り続けるため、削除対象は常に現在のMapを正本として求めます。
        handle.visualEntities.keys.toList()
            // パネル枠もvisualEntitiesで管理していますが、view.visualsには含まれない
            // 内部IDです。枠を差分削除すると更新後に背景だけが消えるため保持します。
            .filter { !it.startsWith(PANEL_FRAME_PREFIX) && it !in newIds }
            .forEach { visualId ->
            removeVisualEntities(handle, visualId)
            handle.visualLayers.remove(visualId)
            handle.hiddenVisualIds.values.forEach { hidden -> hidden.remove(visualId) }
            handle.hiddenVisualBodyIds.values.forEach { hidden -> hidden.remove(visualId) }
        }
        handle.hiddenVisualIds.entries.removeIf { it.value.isEmpty() }
        handle.hiddenVisualBodyIds.entries.removeIf { it.value.isEmpty() }
        newView.visuals.sortedBy(GestureGuiVisual::layer).forEach { visual ->
            val current = handle.visualEntities[visual.visualId]
            val compatible = when (visual) {
                is GestureGuiVisual.Block -> current is BlockDisplay && current.isValid
                is GestureGuiVisual.Item -> current is ItemDisplay && current.isValid
                is GestureGuiVisual.Text -> current is TextDisplay && current.isValid
            }
            val entity = if (compatible) current!! else {
                val created = when (visual) {
                    // 差分で新規生成した実体は、公開画面であっても送信先を
                    // showImmediatelyへ統一するため、デフォルト可視を切ります。
                    is GestureGuiVisual.Block -> spawnBlock(handle.background.first().world, pose, visual, false)
                    is GestureGuiVisual.Item -> spawnItem(handle.background.first().world, pose, visual, false)
                    is GestureGuiVisual.Text -> spawnText(handle.background.first().world, pose, visual, false)
                }
                // 先に新しい実体を生成します。生成に失敗しても旧実体を残し、
                // 画面が一時的に空白になることを防ぎます。
                removeVisualEntities(handle, visual.visualId)
                handle.contents += created
                created
            }
            mark(entity, sessionId, revision, visual.visualId)
            applyVisual(entity, pose, visual)
            handle.visualEntities[visual.visualId] = entity
            handle.visualLayers[visual.visualId] = visual.layer
            syncOutlineEntities(handle, sessionId, revision, pose, visual)
        }
        // 差分適用の途中で例外や外部プラグインのEntity操作が発生すると、Mapへ
        // 登録されていない実体だけがcontentsに残る場合があります。通常のvisual／
        // パネル枠はすべてMapで追跡しているため、ここで孤立Entityを一括回収し、
        // 説明TextDisplayが更新回数に応じて累積することを防ぎます。
        val trackedEntities = handle.visualEntities.values.toSet() +
            handle.visualOutlineEntities.values.flatten()
        handle.contents.toList()
            .filterNot { it in trackedEntities }
            .forEach { entity ->
                handle.contents.remove(entity)
                entity.remove()
            }
    }

    private fun applyVisual(entity: Entity, pose: GestureGuiScreenPose, visual: GestureGuiVisual) {
        entity.teleport(if (visual is GestureGuiVisual.Text) textLocation(entity.world, pose, visual.x, visual.y, visual.layer)
        else visualLocation(entity.world, pose, visual.x, visual.y, visual.layer))
        when {
            entity is BlockDisplay && visual is GestureGuiVisual.Block -> {
                entity.block = visual.blockData
                entity.setTransformation(blockTransform(visual.width.toFloat(), visual.height.toFloat()))
                applyGlow(entity, visual.glowColor)
            }
            entity is ItemDisplay && visual is GestureGuiVisual.Item -> {
                entity.setItemStack(visual.item.clone())
                entity.setTransformation(Transformation(Vector3f(), AxisAngle4f(), Vector3f(visual.scale.toFloat()), AxisAngle4f()))
                applyGlow(entity, visual.glowColor)
            }
            entity is TextDisplay && visual is GestureGuiVisual.Text -> {
                entity.text(visual.text)
                entity.lineWidth = visual.lineWidth
                entity.isSeeThrough = visual.seeThrough
                entity.alignment = TextDisplay.TextAlignment.CENTER
                entity.setTransformation(Transformation(Vector3f(), AxisAngle4f(), Vector3f(GestureGuiTextMetrics.toDisplayScale(visual.size)), AxisAngle4f()))
            }
        }
    }

    /** 視点追従・パン・子画面再配置時に枠も主Visualと同じposeへ移動します。 */
    private fun updateOutlinePose(
        handle: ScreenHandle,
        pose: GestureGuiScreenPose,
        visual: GestureGuiVisual,
    ) {
        if (visual !is GestureGuiVisual.Block) return
        val outlines = handle.visualOutlineEntities[visual.visualId] ?: return
        val segments = outlineSegments(visual)
        outlines.forEachIndexed { index, entity ->
            val segment = segments.getOrNull(index) ?: return@forEachIndexed
            entity.teleport(outlineLocation(entity.world, pose, visual, segment))
        }
    }

    /**
     * 差分更新で枠の生成・更新・削除を一元化します。
     *
     * 枠は主VisualのIDで追跡します。これにより、主Visualの表示抑制・アクセス変更・
     * セッション終了が枠だけ取り残すことなく、同じライフサイクルで処理されます。
     */
    private fun syncOutlineEntities(
        handle: ScreenHandle,
        sessionId: UUID,
        revision: Long,
        pose: GestureGuiScreenPose,
        visual: GestureGuiVisual,
    ) {
        if (visual !is GestureGuiVisual.Block || visual.outline == null) {
            handle.visualOutlineEntities.remove(visual.visualId)?.forEach { entity ->
                handle.contents.remove(entity)
                entity.remove()
            }
            return
        }

        val world = handle.background.firstOrNull()?.world ?: return
        val current = handle.visualOutlineEntities[visual.visualId]
        val outlines = if (current != null && current.size == OUTLINE_SEGMENT_COUNT &&
            current.all { it.isValid }
        ) {
            current
        } else {
            // 新しい4本を先に作成し、作成に失敗した場合は途中生成分を回収します。
            val created = createOutlineEntities(world, pose, visual, visibleByDefault = false)
            current?.forEach { entity ->
                handle.contents.remove(entity)
                entity.remove()
            }
            handle.contents.addAll(created)
            handle.visualOutlineEntities[visual.visualId] = created
            created
        }
        val segments = outlineSegments(visual)
        outlines.forEachIndexed { index, entity ->
            val segment = segments.getOrNull(index) ?: return@forEachIndexed
            mark(entity, sessionId, revision, visual.visualId)
            applyOutlineSegment(entity, pose, visual, segment)
        }
    }

    /** 混在配置枠の4本を生成します。表示開始は主Visualと同じcontents経路に委ねます。 */
    private fun createOutlineEntities(
        world: World,
        pose: GestureGuiScreenPose,
        visual: GestureGuiVisual,
        visibleByDefault: Boolean,
    ): MutableList<BlockDisplay> {
        val block = visual as? GestureGuiVisual.Block ?: return mutableListOf()
        val outline = block.outline ?: return mutableListOf()
        val created = mutableListOf<BlockDisplay>()
        try {
            outlineSegments(block).forEach { segment ->
                created += world.spawn(
                    outlineLocation(world, pose, block, segment),
                    BlockDisplay::class.java,
                ) {
                    prepareDisplay(it, pose)
                    it.isVisibleByDefault = visibleByDefault
                    it.block = outline.blockData.clone()
                    it.setTransformation(blockTransform(segment.width.toFloat(), segment.height.toFloat()))
                    hideAxiomDisplayGizmo(it)
                }
            }
            return created
        } catch (failure: Throwable) {
            created.forEach(BlockDisplay::remove)
            throw failure
        }
    }

    private fun applyOutlineSegment(
        entity: BlockDisplay,
        pose: GestureGuiScreenPose,
        visual: GestureGuiVisual.Block,
        segment: GestureGuiOutlineSegment,
    ) {
        val outline = visual.outline ?: return
        // BlockDataは外部Viewの再利用中に変更される可能性があるため、各Entityへ
        // 独立したコピーを渡します。枠同士が同じmutable BlockDataを共有しません。
        entity.block = outline.blockData.clone()
        entity.setTransformation(blockTransform(segment.width.toFloat(), segment.height.toFloat()))
        entity.teleport(outlineLocation(entity.world, pose, visual, segment))
    }

    private fun outlineSegments(visual: GestureGuiVisual.Block): List<GestureGuiOutlineSegment> =
        GestureGuiOutlineGeometry.segments(
            visual.width,
            visual.height,
            visual.outline?.thicknessRatio ?: return emptyList(),
        )

    private fun outlineLocation(
        world: World,
        pose: GestureGuiScreenPose,
        visual: GestureGuiVisual.Block,
        segment: GestureGuiOutlineSegment,
    ): Location = visualLocation(
        world,
        pose,
        visual.x + segment.x,
        visual.y + segment.y,
        visual.layer.toDouble() + OUTLINE_LAYER_OFFSET,
    )

    private fun applyHover(entity: TextDisplay, pose: GestureGuiScreenPose, hover: GestureGuiHoverText) {
        entity.teleport(textLocation(entity.world, pose, hover.x, hover.y, hover.layer))
        entity.text(hover.text)
        entity.lineWidth = hover.lineWidth
        entity.isSeeThrough = false
        entity.alignment = TextDisplay.TextAlignment.CENTER
        val scale = GestureGuiTextMetrics.toDisplayScale(hover.size)
        entity.setTransformation(Transformation(Vector3f(), AxisAngle4f(), Vector3f(scale), AxisAngle4f()))
    }

    private fun isVisualVisible(handle: ScreenHandle, entity: Entity, playerId: UUID): Boolean {
        // 枠Entityも主Visualと同じIDをPDCへ保持します。Mapを全走査して逆引きすると
        // 枠の本数に比例して判定コストが増えるため、Entity側のタグを正本にして、
        // 主Visual・枠・将来の装飾で同じ可視性規則を共有します。
        val visualId = entity.persistentDataContainer.get(visualKey, PersistentDataType.STRING)
            ?: return true
        if (visualId in (handle.hiddenVisualIds[playerId] ?: emptySet())) return false
        return entity != handle.visualEntities[visualId] ||
            visualId !in (handle.hiddenVisualBodyIds[playerId] ?: emptySet())
    }

    /** 主Visualと、そのVisualに属する枠Entityを同じ表示単位として返します。 */
    private fun visualEntities(handle: ScreenHandle, visualId: String): List<Entity> = buildList {
        handle.visualEntities[visualId]?.let(::add)
        handle.visualOutlineEntities[visualId]?.let(::addAll)
    }

    /** 主Visualと枠をまとめて破棄し、差分更新時の孤立Entityを残しません。 */
    private fun removeVisualEntities(handle: ScreenHandle, visualId: String) {
        handle.visualOutlineEntities.remove(visualId)?.forEach { entity ->
            handle.contents.remove(entity)
            entity.remove()
        }
        handle.visualEntities.remove(visualId)?.let { entity ->
            handle.contents.remove(entity)
            entity.remove()
        }
    }

    /**
     * 開閉アニメーションを伴わない部分更新用に、背景を実寸へ確定して画面全体を表示します。
     * spawnScreen() は初回オープン演出のため背景をゼロスケールで生成するので、更新経路では
     * showEntity() だけでなく、必ず実寸の復元までを一まとまりで行います。
     */
    fun showImmediately(handle: ScreenHandle, panel: com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel) {
        setBackgroundSize(handle, panel.width.toFloat(), panel.height.toFloat(), interpolationTicks = 0)
        showBackground(handle)
        showContents(handle)
    }

    fun hideContents(handle: ScreenHandle) {
        // 終了時はアクセス権を失った参加者も含め、全オンラインへ非表示を送ります。
        handle.contents.forEach { entity -> Bukkit.getOnlinePlayers().forEach { it.hideEntity(plugin, entity) } }
        handle.background.forEach { entity -> Bukkit.getOnlinePlayers().forEach { it.hideEntity(plugin, entity) } }
    }

    /**
     * ScreenHandleが保持する実体を破棄し、ハンドルも空に戻します。
     *
     * 表示更新・終了処理の遅延Runnableが古いハンドルを参照しても、破棄済み
     * Entityを再表示できないように、Entityのremoveだけでなく管理コレクションも
     * 同時に無効化します。
     */
    fun remove(handle: ScreenHandle) {
        handle.all.toSet().forEach(Entity::remove)
        handle.background.clear()
        handle.contents.clear()
        handle.visualEntities.clear()
        handle.visualOutlineEntities.clear()
        handle.visualLayers.clear()
        handle.hiddenVisualIds.clear()
        handle.hiddenVisualBodyIds.clear()
    }

    /**
     * セッションタグを正本として、ハンドルから漏れた表示Entityも回収します。
     *
     * ホバーTextDisplayや操作者ごとのInteractionはScreenHandleの外で管理されるため、
     * Actorの登録漏れなどが起きても終了時に残らないよう、ロード済み全ワールドを
     * セッションIDで走査します。タグは本プラグインが生成したEntityにだけ付くため、
     * 通常のDisplayやプレイヤーを巻き込みません。
     */
    fun removeSessionEntities(sessionId: UUID) {
        val expected = sessionId.toString()
        Bukkit.getWorlds().forEach { world ->
            world.entities.toList()
                .filter { entity ->
                    entity.persistentDataContainer.get(sessionKey, PersistentDataType.STRING) == expected
                }
                .forEach(Entity::remove)
        }
    }

    fun spawnModalOverlay(
        owner: Player,
        sessionId: UUID,
        revision: Long,
        pose: GestureGuiScreenPose,
        material: Material = Material.GRAY_STAINED_GLASS,
        definition: com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenDefinition,
    ): BlockDisplay = spawnPanelBlock(
        owner.world, pose, Bukkit.createBlockData(material),
        0.0, 0.0, pose.width, pose.height, MODAL_OVERLAY_LAYER,
        visibleByDefault = false,
    ).also {
        mark(it, sessionId, revision)
        hideAxiomDisplayGizmo(it)
        // モーダル子画面を見られる第三者にも同じ遮蔽面を送ります。所有者だけへ
        // overlayを配ると、共有画面では子画面の背後に親の操作面が見えたままになり、
        // 表示と入力の遮蔽範囲が操作者ごとに分岐します。
        Bukkit.getOnlinePlayers()
            .filter { player -> definition.canView(owner.uniqueId, player.uniqueId) }
            .forEach { player -> player.showEntity(plugin, it) }
    }

    fun updateModalOverlay(overlay: BlockDisplay, pose: GestureGuiScreenPose) {
        overlay.teleport(visualLocation(overlay.world, pose, 0.0, 0.0, MODAL_OVERLAY_LAYER))
    }

    fun spawnCatcher(
        player: Player,
        sessionId: UUID,
        revision: Long,
        location: Location,
        responsive: Boolean,
    ): CatcherHandle {
        val entity = player.world.spawn(location, Interaction::class.java) {
            it.isPersistent = false
            // 後から参加したプレイヤーにも送信されないよう、生成時点から個人表示に固定します。
            it.isVisibleByDefault = false
            // InteractionのLocationは底面基準です。サービス側で目位置から
            // 半分だけ下げて配置することで、ヒットボックスの中央を視点へ一致させます。
            it.interactionWidth = GESTURE_CATCHER_SIZE
            it.interactionHeight = GESTURE_CATCHER_SIZE
            // Interactionの応答設定は腕振り等のクライアント応答を制御します。
            // 実際の操作可否はGestureGuiService/Listenerで別途判定します。
            it.isResponsive = responsive
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
        hoverVisual: GestureGuiVisual.Block? = null,
    ): HoverHandle {
        val hoverBlockData = hover.hoverBlockData
        val blockEntity = if (
            hoverVisual != null &&
            hover.hoverBlockVisualId == hoverVisual.visualId &&
            hoverBlockData != null
        ) {
            spawnHoverBlock(player.world, sessionId, revision, pose, hoverVisual, hoverBlockData)
        } else {
            null
        }
        val entity = try {
            player.world.spawn(
                textLocation(player.world, pose, hover.x, hover.y, hover.layer),
                TextDisplay::class.java,
            ) {
                prepareTextDisplay(it, pose)
                it.isVisibleByDefault = false
                applyHover(it, pose, hover)
                mark(it, sessionId, revision)
                hideAxiomDisplayGizmo(it)
            }
        } catch (failure: Throwable) {
            // TextDisplay生成に失敗しても、先に作ったホバー面を残さないようにします。
            blockEntity?.remove()
            throw failure
        }
        // isVisibleByDefault=falseの実体はtracking開始後に個別表示します。同一tickのshowは
        // クライアントへspawn packetが送られる前に消費される実装差があるため、次tickへ分離します。
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (player.isOnline && entity.isValid) player.showEntity(plugin, entity)
            if (player.isOnline && blockEntity?.isValid == true) player.showEntity(plugin, blockEntity)
        })
        return HoverHandle(player.uniqueId, entity, blockEntity)
    }

    fun updateHover(
        player: Player,
        handle: HoverHandle,
        sessionId: UUID,
        revision: Long,
        pose: GestureGuiScreenPose,
        hover: GestureGuiHoverText,
        hoverVisual: GestureGuiVisual.Block? = null,
    ) {
        // 同じ要素IDのまま画面が更新される場合も、位置だけでなく文面・幅・縮尺を
        // 更新します。設定変更後に古いホバー説明が残る問題を防ぎます。
        applyHover(handle.entity, pose, hover)
        val blockData = hover.hoverBlockData
        if (hoverVisual != null &&
            hover.hoverBlockVisualId == hoverVisual.visualId &&
            blockData != null
        ) {
            val visual = hoverVisual
            val current = handle.blockEntity
            if (current == null || !current.isValid) {
                handle.blockEntity = spawnHoverBlock(
                    player.world,
                    sessionId,
                    revision,
                    pose,
                    visual,
                    blockData,
                )
                val created = handle.blockEntity
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (player.isOnline && created?.isValid == true) player.showEntity(plugin, created)
                })
            } else {
                applyHoverBlock(current, pose, visual, blockData)
            }
        } else {
            handle.blockEntity?.remove()
            handle.blockEntity = null
        }
    }

    fun removeHover(handle: HoverHandle) {
        handle.entity.remove()
        handle.blockEntity?.remove()
        handle.blockEntity = null
    }

    fun ownsCatcher(entity: Entity, playerId: UUID? = null): Boolean {
        val owner = entity.persistentDataContainer.get(actorKey, PersistentDataType.STRING)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return false
        return playerId == null || owner == playerId
    }

    private fun spawnBlock(
        world: World,
        pose: GestureGuiScreenPose,
        visual: GestureGuiVisual.Block,
        visibleByDefault: Boolean = true,
    ): BlockDisplay =
        world.spawn(visualLocation(world, pose, visual.x, visual.y, visual.layer), BlockDisplay::class.java) {
            prepareDisplay(it, pose)
            it.isVisibleByDefault = visibleByDefault
            it.block = visual.blockData
            it.setTransformation(blockTransform(visual.width.toFloat(), visual.height.toFloat()))
            applyGlow(it, visual.glowColor)
            hideAxiomDisplayGizmo(it)
        }

    /** ホバー中だけ操作者へ配布する、通常BlockDisplayと同寸法の差し替え面です。 */
    private fun spawnHoverBlock(
        world: World,
        sessionId: UUID,
        revision: Long,
        pose: GestureGuiScreenPose,
        visual: GestureGuiVisual.Block,
        blockData: BlockData,
    ): BlockDisplay = world.spawn(
        visualLocation(world, pose, visual.x, visual.y, visual.layer),
        BlockDisplay::class.java,
    ) {
        prepareDisplay(it, pose)
        it.isVisibleByDefault = false
        applyHoverBlock(it, pose, visual, blockData)
        mark(it, sessionId, revision)
        hideAxiomDisplayGizmo(it)
    }

    private fun applyHoverBlock(
        entity: BlockDisplay,
        pose: GestureGuiScreenPose,
        visual: GestureGuiVisual.Block,
        blockData: BlockData,
    ) {
        // BlockDataは共有Viewから参照されるため、Entityごとに複製して外部変更の影響を
        // 切り離します。通常visualの寸法・ローカル座標をそのまま引き継ぐことで、
        // ホバー中だけボタン面を差し替えても縁取りや当たり判定の位置は動きません。
        entity.block = blockData.clone()
        entity.setTransformation(blockTransform(visual.width.toFloat(), visual.height.toFloat()))
        entity.teleport(visualLocation(entity.world, pose, visual.x, visual.y, visual.layer))
    }

    private fun spawnPanelBlock(
        world: World,
        pose: GestureGuiScreenPose,
        blockData: org.bukkit.block.data.BlockData,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        layer: Int,
        initiallyScaleZero: Boolean = false,
        visibleByDefault: Boolean = true,
    ): BlockDisplay = world.spawn(visualLocation(world, pose, x, y, layer), BlockDisplay::class.java) {
        prepareDisplay(it, pose)
        it.isVisibleByDefault = visibleByDefault
        it.block = blockData
        // 背景の開幕Transformはspawn packetへ最大サイズを一度も載せないよう、生成Consumer内で確定します。
        // 生成後にscale 0へ戻す方式では、クライアントが初期Transformだけを1 frame描画する競合が起きます。
        it.setTransformation(
            if (initiallyScaleZero) scaleZeroTransform() else blockTransform(width.toFloat(), height.toFloat()),
        )
        hideAxiomDisplayGizmo(it)
    }

    private fun spawnItem(
        world: World,
        pose: GestureGuiScreenPose,
        visual: GestureGuiVisual.Item,
        visibleByDefault: Boolean = true,
    ): ItemDisplay =
        world.spawn(visualLocation(world, pose, visual.x, visual.y, visual.layer), ItemDisplay::class.java) {
            prepareDisplay(it, pose)
            it.isVisibleByDefault = visibleByDefault
            it.setItemStack(visual.item.clone())
            it.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.GUI
            val scale = visual.scale.toFloat()
            it.setTransformation(Transformation(Vector3f(), AxisAngle4f(), Vector3f(scale), AxisAngle4f()))
            applyGlow(it, visual.glowColor)
            hideAxiomDisplayGizmo(it)
        }

    private fun spawnText(
        world: World,
        pose: GestureGuiScreenPose,
        visual: GestureGuiVisual.Text,
        visibleByDefault: Boolean = true,
    ): TextDisplay =
        world.spawn(textLocation(world, pose, visual.x, visual.y, visual.layer), TextDisplay::class.java) {
            prepareTextDisplay(it, pose)
            it.isVisibleByDefault = visibleByDefault
            it.text(visual.text)
            it.lineWidth = visual.lineWidth
            it.isSeeThrough = visual.seeThrough
            it.alignment = TextDisplay.TextAlignment.CENTER
            val scale = GestureGuiTextMetrics.toDisplayScale(visual.size)
            it.setTransformation(Transformation(Vector3f(), AxisAngle4f(), Vector3f(scale), AxisAngle4f()))
            hideAxiomDisplayGizmo(it)
        }

    /**
     * Axiom Paper Pluginの公開APIで、Kantanの表示エンティティごとのGizmoを
     * 操作対象から除外します。Axiomがないサーバーでは呼び出さず、任意連携を
     * 失敗させません。API世代差は一つの警告へ閉じ込め、表示生成自体は継続します。
     */
    private fun hideAxiomDisplayGizmo(display: Display) {
        if (!axiomEnabled) return
        runCatching { AxiomEntityAPI.getAPI().hideDisplayGizmo(display) }
            .onFailure { failure ->
                if (!axiomWarningLogged) {
                    axiomWarningLogged = true
                    plugin.logger.warning(
                        "AxiomのDisplay Gizmo抑制に失敗しました。表示は継続します: ${failure.message}",
                    )
                }
            }
    }

    private fun prepareDisplay(display: Display, pose: GestureGuiScreenPose) {
        display.isPersistent = false
        display.billboard = Display.Billboard.FIXED
        display.teleportDuration = 1
        display.interpolationDelay = 0
        display.interpolationDuration = 3
        // 周囲のblock/sky lightに左右されずGUIの可読性を保ちます。
        display.brightness = Display.Brightness(15, 15)
        display.setRotation(GestureGuiGeometry.displayYaw(pose), GestureGuiGeometry.displayPitch(pose))
    }

    /** 選択ハイライト等に用いるDisplayのglowを設定します。colorがnullならglowなし。 */
    private fun applyGlow(display: Display, glowColor: Int?) {
        display.isGlowing = glowColor != null
        display.setGlowColorOverride(glowColor?.let(Color::fromARGB))
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

    private fun setBackgroundScaleZero(backgrounds: List<BlockDisplay>, interpolationTicks: Int) {
        backgrounds.forEach {
            it.interpolationDuration = interpolationTicks
            it.interpolationDelay = 0
            it.setTransformation(scaleZeroTransform())
        }
    }

    private fun scaleZeroTransform() = Transformation(
        Vector3f(),
        AxisAngle4f(),
        Vector3f(),
        AxisAngle4f(),
    )

    private fun blockTransform(width: Float, height: Float) = Transformation(
        Vector3f(-width / 2f, -height / 2f, 0f),
        AxisAngle4f(),
        Vector3f(width, height, BLOCK_NORMAL_DEPTH),
        AxisAngle4f(),
    )

    private fun mark(entity: Entity, sessionId: UUID, revision: Long, visualId: String? = null) {
        systemEntityRegistry.mark(entity, plugin)
        entity.persistentDataContainer.set(sessionKey, PersistentDataType.STRING, sessionId.toString())
        entity.persistentDataContainer.set(revisionKey, PersistentDataType.LONG, revision)
        if (visualId != null) {
            entity.persistentDataContainer.set(visualKey, PersistentDataType.STRING, visualId)
        } else {
            entity.persistentDataContainer.remove(visualKey)
        }
    }

    private fun visualLocation(
        world: World,
        pose: GestureGuiScreenPose,
        x: Double,
        y: Double,
        layer: Int,
    ): Location = visualLocation(world, pose, x, y, layer.toDouble())

    private fun visualLocation(
        world: World,
        pose: GestureGuiScreenPose,
        x: Double,
        y: Double,
        layer: Double,
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
    ): Location = visualLocation(
        world,
        pose,
        x,
        y - TEXT_BASELINE_OFFSET,
        GestureGuiTextDepth.effectiveLayer(layer),
    ).apply {
        yaw = GestureGuiGeometry.textDisplayYaw(pose)
        pitch = GestureGuiGeometry.textDisplayPitch(pose)
    }

    private companion object {
        const val PANEL_FRAME_PREFIX = "__panel_frame_"
        // 斜め視点の深度量子化でも隣接レイヤーが重ならないよう、従来値の5/3倍を確保します。
        const val LAYER_DEPTH = 0.005
        /**
         * ホバー置換が置換対象より前面へ浮く論理層数です。
         * TextDisplayの層間距離を元の25%へ縮めた後も、置換対象の前面を確保するため
         * 従来の4層分の実距離（0.02 block）を維持します。
         */
        const val HOVER_FLOAT_LAYERS = 16
        /** GestureGui APIが許容する層の上限です。 */
        const val MAX_LAYER = 40
        // パネルの画面法線方向の厚みは従来値の約2倍です。
        const val BLOCK_NORMAL_DEPTH = 0.025f
        const val PANEL_BACKGROUND_LAYER = 0
        // 背景と枠は同じ厚みを持つため、中心間距離を背景厚より大きくして立体領域の交差を防ぎます。
        // 6 layer × 0.005 = 0.030 blockで、厚み0.025 blockに安全余白を確保します。
        const val PANEL_FRAME_LAYER = 6
        /** TextDisplayのフォント上方向ベースライン分を中心座標から補正します。 */
        const val TEXT_BASELINE_OFFSET = 0.018
        // 0.24 block手前に置き、0.25 block手前から始まる子画面の直後へ重ねます。
        const val MODAL_OVERLAY_LAYER = 48
        /** 主Visualの枠を前面へ出すための半層です。整数層と同じ向きで配置します。 */
        const val OUTLINE_LAYER_OFFSET = 0.5
        const val OUTLINE_SEGMENT_COUNT = 4
    }

    private data class PanelPart(val x: Double, val y: Double, val width: Double, val height: Double)
}

/**
 * プレイヤー入力を吸収するInteractionの一辺です。
 * 生成側と配置側で別のリテラルを持つと、中央合わせがずれるため共有します。
 */
internal const val GESTURE_CATCHER_SIZE: Float = 0.18f
