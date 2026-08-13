package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleCollisionMode
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleCollisionProperties
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleMotionPresetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleMotionPresetInfo
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleMotionProperties

internal enum class DisplayParticleMotionKind {
    STATIC, INERTIAL, BALLISTIC, BUOYANT, DRIFT, BURST, ORBIT, ATTRACT
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
        DisplayParticleMotionPreset("cc:ballistic", DisplayParticleMotionKind.BALLISTIC, DisplayEffectVector3(0.0, -0.012, 0.0), 0.96, restitution = 0.45),
        DisplayParticleMotionPreset("cc:buoyant", DisplayParticleMotionKind.BUOYANT, DisplayEffectVector3(0.0, 0.002, 0.0), 0.96, turbulenceStrength = 0.0045, turbulenceFrequency = 0.22),
        DisplayParticleMotionPreset("cc:drift", DisplayParticleMotionKind.DRIFT, velocityRetentionPerTick = 0.97, turbulenceStrength = 0.0105, turbulenceFrequency = 0.16),
        DisplayParticleMotionPreset("cc:burst", DisplayParticleMotionKind.BURST, DisplayEffectVector3(0.0, -0.004, 0.0), 0.92, radialSpeed = 0.085, burstInitializer = true),
        DisplayParticleMotionPreset("cc:orbit", DisplayParticleMotionKind.ORBIT, velocityRetentionPerTick = 0.94, spawnRadius = 0.45, orbitRadiansPerTick = 0.18, radialPullPerTick = 0.02),
        DisplayParticleMotionPreset("cc:attract", DisplayParticleMotionKind.ATTRACT, velocityRetentionPerTick = 0.96, spawnRadius = 0.8, attractionPerTick = 0.012, maxSpeed = 0.16)
    )
    private val byId = presets.associateBy { it.id }

    fun require(id: String): DisplayParticleMotionPreset = requireNotNull(byId[id]) { "未登録の移動プリセットです: $id" }
    fun list(): List<DisplayParticleMotionPreset> = presets

    fun info(): List<DisplayParticleMotionPresetInfo> = presets.map {
        DisplayParticleMotionPresetInfo(DisplayParticleMotionPresetId(it.id))
    }

    fun resolve(
        id: DisplayParticleMotionPresetId,
        properties: DisplayParticleMotionProperties,
        collisionMode: DisplayParticleCollisionMode,
        collisionProperties: DisplayParticleCollisionProperties
    ): DisplayParticleMotionPreset {
        val base = require(id.value)
        validateProperties(base.kind, properties, collisionMode, collisionProperties)
        return base.copy(
            accelerationPerTick = properties.acceleration ?: base.accelerationPerTick,
            velocityRetentionPerTick = properties.velocityRetention ?: base.velocityRetentionPerTick,
            turbulenceStrength = properties.turbulenceStrength ?: base.turbulenceStrength,
            turbulenceFrequency = properties.turbulenceFrequency ?: base.turbulenceFrequency,
            radialSpeed = properties.radialSpeed ?: base.radialSpeed,
            spawnRadius = properties.spawnRadius ?: base.spawnRadius,
            orbitRadiansPerTick = properties.orbitSpeed ?: base.orbitRadiansPerTick,
            radialPullPerTick = properties.radialPull ?: base.radialPullPerTick,
            attractionPerTick = properties.attraction ?: base.attractionPerTick,
            maxSpeed = properties.maxSpeed ?: base.maxSpeed,
            restitution = collisionProperties.restitution ?: base.restitution
        )
    }

    private fun validateProperties(
        kind: DisplayParticleMotionKind,
        properties: DisplayParticleMotionProperties,
        collisionMode: DisplayParticleCollisionMode,
        collisionProperties: DisplayParticleCollisionProperties
    ) {
        val specified = buildSet {
            if (properties.initialVelocity != null) add("initial-velocity")
            if (properties.acceleration != null) add("acceleration")
            if (properties.velocityRetention != null) add("retention")
            if (properties.turbulenceStrength != null) add("turbulence")
            if (properties.turbulenceFrequency != null) add("frequency")
            if (properties.radialSpeed != null) add("radial-speed")
            if (properties.spawnRadius != null) add("spawn-radius")
            if (properties.orbitSpeed != null) add("orbit-speed")
            if (properties.radialPull != null) add("radial-pull")
            if (properties.attraction != null) add("attraction")
            if (properties.maxSpeed != null) add("max-speed")
        }
        val allowed = when (kind) {
            DisplayParticleMotionKind.STATIC -> emptySet()
            DisplayParticleMotionKind.INERTIAL -> setOf("initial-velocity", "retention")
            DisplayParticleMotionKind.BALLISTIC -> setOf("initial-velocity", "acceleration", "retention")
            DisplayParticleMotionKind.BUOYANT, DisplayParticleMotionKind.DRIFT ->
                setOf("initial-velocity", "acceleration", "retention", "turbulence", "frequency")
            DisplayParticleMotionKind.BURST -> setOf("initial-velocity", "acceleration", "retention", "radial-speed")
            DisplayParticleMotionKind.ORBIT -> setOf("initial-velocity", "spawn-radius", "orbit-speed", "radial-pull")
            DisplayParticleMotionKind.ATTRACT -> setOf("initial-velocity", "spawn-radius", "attraction", "retention", "max-speed")
        }
        require(specified.all(allowed::contains)) {
            "${kind.name.lowercase()}では使用できない動作プロパティです: ${(specified - allowed).sorted().joinToString()}"
        }
        require(collisionProperties.restitution == null || collisionMode == DisplayParticleCollisionMode.BOUNCE) {
            "restitutionはcollision=bounceの場合だけ指定できます"
        }
    }
}
