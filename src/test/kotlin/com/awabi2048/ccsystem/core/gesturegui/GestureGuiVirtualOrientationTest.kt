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
    fun `block quatは旧ブロック向きに一致する`() {
        // 旧 Bukkit 経路と同一：+Z→+normal（裏面配置）、上下は正立を保つ。
        val pose = tiltedPose()
        val quat = GestureGuiVirtualScreens.blockQuat(pose)
        assertEquals(1.0, quat.lengthSquared().toDouble(), 1.0e-6)
        assertTrue(quat.appliedTo(0f, 0f, 1f).dotOf(pose.normal.x, pose.normal.y, pose.normal.z) > 0.999)
        assertTrue(quat.appliedTo(0f, 1f, 0f).dotOf(pose.up.x, pose.up.y, pose.up.z) > 0.999)
    }

    @Test
    fun `text quatは旧テキスト向きに一致する`() {
        // 旧 Bukkit 経路と同一：+Z→-normal（閲覧者向き）、上下は正立を保つ。
        val pose = tiltedPose()
        val quat = GestureGuiVirtualScreens.textQuat(pose)
        assertEquals(1.0, quat.lengthSquared().toDouble(), 1.0e-6)
        assertTrue(quat.appliedTo(0f, 0f, 1f).dotOf(-pose.normal.x, -pose.normal.y, -pose.normal.z) > 0.999)
        assertTrue(quat.appliedTo(0f, 1f, 0f).dotOf(pose.up.x, pose.up.y, pose.up.z) > 0.999)
    }

    @Test
    fun `面はlayer平面上に置かれる`() {
        // 面中心は spawn 座標から向き方向へ半厚み出ます。面自体は layer 平面上です。
        // 旧 local 値 (-w/2,-h/2,0) の回転により、旧表示と同一の前後関係になります。
        val pose = tiltedPose()
        val quat = GestureGuiVirtualScreens.blockQuat(pose)
        val width = 2.9
        val height = 1.0
        val depth = 0.025f
        val translation = GestureGuiVirtualScreens.facePlaneTranslation(quat, width, height)
        val faceCenter = translation.add(quat.transform(Vector3f((width / 2.0).toFloat(), (height / 2.0).toFloat(), depth / 2f)))
        val facing = quat.transform(Vector3f(0f, 0f, 1f), Vector3f())
        // 面中心は向き方向へ半厚み（0.0125）だけ出ます。
        val along = faceCenter.dotOf(facing.x.toDouble(), facing.y.toDouble(), facing.z.toDouble())
        assertEquals(0.0125, along, 1.0e-5)
        // 面中心の向き直交成分は 0 です（面が spawn 座標中心に載ります）。
        assertEquals(0.0, faceCenter.length().toDouble() - along, 1.0e-5)
    }
}
