package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiHit
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiRay
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenDefinition
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenLayout
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVerticalSlot
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
        layout: GestureGuiScreenLayout = GestureGuiScreenLayout.VERTICAL,
        verticalSlots: List<GestureGuiVerticalSlot>? = null,
    ): Boolean {
        require(screenCount in 1..3) { "gesture GUI requires one to three screens" }
        require(sizes.size == screenCount) { "gesture GUI screen size count must match screen count" }
        validateVerticalSlots(layout, sizes, verticalSlots)
        val direction = rayDirection.normalized()
        val rayYaw = Math.toDegrees(atan2(-direction.x, direction.z))
        val rayPitch = Math.toDegrees(-asin(direction.y.coerceIn(-1.0, 1.0)))
        return when (layout) {
            GestureGuiScreenLayout.VERTICAL -> {
                val horizontalHalfAngle = sizes.maxOf { Math.toDegrees(atan((it.first / 2.0) / SCREEN_DISTANCE)) }
                val pitches = verticalCenters(sizes, verticalSlots)
                val top = pitches.indices.minOf { pitches[it] - Math.toDegrees(atan((sizes[it].second / 2.0) / SCREEN_DISTANCE)) }
                val bottom = pitches.indices.maxOf { pitches[it] + Math.toDegrees(atan((sizes[it].second / 2.0) / SCREEN_DISTANCE)) }
                angularDistance(rayYaw, retainedYawDegrees) <= horizontalHalfAngle && rayPitch in top..bottom
            }
            GestureGuiScreenLayout.HORIZONTAL -> {
                // 横並びでは画面幅に応じて水平視野が広がり、縦方向は最も高い画面で決まります。
                val yaws = centerYaws(sizes)
                val left = yaws.indices.minOf { yaws[it] - Math.toDegrees(atan((sizes[it].first / 2.0) / SCREEN_DISTANCE)) }
                val right = yaws.indices.maxOf { yaws[it] + Math.toDegrees(atan((sizes[it].first / 2.0) / SCREEN_DISTANCE)) }
                val verticalHalfAngle = sizes.maxOf { Math.toDegrees(atan((it.second / 2.0) / SCREEN_DISTANCE)) }
                val relativeYaw = (rayYaw - retainedYawDegrees + 540.0) % 360.0 - 180.0
                relativeYaw in left..right && rayPitch in -verticalHalfAngle..verticalHalfAngle
            }
        }
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

    /** 可変画面の左右角に約2度の余白を加え、中央yawオフセットを動的に配置します。左が負です。 */
    fun centerYaws(sizes: List<Pair<Double, Double>>): List<Double> {
        require(sizes.size in 1..3) { "gesture GUI requires one to three screens" }
        if (sizes.size == 1) return listOf(0.0)
        val halfAngles = sizes.map { Math.toDegrees(atan((it.first / 2.0) / SCREEN_DISTANCE)) }
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
        layout: GestureGuiScreenLayout = GestureGuiScreenLayout.VERTICAL,
        verticalSlots: List<GestureGuiVerticalSlot>? = null,
        tiltScale: Double = 1.0,
    ): List<GestureGuiScreenPose> {
        require(yawDegrees.isFinite()) { "gesture GUI yaw must be finite" }
        require(sizes.size == screenCount) { "gesture GUI screen size count must match screen count" }
        validateVerticalSlots(layout, sizes, verticalSlots)
        val yaw = Math.toRadians(yawDegrees)
        val forward = GestureGuiVector3(-sin(yaw), 0.0, cos(yaw))
        val right = GestureGuiVector3(-cos(yaw), 0.0, -sin(yaw))

        return when (layout) {
            GestureGuiScreenLayout.VERTICAL ->
                verticalCenters(sizes, verticalSlots).mapIndexed { index, pitchDegrees ->
                    // 中心位置は無倍率の配置角で決めます。向きだけにtiltScaleを
                    // 掛けることで、辺縁の隙間設計を保ったまま傾きを抑えます。
                    // 両方へ掛けると寸法据え置きで辺縁が重なり合います。
                    val positionPitch = Math.toRadians(pitchDegrees)
                    val tiltPitch = Math.toRadians(pitchDegrees * tiltScale)
                    val positionDirection = GestureGuiVector3(
                        forward.x * cos(positionPitch),
                        -sin(positionPitch),
                        forward.z * cos(positionPitch),
                    )
                    val direction = GestureGuiVector3(
                        forward.x * cos(tiltPitch),
                        -sin(tiltPitch),
                        forward.z * cos(tiltPitch),
                    )
                    // Displayのローカル+Zに合わせ、normalは傾き調整後の向きへ合わせます。
                    // right×normalから、各画面のtiltに沿って傾いた上方向を導出します。
                    val normal = direction
                    val up = right.cross(normal).normalized()
                    GestureGuiScreenPose(
                        screenIndex = index,
                        centerPitchDegrees = pitchDegrees,
                        center = eye + positionDirection * SCREEN_DISTANCE,
                        right = right,
                        up = up,
                        normal = normal,
                        width = sizes[index].first,
                        height = sizes[index].second,
                    )
                }
            GestureGuiScreenLayout.HORIZONTAL ->
                // 左右並びは各画面の中心方向へnormalを振り、首を振って正面から
                // 参照できるようにします。最初の画面が左、後続が右へ配置されます。
                centerYaws(sizes).mapIndexed { index, yawOffsetDegrees ->
                    val screenYaw = yawDegrees + yawOffsetDegrees
                    val screenYawRad = Math.toRadians(screenYaw)
                    val direction = GestureGuiVector3(-sin(screenYawRad), 0.0, cos(screenYawRad))
                    val normal = direction
                    val screenRight = GestureGuiVector3(-cos(screenYawRad), 0.0, -sin(screenYawRad))
                    val up = screenRight.cross(normal).normalized()
                    GestureGuiScreenPose(
                        screenIndex = index,
                        centerPitchDegrees = 0.0,
                        center = eye + direction * SCREEN_DISTANCE,
                        right = screenRight,
                        up = up,
                        normal = normal,
                        width = sizes[index].first,
                        height = sizes[index].second,
                    )
                }
        }
    }

    private const val SCREEN_VERTICAL_GAP_DEGREES = 2.0

    /**
     * 指定スロットを3画面ぶんの縦配置へ投影します。欠けたスロットは最大寸法で予約し、
     * 実体を生成せずに上・中・下の視野関係だけを維持します。
     */
    private fun verticalCenters(
        sizes: List<Pair<Double, Double>>,
        verticalSlots: List<GestureGuiVerticalSlot>?,
    ): List<Double> {
        if (verticalSlots == null) return centerPitches(sizes)
        val fallback = sizes.maxBy { it.first * it.second }
        val slotSizes = List(3) { slotIndex ->
            val viewIndex = verticalSlots.indexOfFirst { it.ordinal == slotIndex }
            if (viewIndex >= 0) sizes[viewIndex] else fallback
        }
        val allCenters = centerPitches(slotSizes)
        return verticalSlots.map { allCenters[it.ordinal] }
    }

    private fun validateVerticalSlots(
        layout: GestureGuiScreenLayout,
        sizes: List<Pair<Double, Double>>,
        verticalSlots: List<GestureGuiVerticalSlot>?,
    ) {
        if (verticalSlots == null) return
        require(layout == GestureGuiScreenLayout.VERTICAL) {
            "gesture GUI vertical slots require vertical layout"
        }
        require(verticalSlots.size == sizes.size) {
            "gesture GUI vertical slot count must match screen count"
        }
        require(verticalSlots.distinct().size == verticalSlots.size) {
            "gesture GUI vertical slots must be unique"
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
