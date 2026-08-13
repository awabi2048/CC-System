package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectQuaternion
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleEmissionRequest
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticlePresetId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DisplayParticleRuntimeTest {
    @Test
    fun `countとEntity数は一対一で、最終scale zeroを描画してから破棄する`() {
        val backend = RecordingBackend()
        val runtime = DisplayParticleRuntime(preset(randomized = false), DisplayParticleEmissionRequest(DisplayParticlePresetId("cc:test"), count = 3, randomSeed = 1), backend)

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
        val runtime = DisplayParticleRuntime(preset(randomized = false), DisplayParticleEmissionRequest(DisplayParticlePresetId("cc:test")), backend)
        runtime.start()
        repeat(8) { runtime.tick() }
        val scales = backend.applied.takeLast(2).map { it.single().scale.x }
        assertTrue(scales[1] < scales[0])
        assertTrue(scales[1] >= 0.0)
    }

    @Test
    fun `生成失敗時は全体を回収する`() {
        val backend = RecordingBackend(failCreate = true)
        val result = DisplayParticleRuntime(preset(randomized = false), DisplayParticleEmissionRequest(DisplayParticlePresetId("cc:test")), backend).start()
        assertTrue(result is DisplayEffectRuntimeResult.Failed)
        assertTrue(backend.disposed)
    }

    @Test
    fun `同じseedは全個体プロパティを再現しcount増加でも既存prefixを維持する`() {
        fun generated(count: Int): List<DisplayParticleState> {
            val backend = RecordingBackend()
            DisplayParticleRuntime(preset(), DisplayParticleEmissionRequest(DisplayParticlePresetId("cc:test"), count = count, randomSeed = 9876), backend).start()
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

    private fun preset(randomized: Boolean = true) = DisplayParticlePreset(
        DisplayParticlePresetId("cc:test"),
        listOf(
            DisplayParticleTexture(DisplayEffectAssetId("minecraft:white_concrete"), 3),
            DisplayParticleTexture(DisplayEffectAssetId("minecraft:gray_concrete"), 1)
        ),
        DisplayEffectVector3(0.1, 0.1, 0.1),
        DisplayEffectVector3(0.2, 0.2, 0.2),
        0.25,
        "cc:inertial",
        DisplayEffectVector3.ZERO,
        DisplayEffectQuaternion.IDENTITY,
        DisplayEffectVector3(0.0, 0.1, 0.0),
        scaleVariation = if (randomized) 0.2 else 0.0,
        angularVelocityVariation = if (randomized) 0.4 else 0.0,
        randomInitialRotation = randomized,
        lifetimeTicks = 8,
        lifetimeVariationTicks = if (randomized) 2 else 0,
        fadeOutTicks = 2,
        fadeOutVariationTicks = 0,
        maxSpawnDelayTicks = 0
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
            motion: DisplayParticleMotionPreset
        ) = DisplayParticleCollisionResult(proposedOffset, proposedVelocity)
        override fun isAlive() = true
        override fun disposeAll(reason: DisplayEffectDisposalReason) { disposed = true }
    }
}
