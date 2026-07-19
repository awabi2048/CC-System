package com.awabi2048.ccsystem.api.cosmetic

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import java.util.UUID

enum class CosmeticType {
    MEDAL,
    PARTICLE
}

enum class CosmeticRefreshPolicy {
    LOGIN,
    EQUIP,
    FACT_CHANGE,
    MANUAL
}

data class CosmeticId(val value: String) {
    init {
        require(ID_PATTERN.matches(value)) {
            "コスメティックIDは namespace:id 形式で指定してください: $value"
        }
    }

    val namespace: String get() = value.substringBefore(':')

    companion object {
        private val ID_PATTERN = Regex("[a-z0-9._-]+:[a-z0-9/._-]+")
    }
}

sealed interface CosmeticRegistrationResult {
    data object Registered : CosmeticRegistrationResult
    data object Replaced : CosmeticRegistrationResult
    data class Rejected(val reason: String) : CosmeticRegistrationResult
}

sealed interface CosmeticOwnershipResult {
    data object Changed : CosmeticOwnershipResult
    data object Unchanged : CosmeticOwnershipResult
    data class Rejected(val reason: String) : CosmeticOwnershipResult
}

sealed interface CosmeticEquipResult {
    data object Equipped : CosmeticEquipResult
    data object Unequipped : CosmeticEquipResult
    data object Unchanged : CosmeticEquipResult
    data class NotOwned(val id: CosmeticId) : CosmeticEquipResult
}

data class CosmeticFacts(
    private val values: Map<String, Any>
) {
    fun value(key: String): Any? = values[key]
    fun string(key: String): String? = values[key] as? String
    fun number(key: String): Number? = values[key] as? Number
    fun boolean(key: String): Boolean? = values[key] as? Boolean
    fun asMap(): Map<String, Any> = values.toMap()

    companion object {
        val EMPTY = CosmeticFacts(emptyMap())
        fun of(values: Map<String, Any>): CosmeticFacts = CosmeticFacts(values.toMap())
    }
}

data class MedalPresentation(
    val text: Component,
    val verticalOffset: Double = 0.35,
    val interactionWidth: Float? = null,
    val interactionHeight: Float? = null,
    val onClick: ((Player) -> Unit)? = null
)

data class MedalDefinition(
    val id: CosmeticId,
    val sourceId: String,
    val revision: Long = 0,
    val refreshPolicies: Set<CosmeticRefreshPolicy> = setOf(
        CosmeticRefreshPolicy.LOGIN,
        CosmeticRefreshPolicy.EQUIP,
        CosmeticRefreshPolicy.FACT_CHANGE
    ),
    val resolve: (UUID, CosmeticFacts) -> MedalPresentation?
)

data class ParticleEmission(
    val particle: Particle,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val offsetZ: Double = 0.0,
    val count: Int = 1,
    val spreadX: Double = 0.0,
    val spreadY: Double = 0.0,
    val spreadZ: Double = 0.0,
    val extra: Double = 0.0,
    val data: Any? = null
)

fun interface ParticleProgram {
    fun tick(context: ParticleProgramContext): List<ParticleEmission>
}

data class ParticleProgramContext(
    val player: Player,
    val origin: Location,
    val tick: Long,
    val facts: CosmeticFacts
)

data class ParticleDefinition(
    val id: CosmeticId,
    val sourceId: String,
    val revision: Long = 0,
    val refreshPolicies: Set<CosmeticRefreshPolicy> = setOf(
        CosmeticRefreshPolicy.LOGIN,
        CosmeticRefreshPolicy.EQUIP,
        CosmeticRefreshPolicy.FACT_CHANGE
    ),
    val resolve: (UUID, CosmeticFacts) -> ParticleProgram?
)

data class CosmeticProfileSnapshot(
    val playerId: UUID,
    val ownedMedals: Set<CosmeticId>,
    val ownedParticles: Set<CosmeticId>,
    val equippedMedal: CosmeticId?,
    val equippedParticle: CosmeticId?
)

data class CosmeticDiagnosticSnapshot(
    val registeredMedals: Int,
    val registeredParticles: Int,
    val loadedProfiles: Int,
    val activeMedalDisplays: Int,
    val activeParticlePrograms: Int,
    val unresolvedEquippedMedals: Set<CosmeticId>,
    val unresolvedEquippedParticles: Set<CosmeticId>,
    val lastSaveFailure: String?
)
