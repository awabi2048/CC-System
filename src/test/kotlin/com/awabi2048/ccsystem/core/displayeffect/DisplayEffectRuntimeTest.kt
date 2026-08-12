package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAppearance
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectNodeDefinition
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectPhysics
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectScalarCurve
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectScalarKeyframe
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectSimulation
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVectorCurve
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVectorKeyframe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class DisplayEffectRuntimeTest {
    @Test
    fun `simulation applies vanilla inspired motion in documented order`() {
        val simulation = DisplayEffectSimulation(
            DisplayEffectPhysics(
                initialVelocity = DisplayEffectVector3(1.0, 2.0, 0.0),
                gravityPerTick = DisplayEffectVector3(0.0, -0.1, 0.0),
                velocityRetentionPerTick = 0.5,
                lifetimeTicks = 2,
                transforms = com.awabi2048.ccsystem.api.displayeffect.DisplayEffectTransformCurves(
                    scale = DisplayEffectVectorCurve(
                        listOf(
                            DisplayEffectVectorKeyframe(0.0, DisplayEffectVector3(1.0, 1.0, 1.0)),
                            DisplayEffectVectorKeyframe(1.0, DisplayEffectVector3.ZERO)
                        )
                    )
                )
            )
        )

        val initial = simulation.initialState()
        val first = assertInstanceOf(
            com.awabi2048.ccsystem.api.displayeffect.DisplayEffectStepResult.Advanced::class.java,
            simulation.step(initial)
        )
        assertEquals(DisplayEffectVector3(1.0, 2.0, 0.0), first.state.originOffset)
        assertEquals(DisplayEffectVector3(0.5, 0.95, 0.0), first.state.velocity)
        assertEquals(DisplayEffectVector3(0.5, 0.5, 0.5), first.frame.scale)

        val second = assertInstanceOf(
            com.awabi2048.ccsystem.api.displayeffect.DisplayEffectStepResult.Advanced::class.java,
            simulation.step(first.state)
        )
        assertEquals(DisplayEffectVector3(1.5, 2.95, 0.0), second.state.originOffset)
        assertEquals(DisplayEffectVector3(0.25, 0.425, 0.0), second.state.velocity)
        assertEquals(DisplayEffectVector3.ZERO, second.frame.scale)
        assertInstanceOf(
            com.awabi2048.ccsystem.api.displayeffect.DisplayEffectStepResult.Completed::class.java,
            simulation.step(second.state)
        )
    }

    @Test
    fun `curve copies input and interpolates linearly`() {
        val source = mutableListOf(
            DisplayEffectScalarKeyframe(0.0, 0.0),
            DisplayEffectScalarKeyframe(1.0, 10.0)
        )
        val curve = DisplayEffectScalarCurve(source)
        source[1] = DisplayEffectScalarKeyframe(1.0, 100.0)

        assertEquals(5.0, curve.sample(0.5))
    }

    @Test
    fun `invalid numeric values are rejected before entering simulation`() {
        assertThrows(IllegalArgumentException::class.java) {
            DisplayEffectVector3(Double.NaN, 0.0, 0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DisplayEffectPhysics(velocityRetentionPerTick = 1.1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DisplayEffectVector3(Double.MAX_VALUE, 0.0, 0.0).normalized()
        }
    }

    @Test
    fun `extreme finite curve endpoints interpolate without intermediate overflow`() {
        val curve = DisplayEffectScalarCurve(
            listOf(
                DisplayEffectScalarKeyframe(0.0, Double.MAX_VALUE),
                DisplayEffectScalarKeyframe(1.0, -Double.MAX_VALUE)
            )
        )

        assertEquals(0.0, curve.sample(0.5))
    }

    @Test
    fun `runtime creates once updates and disposes on expiration`() {
        val backend = RecordingBackend()
        val runtime = DisplayEffectRuntime(definition(), backend, TEST_INSTANCE_ID)

        assertEquals(DisplayEffectRuntimeResult.Started, runtime.start())
        assertEquals(1, backend.created.size)
        assertEquals(TEST_INSTANCE_ID, backend.created.single().request.instanceId)
        assertEquals("test", backend.created.single().request.nodeId.value)
        assertEquals("minecraft:red_concrete", backend.created.single().request.appearance.assetId.value)
        assertEquals(DisplayEffectRuntimeResult.Advanced, runtime.tick())
        assertEquals(DisplayEffectRuntimeResult.Advanced, runtime.tick())
        assertEquals(
            DisplayEffectRuntimeResult.Stopped(DisplayEffectDisposalReason.EXPIRED),
            runtime.tick()
        )
        assertEquals(1, backend.disposed.size)
        assertEquals(DisplayEffectDisposalReason.EXPIRED, backend.disposed.single().reason)
        assertEquals(DisplayEffectRuntimeStatus.STOPPED, runtime.status)
        assertEquals(DisplayEffectRuntimeResult.Ignored, runtime.tick())
    }

    @Test
    fun `backend invalidation disposes the logical instance`() {
        val backend = RecordingBackend()
        val runtime = DisplayEffectRuntime(definition(), backend)
        runtime.start()
        backend.alive = false

        assertEquals(
            DisplayEffectRuntimeResult.Stopped(DisplayEffectDisposalReason.BACKEND_INVALIDATED),
            runtime.tick()
        )
        assertEquals(DisplayEffectDisposalReason.BACKEND_INVALIDATED, backend.disposed.single().reason)
    }

    @Test
    fun `backend reports unavailable world as a normal stopped runtime`() {
        val backend = RecordingBackend()
        val runtime = DisplayEffectRuntime(definition(), backend)
        runtime.start()
        backend.worldAvailable = false

        assertEquals(
            DisplayEffectRuntimeResult.Stopped(DisplayEffectDisposalReason.WORLD_UNAVAILABLE),
            runtime.tick()
        )
        assertEquals(DisplayEffectDisposalReason.WORLD_UNAVAILABLE, backend.disposed.single().reason)
    }

    @Test
    fun `overflow during a step fails and still disposes backend`() {
        val backend = RecordingBackend()
        val runtime = DisplayEffectRuntime(
            DisplayEffectNodeDefinition(
                nodeId = com.awabi2048.ccsystem.api.displayeffect.DisplayEffectNodeId("overflow"),
                appearance = DisplayEffectAppearance.Block(
                    com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId("minecraft:red_concrete")
                ),
                physics = DisplayEffectPhysics(
                    initialVelocity = DisplayEffectVector3(Double.MAX_VALUE, 0.0, 0.0),
                    gravityPerTick = DisplayEffectVector3(Double.MAX_VALUE, 0.0, 0.0)
                )
            ),
            backend
        )
        runtime.start()

        val result = runtime.tick()

        assertTrue(result is DisplayEffectRuntimeResult.Failed)
        assertEquals(DisplayEffectRuntimeStatus.FAILED, runtime.status)
        assertEquals(DisplayEffectDisposalReason.FAILED, backend.disposed.single().reason)
    }

    private fun definition(): DisplayEffectNodeDefinition = DisplayEffectNodeDefinition(
        nodeId = com.awabi2048.ccsystem.api.displayeffect.DisplayEffectNodeId("test"),
        appearance = DisplayEffectAppearance.Block(
            com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId("minecraft:red_concrete")
        ),
        physics = DisplayEffectPhysics(lifetimeTicks = 2)
    )

    companion object {
        private val TEST_INSTANCE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }

    private class RecordingBackend : DisplayEffectBackend {
        data class Created(val handle: DisplayEffectHandle, val request: DisplayEffectSpawnRequest)
        data class Applied(val handle: DisplayEffectHandle, val frame: com.awabi2048.ccsystem.api.displayeffect.DisplayEffectFrame)
        data class Disposed(val handle: DisplayEffectHandle, val reason: DisplayEffectDisposalReason)

        private var nextToken = 1L
        private val handles = mutableSetOf<DisplayEffectHandle>()
        var alive: Boolean = true
        var worldAvailable: Boolean = true
        val created = mutableListOf<Created>()
        val applied = mutableListOf<Applied>()
        val disposed = mutableListOf<Disposed>()

        override fun create(request: DisplayEffectSpawnRequest): DisplayEffectHandle {
            val handle = DisplayEffectHandle(nextToken++)
            handles += handle
            created += Created(handle, request)
            return handle
        }

        override fun apply(
            handle: DisplayEffectHandle,
            frame: com.awabi2048.ccsystem.api.displayeffect.DisplayEffectFrame
        ) {
            check(handle in handles) { "未登録のhandleへapplyされました" }
            applied += Applied(handle, frame)
        }

        override fun isAlive(handle: DisplayEffectHandle): Boolean {
            if (!worldAvailable) {
                throw DisplayEffectWorldUnavailableException("test world unavailable")
            }
            return alive && handle in handles
        }

        override fun dispose(handle: DisplayEffectHandle, reason: DisplayEffectDisposalReason) {
            handles.remove(handle)
            disposed += Disposed(handle, reason)
        }
    }
}
