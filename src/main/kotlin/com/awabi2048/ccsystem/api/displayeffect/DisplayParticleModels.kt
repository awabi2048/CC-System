package com.awabi2048.ccsystem.api.displayeffect

/** プロパティ駆動の単一Displayパーティクル・プリセットIDです。 */
@JvmInline
value class DisplayParticlePresetId(val value: String) {
    init {
        require(PATTERN.matches(value)) { "DisplayパーティクルIDはnamespace:path形式で指定してください: $value" }
    }

    companion object {
        private val PATTERN = Regex("[a-z0-9._-]+:[a-z0-9/._-]+")
    }
}

enum class DisplayParticleVisibilityMode {
    /** 通常表示距離内にプレイヤーがいない場合は生成しません。 */
    NORMAL,

    /** プレイヤーの有無にかかわらず生成します。Entityの追跡距離自体は変更しません。 */
    FORCE
}

/** プリセットの性質を保ったまま、発生位置・速度・個数だけを呼び出し側から変化させます。 */
data class DisplayParticleEmissionRequest(
    val presetId: DisplayParticlePresetId,
    val delta: DisplayEffectVector3 = DisplayEffectVector3.ZERO,
    val speed: Double = 0.0,
    val count: Int = 1,
    val visibilityMode: DisplayParticleVisibilityMode = DisplayParticleVisibilityMode.NORMAL,
    val randomSeed: Long = System.nanoTime()
) {
    init {
        require(delta.x >= 0.0 && delta.y >= 0.0 && delta.z >= 0.0) { "deltaは負数にできません: $delta" }
        require(speed.isFinite() && speed in 0.0..MAX_SPEED) { "speedは0..${MAX_SPEED}で指定してください: $speed" }
        require(count in 1..MAX_COUNT) { "countは1..${MAX_COUNT}で指定してください: $count" }
    }

    companion object {
        const val MAX_COUNT = 32
        const val MAX_SPEED = 2.0
    }
}

/** 補完や管理機能へ公開する、低水準BlockDataを含まないプリセット情報です。 */
data class DisplayParticlePresetInfo(
    val id: DisplayParticlePresetId,
    val lifetimeTicks: Int,
    val fadeOutTicks: Int
)
