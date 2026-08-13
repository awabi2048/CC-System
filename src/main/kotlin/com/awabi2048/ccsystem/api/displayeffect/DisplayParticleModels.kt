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

@JvmInline
value class DisplayParticleMotionPresetId(val value: String) {
    init {
        require(PATTERN.matches(value)) { "Displayパーティクル動作IDはnamespace:path形式で指定してください: $value" }
    }

    companion object {
        private val PATTERN = Regex("[a-z0-9._-]+:[a-z0-9/._-]+")
    }
}

enum class DisplayParticleCollisionMode {
    NONE, REMOVE, STOP, SLIDE, BOUNCE
}

/** 動作プリセットの値へ乗算・上書きする、放出単位の調整値です。 */
data class DisplayParticleMotionProperties(
    val initialVelocity: DisplayEffectVector3? = null,
    val acceleration: DisplayEffectVector3? = null,
    val velocityRetention: Double? = null,
    val turbulenceStrength: Double? = null,
    val turbulenceFrequency: Double? = null,
    val radialSpeed: Double? = null,
    val spawnRadius: Double? = null,
    val orbitSpeed: Double? = null,
    val radialPull: Double? = null,
    val attraction: Double? = null,
    val maxSpeed: Double? = null
) {
    init {
        initialVelocity?.let {
            require(it.lengthSquared() <= MAX_INITIAL_SPEED * MAX_INITIAL_SPEED) {
                "initial velocityの速さは${MAX_INITIAL_SPEED}以下で指定してください: $it"
            }
        }
        acceleration?.let {
            require(it.lengthSquared() <= MAX_ACCELERATION * MAX_ACCELERATION) {
                "accelerationの大きさは${MAX_ACCELERATION}以下で指定してください: $it"
            }
        }
        listOf(
            "turbulence" to (turbulenceStrength to 0.05),
            "radial-speed" to (radialSpeed to 2.0),
            "spawn-radius" to (spawnRadius to 32.0),
            "radial-pull" to (radialPull to 2.0),
            "attraction" to (attraction to 0.1)
        ).forEach { (name, pair) -> pair.first?.let {
            require(it.isFinite() && it in 0.0..pair.second) { "${name}は0..${pair.second}で指定してください: $it" }
        }
        }
        orbitSpeed?.let {
            require(it.isFinite() && it in -Math.PI..Math.PI) { "orbit-speedは-pi..piで指定してください: $it" }
        }
        velocityRetention?.let {
            require(it.isFinite() && it in 0.0..1.0) { "retentionは0..1で指定してください: $it" }
        }
        turbulenceFrequency?.let {
            require(it.isFinite() && it in 0.0..Math.PI) { "frequencyは0..piで指定してください: $it" }
        }
        maxSpeed?.let {
            require(it.isFinite() && it in 0.001..2.0) { "max-speedは0.001..2で指定してください: $it" }
        }
    }

    companion object {
        const val MAX_INITIAL_SPEED = 2.0
        const val MAX_ACCELERATION = 0.1
    }
}

data class DisplayParticleCollisionProperties(val restitution: Double? = null) {
    init {
        restitution?.let {
            require(it.isFinite() && it in 0.0..1.0) { "restitutionは0..1で指定してください: $it" }
        }
    }
}

/** プリセットの性質を保ったまま、発生位置・速度・個数だけを呼び出し側から変化させます。 */
data class DisplayParticleEmissionRequest(
    val presetId: DisplayParticlePresetId,
    val motionPresetId: DisplayParticleMotionPresetId,
    val collisionMode: DisplayParticleCollisionMode,
    val motionProperties: DisplayParticleMotionProperties = DisplayParticleMotionProperties(),
    val collisionProperties: DisplayParticleCollisionProperties = DisplayParticleCollisionProperties(),
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
    val fadeOutTicks: Int,
    val defaultMotionPresetId: DisplayParticleMotionPresetId,
    val defaultCollisionMode: DisplayParticleCollisionMode
)

data class DisplayParticleMotionPresetInfo(val id: DisplayParticleMotionPresetId)
