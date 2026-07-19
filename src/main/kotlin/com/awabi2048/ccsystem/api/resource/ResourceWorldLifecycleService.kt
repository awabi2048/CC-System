package com.awabi2048.ccsystem.api.resource

import org.bukkit.NamespacedKey
import java.time.Instant
import java.util.UUID

enum class ResourceWorldState {
    CREATING,
    PREGENERATING,
    READY,
    RESETTING,
    UNLOADING,
    DELETED,
    FAILED
}

data class ResourceWorldGeneration(
    val worldKey: NamespacedKey,
    val runtimeWorldName: String,
    val resourceType: String,
    val variation: String,
    val generationId: UUID,
    val state: ResourceWorldState
)

data class ResourceWorldStateChange(
    val previous: ResourceWorldGeneration?,
    val current: ResourceWorldGeneration,
    val occurredAt: Instant
)

fun interface ResourceWorldStateListener {
    fun onStateChanged(change: ResourceWorldStateChange)
}

fun interface ResourceWorldStateSubscription : AutoCloseable {
    override fun close()
}

interface ResourceWorldLifecycleService {
    fun getGeneration(worldKey: NamespacedKey): ResourceWorldGeneration?

    fun getGenerations(): List<ResourceWorldGeneration>

    fun isResourceWorld(worldKey: NamespacedKey): Boolean = getGeneration(worldKey)?.state != ResourceWorldState.DELETED

    fun isReady(worldKey: NamespacedKey): Boolean = getGeneration(worldKey)?.state == ResourceWorldState.READY

    fun subscribe(owner: String, listener: ResourceWorldStateListener): ResourceWorldStateSubscription

    fun unsubscribeOwner(owner: String)
}
