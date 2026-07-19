package com.awabi2048.ccsystem.api.cosmetic

import java.util.UUID

interface CosmeticFactRegistry {
    fun register(providerId: String, provider: (UUID) -> Map<String, Any>): Boolean
    fun unregister(providerId: String): Boolean
    fun snapshot(playerId: UUID): CosmeticFacts
    fun notifyChanged(playerId: UUID)
}

interface CosmeticPlatform {
    fun registerMedal(definition: MedalDefinition): CosmeticRegistrationResult
    fun registerParticle(definition: ParticleDefinition): CosmeticRegistrationResult
    fun unregisterSource(sourceId: String)

    fun setOwned(playerId: UUID, type: CosmeticType, id: CosmeticId, owned: Boolean): CosmeticOwnershipResult
    fun equip(playerId: UUID, type: CosmeticType, id: CosmeticId?): CosmeticEquipResult
    fun profile(playerId: UUID): CosmeticProfileSnapshot

    fun facts(): CosmeticFactRegistry
    fun refresh(playerId: UUID)
    fun diagnostics(): CosmeticDiagnosticSnapshot
}
