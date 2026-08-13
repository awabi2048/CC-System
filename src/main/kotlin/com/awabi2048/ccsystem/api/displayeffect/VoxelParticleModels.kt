package com.awabi2048.ccsystem.api.displayeffect

/**
 * CC-System が管理するボクセル粒子パターンの論理 ID です。
 * 利用側へ BlockData の割り当てを公開せず、表現と負荷計算を共通基盤へ集約するために使用します。
 */
@JvmInline
value class VoxelParticlePatternId(val value: String) {
    init {
        require(PATTERN.matches(value)) { "ボクセル粒子 ID は namespace:path 形式で指定してください: $value" }
    }

    companion object {
        private val PATTERN = Regex("[a-z0-9._-]+:[a-z0-9/._-]+")
    }
}

/** バニラ /particle の normal/force に対応する生成方針です。 */
enum class VoxelParticleVisibilityMode {
    /** 生成地点の通常表示距離内にプレイヤーがいない場合は生成しません。 */
    NORMAL,

    /** プレイヤーの有無にかかわらず生成します。Entity 自体の追跡距離は通常設定に従います。 */
    FORCE
}

/**
 * バニラ風コマンドからボクセル粒子を生成するための意味データです。
 * delta は発生原点のガウス分布、speed は各軸の初速度分布の標準偏差として扱います。
 */
data class VoxelParticleEmissionRequest(
    val patternId: VoxelParticlePatternId,
    val delta: DisplayEffectVector3 = DisplayEffectVector3.ZERO,
    val speed: Double = 0.0,
    val count: Int = 1,
    val visibilityMode: VoxelParticleVisibilityMode = VoxelParticleVisibilityMode.NORMAL,
    val randomSeed: Long = System.nanoTime()
) {
    init {
        require(delta.x >= 0.0 && delta.y >= 0.0 && delta.z >= 0.0) {
            "ボクセル粒子の delta は負数にできません: $delta"
        }
        require(speed.isFinite() && speed in 0.0..MAX_SPEED) {
            "ボクセル粒子の speed は 0..$MAX_SPEED で指定してください: $speed"
        }
        require(count in 1..MAX_COUNT) {
            "ボクセル粒子の count は 1..$MAX_COUNT で指定してください: $count"
        }
    }

    companion object {
        const val MAX_COUNT: Int = 8
        const val MAX_SPEED: Double = 2.0
    }
}

/** 補完や管理画面が低水準の BlockData を知らずにカタログを参照するための情報です。 */
data class VoxelParticlePatternInfo(
    val id: VoxelParticlePatternId,
    val voxelCount: Int,
    val lifetimeTicks: Int
)
