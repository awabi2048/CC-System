package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import org.bukkit.Particle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiGazeDebugTest {
    private fun snapshot(
        inside: Boolean = true,
        envelopeApplicable: Boolean = true,
        envelopeInside: Boolean = true,
        hitDistance: Double? = 1.8,
    ) = GazeDebugSnapshot(
        inside = inside,
        envelopeApplicable = envelopeApplicable,
        envelopeInside = envelopeInside,
        hitDistance = hitDistance,
        hitElementId = hitDistance?.let { "viewport-empty" },
        hitScreenIndex = hitDistance?.let { 0 },
        nearestCenterDistance = 1.9,
        range = 3.0,
        motionName = "STOPPED",
        followDirty = true,
        dummyActive = false,
        displacement = 0.62,
        origin = GestureGuiVector3(0.0, 64.0, 0.0),
        direction = GestureGuiVector3(0.0, 0.0, 1.0),
    )

    @Test
    fun `ray points advance at fixed steps up to the maximum distance`() {
        val points = GestureGuiGazeDebug.rayPoints(
            GestureGuiVector3(0.0, 0.0, 0.0),
            GestureGuiVector3(0.0, 0.0, 2.0),
            maxDistance = 1.0,
            step = 0.25,
        )
        assertEquals(4, points.size)
        assertEquals(0.25, points[0].z, 1.0e-9)
        assertEquals(1.0, points[3].z, 1.0e-9)
    }

    @Test
    fun `ray points are capped and empty below one step`() {
        val capped = GestureGuiGazeDebug.rayPoints(
            GestureGuiVector3(0.0, 0.0, 0.0),
            GestureGuiVector3(0.0, 0.0, 1.0),
            maxDistance = 10.0,
            step = 0.25,
            maxPoints = 3,
        )
        assertEquals(3, capped.size)
        assertTrue(
            GestureGuiGazeDebug.rayPoints(
                GestureGuiVector3(0.0, 0.0, 0.0),
                GestureGuiVector3(0.0, 0.0, 1.0),
                maxDistance = 0.1,
            ).isEmpty(),
        )
    }

    @Test
    fun `particle plan colors the ray by gaze result and marks the hit`() {
        val insidePlan = GestureGuiGazeDebug.particlePlan(snapshot(inside = true))
        assertEquals(Particle.END_ROD, insidePlan.rayParticle)
        assertEquals(Particle.HAPPY_VILLAGER, insidePlan.markerParticle)
        assertEquals(1.8, insidePlan.markerPoint?.z ?: 0.0, 1.0e-9)

        val outsidePlan = GestureGuiGazeDebug.particlePlan(
            snapshot(inside = false, envelopeInside = false, hitDistance = 1.8),
        )
        assertEquals(Particle.SMOKE, outsidePlan.rayParticle)
        assertEquals(Particle.CRIT, outsidePlan.markerParticle)

        val missedPlan = GestureGuiGazeDebug.particlePlan(snapshot(inside = false, hitDistance = null))
        assertNull(missedPlan.markerParticle)
        assertNull(missedPlan.markerPoint)
        // ヒットなしでは操作可能距離まで打点します（0.25間隔・上限12点）。
        assertEquals(12, missedPlan.points.size)
    }

    @Test
    fun `subtitle placeholders round values for display`() {
        val placeholders = GestureGuiGazeDebug.subtitlePlaceholders(snapshot())
        assertEquals("内", placeholders["inside"])
        assertEquals("○", placeholders["envelope"])
        assertEquals("○1.80", placeholders["hit"])
        assertEquals("1.90", placeholders["dist"])
        assertEquals("3.00", placeholders["range"])
        assertEquals("STOPPED", placeholders["motion"])
        assertEquals(" D", placeholders["dirty"])
        assertEquals("", placeholders["dummy"])
        assertEquals("0.62", placeholders["displacement"])
    }

    @Test
    fun `subtitle placeholders cover single screen and miss cases`() {
        val single = GestureGuiGazeDebug.subtitlePlaceholders(
            snapshot(envelopeApplicable = false, envelopeInside = true, hitDistance = null),
        )
        assertEquals("―", single["envelope"])
        assertEquals("×", single["hit"])
    }
}
