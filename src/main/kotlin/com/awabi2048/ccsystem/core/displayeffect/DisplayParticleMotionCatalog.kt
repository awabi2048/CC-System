package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3

internal enum class DisplayParticleMotionKind {
    STATIC, INERTIAL, BALLISTIC, BUOYANT, DRIFT, BURST, ORBIT, ATTRACT
}

internal enum class DisplayParticleCollisionMode {
    NONE, REMOVE, STOP, SLIDE, BOUNCE
}

/** 外観から独立して再利用できる、1 tickごとの運動更新則です。 */
internal data class DisplayParticleMotionPreset(
    val id: String,
    val kind: DisplayParticleMotionKind,
    val accelerationPerTick: DisplayEffectVector3 = DisplayEffectVector3.ZERO,
    val velocityRetentionPerTick: Double = 1.0,
    val turbulenceStrength: Double = 0.0,
    val turbulenceFrequency: Double = 0.0,
    val radialSpeed: Double = 0.0,
    val burstInitializer: Boolean = false,
    val spawnRadius: Double = 0.0,
    val orbitRadiansPerTick: Double = 0.0,
    val radialPullPerTick: Double = 0.0,
    val attractionPerTick: Double = 0.0,
    val maxSpeed: Double = 1.0,
    val collisionMode: DisplayParticleCollisionMode = DisplayParticleCollisionMode.NONE,
    val restitution: Double = 0.5
) {
    init {
        require(id.startsWith("cc:")) { "移動プリセットIDはcc namespaceで定義してください" }
        require(velocityRetentionPerTick in 0.0..1.0)
        require(turbulenceStrength >= 0.0 && turbulenceFrequency >= 0.0)
        require(radialSpeed >= 0.0 && spawnRadius >= 0.0 && attractionPerTick >= 0.0 && maxSpeed > 0.0)
        require(restitution in 0.0..1.0)
    }
}

internal object DisplayParticleMotionCatalog {
    private val presets = listOf(
        DisplayParticleMotionPreset("cc:static", DisplayParticleMotionKind.STATIC),
        DisplayParticleMotionPreset("cc:inertial", DisplayParticleMotionKind.INERTIAL, velocityRetentionPerTick = 0.98),
        DisplayParticleMotionPreset("cc:ballistic", DisplayParticleMotionKind.BALLISTIC, DisplayEffectVector3(0.0, -0.012, 0.0), 0.96, collisionMode = DisplayParticleCollisionMode.BOUNCE, restitution = 0.45),
        DisplayParticleMotionPreset("cc:buoyant", DisplayParticleMotionKind.BUOYANT, DisplayEffectVector3(0.0, 0.002, 0.0), 0.96, turbulenceStrength = 0.0045, turbulenceFrequency = 0.22),
        DisplayParticleMotionPreset("cc:drift", DisplayParticleMotionKind.DRIFT, velocityRetentionPerTick = 0.97, turbulenceStrength = 0.0105, turbulenceFrequency = 0.16),
        DisplayParticleMotionPreset("cc:burst", DisplayParticleMotionKind.INERTIAL, DisplayEffectVector3(0.0, -0.004, 0.0), 0.92, radialSpeed = 0.085, burstInitializer = true, collisionMode = DisplayParticleCollisionMode.REMOVE),
        DisplayParticleMotionPreset("cc:orbit", DisplayParticleMotionKind.ORBIT, velocityRetentionPerTick = 0.94, spawnRadius = 0.45, orbitRadiansPerTick = 0.18),
        DisplayParticleMotionPreset("cc:attract", DisplayParticleMotionKind.ATTRACT, velocityRetentionPerTick = 0.96, spawnRadius = 0.8, attractionPerTick = 0.012, maxSpeed = 0.16)
    )
    private val byId = presets.associateBy { it.id }

    fun require(id: String): DisplayParticleMotionPreset = requireNotNull(byId[id]) { "未登録の移動プリセットです: $id" }
    fun list(): List<DisplayParticleMotionPreset> = presets
}
