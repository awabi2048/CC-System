package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectQuaternion
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleEmissionRequest
import java.util.Random
import kotlin.math.abs

internal data class DisplayParticleState(
    val ageTicks: Int,
    val originOffset: DisplayEffectVector3,
    val velocity: DisplayEffectVector3,
    val rotation: DisplayEffectQuaternion,
    val scale: DisplayEffectVector3
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
    private var renderGraceTicks = 0
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
            if (!backend.isAlive()) return stop(DisplayEffectDisposalReason.BACKEND_INVALIDATED)
            if (renderGraceTicks > 0) {
                renderGraceTicks--
                return if (renderGraceTicks == 0) stop(DisplayEffectDisposalReason.EXPIRED)
                else DisplayEffectRuntimeResult.Advanced
            }
            states = states.map(::advance)
            backend.apply(states)
            if (states.all { it.ageTicks >= preset.lifetimeTicks }) renderGraceTicks = RENDER_GRACE_TICKS
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
        val age = (state.ageTicks + 1).coerceAtMost(preset.lifetimeTicks)
        val velocity = (state.velocity + preset.accelerationPerTick) * preset.velocityRetentionPerTick
        return DisplayParticleState(
            age,
            state.originOffset + state.velocity,
            velocity,
            state.rotation.multiply(rotationDelta(preset.angularVelocityRadiansPerTick)),
            scaleAt(age)
        )
    }

    private fun scaleAt(age: Int): DisplayEffectVector3 {
        val fadeStart = preset.lifetimeTicks - preset.fadeOutTicks
        if (age >= fadeStart) {
            val progress = (age - fadeStart).toDouble() / preset.fadeOutTicks.toDouble()
            return preset.peakScale * (1.0 - smoothStep(progress))
        }
        val peakAge = (preset.lifetimeTicks * preset.peakScaleProgress).coerceAtLeast(1.0)
        val progress = (age / peakAge).coerceIn(0.0, 1.0)
        return preset.initialScale.lerp(preset.peakScale, smoothStep(progress))
    }

    private fun initialStates(request: DisplayParticleEmissionRequest): List<DisplayParticleState> {
        val random = Random(request.randomSeed)
        return List(request.count) {
            DisplayParticleState(
                0,
                DisplayEffectVector3(random.nextGaussian() * request.delta.x, random.nextGaussian() * request.delta.y, random.nextGaussian() * request.delta.z),
                preset.initialVelocity + DisplayEffectVector3(random.nextGaussian() * request.speed, random.nextGaussian() * request.speed, random.nextGaussian() * request.speed),
                preset.initialRotation,
                preset.initialScale
            )
        }
    }

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
    }
}
