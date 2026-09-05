package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiBounds
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiElement
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiRay
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenDefinition
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVerticalSlot
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

        val expanded = List(3) { GestureGuiGeometry.SCREEN_WIDTH to GestureGuiGeometry.SCREEN_HEIGHT }
        val dynamic = GestureGuiGeometry.centerPitches(expanded)
        assertEquals(0.0, dynamic[1], 1.0e-9)
        assertTrue(dynamic[2] > 30.0)
    }

    @Test
    fun `vertical slots place two views in the top and middle of a three-slot layout`() {
        val slots = listOf(GestureGuiVerticalSlot.TOP, GestureGuiVerticalSlot.MIDDLE)
        val poses = GestureGuiGeometry.poses(
            GestureGuiVector3(0.0, 0.0, 0.0),
            0.0,
            screenCount = 2,
            verticalSlots = slots,
        )

        assertEquals(2, poses.size)
        assertTrue(poses[0].centerPitchDegrees < -30.0)
        assertEquals(0.0, poses[1].centerPitchDegrees, 1.0e-9)
        assertEquals(0, poses[0].screenIndex)
        assertEquals(1, poses[1].screenIndex)
    }

    @Test
    fun `tilt scale halves the vertical spread while keeping the middle centered`() {
        val slots = listOf(GestureGuiVerticalSlot.TOP, GestureGuiVerticalSlot.MIDDLE)
        val normal = GestureGuiGeometry.poses(
            GestureGuiVector3(0.0, 0.0, 0.0),
            0.0,
            screenCount = 2,
            verticalSlots = slots,
        )
        val halved = GestureGuiGeometry.poses(
            GestureGuiVector3(0.0, 0.0, 0.0),
            0.0,
            screenCount = 2,
            verticalSlots = slots,
            tiltScale = 0.5,
        )

        assertEquals(2, halved.size)
        assertEquals(normal[0].centerPitchDegrees * 0.5, halved[0].centerPitchDegrees, 1.0e-9)
        assertEquals(normal[1].centerPitchDegrees * 0.5, halved[1].centerPitchDegrees, 1.0e-9)
        // 画面間の上下関係は維持されます。
        assertTrue(halved[0].center.y > halved[1].center.y)
    }

    @Test
    fun `vertical screen envelope excludes the missing bottom slot`() {
        fun direction(pitch: Double): GestureGuiVector3 {
            val pitchRadians = Math.toRadians(pitch)
            return GestureGuiVector3(0.0, -kotlin.math.sin(pitchRadians), kotlin.math.cos(pitchRadians))
        }

        val slots = listOf(GestureGuiVerticalSlot.TOP, GestureGuiVerticalSlot.MIDDLE)
        assertTrue(
            GestureGuiGeometry.containsScreenEnvelope(
                direction(-35.0), 0.0, 2,
                verticalSlots = slots,
            )
        )
        assertTrue(
            GestureGuiGeometry.containsScreenEnvelope(
                direction(15.0), 0.0, 2,
                verticalSlots = slots,
            )
        )
        assertFalse(
            GestureGuiGeometry.containsScreenEnvelope(
                direction(35.0), 0.0, 2,
                verticalSlots = slots,
            )
        )
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
    fun `text display rotation compensates its reversed surface normal`() {
        val eastFacingPose = GestureGuiGeometry.poses(GestureGuiVector3(0.0, 0.0, 0.0), 90.0, 3)[1]
        assertEquals(-90.0, GestureGuiGeometry.textDisplayYaw(eastFacingPose).toDouble(), 1.0e-6)
        assertEquals(0.0, GestureGuiGeometry.textDisplayPitch(eastFacingPose).toDouble(), 1.0e-6)

        val lowerPose = GestureGuiGeometry.poses(GestureGuiVector3(0.0, 0.0, 0.0), 0.0, 1).single()
        assertEquals(180.0, kotlin.math.abs(GestureGuiGeometry.textDisplayYaw(lowerPose).toDouble()), 1.0e-6)
        assertEquals(-20.0, GestureGuiGeometry.textDisplayPitch(lowerPose).toDouble(), 1.0e-6)
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
        assertFalse(GestureGuiGeometry.containsScreenEnvelope(direction(50.0, 0.0), 0.0, 3))
        assertFalse(GestureGuiGeometry.containsScreenEnvelope(direction(0.0, 62.0), 0.0, 3))
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
                GestureGuiRay(GestureGuiVector3(0.0, 0.0, 0.0), GestureGuiVector3(1.1, 0.0, 1.0)),
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
