package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiBounds
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiElement
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiRay
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenDefinition
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiGeometryTest {
    @Test
    fun `screen pitches follow the one two and three screen specification`() {
        assertEquals(listOf(20.0), GestureGuiGeometry.centerPitches(1))
        assertEquals(listOf(-20.0, 20.0), GestureGuiGeometry.centerPitches(2))
        assertEquals(listOf(-30.0, 0.0, 30.0), GestureGuiGeometry.centerPitches(3))
    }

    @Test
    fun `pose normal points from player eye to screen center`() {
        val pose = GestureGuiGeometry.poses(GestureGuiVector3(10.0, 64.0, 20.0), 0.0, 1).single()
        val eye = GestureGuiVector3(10.0, 64.0, 20.0)

        assertEquals(10.0, pose.center.x, 1.0e-9)
        assertEquals(64.0 - 1.5 * kotlin.math.sin(Math.toRadians(20.0)), pose.center.y, 1.0e-9)
        assertEquals(20.0 + 1.5 * kotlin.math.cos(Math.toRadians(20.0)), pose.center.z, 1.0e-9)
        val expectedNormal = (pose.center - eye).normalized()
        assertEquals(expectedNormal.x, pose.normal.x, 1.0e-9)
        assertEquals(expectedNormal.y, pose.normal.y, 1.0e-9)
        assertEquals(expectedNormal.z, pose.normal.z, 1.0e-9)
        assertEquals(0.0, pose.right.dot(pose.up), 1.0e-9)
        assertEquals(0.0, pose.up.dot(pose.normal), 1.0e-9)
    }

    @Test
    fun `display rotation follows the screen normal without reversing yaw`() {
        val eastFacingPose = GestureGuiGeometry.poses(GestureGuiVector3(0.0, 0.0, 0.0), 90.0, 3)[1]
        // Minecraft yaw +90°は-X向きであり、画面法線も視点から画面へ向く-Xです。
        assertEquals(90.0, GestureGuiGeometry.displayYaw(eastFacingPose).toDouble(), 1.0e-6)
        assertEquals(0.0, GestureGuiGeometry.displayPitch(eastFacingPose).toDouble(), 1.0e-6)

        val lowerPose = GestureGuiGeometry.poses(GestureGuiVector3(0.0, 0.0, 0.0), 0.0, 1).single()
        assertEquals(20.0, GestureGuiGeometry.displayPitch(lowerPose).toDouble(), 1.0e-6)
    }

    @Test
    fun `multiple screen envelope includes vertical gaps but excludes its outside`() {
        fun direction(yaw: Double, pitch: Double): GestureGuiVector3 {
            val yawRadians = Math.toRadians(yaw)
            val pitchRadians = Math.toRadians(pitch)
            return GestureGuiVector3(
                -kotlin.math.sin(yawRadians) * kotlin.math.cos(pitchRadians),
                -kotlin.math.sin(pitchRadians),
                kotlin.math.cos(yawRadians) * kotlin.math.cos(pitchRadians),
            )
        }

        assertTrue(GestureGuiGeometry.containsScreenEnvelope(direction(0.0, 0.0), 0.0, 2))
        assertTrue(GestureGuiGeometry.containsScreenEnvelope(direction(0.0, 15.0), 0.0, 3))
        assertFalse(GestureGuiGeometry.containsScreenEnvelope(direction(31.0, 0.0), 0.0, 3))
        assertFalse(GestureGuiGeometry.containsScreenEnvelope(direction(0.0, 46.0), 0.0, 3))
    }

    @Test
    fun `ray selects the element using the same local coordinates as rendering`() {
        val pose = GestureGuiGeometry.poses(GestureGuiVector3(0.0, 0.0, 0.0), 0.0, 3)[1]
        val definition = GestureGuiScreenDefinition(
            "center",
            listOf(GestureGuiElement("button", GestureGuiBounds(-0.2, -0.2, 0.2, 0.2))),
        )
        val hit = GestureGuiGeometry.hitTest(
            GestureGuiRay(GestureGuiVector3(0.0, 0.0, 0.0), GestureGuiVector3(0.0, 0.0, 1.0)),
            listOf(pose to definition),
        )

        assertEquals("button", hit?.elementId)
        assertEquals(1.5, hit?.distance ?: 0.0, 1.0e-9)
    }

    @Test
    fun `ray outside the panel and ray from behind do not hit`() {
        val pose = GestureGuiGeometry.poses(GestureGuiVector3(0.0, 0.0, 0.0), 0.0, 3)[1]
        val definition = GestureGuiScreenDefinition("center", emptyList())

        assertNull(
            GestureGuiGeometry.hitTest(
                GestureGuiRay(GestureGuiVector3(0.0, 0.0, 0.0), GestureGuiVector3(1.0, 0.0, 1.0)),
                listOf(pose to definition),
            )
        )
        assertNull(
            GestureGuiGeometry.hitTest(
                GestureGuiRay(GestureGuiVector3(0.0, 0.0, 3.0), GestureGuiVector3(0.0, 0.0, -1.0)),
                listOf(pose to definition),
            )
        )
    }

    @Test
    fun `nearest visible screen wins independently of declaration order`() {
        val near = GestureGuiGeometry.poses(GestureGuiVector3(0.0, 0.0, 0.0), 0.0, 3)[1]
        val far = near.copy(center = GestureGuiVector3(0.0, 0.0, 2.5), screenIndex = 2)
        val definition = GestureGuiScreenDefinition("screen", emptyList())
        val hit = GestureGuiGeometry.hitTest(
            GestureGuiRay(GestureGuiVector3(0.0, 0.0, 0.0), GestureGuiVector3(0.0, 0.0, 1.0)),
            listOf(far to definition, near to definition),
        )

        assertEquals(1, hit?.screenIndex)
    }
}
