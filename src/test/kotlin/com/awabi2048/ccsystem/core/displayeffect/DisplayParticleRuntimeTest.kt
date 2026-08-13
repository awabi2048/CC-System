package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectQuaternion
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleEmissionRequest
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleCollisionMode
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleMotionPresetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticlePresetId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DisplayParticleRuntimeTest {
    @Test
    fun `生成直後はscale zeroで描画しsmoothstepで拡大する`() {
        val backend = RecordingBackend()
        val runtime = DisplayParticleRuntime(
            preset(randomized = false),
            request(),
            backend
        )

        runtime.start()
        assertEquals(DisplayEffectVector3.ZERO, backend.created.single().single().scale)

        runtime.tick()
        runtime.tick()
        val scales = backend.applied.take(2).map { it.single().scale.x }
        assertEquals(0.05, scales[0], 1.0E-9)
        assertEquals(0.1, scales[1], 1.0E-9)
    }

    @Test
    fun `遅延出現中は寿命を消費せず解除後にscale zeroから拡大する`() {
        val delayed = (0L..100L).firstNotNullOf { seed ->
            val backend = RecordingBackend()
            val runtime = DisplayParticleRuntime(
                preset(randomized = false, maxSpawnDelayTicks = 3),
                request(seed = seed),
                backend
            )
            runtime.start()
            backend.created.single().single().takeIf { it.ageTicks < 0 }?.let { Triple(runtime, backend, it) }
        }
        val (runtime, backend, initial) = delayed

        repeat(-initial.ageTicks) { runtime.tick() }
        val released = backend.applied.last().single()
        assertEquals(0, released.ageTicks)
        assertEquals(DisplayParticlePhase.ACTIVE, released.phase)
        assertEquals(DisplayEffectVector3.ZERO, released.scale)

        runtime.tick()
        val firstVisible = backend.applied.last().single()
        assertEquals(1, firstVisible.ageTicks)
        assertEquals(0.05, firstVisible.scale.x, 1.0E-9)
    }

    @Test
    fun `countとEntity数は一対一で、最終scale zeroを描画してから破棄する`() {
        val backend = RecordingBackend()
        val runtime = DisplayParticleRuntime(preset(randomized = false), request(count = 3, seed = 1), backend)

        assertEquals(3, runtime.entityCount)
        assertEquals(DisplayEffectRuntimeResult.Started, runtime.start())
        repeat(11) { runtime.tick() }

        assertEquals(3, backend.created.single().size)
        assertTrue(backend.applied.last().all { it.scale == DisplayEffectVector3.ZERO })
        assertTrue(backend.disposed)
    }

    @Test
    fun `fade区間ではsmoothstepでscaleが単調減少する`() {
        val backend = RecordingBackend()
        val runtime = DisplayParticleRuntime(preset(randomized = false), request(), backend)
        runtime.start()
        repeat(8) { runtime.tick() }
        val scales = backend.applied.takeLast(2).map { it.single().scale.x }
        assertTrue(scales[1] < scales[0])
        assertTrue(scales[1] >= 0.0)
    }

    @Test
    fun `生成失敗時は全体を回収する`() {
        val backend = RecordingBackend(failCreate = true)
        val result = DisplayParticleRuntime(preset(randomized = false), request(), backend).start()
        assertTrue(result is DisplayEffectRuntimeResult.Failed)
        assertTrue(backend.disposed)
    }

    @Test
    fun `同じseedは全個体プロパティを再現しcount増加でも既存prefixを維持する`() {
        fun generated(count: Int): List<DisplayParticleState> {
            val backend = RecordingBackend()
            DisplayParticleRuntime(preset(), request(count = count, seed = 9876), backend).start()
            return backend.created.single()
        }

        val first = generated(8)
        assertEquals(first, generated(8))
        assertEquals(first, generated(12).take(8))
        assertTrue(first.map { it.rotation }.distinct().size > 1)
        assertTrue(first.map { it.angularVelocityRadiansPerTick }.distinct().size > 1)
        assertTrue(first.map { it.initialScale }.distinct().size > 1)
        assertTrue(first.map { it.lifetimeTicks }.distinct().size > 1)
        assertTrue(first.map { it.textureAssetId }.distinct().size > 1)
    }

    private fun preset(randomized: Boolean = true, maxSpawnDelayTicks: Int = 0) = DisplayParticlePreset(
        DisplayParticlePresetId("cc:test"),
        listOf(
            DisplayParticleTexture(DisplayEffectAssetId("minecraft:white_concrete"), 3),
            DisplayParticleTexture(DisplayEffectAssetId("minecraft:gray_concrete"), 1)
        ),
        DisplayEffectVector3(0.1, 0.1, 0.1),
        DisplayEffectVector3(0.2, 0.2, 0.2),
        0.25,
        2,
        DisplayEffectQuaternion.IDENTITY,
        DisplayEffectVector3(0.0, 0.1, 0.0),
        scaleVariation = if (randomized) 0.2 else 0.0,
        angularVelocityVariation = if (randomized) 0.4 else 0.0,
        randomInitialRotation = randomized,
        lifetimeTicks = 8,
        lifetimeVariationTicks = if (randomized) 2 else 0,
        fadeOutTicks = 2,
        fadeOutVariationTicks = 0,
        maxSpawnDelayTicks = maxSpawnDelayTicks
    )

    private fun request(count: Int = 1, seed: Long = 1) = DisplayParticleEmissionRequest(
        DisplayParticlePresetId("cc:test"),
        DisplayParticleMotionPresetId("cc:inertial"),
        DisplayParticleCollisionMode.NONE,
        count = count,
        randomSeed = seed
    )

    private class RecordingBackend(private val failCreate: Boolean = false) : DisplayParticleBackend {
        val created = mutableListOf<List<DisplayParticleState>>()
        val applied = mutableListOf<List<DisplayParticleState>>()
        var disposed = false

        override fun create(preset: DisplayParticlePreset, states: List<DisplayParticleState>) {
            created += states
            if (failCreate) error("create failure")
        }

        override fun apply(states: List<DisplayParticleState>) { applied += states }
        override fun resolveCollision(
            previousOffset: DisplayEffectVector3,
            proposedOffset: DisplayEffectVector3,
            proposedVelocity: DisplayEffectVector3,
            motion: DisplayParticleMotionPreset,
            collisionMode: DisplayParticleCollisionMode
        ) = DisplayParticleCollisionResult(proposedOffset, proposedVelocity)
        override fun isAlive() = true
        override fun disposeAll(reason: DisplayEffectDisposalReason) { disposed = true }
    }
}
