package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiHit
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiRay
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenDefinition
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import kotlin.math.cos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.atan
import kotlin.math.sin

/** ジェスチャーGUIの配置と視線判定を一つの座標定義から算出します。 */
object GestureGuiGeometry {
    const val SCREEN_WIDTH: Double = 2.1213203435596424
    const val SCREEN_HEIGHT: Double = 1.0606601717798212
    const val SCREEN_DISTANCE: Double = 1.5

    private const val INTERSECTION_EPSILON = 1.0e-9

    /** BukkitのEntity回転へ渡すyawです。画面のローカル+Zを正面法線へ合わせます。 */
    fun displayYaw(pose: GestureGuiScreenPose): Float =
        Math.toDegrees(atan2(-pose.normal.x, pose.normal.z)).toFloat()

    /** BukkitのEntity回転へ渡すpitchです。Minecraftでは下向きが正です。 */
    fun displayPitch(pose: GestureGuiScreenPose): Float =
        Math.toDegrees(-asin(pose.normal.y.coerceIn(-1.0, 1.0))).toFloat()

    /** TextDisplayは描画面の表がローカル-Z側にあるため、画面の+Z法線と逆向きのEntity回転を返します。 */
    fun textDisplayYaw(pose: GestureGuiScreenPose): Float =
        Math.toDegrees(atan2(pose.normal.x, -pose.normal.z)).toFloat()

    fun textDisplayPitch(pose: GestureGuiScreenPose): Float =
        Math.toDegrees(asin(pose.normal.y.coerceIn(-1.0, 1.0))).toFloat()

    /**
     * 複数画面の外周と、その間の余白を一つの連続した操作領域として判定します。
     * 個々の傾斜面との交差ではなく視線角を使うため、画面間の空間でも追従を開始しません。
     */
    fun containsScreenEnvelope(
        rayDirection: GestureGuiVector3,
        retainedYawDegrees: Double,
        screenCount: Int,
        sizes: List<Pair<Double, Double>> = List(screenCount) { SCREEN_WIDTH to SCREEN_HEIGHT },
    ): Boolean {
        require(screenCount in 1..3) { "gesture GUI requires one to three screens" }
        require(sizes.size == screenCount) { "gesture GUI screen size count must match screen count" }
        val direction = rayDirection.normalized()
        val rayYaw = Math.toDegrees(atan2(-direction.x, direction.z))
        val rayPitch = Math.toDegrees(-asin(direction.y.coerceIn(-1.0, 1.0)))
        val horizontalHalfAngle = sizes.maxOf { Math.toDegrees(atan((it.first / 2.0) / SCREEN_DISTANCE)) }
        val pitches = centerPitches(sizes)
        val top = pitches.indices.minOf { pitches[it] - Math.toDegrees(atan((sizes[it].second / 2.0) / SCREEN_DISTANCE)) }
        val bottom = pitches.indices.maxOf { pitches[it] + Math.toDegrees(atan((sizes[it].second / 2.0) / SCREEN_DISTANCE)) }
        return angularDistance(rayYaw, retainedYawDegrees) <= horizontalHalfAngle && rayPitch in top..bottom
    }

    private fun angularDistance(first: Double, second: Double): Double =
        kotlin.math.abs(((first - second + 540.0) % 360.0) - 180.0)

    /** 画面数ごとの上・中・下の並びを、Minecraft pitch（下が正）で返します。 */
    fun centerPitches(screenCount: Int): List<Double> = when (screenCount) {
        1 -> listOf(20.0)
        2 -> listOf(-20.0, 20.0)
        3 -> listOf(-30.0, 0.0, 30.0)
        else -> throw IllegalArgumentException("gesture GUI supports one to three screens")
    }

    /** 可変画面の上下角に約2度の余白を加え、中央pitchを動的に配置します。 */
    fun centerPitches(sizes: List<Pair<Double, Double>>): List<Double> {
        require(sizes.size in 1..3) { "gesture GUI requires one to three screens" }
        if (sizes.size == 1) return listOf(20.0)
        val halfAngles = sizes.map { Math.toDegrees(atan((it.second / 2.0) / SCREEN_DISTANCE)) }
        val centers = MutableList(sizes.size) { 0.0 }
        for (index in 1 until centers.size) {
            centers[index] = centers[index - 1] + halfAngles[index - 1] + halfAngles[index] + SCREEN_VERTICAL_GAP_DEGREES
        }
        val midpoint = (centers.first() + centers.last()) / 2.0
        return centers.map { it - midpoint }
    }

    /** 画面中心への視線を法線とし、描画と当たり判定で共有する直交基底を作ります。 */
    fun poses(
        eye: GestureGuiVector3,
        yawDegrees: Double,
        screenCount: Int,
        sizes: List<Pair<Double, Double>> = List(screenCount) { SCREEN_WIDTH to SCREEN_HEIGHT },
    ): List<GestureGuiScreenPose> {
        require(yawDegrees.isFinite()) { "gesture GUI yaw must be finite" }
        require(sizes.size == screenCount) { "gesture GUI screen size count must match screen count" }
        val yaw = Math.toRadians(yawDegrees)
        val forward = GestureGuiVector3(-sin(yaw), 0.0, cos(yaw))
        val right = GestureGuiVector3(-cos(yaw), 0.0, -sin(yaw))

        return centerPitches(sizes).mapIndexed { index, pitchDegrees ->
            val pitch = Math.toRadians(pitchDegrees)
            val direction = GestureGuiVector3(
                forward.x * cos(pitch),
                -sin(pitch),
                forward.z * cos(pitch),
            )
            // Displayのローカル+Zに合わせ、normalは開設者の目から画面中心へ向けます。
            // right×normalから、各画面のpitchに沿って傾いた上方向を導出します。
            val normal = direction
            val up = right.cross(normal).normalized()
            GestureGuiScreenPose(
                screenIndex = index,
                centerPitchDegrees = pitchDegrees,
                center = eye + direction * SCREEN_DISTANCE,
                right = right,
                up = up,
                normal = normal,
                width = sizes[index].first,
                height = sizes[index].second,
            )
        }
    }

    private const val SCREEN_VERTICAL_GAP_DEGREES = 2.0

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
        // normalは視点から画面へ向くため、正面からの視線との内積は正になります。
        // 背面からの操作と面に平行な視線を同じ入口で除外します。
        val denominator = direction.dot(pose.normal)
        if (denominator <= INTERSECTION_EPSILON) return null

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
