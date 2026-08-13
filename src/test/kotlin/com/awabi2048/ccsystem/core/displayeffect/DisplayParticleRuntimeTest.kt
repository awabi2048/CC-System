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
        val runtime = DisplayParticleRuntime(preset(), DisplayParticleEmissionRequest(DisplayParticlePresetId("cc:test"), count = 3, randomSeed = 1), backend)

        assertEquals(3, runtime.entityCount)
        assertEquals(DisplayEffectRuntimeResult.Started, runtime.start())
        repeat(6) { runtime.tick() }

        assertEquals(3, backend.created.single().size)
        assertTrue(backend.applied.last().all { it.scale == DisplayEffectVector3.ZERO })
        assertTrue(backend.disposed)
    }

    @Test
    fun `fade区間ではsmoothstepでscaleが単調減少する`() {
        val backend = RecordingBackend()
        val runtime = DisplayParticleRuntime(preset(), DisplayParticleEmissionRequest(DisplayParticlePresetId("cc:test")), backend)
        runtime.start()
        repeat(4) { runtime.tick() }
        val scales = backend.applied.takeLast(2).map { it.single().scale.x }
        assertTrue(scales[1] < scales[0])
        assertTrue(scales[1] >= 0.0)
    }

    @Test
    fun `生成失敗時は全体を回収する`() {
        val backend = RecordingBackend(failCreate = true)
        val result = DisplayParticleRuntime(preset(), DisplayParticleEmissionRequest(DisplayParticlePresetId("cc:test")), backend).start()
        assertTrue(result is DisplayEffectRuntimeResult.Failed)
        assertTrue(backend.disposed)
    }

    private fun preset() = DisplayParticlePreset(
        DisplayParticlePresetId("cc:test"),
        DisplayEffectAssetId("minecraft:white_concrete"),
        DisplayEffectVector3(0.1, 0.1, 0.1),
        DisplayEffectVector3(0.2, 0.2, 0.2),
        0.25,
        DisplayEffectVector3.ZERO,
        DisplayEffectVector3.ZERO,
        1.0,
        DisplayEffectQuaternion.IDENTITY,
        DisplayEffectVector3(0.0, 0.1, 0.0),
        lifetimeTicks = 4,
        fadeOutTicks = 2
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
        override fun isAlive() = true
        override fun disposeAll(reason: DisplayEffectDisposalReason) { disposed = true }
    }
}
