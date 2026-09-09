package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 基底由来 quaternion と translation 解決の回帰試験です。
 *
 * entity 回転を 0 固定し、向き・位置を変形へ載せるため、基底写像と
 * translation 解決が決定論的であることをここで固定します。
 */
class GestureGuiVirtualOrientationTest {
    private fun tiltedPose(): GestureGuiScreenPose {
        // 実生成と同一に yaw・tilt から組み立て、厳密に直交させます。
        // 角度の丸めが基底へ混入すると、quaternion へ shear として伝搬するためです。
        val yaw = Math.toRadians(-178.0)
        val tilt = Math.asin(0.289)
        val right = GestureGuiVector3(-Math.cos(yaw), 0.0, -Math.sin(yaw))
        val forward = GestureGuiVector3(-Math.sin(yaw), 0.0, Math.cos(yaw))
        val normal = (forward * Math.cos(tilt) + GestureGuiVector3(0.0, 1.0, 0.0) * Math.sin(tilt)).normalized()
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

    private fun Vector3f.dotOf(x: Double, y: Double, z: Double): Double =
        (this.x * x + this.y * y + this.z * z).toDouble()

    @Test
    fun `block quatは旧ブロック向きに一致する`() {
        // 旧 Bukkit 経路と同一：+Z→+normal（裏面配置）、上下は正立を保つ。
        val pose = tiltedPose()
        val quat = GestureGuiVirtualScreens.blockQuat(pose)
        assertEquals(1.0, quat.lengthSquared().toDouble(), 1.0e-6)
        val mapped = quat.transform(Vector3f(0f, 0f, 1f), Vector3f())
        assertTrue(mapped.dotOf(pose.normal.x, pose.normal.y, pose.normal.z) > 0.999)
        val upMapped = quat.transform(Vector3f(0f, 1f, 0f), Vector3f())
        assertTrue(upMapped.dotOf(pose.up.x, pose.up.y, pose.up.z) > 0.999)
    }

    @Test
    fun `text quatは旧テキスト向きに一致する`() {
        // 旧 Bukkit 経路と同一：+Z→-normal（閲覧者向き）、上下は正立を保つ。
        val pose = tiltedPose()
        val quat = GestureGuiVirtualScreens.textQuat(pose)
        assertEquals(1.0, quat.lengthSquared().toDouble(), 1.0e-6)
        val mapped = quat.transform(Vector3f(0f, 0f, 1f), Vector3f())
        assertTrue(mapped.dotOf(-pose.normal.x, -pose.normal.y, -pose.normal.z) > 0.999)
        val upMapped = quat.transform(Vector3f(0f, 1f, 0f), Vector3f())
        assertTrue(upMapped.dotOf(pose.up.x, pose.up.y, pose.up.z) > 0.999)
    }

    @Test
    fun `anchor一致時は旧local値そのままに戻る`() {
        val pose = tiltedPose()
        val quat = GestureGuiVirtualScreens.blockQuat(pose)
        val anchor = GestureGuiVirtualScreens.PlacedPoint(100.0, 64.0, 200.0, 0f, 0f)
        val oldLocal = Vector3f(-1.45f, -0.5f, 0f)
        val translation = GestureGuiVirtualScreens.resolveTranslation(quat, anchor, anchor, oldLocal)
        assertEquals(oldLocal.x, translation.x, 1.0e-6f)
        assertEquals(oldLocal.y, translation.y, 1.0e-6f)
        assertEquals(oldLocal.z, translation.z, 1.0e-6f)
    }

    @Test
    fun `移動差分は回転済みで載る`() {
        // anchor 不変で目標だけが動くと、差分が同一回転で載ります。
        val pose = tiltedPose()
        val quat = GestureGuiVirtualScreens.blockQuat(pose)
        val anchor = GestureGuiVirtualScreens.PlacedPoint(100.0, 64.0, 200.0, 0f, 0f)
        val target = GestureGuiVirtualScreens.PlacedPoint(101.0, 64.5, 200.0, 0f, 0f)
        val translation = GestureGuiVirtualScreens.resolveTranslation(
            quat, anchor, target, Vector3f(0f, 0f, 0f),
        )
        // 差分 (1,0.5,0) の長さは回転で保たれます。
        val expected = Math.sqrt(1.25)
        assertEquals(expected, translation.length().toDouble(), 1.0e-5)
        // 逆回転で元の差分へ戻ります。
        val back = org.joml.Quaternionf(quat).transform(Vector3f(translation))
        assertEquals(1.0, back.x.toDouble(), 1.0e-5)
        assertEquals(0.5, back.y.toDouble(), 1.0e-5)
        assertEquals(0.0, back.z.toDouble(), 1.0e-5)
    }
}
