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
        tiltScale: Double = 1.0,
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
                // 包絡は解き直し後の中心角で評価し、表示と判定の範囲を一致させます。
                // 上下端は角度和の近似ではなく、傾き確定後の実辺端角そのもので求めます。
                // 位置と向きを分離した配置では atan((h/2)/D) が実辺とずれるため、
                // ヒンジ結合と同じ辺点角を使い、描画・当たり・包絡を一致させます。
                val arrangement = verticalStripArrangement(sizes, verticalSlots, tiltScale)
                val top = arrangement.indices.minOf { index ->
                    edgePitchAngle(
                        arrangement[index].centerOffset,
                        sizes[index].second,
                        arrangement[index].tiltPitchDegrees,
                        topEdge = true,
                    )
                }
                val bottom = arrangement.indices.maxOf { index ->
                    edgePitchAngle(
                        arrangement[index].centerOffset,
                        sizes[index].second,
                        arrangement[index].tiltPitchDegrees,
                        topEdge = false,
                    )
                }
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
                verticalStripArrangement(sizes, verticalSlots, tiltScale).mapIndexed { index, angles ->
                    // 中心位置はヒンジ結合が解いた3Dオフセットをyaw回転して求めます。
                    // 球面拘束を外すことで、隣接画面との共有辺が空間的に一致します。
                    // 向きだけに tiltScale を掛ける点は変えず、辺縁の傾き設計を保ちます。
                    val tiltPitch = Math.toRadians(angles.tiltPitchDegrees)
                    val direction = GestureGuiVector3(
                        forward.x * cos(tiltPitch),
                        -sin(tiltPitch),
                        forward.z * cos(tiltPitch),
                    )
                    // Displayのローカル+Zに合わせ、normalは傾き調整後の向きへ合わせます。
                    // right×normalから、各画面のtiltに沿って傾いた上方向を導出します。
                    val normal = direction
                    val up = right.cross(normal).normalized()
                    // yaw=0正準系のオフセットを水平 yaw 回転へ載せます。
                    // 中心高さはそのままに、奥行きだけを現在の正面方向へ向けます。
                    val offset = angles.centerOffset
                    GestureGuiScreenPose(
                        screenIndex = index,
                        centerPitchDegrees = angles.centerPitchDegrees,
                        center = eye + GestureGuiVector3(forward.x * offset.z, offset.y, forward.z * offset.z),
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
     * 画面の配置角と傾き角と中心オフセットです。中心位置は球面拘束を受けず、
     * yaw=0・目原点系での3Dオフセットそのもので決まります。両者が一致するときは
     * 従来の湾曲配置(各画面が目を向く)と等しくなります。
     * centerPitchDegrees は解決後の中心の方向角であり、情報表示・包絡の目安に使います。
     */
    internal data class VerticalStripAngles(
        val centerPitchDegrees: Double,
        val tiltPitchDegrees: Double,
        /** yaw=0・目原点系での中心オフセットです。x は常に 0 です。 */
        val centerOffset: GestureGuiVector3,
    )

    /**
     * 縦配置の画面群を、辺同士が同一3D直線を共有するヒンジ結合として解きます。
     *
     * 角度上の隙間（約2度）で辺を離す従来方式では、横から見ると上下辺の間に
     * 空間的な段差が残ります。ヒンジ結合では隣接画面の辺中点をそのまま共有し、
     * 開いた本のように辺を一点で接合します。中央スロットを従来arc位置の球面上へ
     * 固定し、そこから上下へ自画面の辺中点が共有点へ一致するよう中心を置くため、
     * 反復計算なしに厳密解が求まります。表示と視線包絡判定の双方が同じ配置を
     * 使うため、判定と描画は一致します。
     * view順で返します。欠けたスロットは最大寸法で空間を予約します。
     */
    internal fun verticalStripArrangement(
        sizes: List<Pair<Double, Double>>,
        verticalSlots: List<GestureGuiVerticalSlot>?,
        tiltScale: Double,
    ): List<VerticalStripAngles> {
        require(tiltScale >= 0.0) { "gesture GUI tilt scale must be non-negative" }
        // スロット順(上→下)へ正規化します。スロットなしはview順をそのまま使います。
        data class SlotEntry(val size: Pair<Double, Double>, val viewIndex: Int)
        val slots: List<SlotEntry> = if (verticalSlots == null) {
            sizes.mapIndexed { index, size -> SlotEntry(size, index) }
        } else {
            val fallback = sizes.maxBy { it.first * it.second }
            List(3) { slotIndex ->
                val viewIndex = verticalSlots.indexOfFirst { it.ordinal == slotIndex }
                val size = if (viewIndex >= 0) sizes[viewIndex] else fallback
                SlotEntry(size, viewIndex)
            }
        }
        val slotSizes = slots.map { it.size }
        val arcPitches = centerPitches(slotSizes)
        val tilts = arcPitches.map { it * tiltScale }
        // 傾き確定後の上方向を yaw=0 正準系で求めます。poses() と同じ right×normal 規則です。
        fun upOf(tiltPitchDegrees: Double): GestureGuiVector3 {
            val tilt = Math.toRadians(tiltPitchDegrees)
            val normal = GestureGuiVector3(0.0, -sin(tilt), cos(tilt))
            return GestureGuiVector3(-1.0, 0.0, 0.0).cross(normal).normalized()
        }
        val ups = tilts.map(::upOf)
        // 指定した辺の3D中点を返します。topEdge=trueなら上辺、falseなら下辺です。
        fun edgeMidpoint(center: GestureGuiVector3, height: Double, up: GestureGuiVector3, topEdge: Boolean): GestureGuiVector3 {
            val sign = if (topEdge) 1.0 else -1.0
            return center + up * (sign * height / 2.0)
        }
        // 解決後の中心の方向角(Minecraft pitch、下が正)を返します。
        fun centerAngle(center: GestureGuiVector3): Double =
            Math.toDegrees(-asin((center.y / center.length()).coerceIn(-1.0, 1.0)))
        val centers = Array(slots.size) { GestureGuiVector3(0.0, 0.0, 0.0) }
        // 中央スロットを従来arc位置の球面上へ固定し、そこから上下へ解きます。
        // 中央固定により、単画面・2画面レイアウトの既存配置が維持されます。
        val anchorIdx = slots.size / 2
        run {
            val anchorPitch = Math.toRadians(arcPitches[anchorIdx])
            centers[anchorIdx] = GestureGuiVector3(
                0.0,
                -SCREEN_DISTANCE * sin(anchorPitch),
                SCREEN_DISTANCE * cos(anchorPitch),
            )
        }
        for (index in anchorIdx + 1 until slots.size) {
            // 一つ上の画面の下辺中点を共有点とし、自画面の上辺中点が一致するよう置きます。
            val shared = edgeMidpoint(centers[index - 1], slotSizes[index - 1].second, ups[index - 1], topEdge = false)
            centers[index] = shared - ups[index] * (slotSizes[index].second / 2.0)
        }
        for (index in anchorIdx - 1 downTo 0) {
            // 一つ下の画面の上辺中点を共有点とし、自画面の下辺中点が一致するよう置きます。
            val shared = edgeMidpoint(centers[index + 1], slotSizes[index + 1].second, ups[index + 1], topEdge = true)
            centers[index] = shared + ups[index] * (slotSizes[index].second / 2.0)
        }
        val bySlot = slots.indices.associateWith { slotIndex ->
            VerticalStripAngles(centerAngle(centers[slotIndex]), tilts[slotIndex], centers[slotIndex])
        }
        return if (verticalSlots == null) {
            slots.indices.map { bySlot.getValue(it) }
        } else {
            // view順へ戻します。各viewはverticalSlots[viewIndex]のスロットに属します。
            verticalSlots.map { slot -> bySlot.getValue(slot.ordinal) }
        }
    }

    /**
     * 中心・高さ・傾きが決まった画面の辺端角(目から見たpitch、度)を返します。
     * 中心は yaw=0・目原点系の3Dオフセットで受け、球面拘束を仮定しません。
     * ヒンジ結合で球面を外れた中心でも、実辺点の方向角を厳密に求められます。
     * pitch配置はyaw回転に対して不変のため、任意yawの配置へそのまま適用できます。
     * 包絡判定と単体テストから同じ辺定義を参照するため internal とします。
     */
    internal fun edgePitchAngle(
        center: GestureGuiVector3,
        height: Double,
        tiltPitchDegrees: Double,
        topEdge: Boolean,
    ): Double {
        val tilt = Math.toRadians(tiltPitchDegrees)
        // 法線方向の上ベクトル: (0, cos t, sin t)。poses() と同じ right×normal 規則です。
        val uy = cos(tilt)
        val uz = sin(tilt)
        val sign = if (topEdge) 1.0 else -1.0
        val px = center.x
        val py = center.y + sign * (height / 2.0) * uy
        val pz = center.z + sign * (height / 2.0) * uz
        val length = kotlin.math.sqrt(px * px + py * py + pz * pz)
        return Math.toDegrees(-asin((py / length).coerceIn(-1.0, 1.0)))
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
