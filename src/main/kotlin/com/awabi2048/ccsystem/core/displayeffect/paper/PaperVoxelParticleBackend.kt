package com.awabi2048.ccsystem.core.displayeffect.paper

import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectDisposalReason
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectWorldUnavailableException
import com.awabi2048.ccsystem.core.displayeffect.VoxelParticlePattern
import com.awabi2048.ccsystem.core.displayeffect.VoxelParticleState
import com.awabi2048.ccsystem.core.displayeffect.VoxelParticleBackend
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import java.util.UUID

/** バニラBlockDataを微小立方体として描画する、billboard非依存のPaper backendです。 */
internal class PaperVoxelParticleBackend(
    private val plugin: Plugin,
    origin: Location,
    private val resolver: PaperMaterialAssetResolver = PaperMaterialAssetResolver(),
    private val ownerPluginName: String,
    private val instanceId: UUID
) : VoxelParticleBackend {
    private data class ManagedVoxel(
        val groupIndex: Int,
        val voxelIndex: Int,
        val entity: BlockDisplay
    )

    private val anchor = origin.clone()
    private val world = requireNotNull(anchor.world) { "ボクセル粒子のanchorにはWorldが必要です" }
    private val managed = mutableListOf<ManagedVoxel>()
    private val instanceKey = NamespacedKey(plugin, "display-effect-instance")
    private val ownerKey = NamespacedKey(plugin, "display-effect-owner")

    override fun create(pattern: VoxelParticlePattern, states: List<VoxelParticleState>) {
        requireMainThread()
        // 全素材をspawn前に解決し、素材不正による部分生成を防ぎます。
        val blockData = pattern.voxels.map { resolver.resolveBlock(it.blockAssetId) }
        try {
            states.forEachIndexed { groupIndex, state ->
                pattern.voxels.forEachIndexed { voxelIndex, voxel ->
                    val location = location(state, voxel.centerOffset)
                    val entity = world.spawn(location, BlockDisplay::class.java)
                    // spawn直後の設定処理が失敗した場合も、この個体を全体ロールバックの対象に含めます。
                    managed += ManagedVoxel(groupIndex, voxelIndex, entity)
                    entity.setBlock(blockData[voxelIndex].clone())
                    configure(entity, voxel.size)
                    markOwned(entity)
                }
            }
        } catch (failure: Throwable) {
            disposeAll(DisplayEffectDisposalReason.FAILED)
            throw failure
        }
    }

    override fun apply(pattern: VoxelParticlePattern, states: List<VoxelParticleState>) {
        requireMainThread()
        managed.forEach { managedVoxel ->
            val target = location(
                states[managedVoxel.groupIndex],
                pattern.voxels[managedVoxel.voxelIndex].centerOffset
            )
            check(managedVoxel.entity.teleport(target)) { "ボクセル粒子の移動に失敗しました" }
        }
    }

    override fun isAlive(): Boolean {
        requireMainThread()
        if (Bukkit.getWorld(world.uid) == null) {
            throw DisplayEffectWorldUnavailableException("ボクセル粒子のWorldが利用できません: ${world.uid}")
        }
        return managed.isNotEmpty() && managed.all { it.entity.isValid && !it.entity.isDead }
    }

    override fun disposeAll(reason: DisplayEffectDisposalReason) {
        requireMainThread()
        managed.asReversed().forEach { voxel ->
            runCatching { if (voxel.entity.isValid && !voxel.entity.isDead) voxel.entity.remove() }
                .onFailure { plugin.logger.warning("[VoxelParticle] cleanup failed: reason=$reason error=${it.message}") }
        }
        managed.clear()
    }

    private fun configure(entity: BlockDisplay, size: Double) {
        entity.setPersistent(false)
        entity.setInvulnerable(true)
        entity.setSilent(true)
        entity.setBillboard(Display.Billboard.FIXED)
        entity.setShadowRadius(0.0f)
        entity.setShadowStrength(0.0f)
        entity.setViewRange(32.0f)
        entity.setInterpolationDuration(0)
        entity.setTeleportDuration(1)
        val half = (-size / 2.0).toFloat()
        val scale = size.toFloat()
        entity.setTransformation(
            Transformation(
                Vector3f(half, half, half),
                AxisAngle4f(),
                Vector3f(scale, scale, scale),
                AxisAngle4f()
            )
        )
    }

    private fun markOwned(entity: BlockDisplay) {
        entity.persistentDataContainer.set(instanceKey, PersistentDataType.STRING, instanceId.toString())
        entity.persistentDataContainer.set(ownerKey, PersistentDataType.STRING, ownerPluginName)
        entity.addScoreboardTag("ccsystem.voxel-particle")
    }

    private fun location(state: VoxelParticleState, local: com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3): Location {
        val target = anchor.clone().add(
            state.originOffset.x + local.x,
            state.originOffset.y + local.y,
            state.originOffset.z + local.z
        )
        if (!world.isChunkLoaded(target.blockX shr 4, target.blockZ shr 4)) {
            throw DisplayEffectWorldUnavailableException("ボクセル粒子の移動先chunkが未ロードです: $target")
        }
        return target
    }

    private fun requireMainThread() = check(Bukkit.isPrimaryThread()) {
        "PaperVoxelParticleBackendはメインスレッドから呼び出してください"
    }
}
