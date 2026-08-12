package com.awabi2048.ccsystem.api.displayeffect

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Display Effectが参照する表示アセットの安定IDです。
 *
 * PaperのMaterialやItemStackをAPIへ漏らさず、後続のBlockDisplay/ItemDisplay
 * backendがアセットIDを実体へ解決できるようにします。実体の解決はbackendの責務です。
 */
@JvmInline
value class DisplayEffectAssetId(val value: String) {
    init {
        require(ASSET_ID_PATTERN.matches(value)) {
            "Display EffectのアセットIDは namespace:path 形式で指定してください: $value"
        }
    }

    companion object {
        private val ASSET_ID_PATTERN = Regex("[a-z0-9._-]+:[a-z0-9/._-]+")
    }
}

/**
 * 1つのDisplay要素を識別する論理IDです。
 * Entity UUIDやパケットEntity IDを外部契約へ持ち込まないために使用します。
 */
@JvmInline
value class DisplayEffectNodeId(val value: String) {
    init {
        require(NODE_ID_PATTERN.matches(value)) {
            "Display EffectのNode IDに使用できない文字が含まれています: $value"
        }
    }

    companion object {
        private val NODE_ID_PATTERN = Regex("[a-z0-9._-]+")
    }
}

/** Display Entity backendが解決する表示種別とアセット参照です。 */
sealed interface DisplayEffectAppearance {
    val assetId: DisplayEffectAssetId

    data class Block(
        override val assetId: DisplayEffectAssetId
    ) : DisplayEffectAppearance

    data class Item(
        override val assetId: DisplayEffectAssetId
    ) : DisplayEffectAppearance
}

/**
 * Bukkit/JOMLに依存しない3次元ベクトルです。
 *
 * 長さ・位置はblock、速度はblock/tick、加速度はblock/tick²として扱います。
 * すべての値は有限値でなければならず、NaN/Infinityを状態へ持ち込めません。
 */
data class DisplayEffectVector3(
    val x: Double,
    val y: Double,
    val z: Double
) {
    init {
        require(x.isFinite() && y.isFinite() && z.isFinite()) {
            "Display Effectのベクトルは有限値でなければなりません: ($x, $y, $z)"
        }
    }

    operator fun plus(other: DisplayEffectVector3): DisplayEffectVector3 =
        DisplayEffectVector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: DisplayEffectVector3): DisplayEffectVector3 =
        DisplayEffectVector3(x - other.x, y - other.y, z - other.z)

    operator fun times(multiplier: Double): DisplayEffectVector3 {
        require(multiplier.isFinite()) { "ベクトル倍率は有限値でなければなりません: $multiplier" }
        return DisplayEffectVector3(x * multiplier, y * multiplier, z * multiplier)
    }

    operator fun div(divisor: Double): DisplayEffectVector3 {
        require(divisor.isFinite() && divisor != 0.0) {
            "ベクトル除算の除数は0以外の有限値でなければなりません: $divisor"
        }
        return DisplayEffectVector3(x / divisor, y / divisor, z / divisor)
    }

    fun lengthSquared(): Double = x * x + y * y + z * z

    fun length(): Double = sqrt(lengthSquared()).also {
        require(it.isFinite()) { "ベクトル長が有限値ではありません: $this" }
    }

    fun normalized(): DisplayEffectVector3 {
        val length = length()
        require(length.isFinite() && length > NORMALIZATION_EPSILON) {
            "ゼロ長ベクトルは正規化できません: $this"
        }
        return this / length
    }

    fun lerp(to: DisplayEffectVector3, progress: Double): DisplayEffectVector3 {
        require(progress.isFinite() && progress in 0.0..1.0) {
            "補間率は0～1の有限値でなければなりません: $progress"
        }
        // 差分(to - this)を先に求めると、両端が大きく符号反転する場合に
        // 実際の補間結果が有限でも中間値だけがInfinityになるため、重み付き和にします。
        val inverseProgress = 1.0 - progress
        return DisplayEffectVector3(
            x * inverseProgress + to.x * progress,
            y * inverseProgress + to.y * progress,
            z * inverseProgress + to.z * progress
        )
    }

    companion object {
        private const val NORMALIZATION_EPSILON = 1.0E-12

        @JvmField
        val ZERO = DisplayEffectVector3(0.0, 0.0, 0.0)
    }
}

/**
 * 正規化済みクォータニオンです。
 *
 * 回転角はradian、回転軸は右手系を使用します。公開コンストラクタを
 * 正規化済み値の生成処理へ集約し、Euler角の順序差をDisplay Effectの状態へ持ち込みません。
 */
@ConsistentCopyVisibility
data class DisplayEffectQuaternion private constructor(
    val x: Double,
    val y: Double,
    val z: Double,
    val w: Double
) {
    fun multiply(other: DisplayEffectQuaternion): DisplayEffectQuaternion = of(
        x = w * other.x + x * other.w + y * other.z - z * other.y,
        y = w * other.y - x * other.z + y * other.w + z * other.x,
        z = w * other.z + x * other.y - y * other.x + z * other.w,
        w = w * other.w - x * other.x - y * other.y - z * other.z
    )

    companion object {
        private const val NORMALIZATION_EPSILON = 1.0E-12

        @JvmField
        val IDENTITY = DisplayEffectQuaternion(0.0, 0.0, 0.0, 1.0)

        /** 任意の有限値を正規化してクォータニオンへ変換します。 */
        @JvmStatic
        fun of(x: Double, y: Double, z: Double, w: Double): DisplayEffectQuaternion {
            require(x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()) {
                "Display Effectのクォータニオンは有限値でなければなりません"
            }
            val length = sqrt(x * x + y * y + z * z + w * w)
            require(length.isFinite() && length > NORMALIZATION_EPSILON) {
                "ゼロ長クォータニオンは使用できません"
            }
            return DisplayEffectQuaternion(x / length, y / length, z / length, w / length)
        }

        /** 軸とradian角から右手系の回転を生成します。 */
        @JvmStatic
        fun fromAxisAngle(axis: DisplayEffectVector3, angleRadians: Double): DisplayEffectQuaternion {
            require(angleRadians.isFinite()) {
                "回転角は有限値でなければなりません: $angleRadians"
            }
            val normalizedAxis = axis.normalized()
            val halfAngle = angleRadians * 0.5
            val sinHalfAngle = sin(halfAngle)
            return of(
                normalizedAxis.x * sinHalfAngle,
                normalizedAxis.y * sinHalfAngle,
                normalizedAxis.z * sinHalfAngle,
                cos(halfAngle)
            )
        }
    }
}

/** Display Entityのbrightness overrideです。Paperのblock/sky light 0～15に対応します。 */
data class DisplayEffectLight(
    val block: Int,
    val sky: Int
) {
    init {
        require(block in 0..15) { "block lightは0～15で指定してください: $block" }
        require(sky in 0..15) { "sky lightは0～15で指定してください: $sky" }
    }
}

/** 数値カーブのキーフレームです。progressは0～1、valueは有限値です。 */
data class DisplayEffectScalarKeyframe(
    val progress: Double,
    val value: Double
) {
    init {
        require(progress.isFinite() && progress in 0.0..1.0) {
            "カーブのprogressは0～1の有限値でなければなりません: $progress"
        }
        require(value.isFinite()) { "カーブの値は有限値でなければなりません: $value" }
    }
}

/** ベクトルカーブのキーフレームです。 */
data class DisplayEffectVectorKeyframe(
    val progress: Double,
    val value: DisplayEffectVector3
) {
    init {
        require(progress.isFinite() && progress in 0.0..1.0) {
            "カーブのprogressは0～1の有限値でなければなりません: $progress"
        }
    }
}

/**
 * 不変の区分線形スカラー補間です。
 * 入力Iterableは生成時にコピーするため、呼出し元のコレクション変更で定義が変化しません。
 */
class DisplayEffectScalarCurve(keyframes: Iterable<DisplayEffectScalarKeyframe>) {
    private val points: List<DisplayEffectScalarKeyframe> = keyframes.toList()

    init {
        require(points.isNotEmpty()) { "Display Effectのカーブには1つ以上のキーフレームが必要です" }
        require(points.first().progress == 0.0) { "カーブの先頭progressは0でなければなりません" }
        require(points.last().progress == 1.0) { "カーブの末尾progressは1でなければなりません" }
        points.zipWithNext().forEach { (left, right) ->
            require(left.progress < right.progress) {
                "カーブのprogressは重複せず、昇順でなければなりません"
            }
        }
    }

    fun keyframes(): List<DisplayEffectScalarKeyframe> = points.toList()

    fun sample(progress: Double): Double {
        require(progress.isFinite() && progress in 0.0..1.0) {
            "カーブのサンプルprogressは0～1の有限値でなければなりません: $progress"
        }
        if (progress <= points.first().progress) return points.first().value
        if (progress >= points.last().progress) return points.last().value

        for (index in 1 until points.size) {
            val right = points[index]
            if (progress <= right.progress) {
                val left = points[index - 1]
                val localProgress = (progress - left.progress) / (right.progress - left.progress)
                val inverseProgress = 1.0 - localProgress
                val value = left.value * inverseProgress + right.value * localProgress
                require(value.isFinite()) { "カーブ補間結果が有限値ではありません: $value" }
                return value
            }
        }
        return points.last().value
    }

    fun requireValueRange(minimum: Double, maximum: Double, description: String) {
        require(minimum.isFinite() && maximum.isFinite() && minimum <= maximum) {
            "カーブの値域が不正です: $minimum..$maximum"
        }
        points.forEach { point ->
            require(point.value in minimum..maximum) {
                "$description は${minimum}～${maximum}でなければなりません: ${point.value}"
            }
        }
    }

    companion object {
        @JvmStatic
        fun constant(value: Double): DisplayEffectScalarCurve = DisplayEffectScalarCurve(
            listOf(
                DisplayEffectScalarKeyframe(0.0, value),
                DisplayEffectScalarKeyframe(1.0, value)
            )
        )
    }
}

/** 不変の区分線形ベクトル補間です。 */
class DisplayEffectVectorCurve(keyframes: Iterable<DisplayEffectVectorKeyframe>) {
    private val points: List<DisplayEffectVectorKeyframe> = keyframes.toList()

    init {
        require(points.isNotEmpty()) { "Display Effectのベクトルカーブには1つ以上のキーフレームが必要です" }
        require(points.first().progress == 0.0) { "カーブの先頭progressは0でなければなりません" }
        require(points.last().progress == 1.0) { "カーブの末尾progressは1でなければなりません" }
        points.zipWithNext().forEach { (left, right) ->
            require(left.progress < right.progress) {
                "カーブのprogressは重複せず、昇順でなければなりません"
            }
        }
    }

    fun keyframes(): List<DisplayEffectVectorKeyframe> = points.toList()

    fun sample(progress: Double): DisplayEffectVector3 {
        require(progress.isFinite() && progress in 0.0..1.0) {
            "カーブのサンプルprogressは0～1の有限値でなければなりません: $progress"
        }
        if (progress <= points.first().progress) return points.first().value
        if (progress >= points.last().progress) return points.last().value

        for (index in 1 until points.size) {
            val right = points[index]
            if (progress <= right.progress) {
                val left = points[index - 1]
                val localProgress = (progress - left.progress) / (right.progress - left.progress)
                return left.value.lerp(right.value, localProgress)
            }
        }
        return points.last().value
    }

    fun requireNonNegative(description: String) {
        points.forEach { point ->
            require(point.value.x >= 0.0 && point.value.y >= 0.0 && point.value.z >= 0.0) {
                "$description は負値を含められません: ${point.value}"
            }
        }
    }

    companion object {
        @JvmStatic
        fun constant(value: DisplayEffectVector3): DisplayEffectVectorCurve = DisplayEffectVectorCurve(
            listOf(
                DisplayEffectVectorKeyframe(0.0, value),
                DisplayEffectVectorKeyframe(1.0, value)
            )
        )
    }
}

/** block/sky lightを独立して補間するbrightnessカーブです。丸めは最近傍整数です。 */
data class DisplayEffectBrightnessCurve(
    val block: DisplayEffectScalarCurve,
    val sky: DisplayEffectScalarCurve
) {
    init {
        block.requireValueRange(0.0, 15.0, "block light")
        sky.requireValueRange(0.0, 15.0, "sky light")
    }

    fun sample(progress: Double): DisplayEffectLight = DisplayEffectLight(
        block = block.sample(progress).roundToInt(),
        sky = sky.sample(progress).roundToInt()
    )

    companion object {
        @JvmStatic
        fun constant(light: DisplayEffectLight): DisplayEffectBrightnessCurve = DisplayEffectBrightnessCurve(
            DisplayEffectScalarCurve.constant(light.block.toDouble()),
            DisplayEffectScalarCurve.constant(light.sky.toDouble())
        )
    }
}

/**
 * Display EntityのTransformationへ変換される時間変化です。
 * originOffset（発生源からの相対位置）とlocalTranslation（Transformation内translation）を分離します。
 */
data class DisplayEffectTransformCurves(
    val localTranslation: DisplayEffectVectorCurve = DisplayEffectVectorCurve.constant(DisplayEffectVector3.ZERO),
    val scale: DisplayEffectVectorCurve = DisplayEffectVectorCurve.constant(
        DisplayEffectVector3(1.0, 1.0, 1.0)
    ),
    val brightness: DisplayEffectBrightnessCurve? = null
) {
    init {
        scale.requireNonNegative("Display Effectのscale")
    }
}

/**
 * Vanilla粒子の更新思想を参考にした物理パラメータです。
 * 厳密なMinecraft本体の内部実装互換ではなく、Display Entity向けの初期モデルです。
 */
data class DisplayEffectPhysics(
    /** 発生源からの相対初期位置。長さの単位はblockです。 */
    val initialOriginOffset: DisplayEffectVector3 = DisplayEffectVector3.ZERO,
    val initialVelocity: DisplayEffectVector3 = DisplayEffectVector3.ZERO,
    val gravityPerTick: DisplayEffectVector3 = DisplayEffectVector3.ZERO,
    val velocityRetentionPerTick: Double = 1.0,
    val lifetimeTicks: Int = 20,
    val initialRotation: DisplayEffectQuaternion = DisplayEffectQuaternion.IDENTITY,
    val angularVelocityRadiansPerTick: DisplayEffectVector3 = DisplayEffectVector3.ZERO,
    val transforms: DisplayEffectTransformCurves = DisplayEffectTransformCurves()
) {
    init {
        require(lifetimeTicks > 0) { "Display EffectのlifetimeTicksは1以上でなければなりません" }
        require(velocityRetentionPerTick.isFinite() && velocityRetentionPerTick in 0.0..1.0) {
            "velocityRetentionPerTickは0～1の有限値でなければなりません: $velocityRetentionPerTick"
        }
    }
}

/** Simulationが保持する論理状態です。originOffsetは発生源からの相対値です。 */
data class DisplayEffectState(
    val ageTicks: Int,
    val originOffset: DisplayEffectVector3,
    val velocity: DisplayEffectVector3,
    val rotation: DisplayEffectQuaternion
) {
    init {
        require(ageTicks >= 0) { "Display EffectのageTicksは0以上でなければなりません" }
    }
}

/** Backendへ渡す1フレーム分の期待表示状態です。 */
data class DisplayEffectFrame(
    /** 発生源へbackendが解決したワールド原点からの相対位置です。 */
    val originOffset: DisplayEffectVector3,
    val localTranslation: DisplayEffectVector3,
    val rotation: DisplayEffectQuaternion,
    val scale: DisplayEffectVector3,
    val brightness: DisplayEffectLight?
) {
    init {
        scale.let {
            require(it.x >= 0.0 && it.y >= 0.0 && it.z >= 0.0) {
                "Display Effectのscaleは負値を含められません: $it"
            }
        }
    }
}

sealed interface DisplayEffectStepResult {
    data class Advanced(
        val state: DisplayEffectState,
        val frame: DisplayEffectFrame
    ) : DisplayEffectStepResult

    data object Completed : DisplayEffectStepResult
}

/**
 * Bukkit非依存の単一Display粒子シミュレーションです。
 *
 * tick順序は次で固定します。
 * 1. 現在のvelocityでoriginOffsetを進める
 * 2. gravityPerTickをvelocityへ加える
 * 3. velocityRetentionPerTickをvelocityへ掛ける
 * 4. angularVelocityRadiansPerTickをrotationへ適用する
 * 5. ageTicksを1増やし、次フレームを生成する
 *
 * lifetimeTicksは移動区間数です。初期フレーム（age=0）から最終フレーム
 * （age=lifetimeTicks）までを生成し、その次のstepでCompletedになります。
 */
class DisplayEffectSimulation(
    val physics: DisplayEffectPhysics
) {
    fun initialState(): DisplayEffectState = DisplayEffectState(
        ageTicks = 0,
        originOffset = physics.initialOriginOffset,
        velocity = physics.initialVelocity,
        rotation = physics.initialRotation
    )

    fun frame(state: DisplayEffectState): DisplayEffectFrame {
        require(state.ageTicks in 0..physics.lifetimeTicks) {
            "Display EffectのageTicksが寿命範囲外です: ${state.ageTicks}"
        }
        val progress = state.ageTicks.toDouble() / physics.lifetimeTicks.toDouble()
        return DisplayEffectFrame(
            originOffset = state.originOffset,
            localTranslation = physics.transforms.localTranslation.sample(progress),
            rotation = state.rotation,
            scale = physics.transforms.scale.sample(progress),
            brightness = physics.transforms.brightness?.sample(progress)
        )
    }

    fun step(state: DisplayEffectState): DisplayEffectStepResult {
        require(state.ageTicks in 0..physics.lifetimeTicks) {
            "Display EffectのageTicksが寿命範囲外です: ${state.ageTicks}"
        }
        if (state.ageTicks >= physics.lifetimeTicks) {
            return DisplayEffectStepResult.Completed
        }

        // Vanilla粒子の初速度を最初の移動へ使う感覚に合わせ、移動後に重力とdragを適用します。
        val nextPosition = state.originOffset + state.velocity
        val nextVelocity = (state.velocity + physics.gravityPerTick) * physics.velocityRetentionPerTick
        val nextRotation = state.rotation.multiply(rotationDelta(physics.angularVelocityRadiansPerTick))
        val nextState = DisplayEffectState(
            ageTicks = state.ageTicks + 1,
            originOffset = nextPosition,
            velocity = nextVelocity,
            rotation = nextRotation
        )
        return DisplayEffectStepResult.Advanced(nextState, frame(nextState))
    }

    private fun rotationDelta(angularVelocity: DisplayEffectVector3): DisplayEffectQuaternion {
        val angle = angularVelocity.length()
        if (abs(angle) < 1.0E-12) {
            return DisplayEffectQuaternion.IDENTITY
        }
        return DisplayEffectQuaternion.fromAxisAngle(angularVelocity / angle, angle)
    }
}

/** 1つのDisplay Nodeを生成するための定義です。複数NodeのEffect定義は後続単位で追加します。 */
data class DisplayEffectNodeDefinition(
    val nodeId: DisplayEffectNodeId,
    val appearance: DisplayEffectAppearance,
    val physics: DisplayEffectPhysics
)
