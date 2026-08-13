package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectQuaternion
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticlePresetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticlePresetInfo

/** 単一BlockDisplayの外観・運動・寿命をまとめた不変プリセットです。 */
internal data class DisplayParticlePreset(
    val id: DisplayParticlePresetId,
    val textures: List<DisplayParticleTexture>,
    val initialScale: DisplayEffectVector3,
    val peakScale: DisplayEffectVector3,
    val peakScaleProgress: Double,
    val scaleInTicks: Int,
    val motionPresetId: String,
    val initialVelocity: DisplayEffectVector3,
    val initialRotation: DisplayEffectQuaternion,
    val angularVelocityRadiansPerTick: DisplayEffectVector3,
    val scaleVariation: Double,
    val angularVelocityVariation: Double,
    val randomInitialRotation: Boolean,
    val lifetimeTicks: Int,
    val lifetimeVariationTicks: Int,
    val fadeOutTicks: Int,
    val fadeOutVariationTicks: Int,
    val maxSpawnDelayTicks: Int
) {
    init {
        require(textures.isNotEmpty()) { "Displayパーティクルには1種類以上のtextureが必要です" }
        require(textures.sumOf { it.weight.toLong() } <= Int.MAX_VALUE) { "texture weightの合計が大きすぎます" }
        listOf(initialScale, peakScale).forEach { scale ->
            require(scale.x in 0.0..0.5 && scale.y in 0.0..0.5 && scale.z in 0.0..0.5) {
                "Displayパーティクルのscaleは各軸0..0.5です: $scale"
            }
        }
        require(peakScaleProgress in 0.0..1.0) { "peakScaleProgressが不正です" }
        val earliestFadeStart =
            (lifetimeTicks - lifetimeVariationTicks) - (fadeOutTicks + fadeOutVariationTicks)
        require(scaleInTicks in 1 until earliestFadeStart) {
            "scaleInTicksは個体差を含む最短のfade開始より前に完了する1以上の値です"
        }
        DisplayParticleMotionCatalog.require(motionPresetId)
        require(scaleVariation in 0.0..0.9) { "scaleVariationは0..0.9です" }
        require(angularVelocityVariation in 0.0..1.0) { "angularVelocityVariationは0..1です" }
        require(lifetimeTicks in 2..200) { "lifetimeTicksは2..200です" }
        require(lifetimeVariationTicks in 0 until lifetimeTicks) { "lifetimeVariationTicksが不正です" }
        require(fadeOutTicks in 1 until lifetimeTicks) { "fadeOutTicksは寿命より短い1以上の値です" }
        require(fadeOutVariationTicks in 0 until fadeOutTicks) { "fadeOutVariationTicksが不正です" }
        require(fadeOutTicks + fadeOutVariationTicks < lifetimeTicks - lifetimeVariationTicks) {
            "個体差適用後もfadeOutTicksはlifetimeTicksより短くなければなりません"
        }
        require(maxSpawnDelayTicks in 0..20) { "maxSpawnDelayTicksは0..20です" }
    }

    fun info() = DisplayParticlePresetInfo(id, lifetimeTicks, fadeOutTicks)
}

/** 同一プリセット内で選択できるBlockテクスチャと正の整数重みです。 */
internal data class DisplayParticleTexture(val assetId: DisplayEffectAssetId, val weight: Int) {
    init { require(weight > 0) { "Displayパーティクルのtexture weightは1以上です" } }
}

/** バニラ粒子の再現表ではなく、独自表現のためのプロパティ組合せ例を管理します。 */
internal object DisplayParticleCatalog {
    private val patterns = listOf(
        preset("cc:ember", "cc:buoyant", textures("orange_concrete" to 5, "yellow_concrete" to 3, "red_concrete" to 2), 0.045, 0.12, 0.22, 3, velocity(0.0, 0.025, 0.0), velocity(0.08, 0.13, 0.05), 0.22, 0.35, 24, 3, 7, 2, 2),
        preset("cc:ash", "cc:drift", textures("gray_concrete" to 5, "light_gray_concrete" to 3, "white_concrete" to 1, "black_concrete" to 1), 0.11, 0.15, 0.35, 5, velocity(0.006, 0.012, -0.004), velocity(0.025, 0.04, 0.02), 0.18, 0.45, 38, 5, 12, 3, 4),
        preset("cc:spark", "cc:burst", textures("yellow_concrete" to 5, "white_concrete" to 2, "orange_concrete" to 2), 0.075, 0.105, 0.12, 2, velocity(0.0, 0.035, 0.0), velocity(0.18, 0.22, 0.12), 0.25, 0.30, 14, 2, 5, 1, 1),
        preset("cc:verdant", "cc:orbit", textures("lime_concrete" to 5, "green_concrete" to 3, "white_concrete" to 1), 0.06, 0.14, 0.40, 4, velocity(0.0, 0.018, 0.0), velocity(0.04, 0.09, 0.03), 0.20, 0.40, 28, 4, 8, 2, 3)
    )
    private val byId = patterns.associateBy { it.id }

    fun list(): List<DisplayParticlePreset> = patterns
    fun find(id: DisplayParticlePresetId): DisplayParticlePreset? = byId[id]

    private fun preset(
        id: String,
        motionPresetId: String,
        textures: List<DisplayParticleTexture>,
        initialSize: Double,
        peakSize: Double,
        peakProgress: Double,
        scaleInTicks: Int,
        initialVelocity: DisplayEffectVector3,
        angularVelocity: DisplayEffectVector3,
        scaleVariation: Double,
        angularVariation: Double,
        lifetime: Int,
        lifetimeVariation: Int,
        fade: Int,
        fadeVariation: Int,
        spawnDelay: Int
    ) = DisplayParticlePreset(
        DisplayParticlePresetId(id),
        textures,
        scale(initialSize),
        scale(peakSize),
        peakProgress,
        scaleInTicks,
        motionPresetId,
        initialVelocity,
        DisplayEffectQuaternion.IDENTITY,
        angularVelocity,
        scaleVariation,
        angularVariation,
        randomInitialRotation = true,
        lifetime,
        lifetimeVariation,
        fade,
        fadeVariation,
        spawnDelay
    )

    private fun scale(value: Double) = DisplayEffectVector3(value, value, value)
    private fun velocity(x: Double, y: Double, z: Double) = DisplayEffectVector3(x, y, z)
    private fun textures(vararg entries: Pair<String, Int>) = entries.map {
        DisplayParticleTexture(DisplayEffectAssetId("minecraft:${it.first}"), it.second)
    }
}
