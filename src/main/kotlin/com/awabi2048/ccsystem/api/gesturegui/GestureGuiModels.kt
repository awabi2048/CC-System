package com.awabi2048.ccsystem.api.gesturegui

import java.util.UUID
import kotlin.math.sqrt

/** ジェスチャーGUIで画面ごとに割り当てられる入力です。 */
enum class GestureGuiGesture {
    PRIMARY,
    SECONDARY,
    SHIFT_PRIMARY,
    SHIFT_SECONDARY,
    SWAP_HAND,
}

/** 共有表示された画面を操作できるプレイヤーの範囲です。 */
enum class GestureGuiAccess {
    OWNER_ONLY,
    ALLOWLIST,
    PUBLIC,
}

/**
 * 画面中央を原点としたローカル座標上の矩形です。
 * Xは画面右方向、Yは画面上方向を正とします。
 */
data class GestureGuiBounds(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double,
) {
    init {
        require(listOf(minX, minY, maxX, maxY).all(Double::isFinite)) {
            "gesture GUI bounds must be finite"
        }
        require(minX < maxX) { "gesture GUI bounds width must be positive" }
        require(minY < maxY) { "gesture GUI bounds height must be positive" }
    }

    fun contains(x: Double, y: Double): Boolean = x in minX..maxX && y in minY..maxY
}

/** 当たり判定と入力割り当てを同じ安定IDへ結び付ける画面要素です。 */
data class GestureGuiElement(
    val elementId: String,
    val bounds: GestureGuiBounds,
    val acceptedGestures: Set<GestureGuiGesture> = emptySet(),
) {
    init {
        require(elementId.isNotBlank()) { "gesture GUI elementId must not be blank" }
    }
}

/**
 * 一枚の画面の純粋な定義です。
 * allowlistは第三者操作を許可するUUIDで、ALLOWLIST以外では空でなければなりません。
 */
data class GestureGuiScreenDefinition(
    val screenId: String,
    val elements: List<GestureGuiElement>,
    val access: GestureGuiAccess = GestureGuiAccess.OWNER_ONLY,
    val allowlist: Set<UUID> = emptySet(),
) {
    init {
        require(screenId.isNotBlank()) { "gesture GUI screenId must not be blank" }
        require(elements.map(GestureGuiElement::elementId).distinct().size == elements.size) {
            "gesture GUI elementId must be unique within a screen"
        }
        require(access == GestureGuiAccess.ALLOWLIST || allowlist.isEmpty()) {
            "gesture GUI allowlist is only valid with ALLOWLIST access"
        }
    }

    fun canOperate(ownerId: UUID, actorId: UUID): Boolean = when (access) {
        GestureGuiAccess.OWNER_ONLY -> actorId == ownerId
        GestureGuiAccess.ALLOWLIST -> actorId == ownerId || actorId in allowlist
        GestureGuiAccess.PUBLIC -> true
    }
}

/** BukkitのLocationへ依存せず座標計算を検証するための三次元ベクトルです。 */
data class GestureGuiVector3(val x: Double, val y: Double, val z: Double) {
    init {
        require(listOf(x, y, z).all(Double::isFinite)) { "gesture GUI vector must be finite" }
    }

    operator fun plus(other: GestureGuiVector3) = GestureGuiVector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: GestureGuiVector3) = GestureGuiVector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Double) = GestureGuiVector3(x * scale, y * scale, z * scale)
    fun dot(other: GestureGuiVector3): Double = x * other.x + y * other.y + z * other.z
    fun length(): Double = sqrt(dot(this))
    fun normalized(): GestureGuiVector3 {
        val length = length()
        require(length > 0.0) { "gesture GUI vector must not be zero" }
        return this * (1.0 / length)
    }
}

data class GestureGuiRay(
    val origin: GestureGuiVector3,
    val direction: GestureGuiVector3,
) {
    init {
        require(direction.length() > 0.0) { "gesture GUI ray direction must not be zero" }
    }
}

/** 画面は鉛直を保ち、right/up/normalがローカル座標軸を表します。 */
data class GestureGuiScreenPose(
    val screenIndex: Int,
    val centerPitchDegrees: Double,
    val center: GestureGuiVector3,
    val right: GestureGuiVector3,
    val up: GestureGuiVector3,
    val normal: GestureGuiVector3,
    val width: Double,
    val height: Double,
)

data class GestureGuiHit(
    val screenIndex: Int,
    val distance: Double,
    val localX: Double,
    val localY: Double,
    val elementId: String?,
)
