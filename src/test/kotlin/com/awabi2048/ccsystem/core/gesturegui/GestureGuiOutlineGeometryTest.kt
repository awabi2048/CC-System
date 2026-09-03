package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiOutlineGeometryTest {
    @Test
    fun `inner outline uses ten percent of the short side and has no gaps`() {
        val segments = GestureGuiOutlineGeometry.segments(0.66, 0.10, 0.10)

        assertEquals(4, segments.size)
        assertSegment(segments[0], 0.0, 0.045, 0.66, 0.01)
        assertSegment(segments[1], 0.0, -0.045, 0.66, 0.01)
        assertSegment(segments[2], -0.325, 0.0, 0.01, 0.08)
        assertSegment(segments[3], 0.325, 0.0, 0.01, 0.08)

        assertTrue(segments.all { it.width > 0.0 && it.height > 0.0 })
    }

    @Test
    fun `local outline segments keep their orientation for any screen rotation`() {
        val segments = GestureGuiOutlineGeometry.segments(0.47, 0.10, 0.10)
        val poses = listOf(0.0, 90.0, 180.0, 270.0).flatMap { yaw ->
            GestureGuiGeometry.poses(
                eye = GestureGuiVector3(0.0, 0.0, 0.0),
                yawDegrees = yaw,
                screenCount = 1,
                sizes = listOf(0.47 to 0.10),
            )
        }

        // rendererはこのローカル矩形を対象Visualと同じposeへ渡すため、yaw/pitchが
        // 変わってもワールド座標から上下左右を再判定しません。正の寸法とローカル軸
        // を使うことで、表示面が反転して縁取りの辺が入れ替わる事象を防ぎます。
        for (pose in poses) {
            fun localOffset(segment: GestureGuiOutlineSegment): GestureGuiVector3 =
                pose.right * segment.x + pose.up * segment.y

            assertTrue(localOffset(segments[0]).dot(pose.up) > 0.0)
            assertTrue(localOffset(segments[1]).dot(pose.up) < 0.0)
            assertTrue(localOffset(segments[2]).dot(pose.right) < 0.0)
            assertTrue(localOffset(segments[3]).dot(pose.right) > 0.0)
        }

        assertEquals(segments[0].y, -segments[1].y, 1.0e-9)
        assertEquals(segments[2].x, -segments[3].x, 1.0e-9)
    }

    private fun assertSegment(
        actual: GestureGuiOutlineSegment,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
    ) {
        assertEquals(x, actual.x, 1.0e-9)
        assertEquals(y, actual.y, 1.0e-9)
        assertEquals(width, actual.width, 1.0e-9)
        assertEquals(height, actual.height, 1.0e-9)
    }
}
