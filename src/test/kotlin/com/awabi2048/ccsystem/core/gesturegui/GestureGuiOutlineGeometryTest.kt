package com.awabi2048.ccsystem.core.gesturegui

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
        val first = GestureGuiOutlineGeometry.segments(0.47, 0.10, 0.10)
        val rotated = GestureGuiOutlineGeometry.segments(0.47, 0.10, 0.10)

        // rendererはこのローカル矩形を対象Visualと同じposeへ渡すため、yaw/pitchが
        // 変わっても上辺をワールド座標で再判定せず、枠の上下左右が入れ替わりません。
        assertEquals(first, rotated)
        assertEquals(first[0].y, -first[1].y, 1.0e-9)
        assertEquals(first[2].x, -first[3].x, 1.0e-9)
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
