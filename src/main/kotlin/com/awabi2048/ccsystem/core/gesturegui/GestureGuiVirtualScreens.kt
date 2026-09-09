package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiHoverText
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiTextAlignment
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiView
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVisual
import com.awabi2048.ccsystem.core.gesturegui.GestureGuiProtocolLibBackend.Companion.TEXT_ALIGN_LEFT
import com.awabi2048.ccsystem.core.gesturegui.GestureGuiProtocolLibBackend.Companion.TEXT_ALIGN_RIGHT
import com.comphenix.protocol.wrappers.WrappedDataValue
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * viewer ごとの仮想画面同期エンジンです。
 *
 * (session, viewer) の [GestureViewerRenderState] を正本に、LOD 遷移時のみ不足分・過剰分を
 * spawn/destroy し、内容変化は stable ID ベースの差分 metadata 更新にします。
 * 無変化の FULL→FULL では 0 packet が原則です。
 *
 * 座標・寸法・層の求め方は Bukkit 描画時と同一（中心＋right*x＋up*y＋normal*(-layer*LATER-DEPTH-lift)、
 * Text は baseline 補正＋有効層、枠は主 visual と同一 pose の 4 分割）であり、
 * 数値 hit-test（[GestureGuiGeometry.hitTest]）と表示位置が一致します。
 *
 * 向きは entity 回転（常に 0）ではなく変形の leftRotation quaternion で与えます。
 * 角度の byte 量子化・欄割付の影響を受けず、float 精度で向きが決まります。
 */
internal class GestureGuiVirtualScreens(
    private val backend: GestureGuiProtocolLibBackend,
) {
    data class PlacedPoint(
        val x: Double,
        val y: Double,
        val z: Double,
        val yawDegrees: Float,
        val pitchDegrees: Float,
    )

    private data class DesiredVisual(
        val key: String,
        val type: EntityType,
        val point: PlacedPoint,
        val fingerprint: String,
        val metadata: (Player) -> List<WrappedDataValue>,
    )

    fun visualPoint(pose: GestureGuiScreenPose, x: Double, y: Double, layer: Double, lift: Double = 0.0): PlacedPoint {
        val point = pose.center + pose.right * x + pose.up * y + pose.normal * (-layer * LAYER_DEPTH - lift)
        // 向きは変形 quaternion が担うため、entity 回転は 0 固定です。
        return PlacedPoint(point.x, point.y, point.z, 0f, 0f)
    }

    fun textPoint(pose: GestureGuiScreenPose, x: Double, y: Double, layer: Int): PlacedPoint {
        val point = pose.center + pose.right * x + pose.up * (y - TEXT_BASELINE_OFFSET) +
            pose.normal * (-GestureGuiTextDepth.effectiveLayer(layer) * LAYER_DEPTH - TEXT_ITEM_SURFACE_LIFT)
        return PlacedPoint(point.x, point.y, point.z, 0f, 0f)
    }

    // -- fingerprint（純粋・単体試験可能） ------------------------------------

    fun panelFingerprint(panel: GestureGuiPanel): String =
        "P|${panel.width}|${panel.height}|${panel.backgroundMaterial}|${panel.frameMaterial}|${panel.frameWidth}"

    fun visualFingerprint(visual: GestureGuiVisual): String = when (visual) {
        is GestureGuiVisual.Block ->
            "B|${visual.x}|${visual.y}|${visual.layer}|${visual.width}|${visual.height}|" +
                "${visual.blockData.asString}|${visual.glowColor}|${visual.outline?.blockData?.asString}|${visual.outline?.thicknessRatio}"
        is GestureGuiVisual.Text ->
            "T|${visual.x}|${visual.y}|${visual.layer}|${GestureGuiProtocolLibBackend.componentJson(visual.text)}|" +
                "${visual.size}|${visual.lineWidth}|${visual.seeThrough}|${visual.alignment}"
        is GestureGuiVisual.Item ->
            "I|${visual.x}|${visual.y}|${visual.layer}|${visual.item.type}|${visual.item.amount}|" +
                "${visual.item.itemMeta?.hashCode()}|${visual.scale}|${visual.glowColor}"
    }

    // -- 画面同期 ------------------------------------------------------------

    /**
     * 1 画面分の viewer 状態を LOD に合わせて同期します。
     *
     * @param screenKey 画面の安定キー（screenId ベース）
     * @param overlay  モーダル遮蔽面（子画面用、なければ null）
     * @param dummyContent ダミー追従中は本体の代わりに単一背景だけ送ります
     */
    /**
     * 1 画面分の viewer 状態を LOD に合わせて同期します。
     *
     * 無変化（LOD・内容・pose・hover 置換が不変）では何も送らず戻ります。
     * 移動・型変更は destroy+spawn、内容のみは metadata 更新にします。
     * hover 系キー（/hover・/hoverBlock）は掃除対象から外し、hover 経路が専有します。
     *
     * @param contentStale 内容 revision が変化し、fingerprint 再評価が必要な場合 true
     */
    fun syncScreen(
        viewer: Player,
        sessionId: java.util.UUID,
        state: GestureViewerRenderState,
        screenKey: String,
        view: GestureGuiView,
        pose: GestureGuiScreenPose,
        lod: GestureViewerLod,
        overlay: OverlayRequest?,
        dummyContent: DummyRequest?,
        contentStale: Boolean,
    ) {
        if (lod == GestureViewerLod.HIDDEN) {
            destroyScreenKeys(viewer, state, screenKey)
            state.lodByScreenKey[screenKey] = GestureViewerLod.HIDDEN
            return
        }
        val storedLod = state.lodByScreenKey[screenKey]
        val sentPose = state.poseByScreenKey[screenKey]
        val poseChanged = sentPose != pose
        // 早期終了：遷移も内容変化も pose 変化も hover 置換変化もなければ 0 work です。
        if (!contentStale && !poseChanged && storedLod == lod && state.hoverEpoch == state.syncedHoverEpoch) {
            return
        }
        val desired = buildDesired(screenKey, view, pose, overlay, dummyContent, lod, state, contentStale)
        val liveKeys = state.virtualIdByKey.keys
            .filter { it == screenKey || it.startsWith("$screenKey/") }
            .filterNot(::isHoverManagedKey)
            .toSet()
        // 追加・移動・型変更・内容変更だけを送ります。無変化には触れません。
        desired.forEach { item ->
            val id = state.virtualIdByKey.getOrPut(item.key) { backend.nextVirtualId() }
            val known = state.contentFingerprintByKey[item.key]
            val live = id in state.liveVirtualIds
            val sentPoint = state.pointByKey[item.key]
            val sentType = state.typeByKey[item.key]
            // 座標・種別は metadata で変えられないため、作り直します。
            // ただしダミー鍵の連続微動は相対移動連鎖で追従し、瞬きをなくします。
            val isDummyKey = dummyContent != null && item.key == "$screenKey/dummy"
            val needsResync = sentPoint != item.point || sentType != null && sentType != item.type
            if (!live || needsResync) {
                if (live && isDummyKey && sentType == item.type && sentPoint != null &&
                    tryRelativeMove(viewer, state, id, item, sentPoint, blockQuat(pose))
                ) {
                    state.contentFingerprintByKey[item.key] = item.fingerprint
                    return@forEach
                }
                if (live) backend.sendDestroy(viewer, listOf(id))
                backend.spawnDisplay(viewer, id, item.type, item.point.x, item.point.y, item.point.z,
                    item.metadata(viewer))
                state.liveVirtualIds += id
                state.contentFingerprintByKey[item.key] = item.fingerprint
                state.pointByKey[item.key] = item.point
                state.typeByKey[item.key] = item.type
                if (isDummyKey) state.quatByKey[item.key] = Quaternionf(blockQuat(pose))
            } else if (contentStale && known != item.fingerprint) {
                backend.sendMetadata(viewer, id, item.metadata(viewer))
                state.contentFingerprintByKey[item.key] = item.fingerprint
            }
        }
        // 過剰分だけを破棄します。全画面 destroy/recreate は行いません。
        (liveKeys - desired.mapTo(HashSet(), DesiredVisual::key)).forEach { key ->
            state.virtualIdByKey.remove(key)?.let { id ->
                backend.sendDestroy(viewer, listOf(id))
                state.liveVirtualIds -= id
            }
            state.contentFingerprintByKey.remove(key)
            state.pointByKey.remove(key)
            state.typeByKey.remove(key)
            state.quatByKey.remove(key)
        }
        state.poseByScreenKey[screenKey] = pose
        state.lodByScreenKey[screenKey] = lod
        state.syncedHoverEpoch = state.hoverEpoch
    }

    fun destroyScreenKeys(viewer: Player, state: GestureViewerRenderState, screenKey: String) {
        state.virtualIdByKey.keys.filter { it == screenKey || it.startsWith("$screenKey/") }.toList().forEach { key ->
            state.virtualIdByKey.remove(key)?.let { id ->
                backend.sendDestroy(viewer, listOf(id))
                state.liveVirtualIds -= id
            }
            state.contentFingerprintByKey.remove(key)
            state.pointByKey.remove(key)
            state.typeByKey.remove(key)
            state.quatByKey.remove(key)
        }
        state.poseByScreenKey.remove(screenKey)
        state.lodByScreenKey.remove(screenKey)
    }

    fun destroyAll(viewer: Player, state: GestureViewerRenderState) {
        if (state.liveVirtualIds.isNotEmpty()) {
            backend.sendDestroy(viewer, state.liveVirtualIds.toList())
            state.liveVirtualIds.clear()
        }
        state.virtualIdByKey.clear()
        state.contentFingerprintByKey.clear()
        state.pointByKey.clear()
        state.typeByKey.clear()
        state.quatByKey.clear()
        state.poseByScreenKey.clear()
        state.lodByScreenKey.clear()
        state.hiddenVisualIds.clear()
        state.hiddenVisualBodyIds.clear()
    }

    /**
     * モーダル遮蔽面の要求です。子画面の描画集合に含め、同じ sweep で管理します。
     *
     * @param pose 親 pose から求めた遮蔽面専用の pose（子画面 pose とは異なります）
     */
    data class OverlayRequest(val material: Material, val width: Double, val height: Double, val pose: GestureGuiScreenPose)
    data class DummyRequest(val width: Double, val height: Double)

    private fun buildDesired(
        screenKey: String,
        view: GestureGuiView?,
        pose: GestureGuiScreenPose,
        overlay: OverlayRequest?,
        dummy: DummyRequest?,
        lod: GestureViewerLod,
        state: GestureViewerRenderState,
        contentStale: Boolean,
    ): List<DesiredVisual> {
        if (dummy != null) {
            // ダミー追従中は本体の古い pose への誤操作を防ぐため、背景 1 枚だけ送ります。
            val key = "$screenKey/dummy"
            val point = visualPoint(pose, 0.0, 0.0, PANEL_BACKGROUND_LAYER)
            val material = DUMMY_PANEL_MATERIAL
            return listOf(DesiredVisual(key, EntityType.BLOCK_DISPLAY, point, "D|${dummy.width}|${dummy.height}") {
                blockMetadata(it, pose, point, dummy.width, dummy.height, Bukkit.createBlockData(material), null)
            })
        }
        val currentView = requireNotNull(view) { "通常同期には view が必要です" }
        val panel = currentView.panel
        val items = mutableListOf<DesiredVisual>()
        // 背景＋枠は BACKGROUND_ONLY でも送ります（上下背景パネルのみ）。
        items += panelBackground(screenKey, panel, pose)
        items += panelFrames(screenKey, panel, pose)
        overlay?.let {
            // 遮蔽面は子画面の描画集合に含め、同じ sweep で管理します。
            // 別経路で送ると子画面同期の過剰分削除に巻き込まれるためです。
            val key = "$screenKey/overlay"
            val point = visualPoint(it.pose, 0.0, 0.0, MODAL_OVERLAY_LAYER)
            items += DesiredVisual(key, EntityType.BLOCK_DISPLAY, point, "O|${it.material}|${it.width}|${it.height}") { viewer ->
                blockMetadata(viewer, it.pose, point, it.width, it.height, Bukkit.createBlockData(it.material), null)
            }
        }
        if (lod == GestureViewerLod.FULL) {
            currentView.visuals.sortedBy(GestureGuiVisual::layer).forEach { visual ->
                if (visual.visualId in state.hiddenVisualIds) return@forEach
                val bodyHidden = visual.visualId in state.hiddenVisualBodyIds
                if (!bodyHidden || visual !is GestureGuiVisual.Block) {
                    items += contentVisual(screenKey, visual, pose, state, contentStale)
                }
                // 枠は本体の表示抑制と連動せず、主 visual が見える間は維持します。
                if (visual is GestureGuiVisual.Block && visual.outline != null) {
                    items += outlineVisuals(screenKey, visual, pose, state, contentStale)
                }
            }
        }
        return items
    }

    private fun panelBackground(screenKey: String, panel: GestureGuiPanel, pose: GestureGuiScreenPose): DesiredVisual {
        val key = "$screenKey/bg"
        val point = visualPoint(pose, 0.0, 0.0, PANEL_BACKGROUND_LAYER)
        val fingerprint = "BG|${panelFingerprint(panel)}"
        return DesiredVisual(key, EntityType.BLOCK_DISPLAY, point, fingerprint) { viewer ->
            blockMetadata(viewer, pose, point, panel.width, panel.height, Bukkit.createBlockData(panel.backgroundMaterial), null)
        }
    }

    private fun panelFrames(screenKey: String, panel: GestureGuiPanel, pose: GestureGuiScreenPose): List<DesiredVisual> {
        val innerHeight = panel.height - panel.frameWidth * 2.0
        val parts = listOf(
            QuadPart(0.0, (panel.height - panel.frameWidth) / 2.0, panel.width, panel.frameWidth),
            QuadPart(0.0, -(panel.height - panel.frameWidth) / 2.0, panel.width, panel.frameWidth),
            QuadPart((panel.width - panel.frameWidth) / 2.0, 0.0, panel.frameWidth, innerHeight),
            QuadPart(-(panel.width - panel.frameWidth) / 2.0, 0.0, panel.frameWidth, innerHeight),
        )
        val frameData = Bukkit.createBlockData(panel.frameMaterial)
        return parts.mapIndexed { index, part ->
            val key = "$screenKey/frame/$index"
            val point = visualPoint(pose, part.x, part.y, PANEL_FRAME_LAYER)
            DesiredVisual(key, EntityType.BLOCK_DISPLAY, point, "F|$index|${part.width}|${part.height}|${panel.frameMaterial}") { viewer ->
                blockMetadata(viewer, pose, point, part.width, part.height, frameData, null)
            }
        }
    }

    private fun contentVisual(
        screenKey: String,
        visual: GestureGuiVisual,
        pose: GestureGuiScreenPose,
        state: GestureViewerRenderState,
        contentStale: Boolean,
    ): DesiredVisual {
        val key = "$screenKey/v/${visual.visualId}"
        // 内容不変時は高コストな fingerprint 再計算（Component JSON 化等）を省きます。
        // 新規キーは比較正本がないため必ず構築します。
        val fingerprint = if (contentStale || state.contentFingerprintByKey[key] == null) {
            visualFingerprint(visual)
        } else {
            state.contentFingerprintByKey.getValue(key)
        }
        return when (visual) {
            is GestureGuiVisual.Block -> {
                val point = visualPoint(pose, visual.x, visual.y, visual.layer.toDouble())
                DesiredVisual(key, EntityType.BLOCK_DISPLAY, point, fingerprint) { viewer ->
                    blockMetadata(viewer, pose, point, visual.width, visual.height, visual.blockData, visual.glowColor)
                }
            }
            is GestureGuiVisual.Item -> {
                val point = visualPoint(pose, visual.x, visual.y, visual.layer.toDouble(), TEXT_ITEM_SURFACE_LIFT)
                DesiredVisual(key, EntityType.ITEM_DISPLAY, point, fingerprint) { viewer ->
                    val scale = visual.scale.toFloat()
                    backend.displayBaseValues(Vector3f(), Vector3f(scale, scale, scale), blockQuat(pose), visual.glowColor) +
                        backend.itemStackValue(visual.item, viewer.world, viewer.location) + backend.itemDisplayTypeValue()
                }
            }
            is GestureGuiVisual.Text -> {
                val point = textPoint(pose, visual.x, visual.y, visual.layer)
                DesiredVisual(key, EntityType.TEXT_DISPLAY, point, fingerprint) { _ ->
                    val scale = GestureGuiTextMetrics.toDisplayScale(visual.size)
                    backend.displayBaseValues(Vector3f(), Vector3f(scale, scale, scale), textQuat(pose), null) +
                        backend.textValues(
                            GestureGuiProtocolLibBackend.componentJson(visual.text),
                            visual.lineWidth, visual.seeThrough, alignmentFlags(visual.alignment),
                        )
                }
            }
        }
    }

    private fun outlineVisuals(
        screenKey: String,
        visual: GestureGuiVisual.Block,
        pose: GestureGuiScreenPose,
        state: GestureViewerRenderState,
        contentStale: Boolean,
    ): List<DesiredVisual> {
        val outline = visual.outline ?: return emptyList()
        return GestureGuiOutlineGeometry.segments(visual.width, visual.height, outline.thicknessRatio).mapIndexed { index, segment ->
            val key = "$screenKey/v/${visual.visualId}/outline/$index"
            val point = visualPoint(pose, visual.x + segment.x, visual.y + segment.y, visual.layer + OUTLINE_LAYER_OFFSET)
            val fingerprint = if (contentStale || state.contentFingerprintByKey[key] == null) {
                "OL|${visual.visualId}|$index|${segment.width}|${segment.height}|${outline.blockData.asString}"
            } else {
                state.contentFingerprintByKey.getValue(key)
            }
            DesiredVisual(key, EntityType.BLOCK_DISPLAY, point, fingerprint) { viewer ->
                blockMetadata(viewer, pose, point, segment.width, segment.height, outline.blockData, null)
            }
        }
    }

    private fun blockMetadata(
        viewer: Player,
        pose: GestureGuiScreenPose,
        point: PlacedPoint,
        width: Double,
        height: Double,
        blockData: org.bukkit.block.data.BlockData,
        glowColor: Int?,
    ): List<WrappedDataValue> {
        val quat = blockQuat(pose)
        return backend.displayBaseValues(
            centeredTranslation(quat, width, height, BLOCK_NORMAL_DEPTH),
            Vector3f(width.toFloat(), height.toFloat(), BLOCK_NORMAL_DEPTH),
            quat,
            glowColor,
        ) + backend.blockStateValue(blockData, viewer.world, viewer.location)
    }

    // -- hover ---------------------------------------------------------------

    /**
     * actor 個人向け hover（説明文＋任意の差し替え面）を同期します。
     * hover 用 Interaction は作らず、5Hz の視線追跡時の ray hit-test 結果だけが入口です。
     */
    fun syncHover(
        viewer: Player,
        state: GestureViewerRenderState,
        screenKey: String,
        pose: GestureGuiScreenPose,
        hover: GestureGuiHoverText,
        hoverBlock: Pair<GestureGuiVisual.Block, BlockData>?,
        identity: String,
    ) {
        val textKey = "$screenKey/hover"
        val blockKey = "$screenKey/hoverBlock"
        val fingerprint = "H|$identity|${hover.x}|${hover.y}|${hover.layer}|" +
            "${GestureGuiProtocolLibBackend.componentJson(hover.text)}|${hover.lineWidth}|${hover.size}"
        val sentPose = state.poseByScreenKey["$screenKey/hoverPose"]
        val poseChanged = sentPose != pose
        val textPoint = textPoint(pose, hover.x, hover.y, hover.layer)
        upsertSingle(viewer, state, textKey, EntityType.TEXT_DISPLAY, textPoint, fingerprint, poseChanged) {
            val scale = GestureGuiTextMetrics.toDisplayScale(hover.size)
            backend.displayBaseValues(Vector3f(), Vector3f(scale, scale, scale), textQuat(pose), null) +
                backend.textValues(
                    GestureGuiProtocolLibBackend.componentJson(hover.text),
                    hover.lineWidth, false, 0,
                )
        }
        if (hoverBlock != null) {
            val (visual, blockData) = hoverBlock
            val point = visualPoint(pose, visual.x, visual.y, visual.layer.toDouble())
            upsertSingle(viewer, state, blockKey, EntityType.BLOCK_DISPLAY, point,
                "HB|$identity|${visual.visualId}|${blockData.asString}", poseChanged) {
                blockMetadata(it, pose, point, visual.width, visual.height, blockData, null)
            }
        } else {
            removeSingle(viewer, state, blockKey)
        }
        state.poseByScreenKey["$screenKey/hoverPose"] = pose
    }

    fun destroyHover(viewer: Player, state: GestureViewerRenderState, screenKey: String) {
        removeSingle(viewer, state, "$screenKey/hover")
        removeSingle(viewer, state, "$screenKey/hoverBlock")
        state.poseByScreenKey.remove("$screenKey/hoverPose")
    }

    /**
     * 背景だけを指定寸法へ変形します。開閉演出用です。
     *
     * 背景キーがなければ scale 0 扱いで生成し、あれば metadata で変形します。
     * 内容物・枠・hover には触れません。fingerprint は演出標識にし、
     * 通常同期が全寸一致へ収束させます。
     */
    fun scaleBackground(
        viewer: Player,
        state: GestureViewerRenderState,
        screenKey: String,
        panel: GestureGuiPanel,
        pose: GestureGuiScreenPose,
        width: Double,
        height: Double,
    ) {
        val key = "$screenKey/bg"
        val point = visualPoint(pose, 0.0, 0.0, PANEL_BACKGROUND_LAYER)
        val fingerprint = "BGA|$width|$height|${panel.backgroundMaterial}"
        val id = state.virtualIdByKey.getOrPut(key) { backend.nextVirtualId() }
        val live = id in state.liveVirtualIds
        val values = blockMetadata(viewer, pose, point, width, height, Bukkit.createBlockData(panel.backgroundMaterial), null)
        if (!live) {
            backend.spawnDisplay(viewer, id, EntityType.BLOCK_DISPLAY, point.x, point.y, point.z, values)
            state.liveVirtualIds += id
        } else {
            backend.sendMetadata(viewer, id, values)
        }
        state.contentFingerprintByKey[key] = fingerprint
        state.pointByKey[key] = point
        state.typeByKey[key] = EntityType.BLOCK_DISPLAY
        state.poseByScreenKey[screenKey] = pose
    }

    /**
     * 内容物（背景以外）を破棄します。閉じる演出の入口用です。
     *
     * 背景キーは残し、縮小波の対象にします。hover は呼び出し側で別途破棄します。
     */
    fun destroyContents(viewer: Player, state: GestureViewerRenderState, screenKey: String) {
        state.virtualIdByKey.keys
            .filter { it.startsWith("$screenKey/") && it != "$screenKey/bg" }
            .filterNot(::isHoverManagedKey)
            .toList().forEach { key ->
                state.virtualIdByKey.remove(key)?.let { id ->
                    backend.sendDestroy(viewer, listOf(id))
                    state.liveVirtualIds -= id
                }
                state.contentFingerprintByKey.remove(key)
                state.pointByKey.remove(key)
                state.typeByKey.remove(key)
                state.quatByKey.remove(key)
            }
    }

    /**
     * ダミー鍵の微動を相対移動で送ります。成功時は想定座標を進めて true を返します。
     *
     * 範囲外（±8 ブロック超）・drift 超過時は false を返し、呼び出し側で
     * destroy+spawn 再同期します。送信失敗時は想定がずれるため、次回 drift 検出で
     * 自動的に再同期されます。
     * 向きの変化は metadata で追従します（再生成なし・補間あり）。
     */
    private fun tryRelativeMove(
        viewer: Player,
        state: GestureViewerRenderState,
        id: Int,
        item: DesiredVisual,
        assumed: PlacedPoint,
        quat: Quaternionf,
    ): Boolean {
        val dx = GestureGuiProtocolLibBackend.relativeShort(item.point.x - assumed.x) ?: return false
        val dy = GestureGuiProtocolLibBackend.relativeShort(item.point.y - assumed.y) ?: return false
        val dz = GestureGuiProtocolLibBackend.relativeShort(item.point.z - assumed.z) ?: return false
        val assumedAfter = PlacedPoint(
            assumed.x + dx / 4096.0,
            assumed.y + dy / 4096.0,
            assumed.z + dz / 4096.0,
            0f,
            0f,
        )
        if (GestureGuiProtocolLibBackend.exceedsDrift(item.point.x, assumedAfter.x) ||
            GestureGuiProtocolLibBackend.exceedsDrift(item.point.y, assumedAfter.y) ||
            GestureGuiProtocolLibBackend.exceedsDrift(item.point.z, assumedAfter.z)
        ) {
            return false
        }
        // 相対移動の向き欄は entity 回転と無関係のため 0 固定です（向きは変形が担います）。
        backend.sendRelativeMove(viewer, id, dx, dy, dz, 0f, 0f)
        state.pointByKey[item.key] = assumedAfter
        val oldQuat = state.quatByKey[item.key]
        if (oldQuat == null || quatAngleDegrees(oldQuat, quat) > DUMMY_QUAT_UPDATE_DEGREES) {
            backend.sendMetadata(viewer, id, item.metadata(viewer))
            state.quatByKey[item.key] = Quaternionf(quat)
        }
        return true
    }

    private fun upsertSingle(
        viewer: Player,
        state: GestureViewerRenderState,
        key: String,
        type: EntityType,
        point: PlacedPoint,
        fingerprint: String,
        poseChanged: Boolean,
        metadata: (Player) -> List<WrappedDataValue>,
    ) {
        val id = state.virtualIdByKey.getOrPut(key) { backend.nextVirtualId() }
        val live = id in state.liveVirtualIds
        if (!live || poseChanged) {
            if (live) backend.sendDestroy(viewer, listOf(id))
            backend.spawnDisplay(viewer, id, type, point.x, point.y, point.z, metadata(viewer))
            state.liveVirtualIds += id
            state.contentFingerprintByKey[key] = fingerprint
        } else if (state.contentFingerprintByKey[key] != fingerprint) {
            backend.sendMetadata(viewer, id, metadata(viewer))
            state.contentFingerprintByKey[key] = fingerprint
        }
    }

    private fun removeSingle(viewer: Player, state: GestureViewerRenderState, key: String) {
        state.virtualIdByKey.remove(key)?.let { id ->
            backend.sendDestroy(viewer, listOf(id))
            state.liveVirtualIds -= id
        }
        state.contentFingerprintByKey.remove(key)
    }

    private fun alignmentFlags(alignment: GestureGuiTextAlignment): Byte = when (alignment) {
        GestureGuiTextAlignment.LEFT -> TEXT_ALIGN_LEFT
        GestureGuiTextAlignment.RIGHT -> TEXT_ALIGN_RIGHT
        GestureGuiTextAlignment.CENTER -> 0
    }

    private data class QuadPart(val x: Double, val y: Double, val width: Double, val height: Double)

    companion object {
        const val LAYER_DEPTH: Double = 0.005
        const val TEXT_ITEM_SURFACE_LIFT: Double = 0.0005
        const val PANEL_BACKGROUND_LAYER: Double = 0.0
        const val PANEL_FRAME_LAYER: Double = 6.0
        const val MODAL_OVERLAY_LAYER: Double = 48.0
        const val OUTLINE_LAYER_OFFSET: Double = 0.5
        const val BLOCK_NORMAL_DEPTH: Float = 0.025f
        const val TEXT_BASELINE_OFFSET: Double = 0.018
        val DUMMY_PANEL_MATERIAL: Material = Material.GLASS

        /** hover 置換が対象より前面へ浮く論理層数です（16層×0.005×0.25≒0.02 block）。 */
        const val HOVER_FLOAT_LAYERS: Int = 16
        const val MAX_LAYER: Int = 40

        fun hoverReplaceLayer(baseLayer: Int): Int = (baseLayer + HOVER_FLOAT_LAYERS).coerceAtMost(MAX_LAYER)

        /**
         * hover 経路が専有するキーかを返します。
         *
         * 通常画面の過剰分掃除はこれらを除外し、生成直後の削除を防ぎます。
         */
        fun isHoverManagedKey(key: String): Boolean =
            key.endsWith("/hover") || key.endsWith("/hoverBlock")

        /** ダミー向きの metadata 追従しきい値（度）です。これ以下は送りません。 */
        const val DUMMY_QUAT_UPDATE_DEGREES: Double = 0.5

        /**
         * 2 向きのなす角（度）を返します。同一回転の符号違いは 0 になります。
         *
         * ダミー追従の向き変化検出に使い、微動での metadata 連投を防ぎます。
         */
        fun quatAngleDegrees(first: Quaternionf, second: Quaternionf): Double {
            val dot = kotlin.math.abs(first.dot(second)).toDouble().coerceIn(0.0, 1.0)
            return Math.toDegrees(2.0 * kotlin.math.acos(dot))
        }

        /**
         * Block・Item 系の向き quaternion です。local +X→right・+Y→up・+Z→−normal
         *（閲覧者向き）へ写す適正回転であり、角度を経由しないため byte 量子化を受けません。
         */
        fun blockQuat(pose: GestureGuiScreenPose): Quaternionf =
            quatFromColumns(pose.right, pose.up, pose.normal * -1.0)

        /**
         * Text 系の向き quaternion です。文字の可視面（local −Z）が閲覧者向きに
         * なるよう、Block 系から local yaw 180° 反転させます（旧 textDisplayYaw 相当）。
         */
        fun textQuat(pose: GestureGuiScreenPose): Quaternionf =
            quatFromColumns(pose.right * -1.0, pose.up, pose.normal)

        /**
         * 矩形 Block 面の中心合わせ平行移動です。
         *
         * entity 回転が 0 のため、local (w/2,h/2,d/2) 中心を quaternion で
         * 同一回転させた逆向きが中心化ベクトルになります。spawn 座標が
         * 厳密に面中心へ一致し、向きの慣習に依存しません。
         */
        fun centeredTranslation(quat: Quaternionf, width: Double, height: Double, depth: Float): Vector3f {
            val half = Vector3f((width / 2.0).toFloat(), (height / 2.0).toFloat(), depth / 2f)
            return quat.transform(half).negate()
        }

        private fun quatFromColumns(
            xAxis: com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3,
            yAxis: com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3,
            zAxis: com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3,
        ): Quaternionf {
            // 列ベクトル指定であり、順序の解釈余地はありません。
            val matrix = Matrix3f(
                Vector3f(xAxis.x.toFloat(), xAxis.y.toFloat(), xAxis.z.toFloat()),
                Vector3f(yAxis.x.toFloat(), yAxis.y.toFloat(), yAxis.z.toFloat()),
                Vector3f(zAxis.x.toFloat(), zAxis.y.toFloat(), zAxis.z.toFloat()),
            )
            return Quaternionf().setFromUnnormalized(matrix).normalize()
        }
    }
}
