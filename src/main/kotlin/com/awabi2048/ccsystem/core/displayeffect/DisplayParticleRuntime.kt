package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectQuaternion
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleEmissionRequest
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal enum class DisplayParticlePhase { WAITING, ACTIVE, RENDER_GRACE, COMPLETED }

internal data class DisplayParticleState(
    val ageTicks: Int,
    val originOffset: DisplayEffectVector3,
    val velocity: DisplayEffectVector3,
    val rotation: DisplayEffectQuaternion,
    val scale: DisplayEffectVector3,
    val textureAssetId: DisplayEffectAssetId,
    val initialScale: DisplayEffectVector3,
    val peakScale: DisplayEffectVector3,
    val angularVelocityRadiansPerTick: DisplayEffectVector3,
    val lifetimeTicks: Int,
    val fadeOutTicks: Int,
    val phase: DisplayParticlePhase,
    val renderGraceTicks: Int
)

internal interface DisplayParticleBackend {
    fun create(preset: DisplayParticlePreset, states: List<DisplayParticleState>)
    fun apply(states: List<DisplayParticleState>)
    fun isAlive(): Boolean
    fun disposeAll(reason: DisplayEffectDisposalReason)
}

/** 単一ボクセル粒子を進行し、scale=0の最終フレームを描画してからEntityを破棄します。 */
internal class DisplayParticleRuntime(
    private val preset: DisplayParticlePreset,
    request: DisplayParticleEmissionRequest,
    private val backend: DisplayParticleBackend
) {
    private var states = initialStates(request)
    private var active = false
    val entityCount: Int = request.count

    fun start(): DisplayEffectRuntimeResult = runCatching {
        if (active) return DisplayEffectRuntimeResult.Ignored
        backend.create(preset, states)
        active = true
        DisplayEffectRuntimeResult.Started
    }.getOrElse { failure ->
        backend.disposeAll(DisplayEffectDisposalReason.FAILED)
        DisplayEffectRuntimeResult.Failed(failure)
    }

    fun tick(): DisplayEffectRuntimeResult {
        if (!active) return DisplayEffectRuntimeResult.Ignored
        return runCatching {
            if (states.all { it.phase == DisplayParticlePhase.COMPLETED }) return stop(DisplayEffectDisposalReason.EXPIRED)
            if (!backend.isAlive()) return stop(DisplayEffectDisposalReason.BACKEND_INVALIDATED)
            states = states.map(::advance)
            backend.apply(states)
            DisplayEffectRuntimeResult.Advanced
        }.getOrElse { failure ->
            if (failure is DisplayEffectWorldUnavailableException) stop(DisplayEffectDisposalReason.WORLD_UNAVAILABLE)
            else {
                active = false
                backend.disposeAll(DisplayEffectDisposalReason.FAILED)
                DisplayEffectRuntimeResult.Failed(failure)
            }
        }
    }

    fun stop(reason: DisplayEffectDisposalReason): DisplayEffectRuntimeResult {
        if (!active) return DisplayEffectRuntimeResult.Ignored
        active = false
        backend.disposeAll(reason)
        return DisplayEffectRuntimeResult.Stopped(reason)
    }

    private fun advance(state: DisplayParticleState): DisplayParticleState {
        if (state.phase == DisplayParticlePhase.COMPLETED) return state
        if (state.phase == DisplayParticlePhase.RENDER_GRACE) {
            val grace = state.renderGraceTicks - 1
            return state.copy(
                renderGraceTicks = grace,
                phase = if (grace <= 0) DisplayParticlePhase.COMPLETED else DisplayParticlePhase.RENDER_GRACE
            )
        }
        if (state.phase == DisplayParticlePhase.WAITING) {
            val age = state.ageTicks + 1
            return state.copy(
                ageTicks = age,
                scale = if (age == 0) state.initialScale else DisplayEffectVector3.ZERO,
                phase = if (age == 0) DisplayParticlePhase.ACTIVE else DisplayParticlePhase.WAITING
            )
        }
        val age = (state.ageTicks + 1).coerceAtMost(state.lifetimeTicks)
        val velocity = (state.velocity + preset.accelerationPerTick) * preset.velocityRetentionPerTick
        return state.copy(
            ageTicks = age,
            originOffset = state.originOffset + state.velocity,
            velocity = velocity,
            rotation = state.rotation.multiply(rotationDelta(state.angularVelocityRadiansPerTick)),
            scale = scaleAt(state, age),
            phase = if (age >= state.lifetimeTicks) DisplayParticlePhase.RENDER_GRACE else DisplayParticlePhase.ACTIVE,
            renderGraceTicks = if (age >= state.lifetimeTicks) RENDER_GRACE_TICKS else 0
        )
    }

    private fun scaleAt(state: DisplayParticleState, age: Int): DisplayEffectVector3 {
        val fadeStart = state.lifetimeTicks - state.fadeOutTicks
        if (age >= fadeStart) {
            val progress = (age - fadeStart).toDouble() / state.fadeOutTicks.toDouble()
            return state.peakScale * (1.0 - smoothStep(progress))
        }
        val peakAge = (state.lifetimeTicks * preset.peakScaleProgress).coerceAtLeast(1.0)
        val progress = (age / peakAge).coerceIn(0.0, 1.0)
        return state.initialScale.lerp(state.peakScale, smoothStep(progress))
    }

    private fun initialStates(request: DisplayParticleEmissionRequest): List<DisplayParticleState> {
        return List(request.count) { index ->
            // 個体ごとに独立seedを導出し、count変更時も既存indexの乱数列を安定させます。
            val particleSeed = deriveSeed(request.randomSeed, index.toLong())
            val positionRandom = Random(deriveSeed(particleSeed, POSITION_SALT))
            val velocityRandom = Random(deriveSeed(particleSeed, VELOCITY_SALT))
            val scaleRandom = Random(deriveSeed(particleSeed, SCALE_SALT))
            val lifetimeRandom = Random(deriveSeed(particleSeed, LIFETIME_SALT))
            val rotationRandom = Random(deriveSeed(particleSeed, ROTATION_SALT))
            val angularRandom = Random(deriveSeed(particleSeed, ANGULAR_SALT))
            val textureRandom = Random(deriveSeed(particleSeed, TEXTURE_SALT))
            val delayRandom = Random(deriveSeed(particleSeed, DELAY_SALT))
            val scaleMultiplier = 1.0 + scaleRandom.symmetric(preset.scaleVariation)
            val lifetime = preset.lifetimeTicks + lifetimeRandom.symmetricInt(preset.lifetimeVariationTicks)
            val fade = preset.fadeOutTicks + lifetimeRandom.symmetricInt(preset.fadeOutVariationTicks)
            val spawnDelay = if (preset.maxSpawnDelayTicks == 0) 0 else delayRandom.nextInt(preset.maxSpawnDelayTicks + 1)
            DisplayParticleState(
                -spawnDelay,
                DisplayEffectVector3(positionRandom.nextGaussian() * request.delta.x, positionRandom.nextGaussian() * request.delta.y, positionRandom.nextGaussian() * request.delta.z),
                preset.initialVelocity + DisplayEffectVector3(velocityRandom.nextGaussian() * request.speed, velocityRandom.nextGaussian() * request.speed, velocityRandom.nextGaussian() * request.speed),
                if (preset.randomInitialRotation) randomRotation(rotationRandom) else preset.initialRotation,
                if (spawnDelay == 0) preset.initialScale * scaleMultiplier else DisplayEffectVector3.ZERO,
                selectTexture(textureRandom),
                preset.initialScale * scaleMultiplier,
                preset.peakScale * scaleMultiplier,
                DisplayEffectVector3(
                    preset.angularVelocityRadiansPerTick.x * (1.0 + angularRandom.symmetric(preset.angularVelocityVariation)),
                    preset.angularVelocityRadiansPerTick.y * (1.0 + angularRandom.symmetric(preset.angularVelocityVariation)),
                    preset.angularVelocityRadiansPerTick.z * (1.0 + angularRandom.symmetric(preset.angularVelocityVariation))
                ),
                lifetime,
                fade,
                if (spawnDelay == 0) DisplayParticlePhase.ACTIVE else DisplayParticlePhase.WAITING,
                0
            )
        }
    }

    private fun selectTexture(random: Random): DisplayEffectAssetId {
        val totalWeight = preset.textures.sumOf { it.weight }
        var selection = random.nextInt(totalWeight)
        preset.textures.forEach { texture ->
            if (selection < texture.weight) return texture.assetId
            selection -= texture.weight
        }
        error("重み付きtexture選択に失敗しました")
    }

    private fun randomRotation(random: Random): DisplayEffectQuaternion {
        // Shoemake法でSO(3)上に偏りのない単位Quaternionを生成します。
        val u1 = random.nextDouble()
        val u2 = random.nextDouble() * Math.PI * 2.0
        val u3 = random.nextDouble() * Math.PI * 2.0
        return DisplayEffectQuaternion.of(
            sqrt(1.0 - u1) * sin(u2),
            sqrt(1.0 - u1) * cos(u2),
            sqrt(u1) * sin(u3),
            sqrt(u1) * cos(u3)
        )
    }

    private fun deriveSeed(parentSeed: Long, stream: Long): Long {
        var value = parentSeed + GOLDEN_GAMMA * (stream + 1L)
        value = (value xor (value ushr 30)) * MIX_1
        value = (value xor (value ushr 27)) * MIX_2
        return value xor (value ushr 31)
    }

    private fun Random.symmetric(range: Double): Double = (nextDouble() * 2.0 - 1.0) * range
    private fun Random.symmetricInt(range: Int): Int = if (range == 0) 0 else nextInt(range * 2 + 1) - range

    private fun rotationDelta(angularVelocity: DisplayEffectVector3): DisplayEffectQuaternion {
        val angle = angularVelocity.length()
        return if (abs(angle) < 1.0E-12) DisplayEffectQuaternion.IDENTITY
        else DisplayEffectQuaternion.fromAxisAngle(angularVelocity / angle, angle)
    }

    private fun smoothStep(progress: Double): Double {
        val t = progress.coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }

    companion object {
        private const val RENDER_GRACE_TICKS = 2
        private const val GOLDEN_GAMMA = -7046029254386353131L
        private const val MIX_1 = -4658895280553007687L
        private const val MIX_2 = -7723592293110705685L
        private const val POSITION_SALT = 1L
        private const val VELOCITY_SALT = 2L
        private const val SCALE_SALT = 3L
        private const val LIFETIME_SALT = 4L
        private const val ROTATION_SALT = 5L
        private const val ANGULAR_SALT = 6L
        private const val TEXTURE_SALT = 7L
        private const val DELAY_SALT = 8L
    }
}
