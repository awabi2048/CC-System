package com.awabi2048.ccsystem.core.displayeffect.paper

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAppearance
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetResolver
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetResolutionException
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectFrame
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectLight
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectBackend
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectDisposalReason
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectHandle
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectSpawnRequest
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectWorldUnavailableException
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.data.BlockData
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3f

/** バニラ素材IDをBlockData／ItemStackへ変換する初期実装です。 */
internal class PaperMaterialAssetResolver : DisplayEffectAssetResolver {
    override fun resolveBlock(assetId: DisplayEffectAssetId): BlockData {
        val material = resolveMaterial(assetId, "BlockDisplay")
        require(material.isBlock) {
            "Display EffectのBlockアセットにはブロック素材を指定してください: ${assetId.value}"
        }
        return material.createBlockData()
    }

    override fun resolveItem(assetId: DisplayEffectAssetId): ItemStack {
        val material = resolveMaterial(assetId, "ItemDisplay")
        require(material.isItem && material != Material.AIR) {
            "Display EffectのItemアセットにはアイテム素材を指定してください: ${assetId.value}"
        }
        return ItemStack(material, 1)
    }

    private fun resolveMaterial(assetId: DisplayEffectAssetId, displayType: String): Material {
        val namespace = assetId.value.substringBefore(':')
        require(namespace == "minecraft") {
            "${displayType}の標準アセット解決はminecraft namespaceだけを扱います。" +
                "カスタム素材はDisplayEffectAssetResolverを差し替えてください: ${assetId.value}"
        }
        val path = assetId.value.substringAfter(':')
        val material = Material.matchMaterial(path)
        require(material != null && material != Material.AIR) {
            "${displayType}の素材が見つかりません: ${assetId.value}"
        }
        return material
    }
}

/**
 * Paper固有の描画設定です。
 *
 * 初期値はシミュレーション結果とクライアント表示の一致を優先して補間を無効化
 * しています。パーティクル風の滑らかな表示が必要な定義では、利用側で1 tickの
 * 補間とCENTER billboardを選択できます。
 */
internal data class PaperDisplayEffectRenderConfig(
    val interpolationDurationTicks: Int = 0,
    val interpolationDelayTicks: Int = 0,
    val teleportDurationTicks: Int = 0,
    val viewRange: Float = 32.0f,
    // Vanilla particleのように視点へ正対する初期表示です。固定姿勢が必要な
    // BlockDisplay用途では、利用側がFIXEDへ変更できます。
    val billboard: Display.Billboard = Display.Billboard.FIXED,
    val itemDisplayTransform: ItemDisplay.ItemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE,
    val visibleByDefault: Boolean = true,
    val persistent: Boolean = false,
    val invulnerable: Boolean = true,
    val silent: Boolean = true
) {
    init {
        require(interpolationDurationTicks in 0..59) {
            "Display EffectのinterpolationDurationTicksは0..59で指定してください: $interpolationDurationTicks"
        }
        require(interpolationDelayTicks in 0..59) {
            "Display EffectのinterpolationDelayTicksは0..59で指定してください: $interpolationDelayTicks"
        }
        require(teleportDurationTicks in 0..59) {
            "Display EffectのteleportDurationTicksは0..59で指定してください: $teleportDurationTicks"
        }
        require(viewRange.isFinite() && viewRange > 0.0f) {
            "Display EffectのviewRangeは正の有限値で指定してください: $viewRange"
        }
    }
}

/**
 * 固定anchorを起点にBlockDisplay／ItemDisplayを管理するPaper backendです。
 *
 * このクラスはメインスレッドからのみ呼び出します。anchorは生成後に追従させない
 * 固定発生点なので、プレイヤーなどを追跡する演出は上位層でbackendを作り直します。
 */
internal class PaperDisplayEffectBackend(
    private val plugin: Plugin,
    origin: Location,
    private val assetResolver: DisplayEffectAssetResolver = PaperMaterialAssetResolver(),
    private val ownerPluginName: String? = null,
    private val renderConfig: PaperDisplayEffectRenderConfig = PaperDisplayEffectRenderConfig()
) : DisplayEffectBackend {
    private data class ManagedDisplay(
        val nodeId: String,
        val entity: Display
    )

    private data class PreparedFrame(
        val location: Location,
        val transformation: Transformation,
        val brightness: Display.Brightness?
    )

    private sealed interface ResolvedAppearance {
        data class Block(val data: BlockData) : ResolvedAppearance
        data class Item(val stack: ItemStack) : ResolvedAppearance
    }

    private val anchor: Location = origin.clone().also { location ->
        requireNotNull(location.world) { "Display EffectのanchorにはWorldが必要です" }
        require(location.x.isFinite() && location.y.isFinite() && location.z.isFinite()) {
            "Display Effectのanchor座標は有限値で指定してください: $location"
        }
    }
    private val world = requireNotNull(anchor.world)
    private val ownerKey = NamespacedKey(plugin, "display-effect")
    private val instanceKey = NamespacedKey(plugin, "display-effect-instance")
    private val nodeKey = NamespacedKey(plugin, "display-effect-node")
    private val consumerKey = NamespacedKey(plugin, "display-effect-owner")
    private val schemaKey = NamespacedKey(plugin, "display-effect-schema")
    private val managedDisplays = linkedMapOf<DisplayEffectHandle, ManagedDisplay>()
    private var nextToken = 1L

    override fun create(request: DisplayEffectSpawnRequest): DisplayEffectHandle {
        requireMainThread()
        check(plugin.isEnabled) { "Display Effect backendのPluginが有効ではありません" }

        // アセットとFloat変換をspawn前に済ませ、初期化途中のEntityを孤児化させません。
        val appearance = resolveAppearance(request.appearance)
        val preparedFrame = prepareFrame(request.initialFrame)
        var entity: Display? = null
        return try {
            entity = when (appearance) {
                is ResolvedAppearance.Block -> world.spawn(preparedFrame.location, BlockDisplay::class.java)
                is ResolvedAppearance.Item -> world.spawn(preparedFrame.location, ItemDisplay::class.java)
            }
            configureEntity(entity)
            when (appearance) {
                is ResolvedAppearance.Block -> (entity as BlockDisplay).setBlock(appearance.data.clone())
                is ResolvedAppearance.Item -> (entity as ItemDisplay).setItemStack(appearance.stack.clone())
            }
            applyPreparedFrame(entity, preparedFrame)
            markOwned(entity, request)

            val handle = nextHandle()
            // ここで初めて管理対象へ登録することで、create途中失敗時のcleanup責任を
            // このメソッド内に閉じ込め、Runtime側のhandle未取得問題を避けます。
            managedDisplays[handle] = ManagedDisplay(request.nodeId.value, entity)
            handle
        } catch (failure: Throwable) {
            entity?.remove()
            throw failure
        }
    }

    override fun apply(handle: DisplayEffectHandle, frame: DisplayEffectFrame) {
        requireMainThread()
        val managed = managedDisplays[handle]
            ?: throw IllegalStateException("未登録のDisplay Effect handleへapplyされました: $handle")
        val preparedFrame = prepareFrame(frame)
        check(managed.entity.isValid && !managed.entity.isDead) {
            "Display Effect Entityが無効です: node=${managed.nodeId} uuid=${managed.entity.uniqueId}"
        }
        applyPreparedFrame(managed.entity, preparedFrame)
    }

    override fun isAlive(handle: DisplayEffectHandle): Boolean {
        requireMainThread()
        val managed = managedDisplays[handle] ?: return false
        if (Bukkit.getWorld(world.uid) == null) {
            throw DisplayEffectWorldUnavailableException("Display EffectのWorldがアンロードされています: ${world.uid}")
        }
        if (!managed.entity.isValid || managed.entity.isDead) return false
        val location = managed.entity.location
        if (!isChunkLoaded(location)) {
            throw DisplayEffectWorldUnavailableException("Display EffectのEntity chunkが未ロードです: $location")
        }
        return true
    }

    override fun dispose(handle: DisplayEffectHandle, reason: DisplayEffectDisposalReason) {
        requireMainThread()
        val managed = managedDisplays.remove(handle) ?: return
        runCatching {
            if (managed.entity.isValid && !managed.entity.isDead) {
                managed.entity.remove()
            }
        }.onFailure { failure ->
            // Runtimeはcleanup例外を主処理へ再送しない契約なので、Paper側で追跡可能にします。
            plugin.logger.warning(
                "[DisplayEffect] Entity cleanup failed: node=${managed.nodeId} " +
                    "uuid=${managed.entity.uniqueId} reason=$reason error=${failure.message}"
            )
        }
    }

    /** Plugin停止時など、Runtimeを経由できない残存Entityを回収します。 */
    internal fun disposeAll(reason: DisplayEffectDisposalReason = DisplayEffectDisposalReason.SHUTDOWN) {
        requireMainThread()
        managedDisplays.keys.toList().forEach { handle -> dispose(handle, reason) }
    }

    private fun resolveAppearance(appearance: DisplayEffectAppearance): ResolvedAppearance = try {
        when (appearance) {
            is DisplayEffectAppearance.Block -> ResolvedAppearance.Block(assetResolver.resolveBlock(appearance.assetId))
            is DisplayEffectAppearance.Item -> ResolvedAppearance.Item(assetResolver.resolveItem(appearance.assetId))
        }
    } catch (failure: DisplayEffectAssetResolutionException) {
        throw failure
    } catch (failure: Exception) {
        throw DisplayEffectAssetResolutionException(
            appearance.assetId,
            "Display Effectのアセット解決に失敗しました: ${appearance.assetId.value}",
            failure
        )
    }

    private fun configureEntity(entity: Display) {
        entity.setPersistent(renderConfig.persistent)
        entity.setInvulnerable(renderConfig.invulnerable)
        entity.setSilent(renderConfig.silent)
        entity.setVisibleByDefault(renderConfig.visibleByDefault)
        entity.setViewRange(renderConfig.viewRange)
        entity.setBillboard(renderConfig.billboard)
        entity.setInterpolationDuration(renderConfig.interpolationDurationTicks)
        entity.setInterpolationDelay(renderConfig.interpolationDelayTicks)
        entity.setTeleportDuration(renderConfig.teleportDurationTicks)
        if (entity is ItemDisplay) {
            entity.setItemDisplayTransform(renderConfig.itemDisplayTransform)
        }
    }

    private fun markOwned(entity: Display, request: DisplayEffectSpawnRequest) {
        val container = entity.persistentDataContainer
        container.set(ownerKey, PersistentDataType.BYTE, 1.toByte())
        container.set(instanceKey, PersistentDataType.STRING, request.instanceId.toString())
        container.set(nodeKey, PersistentDataType.STRING, request.nodeId.value)
        ownerPluginName?.let { container.set(consumerKey, PersistentDataType.STRING, it) }
        container.set(schemaKey, PersistentDataType.INTEGER, 1)
        entity.addScoreboardTag(OWNER_TAG)
    }

    private fun prepareFrame(frame: DisplayEffectFrame): PreparedFrame {
        val targetLocation = worldLocation(frame.originOffset)
        if (!isChunkLoaded(targetLocation)) {
            throw DisplayEffectWorldUnavailableException(
                "Display Effectの移動先chunkは未ロードのため、強制ロードせず停止します: $targetLocation"
            )
        }
        val translation = Vector3f(
            finiteFloat(frame.localTranslation.x, "localTranslation.x"),
            finiteFloat(frame.localTranslation.y, "localTranslation.y"),
            finiteFloat(frame.localTranslation.z, "localTranslation.z")
        )
        val scale = Vector3f(
            finiteFloat(frame.scale.x, "scale.x"),
            finiteFloat(frame.scale.y, "scale.y"),
            finiteFloat(frame.scale.z, "scale.z")
        )
        val quaternion = Quaternionf(
            finiteFloat(frame.rotation.x, "rotation.x"),
            finiteFloat(frame.rotation.y, "rotation.y"),
            finiteFloat(frame.rotation.z, "rotation.z"),
            finiteFloat(frame.rotation.w, "rotation.w")
        )
        val rotation = AxisAngle4f(quaternion)
        require(rotation.angle.isFinite() && rotation.x.isFinite() && rotation.y.isFinite() && rotation.z.isFinite()) {
            "Display Effectの回転をPaper形式へ変換できません: ${frame.rotation}"
        }
        return PreparedFrame(
            location = targetLocation,
            transformation = Transformation(
                translation,
                rotation,
                scale,
                // rightRotationは初期仕様では使用せず、回転の合成順を固定します。
                AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f)
            ),
            brightness = frame.brightness?.let(::toPaperBrightness)
        )
    }

    private fun applyPreparedFrame(entity: Display, frame: PreparedFrame) {
        check(entity.teleport(frame.location)) {
            "Display Effect Entityのteleportに失敗しました: ${entity.uniqueId}"
        }
        entity.setTransformation(frame.transformation)
        entity.setBrightness(frame.brightness)
    }

    private fun toPaperBrightness(light: DisplayEffectLight): Display.Brightness =
        Display.Brightness(light.block, light.sky)

    private fun worldLocation(offset: com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3): Location {
        val x = finiteCoordinate(anchor.x + offset.x, "x")
        val y = finiteCoordinate(anchor.y + offset.y, "y")
        val z = finiteCoordinate(anchor.z + offset.z, "z")
        return anchor.clone().apply {
            this.x = x
            this.y = y
            this.z = z
        }
    }

    private fun isChunkLoaded(location: Location): Boolean {
        val blockX = location.blockX
        val blockZ = location.blockZ
        return world.isChunkLoaded(blockX shr 4, blockZ shr 4)
    }

    private fun nextHandle(): DisplayEffectHandle {
        check(nextToken != Long.MAX_VALUE) { "Display Effect handleが枯渇しました" }
        return DisplayEffectHandle(nextToken++)
    }

    private fun requireMainThread() {
        check(Bukkit.isPrimaryThread()) {
            "PaperDisplayEffectBackendはメインスレッドから呼び出してください"
        }
    }

    private fun finiteCoordinate(value: Double, axis: String): Double {
        require(value.isFinite()) { "Display Effectの${axis}座標が有限値ではありません: $value" }
        require(value in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()) {
            "Display Effectの${axis}座標がPaperのchunk座標範囲外です: $value"
        }
        return value
    }

    private fun finiteFloat(value: Double, component: String): Float {
        val converted = value.toFloat()
        require(value.isFinite() && converted.isFinite()) {
            "Display Effectの${component}をPaperのFloatへ変換できません: $value"
        }
        return converted
    }

    private companion object {
        private const val OWNER_TAG = "ccsystem.display-effect"
    }
}
