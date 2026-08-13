package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.VoxelParticleEmissionRequest
import java.util.Random

internal data class VoxelParticleState(
    val ageTicks: Int,
    val originOffset: DisplayEffectVector3,
    val velocity: DisplayEffectVector3
)

internal interface VoxelParticleBackend {
    fun create(pattern: VoxelParticlePattern, states: List<VoxelParticleState>)
    fun apply(pattern: VoxelParticlePattern, states: List<VoxelParticleState>)
    fun isAlive(): Boolean
    fun disposeAll(reason: DisplayEffectDisposalReason)
}

/**
 * 複数パターン・複数ボクセルを1つの論理インスタンスとして進行させます。
 * 物理状態はパターン単位で共有し、個々のBlockDisplayへ重複して持たせません。
 */
internal class VoxelParticleRuntime(
    private val pattern: VoxelParticlePattern,
    request: VoxelParticleEmissionRequest,
    private val backend: VoxelParticleBackend
) {
    private var states: List<VoxelParticleState> = createInitialStates(request)
    private var active = false

    val entityCount: Int = pattern.voxels.size * request.count

    fun start(): DisplayEffectRuntimeResult {
        if (active) return DisplayEffectRuntimeResult.Ignored
        return runCatching {
            backend.create(pattern, states)
            active = true
            DisplayEffectRuntimeResult.Started
        }.getOrElse { failure ->
            backend.disposeAll(DisplayEffectDisposalReason.FAILED)
            DisplayEffectRuntimeResult.Failed(failure)
        }
    }

    fun tick(): DisplayEffectRuntimeResult {
        if (!active) return DisplayEffectRuntimeResult.Ignored
        return runCatching {
            if (!backend.isAlive()) {
                return stop(DisplayEffectDisposalReason.BACKEND_INVALIDATED)
            }
            val advanced = states.map(::advance)
            if (advanced.all { it.ageTicks > pattern.lifetimeTicks }) {
                return stop(DisplayEffectDisposalReason.EXPIRED)
            }
            states = advanced
            backend.apply(pattern, states)
            DisplayEffectRuntimeResult.Advanced
        }.getOrElse { failure ->
            if (failure is DisplayEffectWorldUnavailableException) {
                stop(DisplayEffectDisposalReason.WORLD_UNAVAILABLE)
            } else {
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

    private fun advance(state: VoxelParticleState): VoxelParticleState = VoxelParticleState(
        ageTicks = state.ageTicks + 1,
        originOffset = state.originOffset + state.velocity,
        velocity = (state.velocity + pattern.gravityPerTick) * pattern.velocityRetentionPerTick
    )

    private fun createInitialStates(request: VoxelParticleEmissionRequest): List<VoxelParticleState> {
        val random = Random(request.randomSeed)
        return List(request.count) {
            VoxelParticleState(
                ageTicks = 0,
                originOffset = DisplayEffectVector3(
                    random.nextGaussian() * request.delta.x,
                    random.nextGaussian() * request.delta.y,
                    random.nextGaussian() * request.delta.z
                ),
                velocity = DisplayEffectVector3(
                    random.nextGaussian() * request.speed,
                    random.nextGaussian() * request.speed,
                    random.nextGaussian() * request.speed
                )
            )
        }
    }
}
