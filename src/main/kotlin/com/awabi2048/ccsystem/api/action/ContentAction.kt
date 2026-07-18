package com.awabi2048.ccsystem.api.action

import org.bukkit.NamespacedKey
import java.time.Instant
import java.util.UUID

enum class ContentActionType {
    FISH_CAUGHT,
    VANILLA_FISH_CAUGHT,
    MINERAL_EXTRACTED,
    TREE_PROCESSED,
    PLANT_GATHERED,
    CROP_HARVESTED,
    COOKING_COMPLETED,
    BREWING_STAGE_COMPLETED,
    BREWING_COMPLETED,
    ARENA_CLEARED,
    MYWORLD_ACTION_COMPLETED,
    SEASONAL_OBJECTIVE_COMPLETED
}

data class ContentAction(
    val actionId: UUID,
    val schemaVersion: Int,
    val occurredAt: Instant,
    val playerId: UUID?,
    val actionType: ContentActionType,
    val amount: Long,
    val worldKey: NamespacedKey?,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(schemaVersion > 0) { "schemaVersion must be positive" }
    }
}
