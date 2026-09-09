package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import org.joml.Quaternionf
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 基底由来 quaternion の回帰試験です。
 *
 * entity 回転を 0 固定し、向きを変形へ載せるため、quaternion が pose 基底へ
 * 正しく写像すること（角度慣習に依存しないこと）をここで固定します。
 */
class GestureGuiVirtualOrientationTest {
    private fun tiltedPose(): GestureGuiScreenPose {
        // yaw -178°・下向き傾きの代表 pose です（実ログ由来）。
        val right = GestureGuiVector3(-0.999, 0.0, -0.035).normalized()
        val normal = GestureGuiVector3(0.031, 0.289, -0.957).normalized()
        val up = right.cross(normal).normalized()
        return GestureGuiScreenPose(
            screenIndex = 0,
            centerPitchDegrees = 0.0,
            center = GestureGuiVector3(113.4, 107.5, 42.3),
            right = right,
            up = up,
            normal = normal,
            width = 2.9,
            height = 1.0,
        )
    }

    private fun Quaternionf.appliedTo(x: Float, y: Float, z: Float): Vector3f =
        transform(Vector3f(x, y, z), Vector3f())

    private fun Vector3f.dotOf(x: Double, y: Double, z: Double): Double =
        (this.x * x + this.y * y + this.z * z).toDouble()

    @Test
    fun `block quatは基底へ写像する`() {
        val pose = tiltedPose()
        val quat = GestureGuiVirtualScreens.blockQuat(pose)
        // 単位 quaternion であること。
        assertEquals(1.0, quat.lengthSquared().toDouble(), 1.0e-6)
        // +X→right・+Y→up・+Z→-normal（閲覧者向き）。
        assertTrue(quat.appliedTo(1f, 0f, 0f).dotOf(pose.right.x, pose.right.y, pose.right.z) > 0.999)
        assertTrue(quat.appliedTo(0f, 1f, 0f).dotOf(pose.up.x, pose.up.y, pose.up.z) > 0.999)
        assertTrue(quat.appliedTo(0f, 0f, 1f).dotOf(-pose.normal.x, -pose.normal.y, -pose.normal.z) > 0.999)
    }

    @Test
    fun `text quatは文字面を閲覧者へ向ける`() {
        val pose = tiltedPose()
        val quat = GestureGuiVirtualScreens.textQuat(pose)
        assertEquals(1.0, quat.lengthSquared().toDouble(), 1.0e-6)
        // 可視面（local -Z）が閲覧者向き（-normal）。
        assertTrue(quat.appliedTo(0f, 0f, -1f).dotOf(-pose.normal.x, -pose.normal.y, -pose.normal.z) > 0.999)
        // 上下は正立を保つ。
        assertTrue(quat.appliedTo(0f, 1f, 0f).dotOf(pose.up.x, pose.up.y, pose.up.z) > 0.999)
    }

    @Test
    fun `中心合わせ平行移動は面中心を原点へ戻す`() {
        val pose = tiltedPose()
        val quat = GestureGuiVirtualScreens.blockQuat(pose)
        val width = 2.9
        val height = 1.0
        val depth = 0.025f
        val translation = GestureGuiVirtualScreens.centeredTranslation(quat, width, height, depth)
        // 面中心（w/2,h/2,d/2）に平行移動を加えると原点へ戻ります。
        val centered = translation.add(quat.transform(Vector3f((width / 2.0).toFloat(), (height / 2.0).toFloat(), depth / 2f)))
        assertEquals(0.0, centered.x.toDouble(), 1.0e-5)
        assertEquals(0.0, centered.y.toDouble(), 1.0e-5)
        assertEquals(0.0, centered.z.toDouble(), 1.0e-5)
    }

    @Test
    fun `無傾斜poseでも写像は保たれる`() {        // 実 pose と同様に up = right×normal で組み立てます（基底の一貫性が前提）。
        val right = GestureGuiVector3(-1.0, 0.0, 0.0)
        val normal = GestureGuiVector3(0.0, 0.0, -1.0)
        val up = right.cross(normal).normalized()
        val pose = GestureGuiScreenPose(
            screenIndex = 0,
            centerPitchDegrees = 0.0,
            center = GestureGuiVector3(0.0, 0.0, 0.0),
            right = right,
            up = up,
            normal = normal,
            width = 2.0,
            height = 1.0,
        )
        val quat = GestureGuiVirtualScreens.blockQuat(pose)
        assertTrue(quat.appliedTo(1f, 0f, 0f).dotOf(right.x, right.y, right.z) > 0.999)
        assertTrue(quat.appliedTo(0f, 1f, 0f).dotOf(up.x, up.y, up.z) > 0.999)
        assertTrue(quat.appliedTo(0f, 0f, 1f).dotOf(-normal.x, -normal.y, -normal.z) > 0.999)
    }

    @Test
    fun `向きのなす角は正しく求まる`() {
        val pose = tiltedPose()
        val base = GestureGuiVirtualScreens.blockQuat(pose)
        // 同一は 0 度、符号違い（同一回転）は 0 度です。
        assertEquals(0.0, GestureGuiVirtualScreens.quatAngleDegrees(base, Quaternionf(base)), 1.0e-6)
        assertEquals(0.0, GestureGuiVirtualScreens.quatAngleDegrees(base, Quaternionf(base).mul(-1f)), 1.0e-6)
        // 90 度回転は 90 度になります。
        val turned = Quaternionf(base).mul(Quaternionf().rotationY(Math.PI.toFloat() / 2f))
        assertEquals(90.0, GestureGuiVirtualScreens.quatAngleDegrees(base, turned), 0.5)
    }
}
