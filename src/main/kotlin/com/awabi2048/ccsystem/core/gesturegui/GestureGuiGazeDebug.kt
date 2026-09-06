package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import com.awabi2048.ccsystem.api.localization.generated.GestureGuiKeys
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import java.time.Duration

/**
 * 視線追従デバッグ用のスナップショットです。
 *
 * 判定・描画へ影響しない観測専用の値だけを保持します。所有者本人への
 * デバッグ表示（視線パーティクル・字幕・位置更新理由チャット）の共通入力です。
 * デバッグ用途のため常時評価します。原因特定後に撤去する前提です。
 */
internal data class GazeDebugSnapshot(
    /** 最終的な画面内外判定です。 */
    val inside: Boolean,
    /** 最も小さい円錐角です（度）。画面なしはnullです。 */
    val coneAngle: Double?,
    /** 当たり判定の距離です。ヒットなし・ダミー表示中はnullです。 */
    val hitDistance: Double?,
    /** 当たった要素IDです。要素なし・ヒットなしはnullです。 */
    val hitElementId: String?,
    /** 当たった画面番号です。ヒットなしはnullです。 */
    val hitScreenIndex: Int?,
    /** 最も近い画面中心までの距離です。画面なしはnullです。 */
    val nearestCenterDistance: Double?,
    /** 操作可能距離です。 */
    val range: Double,
    /** 追従の運動状態名です（MOVING/SETTLING/STOPPED/PINNED）。 */
    val motionName: String,
    /** 停止確定待ちの移動があるかです。 */
    val followDirty: Boolean,
    /** ダミー表示中かです。 */
    val dummyActive: Boolean,
    /** 前回確定位置からの変位です（ブロック単位）。 */
    val displacement: Double,
    /** 視線の起点（目位置）です。 */
    val origin: GestureGuiVector3,
    /** 視線方向（正規化済み）です。 */
    val direction: GestureGuiVector3,
)

/** デバッグ表示のパーティクル計画です。Bukkit呼び出しを含まないため単体テストできます。 */
internal data class GazeParticlePlan(
    /** 視線レイ用の粒子です。内外で色分けします。 */
    val rayParticle: Particle,
    /** レイ上の打点です。 */
    val points: List<GestureGuiVector3>,
    /** ヒット点マーカー用の粒子です。ヒットなしはnullです。 */
    val markerParticle: Particle?,
    /** ヒット点マーカーの位置です。ヒットなしはnullです。 */
    val markerPoint: GestureGuiVector3?,
)

/**
 * 視線デバッグの純粋計算とBukkit描画をまとめます。
 *
 * パーティクルは所有者本人のみに可視な `Player.spawnParticle` を使い、
 * 他プレイヤーへ影響しません。字幕は毎tick再送します（ちらつき・パケット増は
 * デバッグ用途として受容します）。原因特定後に撤去する前提です。
 */
internal object GestureGuiGazeDebug {
    /** 視線レイの打点間隔です（ブロック単位）。 */
    const val RAY_POINT_STEP: Double = 0.25

    /** 視線レイの最大打点数です。 */
    const val RAY_POINT_MAX: Int = 12

    /**
     * 視線レイ上の打点を返します。
     *
     * 方向は内部で正規化するため、呼び出し側で単位ベクトル化は不要です。
     * 最大距離が間隔未満の場合は空を返します。
     */
    fun rayPoints(
        origin: GestureGuiVector3,
        direction: GestureGuiVector3,
        maxDistance: Double,
        step: Double = RAY_POINT_STEP,
        maxPoints: Int = RAY_POINT_MAX,
    ): List<GestureGuiVector3> {
        require(step > 0.0) { "gaze debug ray step must be positive" }
        require(maxPoints > 0) { "gaze debug ray max points must be positive" }
        if (!maxDistance.isFinite() || maxDistance < step) return emptyList()
        val unit = direction.normalized()
        val points = mutableListOf<GestureGuiVector3>()
        var distance = step
        while (distance <= maxDistance + 1.0e-9 && points.size < maxPoints) {
            points += origin + unit * distance
            distance += step
        }
        return points
    }

    /**
     * スナップショットからパーティクル計画を組み立てます。
     *
     * レイはヒット点（なければ操作可能距離）まで伸ばし、内外で粒子種を変えます。
     * ヒット点には別種マーカーを置きます。
     */
    fun particlePlan(snapshot: GazeDebugSnapshot): GazeParticlePlan {
        val maxDistance = minOf(snapshot.hitDistance ?: snapshot.range, snapshot.range)
        val points = rayPoints(snapshot.origin, snapshot.direction, maxDistance)
        return if (snapshot.inside) {
            GazeParticlePlan(
                rayParticle = Particle.END_ROD,
                points = points,
                markerParticle = snapshot.hitDistance?.let { Particle.HAPPY_VILLAGER },
                markerPoint = snapshot.hitDistance?.let { snapshot.origin + snapshot.direction * it },
            )
        } else {
            GazeParticlePlan(
                rayParticle = Particle.SMOKE,
                points = points,
                markerParticle = snapshot.hitDistance?.let { Particle.CRIT },
                markerPoint = snapshot.hitDistance?.let { snapshot.origin + snapshot.direction * it },
            )
        }
    }

    /** 字幕テンプレートへの差し込み値を組み立てます。数値は小数2桁へ丸めます。 */
    fun subtitlePlaceholders(snapshot: GazeDebugSnapshot): Map<String, Any> = mapOf<String, Any>(
        "inside" to if (snapshot.inside) "内" else "外",
        "cone" to (snapshot.coneAngle?.let { "%.0f/60".format(it) } ?: "--"),
        "hit" to (snapshot.hitDistance?.let { "○%.2f".format(it) } ?: "×"),
        "dist" to (snapshot.nearestCenterDistance?.let { "%.2f".format(it) } ?: "--"),
        "range" to "%.2f".format(snapshot.range),
        "motion" to snapshot.motionName,
        "dirty" to if (snapshot.followDirty) " D" else "",
        "dummy" to if (snapshot.dummyActive) " DMY" else "",
        "displacement" to "%.2f".format(snapshot.displacement),
    )
}

/** 所有者本人へのデバッグ描画（パーティクル・字幕・チャット）のBukkit境界です。 */
internal object GestureGuiGazeDebugRenderer {
    private val legacy = LegacyComponentSerializer.legacySection()
    private val subtitleTimes = Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ZERO)

    /**
     * 毎tickのデバッグ表示を更新します。
     *
     * 判定・描画へ影響しない観測専用の処理です。パーティクルは所有者本人のみに
     * 可視です。字幕は内容の変化有無にかかわらず毎tick再送します。
     */
    fun renderTick(owner: Player, snapshot: GazeDebugSnapshot) {
        val plan = GestureGuiGazeDebug.particlePlan(snapshot)
        plan.points.forEach { point ->
            owner.spawnParticle(
                plan.rayParticle,
                Location(owner.world, point.x, point.y, point.z),
                1, 0.0, 0.0, 0.0, 0.0,
            )
        }
        val markerParticle = plan.markerParticle
        val markerPoint = plan.markerPoint
        if (markerParticle != null && markerPoint != null) {
            owner.spawnParticle(
                markerParticle,
                Location(owner.world, markerPoint.x, markerPoint.y, markerPoint.z),
                3, 0.02, 0.02, 0.02, 0.0,
            )
        }
        val text = CCSystem.getAPI().getLocalized(
            owner,
            GestureGuiKeys.GESTURE_GUI_DEBUG_SUBTITLE,
            GestureGuiGazeDebug.subtitlePlaceholders(snapshot),
        )
        owner.showTitle(Title.title(Component.empty(), legacy.deserialize(text), subtitleTimes))
    }

    /**
     * 位置更新の理由を所有者のチャットへ出力します。
     *
     * 再召喚・ダミー開始／復帰・本体復帰・固定解除など、見た目の位置が変わる
     * 経路からのみ呼び出します。高頻度の凍結スキップからは呼びません。
     */
    fun notify(
        owner: Player,
        key: com.awabi2048.ccsystem.api.localization.LocalizationKey<String>,
        placeholders: Map<String, Any> = emptyMap(),
    ) {
        owner.sendMessage(CCSystem.getAPI().getLocalized(owner, key, placeholders))
    }

    /** セッション終了時に残った字幕を消します。 */
    fun clear(owner: Player) {
        owner.clearTitle()
    }
}
