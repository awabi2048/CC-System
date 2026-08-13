package com.awabi2048.ccsystem.core.displayeffect.paper

import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectDisposalReason
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectWorldUnavailableException
import com.awabi2048.ccsystem.core.displayeffect.DisplayParticleBackend
import com.awabi2048.ccsystem.core.displayeffect.DisplayParticlePreset
import com.awabi2048.ccsystem.core.displayeffect.DisplayParticleState
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID

/** 1パーティクルを1つの微小BlockDisplayとして描画する、billboard非依存のbackendです。 */
internal class PaperDisplayParticleBackend(
    private val plugin: Plugin,
    origin: Location,
    private val resolver: PaperMaterialAssetResolver,
    private val ownerPluginName: String,
    private val instanceId: UUID
) : DisplayParticleBackend {
    private val anchor = origin.clone()
    private val world = requireNotNull(anchor.world) { "DisplayパーティクルのanchorにはWorldが必要です" }
    private val managed = mutableListOf<BlockDisplay?>()
    private val instanceKey = NamespacedKey(plugin, "display-effect-instance")
    private val ownerKey = NamespacedKey(plugin, "display-effect-owner")

    override fun create(preset: DisplayParticlePreset, states: List<DisplayParticleState>) {
        requireMainThread()
        // 候補をすべてspawn前に検証し、低確率素材だけが実行時に部分失敗することを防ぎます。
        val blockDataByAsset = preset.textures.associate { it.assetId to resolver.resolveBlock(it.assetId) }
        try {
            states.forEach { state ->
                val entity = world.spawn(location(state), BlockDisplay::class.java)
                // spawn後の設定失敗も同じ生成要求の原子的ロールバック対象へ含めます。
                managed += entity
                entity.setBlock(requireNotNull(blockDataByAsset[state.textureAssetId]).clone())
                configure(entity)
                markOwned(entity)
                applyTransform(entity, state)
            }
        } catch (failure: Throwable) {
            disposeAll(DisplayEffectDisposalReason.FAILED)
            throw failure
        }
    }

    override fun apply(states: List<DisplayParticleState>) {
        requireMainThread()
        check(states.size == managed.size) { "Displayパーティクルの状態数とEntity数が一致しません" }
        managed.forEachIndexed { index, entity ->
            if (entity == null) return@forEachIndexed
            if (states[index].phase == com.awabi2048.ccsystem.core.displayeffect.DisplayParticlePhase.COMPLETED) {
                if (entity.isValid && !entity.isDead) entity.remove()
                managed[index] = null
                return@forEachIndexed
            }
            check(entity.teleport(location(states[index]))) { "Displayパーティクルの移動に失敗しました" }
            applyTransform(entity, states[index])
        }
    }

    override fun isAlive(): Boolean {
        requireMainThread()
        if (Bukkit.getWorld(world.uid) == null) throw DisplayEffectWorldUnavailableException("Worldが利用できません: ${world.uid}")
        val active = managed.filterNotNull()
        return active.isNotEmpty() && active.all { it.isValid && !it.isDead }
    }

    override fun disposeAll(reason: DisplayEffectDisposalReason) {
        requireMainThread()
        managed.asReversed().filterNotNull().forEach { entity ->
            runCatching { if (entity.isValid && !entity.isDead) entity.remove() }
                .onFailure { plugin.logger.warning("[DisplayParticle] cleanup failed: reason=$reason error=${it.message}") }
        }
        managed.clear()
    }

    private fun configure(entity: BlockDisplay) {
        entity.setPersistent(false)
        entity.setInvulnerable(true)
        entity.setSilent(true)
        entity.setBillboard(Display.Billboard.FIXED)
        entity.setShadowRadius(0.0f)
        entity.setShadowStrength(0.0f)
        entity.setViewRange(32.0f)
        entity.setInterpolationDuration(1)
        entity.setInterpolationDelay(0)
        entity.setTeleportDuration(1)
    }

    private fun applyTransform(entity: BlockDisplay, state: DisplayParticleState) {
        val scale = Vector3f(state.scale.x.toFloat(), state.scale.y.toFloat(), state.scale.z.toFloat())
        val quaternion = Quaternionf(state.rotation.x.toFloat(), state.rotation.y.toFloat(), state.rotation.z.toFloat(), state.rotation.w.toFloat())
        val rotation = AxisAngle4f(quaternion)
        // Blockモデルの中心(0.5, 0.5, 0.5)を回転・拡縮後もEntity原点へ固定します。
        val transformedCenter = quaternion.transform(Vector3f(scale).mul(0.5f))
        val translation = transformedCenter.negate()
        entity.setTransformation(Transformation(translation, rotation, scale, AxisAngle4f()))
    }

    private fun markOwned(entity: BlockDisplay) {
        entity.persistentDataContainer.set(instanceKey, PersistentDataType.STRING, instanceId.toString())
        entity.persistentDataContainer.set(ownerKey, PersistentDataType.STRING, ownerPluginName)
        entity.addScoreboardTag("ccsystem.display-particle")
    }

    private fun location(state: DisplayParticleState): Location {
        val target = anchor.clone().add(state.originOffset.x, state.originOffset.y, state.originOffset.z)
        if (!world.isChunkLoaded(target.blockX shr 4, target.blockZ shr 4)) {
            throw DisplayEffectWorldUnavailableException("移動先chunkが未ロードです: $target")
        }
        return target
    }

    private fun requireMainThread() = check(Bukkit.isPrimaryThread()) { "PaperDisplayParticleBackendはメインスレッドから呼び出してください" }
}
