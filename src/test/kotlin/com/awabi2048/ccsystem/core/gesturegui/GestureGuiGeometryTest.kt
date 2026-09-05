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
    fun `tilt scale flattens orientation while keeping edges adjacent`() {
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
        // Orientation halves while edges stay adjacent.
        fun pitchOf(normal: GestureGuiVector3): Double =
            Math.toDegrees(kotlin.math.asin((-normal.y).coerceIn(-1.0, 1.0)))
        assertEquals(pitchOf(normal[0].normal) * 0.5, pitchOf(halved[0].normal), 1.0e-9)
        // ヒンジ結合により共有辺は同一3D直線となり、隙間は生じません。
        // 上画面の下辺と下画面の上辺の方向角差がゼロであることを検証します。
        fun edgePitch(
            pose: com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose,
            topEdge: Boolean,
        ): Double {
            val sign = if (topEdge) 1.0 else -1.0
            val px = pose.center.x
            val py = pose.center.y + sign * (pose.height / 2.0) * pose.up.y
            val pz = pose.center.z + sign * (pose.height / 2.0) * pose.up.z
            val length = kotlin.math.sqrt(px * px + py * py + pz * pz)
            return Math.toDegrees(kotlin.math.asin((py / length).coerceIn(-1.0, 1.0)))
        }
        val gap = edgePitch(halved[1], topEdge = true) - edgePitch(halved[0], topEdge = false)
        assertEquals(0.0, gap, 1.0e-9, "hinge edges must coincide: gap=$gap")
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

    @Test
    fun `hinge joint shares the same 3d edge midpoint`() {
        // ヒンジ結合では上画面の下辺中点と下画面の上辺中点が3D空間で一致します。
        // 角度上だけでなく空間的にも段差が残らないことを検証します。
        val slots = listOf(GestureGuiVerticalSlot.TOP, GestureGuiVerticalSlot.MIDDLE)
        val poses = GestureGuiGeometry.poses(
            GestureGuiVector3(0.0, 0.0, 0.0),
            0.0,
            screenCount = 2,
            verticalSlots = slots,
        )
        fun edgeMidpoint(
            pose: com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose,
            topEdge: Boolean,
        ): GestureGuiVector3 {
            val sign = if (topEdge) 1.0 else -1.0
            return pose.center + pose.up * (sign * pose.height / 2.0)
        }
        val upperBottom = edgeMidpoint(poses[0], topEdge = false)
        val lowerTop = edgeMidpoint(poses[1], topEdge = true)
        assertEquals(lowerTop.x, upperBottom.x, 1.0e-9)
        assertEquals(lowerTop.y, upperBottom.y, 1.0e-9)
        assertEquals(lowerTop.z, upperBottom.z, 1.0e-9)
    }

    @Test
    fun `vertical screen envelope matches the tilted edge angles`() {
        // ヒンジ結合では隣接画面が同一3D直線を共有するため、包絡の上下端は
        // 実辺点の方向角そのものに一致しなければなりません。
        // tiltScale=0.5 の傾き確定後でも一致することを検証します。
        listOf(1.0, 0.5).forEach { tilt ->
            val sizes = listOf(
                GestureGuiGeometry.SCREEN_WIDTH to GestureGuiGeometry.SCREEN_HEIGHT,
                GestureGuiGeometry.SCREEN_WIDTH to GestureGuiGeometry.SCREEN_HEIGHT,
            )
            val slots = listOf(GestureGuiVerticalSlot.TOP, GestureGuiVerticalSlot.MIDDLE)
            val arrangement = GestureGuiGeometry.verticalStripArrangement(sizes, slots, tilt)
            val expectedTop = arrangement.indices.minOf { index ->
                GestureGuiGeometry.edgePitchAngle(
                    arrangement[index].centerOffset,
                    sizes[index].second,
                    arrangement[index].tiltPitchDegrees,
                    topEdge = true,
                )
            }
            val expectedBottom = arrangement.indices.maxOf { index ->
                GestureGuiGeometry.edgePitchAngle(
                    arrangement[index].centerOffset,
                    sizes[index].second,
                    arrangement[index].tiltPitchDegrees,
                    topEdge = false,
                )
            }
            fun direction(pitch: Double): GestureGuiVector3 {
                val pitchRadians = Math.toRadians(pitch)
                return GestureGuiVector3(0.0, -kotlin.math.sin(pitchRadians), kotlin.math.cos(pitchRadians))
            }
            fun inside(pitch: Double): Boolean = GestureGuiGeometry.containsScreenEnvelope(
                direction(pitch),
                0.0,
                2,
                sizes,
                com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenLayout.VERTICAL,
                slots,
                tilt,
            )
            // 走査で求めた包絡境界が実辺端と一致します。
            var scannedTop = Double.MAX_VALUE
            var scannedBottom = -Double.MAX_VALUE
            var pitch = -89.0
            while (pitch <= 89.0) {
                if (inside(pitch)) {
                    if (pitch < scannedTop) scannedTop = pitch
                    if (pitch > scannedBottom) scannedBottom = pitch
                }
                pitch += 0.05
            }
            assertEquals(expectedTop, scannedTop, 0.1, "tilt=$tilt")
            assertEquals(expectedBottom, scannedBottom, 0.1, "tilt=$tilt")
        }
    }
}
