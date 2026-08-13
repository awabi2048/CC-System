package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiHit
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiRay
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenDefinition
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import kotlin.math.cos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sin

/** ジェスチャーGUIの配置と視線判定を一つの座標定義から算出します。 */
object GestureGuiGeometry {
    const val SCREEN_WIDTH: Double = 1.5
    const val SCREEN_HEIGHT: Double = 0.75
    const val SCREEN_DISTANCE: Double = 1.5

    private const val INTERSECTION_EPSILON = 1.0e-9

    /** BukkitのEntity回転へ渡すyawです。画面のローカル+Zを正面法線へ合わせます。 */
    fun displayYaw(pose: GestureGuiScreenPose): Float =
        Math.toDegrees(atan2(-pose.normal.x, pose.normal.z)).toFloat()

    /** BukkitのEntity回転へ渡すpitchです。Minecraftでは下向きが正です。 */
    fun displayPitch(pose: GestureGuiScreenPose): Float =
        Math.toDegrees(-asin(pose.normal.y.coerceIn(-1.0, 1.0))).toFloat()

    /** 画面数ごとの上・中・下の並びを、Minecraft pitch（下が正）で返します。 */
    fun centerPitches(screenCount: Int): List<Double> = when (screenCount) {
        1 -> listOf(20.0)
        2 -> listOf(-20.0, 20.0)
        3 -> listOf(-30.0, 0.0, 30.0)
        else -> throw IllegalArgumentException("gesture GUI supports one to three screens")
    }

    /** 画面中心への視線を法線とし、描画と当たり判定で共有する直交基底を作ります。 */
    fun poses(eye: GestureGuiVector3, yawDegrees: Double, screenCount: Int): List<GestureGuiScreenPose> {
        require(yawDegrees.isFinite()) { "gesture GUI yaw must be finite" }
        val yaw = Math.toRadians(yawDegrees)
        val forward = GestureGuiVector3(-sin(yaw), 0.0, cos(yaw))
        val right = GestureGuiVector3(-cos(yaw), 0.0, -sin(yaw))

        return centerPitches(screenCount).mapIndexed { index, pitchDegrees ->
            val pitch = Math.toRadians(pitchDegrees)
            val direction = GestureGuiVector3(
                forward.x * cos(pitch),
                -sin(pitch),
                forward.z * cos(pitch),
            )
            // normalは画面から開設者の目へ向く正面方向です。rightとの外積から、
            // 各画面のpitchに沿って傾いた上方向を一意に導出します。
            val normal = direction * -1.0
            val up = normal.cross(right).normalized()
            GestureGuiScreenPose(
                screenIndex = index,
                centerPitchDegrees = pitchDegrees,
                center = eye + direction * SCREEN_DISTANCE,
                right = right,
                up = up,
                normal = normal,
                width = SCREEN_WIDTH,
                height = SCREEN_HEIGHT,
            )
        }
    }

    /** 正面から画面矩形に入った交点のうち、視点に最も近いものを返します。 */
    fun hitTest(
        ray: GestureGuiRay,
        screens: List<Pair<GestureGuiScreenPose, GestureGuiScreenDefinition>>,
    ): GestureGuiHit? {
        val direction = ray.direction.normalized()
        return screens.mapNotNull { (pose, definition) ->
            hitTest(ray.origin, direction, pose, definition)
        }.minByOrNull(GestureGuiHit::distance)
    }

    private fun hitTest(
        origin: GestureGuiVector3,
        direction: GestureGuiVector3,
        pose: GestureGuiScreenPose,
        definition: GestureGuiScreenDefinition,
    ): GestureGuiHit? {
        // 背面からの操作と面に平行な視線を同じ入口で除外します。
        val denominator = direction.dot(pose.normal)
        if (denominator >= -INTERSECTION_EPSILON) return null

        val distance = (pose.center - origin).dot(pose.normal) / denominator
        if (distance <= INTERSECTION_EPSILON) return null

        val local = origin + direction * distance - pose.center
        val localX = local.dot(pose.right)
        val localY = local.dot(pose.up)
        if (localX !in -pose.width / 2.0..pose.width / 2.0) return null
        if (localY !in -pose.height / 2.0..pose.height / 2.0) return null

        // 重なりを許す描画要素では、後から宣言した前景要素を優先します。
        val elementId = definition.elements.lastOrNull { it.bounds.contains(localX, localY) }?.elementId
        return GestureGuiHit(pose.screenIndex, distance, localX, localY, elementId)
    }
}
