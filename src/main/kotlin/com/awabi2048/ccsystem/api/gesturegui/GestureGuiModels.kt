package com.awabi2048.ccsystem.api.gesturegui

import java.util.UUID
import kotlin.math.sqrt
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player

/** 画面の実寸と共通外観です。寸法・枠幅はブロック単位です。 */
data class GestureGuiPanel(
    val width: Double = DEFAULT_WIDTH,
    val height: Double = DEFAULT_HEIGHT,
    val backgroundMaterial: Material = Material.BLACK_CONCRETE,
    val frameMaterial: Material = Material.CYAN_TERRACOTTA,
    val frameWidth: Double = 0.045,
) {
    init {
        require(width.isFinite() && height.isFinite() && width > 0.0 && height > 0.0) {
            "gesture GUI panel size must be positive and finite"
        }
        require(frameWidth.isFinite() && frameWidth > 0.0 && frameWidth * 2.0 < minOf(width, height)) {
            "gesture GUI frame width must fit inside the panel"
        }
    }

    companion object {
        // 従来比で縦横を√2倍にし、縦横比を維持したまま面積を2倍にします。
        const val DEFAULT_WIDTH: Double = 2.1213203435596424
        const val DEFAULT_HEIGHT: Double = 1.0606601717798212
    }
}

/** 親画面上へ重ねる子画面の配置と入力方針です。 */
data class GestureGuiChildOptions(    val parentScreenId: String,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val allowParentInteraction: Boolean = false,
    /** 子画面の出現・消滅アニメーションを使うか。短時間の確認画面では無効化できます。 */
    val animated: Boolean = true,
    /** 子画面背景のオーバーレイ素材。nullならデフォルト(灰色)。確認子画面など赤ガスに変えたい場合に指定 */
    val overlayMaterial: Material? = null,
) {
    init {
        require(parentScreenId.isNotBlank()) { "gesture GUI child parentScreenId must not be blank" }
        require(offsetX.isFinite() && offsetY.isFinite()) { "gesture GUI child offset must be finite" }
    }
}

/**
 * Gesture GUIセッションが入力受付を終了した時に呼び出される通知です。
 *
 * 通知は終了アニメーションの完了を待たず、セッションが論理的に閉じられた時点で
 * サーバーのメインスレッドから一度だけ行われます。通知を受けた利用側は、保持している
 * Dialogや入力トークンなどの所有状態を解放できます。通知内で同じセッションを閉じ直す
 * 必要はありません。
 */
enum class GestureGuiCloseReason {
    SHIFT_JUMP,
}

fun interface GestureGuiSessionListener {
    fun onClosed(ownerId: UUID, sessionId: UUID)

    /** セッションが閉じる前の入力理由を、必要な利用側へ通知します。 */
    fun onCloseRequested(
        ownerId: UUID,
        sessionId: UUID,
        actorId: UUID,
        reason: GestureGuiCloseReason,
    ) {
    }
}

/** 固定位置モードの画面配置を指定します。open時に指定すると画面をワールド固定し、プレイヤー追従しなくなります。 */
data class GestureGuiOpenOptions(
    /** 画面を固定するワールド位置。nullならプレイヤー追従モード */
    val anchor: Location? = null,
    /** セッション終了時に呼び出す利用側のライフサイクル通知 */
    val sessionListener: GestureGuiSessionListener? = null,
    /** 主要画面の並び方向。既定は従来の縦配置 */
    val layout: GestureGuiScreenLayout = GestureGuiScreenLayout.VERTICAL,
    /**
     * 縦配置で各viewを置くスロット。nullなら従来どおりview数に応じて連続配置します。
     * 例えばTOPとMIDDLEだけを指定すると、下スロットを画面で埋めずに上・中へ配置できます。
     */
    val verticalSlots: List<GestureGuiVerticalSlot>? = null,
    /**
     * SECONDARY/SHIFT_SECONDARYをGesture GUIのActionへ変換するか。
     * Inventory GUIの右クリック仕様とは独立した、Interaction入力だけの設定です。
     * 既定値は既存利用者との互換性を保つためtrueです。
     */
    val secondaryInputEnabled: Boolean = true,
    /**
     * このセッションの参加者がワールドへ送る左右クリックを、画面外も含めて吸収するか。
     * Inventory GUIのクリックイベントや、secondaryInputEnabledのAction解決には影響しません。
     */
    val suppressWorldClicks: Boolean = false,
) {
    init {
        require(anchor == null || (anchor.x.isFinite() && anchor.y.isFinite() && anchor.z.isFinite())) {
            "gesture GUI open anchor must be finite"
        }
        verticalSlots?.let { slots ->
            require(layout == GestureGuiScreenLayout.VERTICAL) {
                "gesture GUI vertical slots require vertical layout"
            }
            require(slots.size in 1..3) {
                "gesture GUI vertical slots must contain one to three slots"
            }
            require(slots.distinct().size == slots.size) {
                "gesture GUI vertical slots must be unique"
            }
        }
    }
}

/**
 * セッション内の主要画面（親画面群）の並び方向です。
 *
 * VERTICAL は従来どおり画面を縦（pitch方向）へ積み、HORIZONTAL は画面を
 * 左右（yaw方向）へ並べます。HORIZONTALでは各画面がその画面の中心方向へ
 * 正対します。画面の並び順は呼び出しが渡すviewsの順序そのものであり、
 * 例えば2画面をHORIZONTALで開くと1つ目が左、2つ目が右へ配置されます。
 */
enum class GestureGuiScreenLayout {
    VERTICAL,
    HORIZONTAL,
}

/** 縦配置で使用する上・中・下の画面スロットです。 */
enum class GestureGuiVerticalSlot {
    TOP,
    MIDDLE,
    BOTTOM,
}

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
 * 画面定義へ追加で適用する、実行時の操作認可です。
 *
 * ALLOWLISTは画面を開いた時点のUUID集合を表すため、ワールド単位の権限や
 * ロールが変化する機能では認可が古くなります。距離や遮蔽など、入力受付にだけ
 * 適用する条件もこのポリシーへ渡せます。
 */
fun interface GestureGuiAccessPolicy {
    fun canOperate(ownerId: UUID, actorId: UUID): Boolean
}

/**
 * 画面定義へ追加で適用する、実行時の表示認可です。
 *
 * 操作認可とは分離されているため、共有画面を表示したまま、距離や視線などの
 * 一時的な条件だけで入力受付を停止できます。画面の表示中に権限が変化する
 * 機能では、呼び出し側が現在の表示権限を再確認できます。
 */
fun interface GestureGuiVisibilityPolicy {
    fun canView(ownerId: UUID, viewerId: UUID): Boolean
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
    val hoverText: GestureGuiHoverText? = null,
    /** Text・Item・Blockのどの表示物に対する操作領域かを明示します。 */
    val targetVisualId: String? = null,
    /**
     * プレイヤーの現在状態に依存する入力可否を、入力時点で再評価します。
     *
     * acceptedGesturesだけで状態依存の入力を無効化すると、view生成後の持ち替えや
     * 外部変更が反映されるまで、表示上は同じボタンなのにクリック音もActionも発生
     * しない状態になります。動的な条件はこのガードへ渡し、画面を再構築せずに
     * 最新状態を判定できるようにします。falseの場合は通常の未対応入力と同じく、
     * イベントをGUI側で消費しますが、効果音とActionは発生させません。
     */
    val gestureGuard: ((Player, GestureGuiGesture) -> Boolean)? = null,
) {
    init {
        require(elementId.isNotBlank()) { "gesture GUI elementId must not be blank" }
        require(targetVisualId == null || targetVisualId.isNotBlank()) {
            "gesture GUI targetVisualId must not be blank"
        }
    }

    /** 静的な入力種別と、必要なら入力時点の動的条件を合わせて判定します。 */
    fun acceptsGesture(player: Player, gesture: GestureGuiGesture): Boolean =
        gesture in acceptedGestures && (gestureGuard?.invoke(player, gesture) ?: true)
}

/** 操作者だけへ表示するホバーテキストです。位置は画面中央基準で自由に指定できます。 */
data class GestureGuiHoverText(
    val text: Component,
    val x: Double,
    val y: Double,
    /** 画面上の文字サイズです。通常テキストと同じ規則でTextDisplay用の倍率へ変換されます。 */
    val size: Double = 0.006,
    val lineWidth: Int = 160,
    /**
     * ホバー表示の層です。replacesVisualIdを指定した場合、深さは置換対象visualの層より
     * 0.02ブロックだけ前面へ解決され、この値は対象を解決できない場合のフォールバック
     * として使われます。置換しないホバーは、この層のまま画面の最前面へ浮きます。
     */
    val layer: Int = 30,
    /**
     * ホバー中だけ一時的に置き換える通常表示のvisualIdです。
     *
     * 既定説明とホバー説明を同じ意味スロットへ表示する画面では、ホバー用の
     * TextDisplayを既定文の上へ重ねると二重表示になります。このIDを指定した
     * 場合、CC-Systemは操作者に対して既定表示を隠し、ホバー終了時に復元します。
     */
    val replacesVisualId: String? = null,
    /** ホバー中に一時表示するBlockDisplayの置換対象visualIdです。 */
    val hoverBlockVisualId: String? = null,
    /** hoverBlockVisualIdへ一時表示するBlockDisplayのBlockDataです。操作者ごとにだけ配布されます。 */
    val hoverBlockData: BlockData? = null,
) {
    init {
        require(x.isFinite() && y.isFinite()) { "gesture GUI hover position must be finite" }
        require(size > 0.0) { "gesture GUI hover size must be positive" }
        require(lineWidth > 0) { "gesture GUI hover lineWidth must be positive" }
        require(layer in 1..40) { "gesture GUI hover layer must be between 1 and 40" }
        require(replacesVisualId == null || replacesVisualId.isNotBlank()) {
            "gesture GUI hover replacesVisualId must not be blank"
        }
        require(hoverBlockVisualId == null || hoverBlockVisualId.isNotBlank()) {
            "gesture GUI hover hoverBlockVisualId must not be blank"
        }
        require((hoverBlockVisualId == null) == (hoverBlockData == null)) {
            "gesture GUI hover hoverBlockVisualId and hoverBlockData must be specified together"
        }
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
    /** 静的なaccess判定へ追加する実行時の操作認可。nullなら従来どおりです。 */
    val accessPolicy: GestureGuiAccessPolicy? = null,
    /** 静的なaccess判定へ追加する実行時の表示認可。nullなら操作認可を表示にも適用します。 */
    val visibilityPolicy: GestureGuiVisibilityPolicy? = null,
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

    private fun hasStaticAccess(actorId: UUID, ownerId: UUID): Boolean = when (access) {
        GestureGuiAccess.OWNER_ONLY -> actorId == ownerId
        GestureGuiAccess.ALLOWLIST -> actorId == ownerId || actorId in allowlist
        GestureGuiAccess.PUBLIC -> true
    }

    fun canOperate(ownerId: UUID, actorId: UUID): Boolean =
        hasStaticAccess(actorId, ownerId) && (accessPolicy?.canOperate(ownerId, actorId) ?: true)

    /** 表示認可が未指定の既存画面では、従来どおり操作認可を表示にも適用します。 */
    fun canView(ownerId: UUID, viewerId: UUID): Boolean =
        hasStaticAccess(viewerId, ownerId) &&
            (visibilityPolicy?.canView(ownerId, viewerId)
                ?: accessPolicy?.canOperate(ownerId, viewerId)
                ?: true)
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
    fun cross(other: GestureGuiVector3): GestureGuiVector3 = GestureGuiVector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )
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
