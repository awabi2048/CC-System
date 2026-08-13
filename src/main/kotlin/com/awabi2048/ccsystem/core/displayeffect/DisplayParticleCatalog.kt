package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectQuaternion
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticlePresetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticlePresetInfo

/** 単一BlockDisplayの外観・運動・寿命をまとめた不変プリセットです。 */
internal data class DisplayParticlePreset(
    val id: DisplayParticlePresetId,
    val blockAssetId: DisplayEffectAssetId,
    val initialScale: DisplayEffectVector3,
    val peakScale: DisplayEffectVector3,
    val peakScaleProgress: Double,
    val initialVelocity: DisplayEffectVector3,
    val accelerationPerTick: DisplayEffectVector3,
    val velocityRetentionPerTick: Double,
    val initialRotation: DisplayEffectQuaternion,
    val angularVelocityRadiansPerTick: DisplayEffectVector3,
    val lifetimeTicks: Int,
    val fadeOutTicks: Int
) {
    init {
        listOf(initialScale, peakScale).forEach { scale ->
            require(scale.x in 0.0..0.5 && scale.y in 0.0..0.5 && scale.z in 0.0..0.5) {
                "Displayパーティクルのscaleは各軸0..0.5です: $scale"
            }
        }
        require(peakScaleProgress in 0.0..1.0) { "peakScaleProgressが不正です" }
        require(velocityRetentionPerTick in 0.0..1.0) { "velocityRetentionPerTickが不正です" }
        require(lifetimeTicks in 2..200) { "lifetimeTicksは2..200です" }
        require(fadeOutTicks in 1 until lifetimeTicks) { "fadeOutTicksは寿命より短い1以上の値です" }
    }

    fun info() = DisplayParticlePresetInfo(id, lifetimeTicks, fadeOutTicks)
}

/** バニラ粒子の再現表ではなく、独自表現のためのプロパティ組合せ例を管理します。 */
internal object DisplayParticleCatalog {
    private val patterns = listOf(
        preset("cc:ember", "orange_concrete", 0.045, 0.12, 0.22, velocity(0.0, 0.025, 0.0), velocity(0.0, 0.0015, 0.0), 0.96, velocity(0.08, 0.13, 0.05), 24, 7),
        preset("cc:ash", "gray_concrete", 0.11, 0.15, 0.35, velocity(0.006, 0.012, -0.004), velocity(0.0, 0.0005, 0.0), 0.97, velocity(0.025, 0.04, 0.02), 38, 12),
        preset("cc:spark", "yellow_concrete", 0.075, 0.105, 0.12, velocity(0.0, 0.035, 0.0), velocity(0.0, -0.004, 0.0), 0.91, velocity(0.18, 0.22, 0.12), 14, 5),
        preset("cc:verdant", "lime_concrete", 0.06, 0.14, 0.40, velocity(0.0, 0.018, 0.0), velocity(0.0, 0.0008, 0.0), 0.95, velocity(0.04, 0.09, 0.03), 28, 8)
    )
    private val byId = patterns.associateBy { it.id }

    fun list(): List<DisplayParticlePreset> = patterns
    fun find(id: DisplayParticlePresetId): DisplayParticlePreset? = byId[id]

    private fun preset(
        id: String,
        material: String,
        initialSize: Double,
        peakSize: Double,
        peakProgress: Double,
        initialVelocity: DisplayEffectVector3,
        acceleration: DisplayEffectVector3,
        retention: Double,
        angularVelocity: DisplayEffectVector3,
        lifetime: Int,
        fade: Int
    ) = DisplayParticlePreset(
        DisplayParticlePresetId(id),
        DisplayEffectAssetId("minecraft:$material"),
        scale(initialSize),
        scale(peakSize),
        peakProgress,
        initialVelocity,
        acceleration,
        retention,
        DisplayEffectQuaternion.IDENTITY,
        angularVelocity,
        lifetime,
        fade
    )

    private fun scale(value: Double) = DisplayEffectVector3(value, value, value)
    private fun velocity(x: Double, y: Double, z: Double) = DisplayEffectVector3(x, y, z)
}
