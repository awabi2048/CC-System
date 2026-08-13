package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.VoxelParticleEmissionRequest
import com.awabi2048.ccsystem.api.displayeffect.VoxelParticlePatternId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class VoxelParticleRuntimeTest {
    @Test
    fun `one logical particle preserves voxel composition while advancing shared state`() {
        val backend = RecordingBackend()
        val runtime = VoxelParticleRuntime(
            pattern(),
            VoxelParticleEmissionRequest(
                VoxelParticlePatternId("minecraft:test"),
                speed = 0.2,
                count = 2,
                randomSeed = 42L
            ),
            backend
        )

        assertEquals(4, runtime.entityCount)
        assertEquals(DisplayEffectRuntimeResult.Started, runtime.start())
        assertEquals(2, backend.createdStates.single().size)
        assertEquals(DisplayEffectRuntimeResult.Advanced, runtime.tick())
        assertEquals(2, backend.appliedStates.single().size)
    }

    @Test
    fun `backend creation failure rolls back the complete logical particle`() {
        val backend = RecordingBackend(failCreate = true)
        val runtime = VoxelParticleRuntime(
            pattern(),
            VoxelParticleEmissionRequest(VoxelParticlePatternId("minecraft:test")),
            backend
        )

        assertInstanceOf(DisplayEffectRuntimeResult.Failed::class.java, runtime.start())
        assertEquals(listOf(DisplayEffectDisposalReason.FAILED), backend.disposals)
    }

    @Test
    fun `request rejects unsafe command scale values`() {
        assertThrows(IllegalArgumentException::class.java) {
            VoxelParticleEmissionRequest(VoxelParticlePatternId("minecraft:test"), count = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            VoxelParticleEmissionRequest(
                VoxelParticlePatternId("minecraft:test"),
                delta = DisplayEffectVector3(-1.0, 0.0, 0.0)
            )
        }
    }

    private fun pattern() = VoxelParticlePattern(
        id = VoxelParticlePatternId("minecraft:test"),
        voxels = listOf(
            VoxelParticleVoxel(DisplayEffectVector3(-0.1, 0.0, 0.0), 0.1, DisplayEffectAssetId("minecraft:red_concrete")),
            VoxelParticleVoxel(DisplayEffectVector3(0.1, 0.0, 0.0), 0.1, DisplayEffectAssetId("minecraft:yellow_concrete"))
        ),
        lifetimeTicks = 2,
        gravityPerTick = DisplayEffectVector3(0.0, -0.01, 0.0),
        velocityRetentionPerTick = 0.9
    )

    private class RecordingBackend(private val failCreate: Boolean = false) : VoxelParticleBackend {
        val createdStates = mutableListOf<List<VoxelParticleState>>()
        val appliedStates = mutableListOf<List<VoxelParticleState>>()
        val disposals = mutableListOf<DisplayEffectDisposalReason>()

        override fun create(pattern: VoxelParticlePattern, states: List<VoxelParticleState>) {
            if (failCreate) error("test creation failure")
            createdStates += states
        }

        override fun apply(pattern: VoxelParticlePattern, states: List<VoxelParticleState>) {
            appliedStates += states
        }

        override fun isAlive(): Boolean = true

        override fun disposeAll(reason: DisplayEffectDisposalReason) {
            disposals += reason
        }
    }
}
