package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiAccess
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiHoverText
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
internal class GestureGuiEntityRenderer(private val plugin: Plugin) {
    private val sessionKey = NamespacedKey(plugin, "gesture_gui_session")
    private val actorKey = NamespacedKey(plugin, "gesture_gui_actor")
    private val revisionKey = NamespacedKey(plugin, "gesture_gui_revision")
    private val axiomEnabled = plugin.server.pluginManager.isPluginEnabled("AxiomPaper")
    private var axiomWarningLogged = false

    internal class ScreenHandle(
        val background: MutableList<BlockDisplay>,
        val contents: MutableList<Entity>,
        val visualEntities: MutableMap<String, Entity>,
        val ownerId: UUID,
        var access: GestureGuiAccess,
        var allowlist: Set<UUID>,
    ) {
        val all: List<Entity> get() = background + contents

        /** ホバー説明で一時的に隠している通常visualを操作者単位で保持します。 */
        val hiddenVisualIds: MutableMap<UUID, MutableSet<String>> = mutableMapOf()
    }

    internal data class CatcherHandle(val actorId: UUID, val entity: Interaction)
    internal data class HoverHandle(val actorId: UUID, val entity: TextDisplay)

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
                mark(entity, sessionId, revision)
            }
            val handle = ScreenHandle(
                backgrounds,
                contents,
                entities,
                owner.uniqueId,
                view.definition.access,
                view.definition.allowlist,
            )
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

    fun updatePose(handle: ScreenHandle, pose: GestureGuiScreenPose, view: GestureGuiView) {
        val panel = view.panel
        // パネル寸法の変更を座標移動だけで済ませると、旧サイズの背景が残り、
        // 子画面やズーム更新時に入力面と見た目がずれます。毎回同じ実寸へ確定します。
        setBackgroundSize(handle, panel.width.toFloat(), panel.height.toFloat(), interpolationTicks = 0)
        handle.background.forEach { it.teleport(visualLocation(it.world, pose, 0.0, 0.0, PANEL_BACKGROUND_LAYER)) }
        val innerHeight = panel.height - panel.frameWidth * 2.0
        val frameParts = listOf(
            PanelPart(0.0, (panel.height - panel.frameWidth) / 2.0, panel.width, panel.frameWidth),
            PanelPart(0.0, -(panel.height - panel.frameWidth) / 2.0, panel.width, panel.frameWidth),
            PanelPart((panel.width - panel.frameWidth) / 2.0, 0.0, panel.frameWidth, innerHeight),
            PanelPart(-(panel.width - panel.frameWidth) / 2.0, 0.0, panel.frameWidth, innerHeight),
        )
        frameParts.forEachIndexed { index, part ->
            handle.visualEntities["__panel_frame_$index"]?.teleport(
                visualLocation(handle.background.first().world, pose, part.x, part.y, PANEL_FRAME_LAYER),
            )
        }
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
        if (handle.access != nextAccess || handle.allowlist != nextAllowlist) {
            // 公開→非公開、またはallowlist変更時に既存クライアントへ残った
            // Entityを明示的に隠し、アクセス定義と可視状態を同一tickで揃えます。
            handle.all.forEach { entity ->
                Bukkit.getOnlinePlayers()
                    .filterNot { player ->
                        when (nextAccess) {
                            GestureGuiAccess.PUBLIC -> true
                            GestureGuiAccess.OWNER_ONLY -> player.uniqueId == handle.ownerId
                            GestureGuiAccess.ALLOWLIST ->
                                player.uniqueId == handle.ownerId || player.uniqueId in nextAllowlist
                        }
                    }
                    .forEach { player -> player.hideEntity(plugin, entity) }
            }
        }
        handle.access = nextAccess
        handle.allowlist = nextAllowlist
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

    /**
     * 通常visualの表示状態を操作者単位で切り替えます。
     *
     * PaperのEntity可視性はプレイヤーごとに管理できるため、PUBLIC画面でも
     * ホバーしている操作者だけへ既定説明の非表示を適用できます。画面更新時に
     * showContents/showToが呼ばれても同じ状態を維持できるよう、表示抑制をハンドル
     * 内へ記録します。
     */
    fun setVisualVisible(handle: ScreenHandle, visualId: String, player: Player, visible: Boolean): Boolean {
        val entity = handle.visualEntities[visualId] ?: return false
        val hidden = handle.hiddenVisualIds.getOrPut(player.uniqueId) { mutableSetOf() }
        if (visible) {
            hidden.remove(visualId)
            if (hidden.isEmpty()) handle.hiddenVisualIds.remove(player.uniqueId)
            player.showEntity(plugin, entity)
        } else {
            hidden += visualId
            player.hideEntity(plugin, entity)
        }
        return true
    }

    private fun viewers(handle: ScreenHandle): Sequence<Player> = when (handle.access) {
        GestureGuiAccess.PUBLIC -> Bukkit.getOnlinePlayers().asSequence()
        GestureGuiAccess.OWNER_ONLY -> listOfNotNull(Bukkit.getPlayer(handle.ownerId)).asSequence()
        GestureGuiAccess.ALLOWLIST -> listOfNotNull(Bukkit.getPlayer(handle.ownerId)).asSequence() +
            handle.allowlist.asSequence().mapNotNull(Bukkit::getPlayer)
    }.filter(Player::isOnline)

    private fun isViewer(handle: ScreenHandle, playerId: UUID): Boolean = when (handle.access) {
        GestureGuiAccess.PUBLIC -> true
        GestureGuiAccess.OWNER_ONLY -> playerId == handle.ownerId
        GestureGuiAccess.ALLOWLIST -> playerId == handle.ownerId || playerId in handle.allowlist
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
            handle.visualEntities.remove(visualId)?.let { entity ->
                handle.contents.remove(entity)
                entity.remove()
            }
            handle.hiddenVisualIds.values.forEach { hidden -> hidden.remove(visualId) }
        }
        handle.hiddenVisualIds.entries.removeIf { it.value.isEmpty() }
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
                current?.let { old -> handle.contents.remove(old); old.remove() }
                handle.contents += created
                created
            }
            mark(entity, sessionId, revision)
            applyVisual(entity, pose, visual)
            handle.visualEntities[visual.visualId] = entity
        }
        // 差分適用の途中で例外や外部プラグインのEntity操作が発生すると、Mapへ
        // 登録されていない実体だけがcontentsに残る場合があります。通常のvisual／
        // パネル枠はすべてMapで追跡しているため、ここで孤立Entityを一括回収し、
        // 説明TextDisplayが更新回数に応じて累積することを防ぎます。
        val trackedEntities = handle.visualEntities.values.toSet()
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
        val visualId = handle.visualEntities.entries.firstOrNull { it.value === entity }?.key ?: return true
        return visualId !in (handle.hiddenVisualIds[playerId] ?: emptySet())
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
        handle.hiddenVisualIds.clear()
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
    ): BlockDisplay = spawnPanelBlock(
        owner.world, pose, Bukkit.createBlockData(material),
        0.0, 0.0, pose.width, pose.height, MODAL_OVERLAY_LAYER,
        visibleByDefault = false,
    ).also {
        mark(it, sessionId, revision)
        hideAxiomDisplayGizmo(it)
        owner.showEntity(plugin, it)
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
    ): HoverHandle {
        val entity = player.world.spawn(
            textLocation(player.world, pose, hover.x, hover.y, hover.layer),
            TextDisplay::class.java,
        ) {
            prepareTextDisplay(it, pose)
            it.isVisibleByDefault = false
            applyHover(it, pose, hover)
            mark(it, sessionId, revision)
            hideAxiomDisplayGizmo(it)
        }
        // isVisibleByDefault=falseの実体はtracking開始後に個別表示します。同一tickのshowは
        // クライアントへspawn packetが送られる前に消費される実装差があるため、次tickへ分離します。
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (player.isOnline && entity.isValid) player.showEntity(plugin, entity)
        })
        return HoverHandle(player.uniqueId, entity)
    }

    fun updateHover(handle: HoverHandle, pose: GestureGuiScreenPose, hover: GestureGuiHoverText) {
        // 同じ要素IDのまま画面が更新される場合も、位置だけでなく文面・幅・縮尺を
        // 更新します。設定変更後に古いホバー説明が残る問題を防ぎます。
        applyHover(handle.entity, pose, hover)
    }

    fun removeHover(handle: HoverHandle) = handle.entity.remove()

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
    ): Location = visualLocation(world, pose, x, y - TEXT_BASELINE_OFFSET, layer).apply {
        yaw = GestureGuiGeometry.textDisplayYaw(pose)
        pitch = GestureGuiGeometry.textDisplayPitch(pose)
    }

    private companion object {
        const val PANEL_FRAME_PREFIX = "__panel_frame_"
        // 斜め視点の深度量子化でも隣接レイヤーが重ならないよう、従来値の5/3倍を確保します。
        const val LAYER_DEPTH = 0.005
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
    }

    private data class PanelPart(val x: Double, val y: Double, val width: Double, val height: Double)
}

/**
 * プレイヤー入力を吸収するInteractionの一辺です。
 * 生成側と配置側で別のリテラルを持つと、中央合わせがずれるため共有します。
 */
internal const val GESTURE_CATCHER_SIZE: Float = 0.18f
