package com.awabi2048.ccsystem.core.resource

import com.awabi2048.ccsystem.api.resource.ResourceWorldGeneration
import com.awabi2048.ccsystem.api.resource.ResourceWorldLifecycleService
import com.awabi2048.ccsystem.api.resource.ResourceWorldState
import com.awabi2048.ccsystem.api.resource.ResourceWorldStateChange
import com.awabi2048.ccsystem.api.resource.ResourceWorldStateListener
import com.awabi2048.ccsystem.api.resource.ResourceWorldStateSubscription
import org.bukkit.NamespacedKey
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class ResourceWorldLifecycleServiceImpl @JvmOverloads constructor(
    private val store: ResourceWorldGenerationStore,
    private val now: () -> Instant = Instant::now,
    private val listenerFailureHandler: (String, Throwable) -> Unit = { _, _ -> }
) : ResourceWorldLifecycleService {
    private data class Registration(val id: Long, val owner: String, val listener: ResourceWorldStateListener)

    private val lock = Any()
    private val registrations = mutableListOf<Registration>()
    private val registrationSequence = AtomicLong()
    private val generations = linkedMapOf<NamespacedKey, ResourceWorldGeneration>()

    init {
        store.load().forEach { generations[it.worldKey] = it }
    }

    override fun getGeneration(worldKey: NamespacedKey): ResourceWorldGeneration? = synchronized(lock) {
        generations[worldKey]
    }

    override fun getGenerations(): List<ResourceWorldGeneration> = synchronized(lock) {
        generations.values.filter { it.state != ResourceWorldState.DELETED }.toList()
    }

    fun beginGeneration(
        worldKey: NamespacedKey,
        runtimeWorldName: String,
        resourceType: String,
        variation: String
    ): ResourceWorldGeneration = replaceGeneration(
        ResourceWorldGeneration(worldKey, runtimeWorldName, resourceType, variation, UUID.randomUUID(), ResourceWorldState.CREATING)
    )

    fun registerExisting(
        worldKey: NamespacedKey,
        runtimeWorldName: String,
        resourceType: String,
        variation: String
    ): ResourceWorldGeneration {
        val existing = getGeneration(worldKey)
        return replaceGeneration(
            ResourceWorldGeneration(
                worldKey,
                runtimeWorldName,
                resourceType,
                variation,
                existing?.generationId ?: UUID.randomUUID(),
                ResourceWorldState.READY
            )
        )
    }

    fun transition(worldKey: NamespacedKey, state: ResourceWorldState): ResourceWorldGeneration {
        val previous = synchronized(lock) { generations[worldKey] }
            ?: throw IllegalStateException("Unknown resource world generation: $worldKey")
        require(state == previous.state || state in allowedTransitions.getValue(previous.state)) {
            "Invalid resource world transition: ${previous.state} -> $state ($worldKey)"
        }
        if (state == previous.state) return previous
        return replaceGeneration(previous.copy(state = state))
    }

    override fun subscribe(owner: String, listener: ResourceWorldStateListener): ResourceWorldStateSubscription {
        require(owner.isNotBlank()) { "owner must not be blank" }
        val id = registrationSequence.incrementAndGet()
        synchronized(lock) { registrations += Registration(id, owner, listener) }
        return ResourceWorldStateSubscription {
            synchronized(lock) { registrations.removeIf { it.id == id } }
        }
    }

    override fun unsubscribeOwner(owner: String) {
        synchronized(lock) { registrations.removeIf { it.owner == owner } }
    }

    private fun replaceGeneration(current: ResourceWorldGeneration): ResourceWorldGeneration {
        val previous: ResourceWorldGeneration?
        val listeners: List<Registration>
        synchronized(lock) {
            previous = generations.put(current.worldKey, current)
            store.save(generations.values)
            listeners = registrations.toList()
        }
        val change = ResourceWorldStateChange(previous, current, now())
        listeners.forEach { registration ->
            runCatching { registration.listener.onStateChanged(change) }
                .onFailure { listenerFailureHandler(registration.owner, it) }
        }
        return current
    }

    companion object {
        private val allowedTransitions = mapOf(
            ResourceWorldState.CREATING to setOf(ResourceWorldState.PREGENERATING, ResourceWorldState.FAILED),
            ResourceWorldState.PREGENERATING to setOf(ResourceWorldState.READY, ResourceWorldState.RESETTING, ResourceWorldState.FAILED),
            ResourceWorldState.READY to setOf(ResourceWorldState.RESETTING, ResourceWorldState.FAILED),
            ResourceWorldState.RESETTING to setOf(ResourceWorldState.UNLOADING, ResourceWorldState.FAILED),
            ResourceWorldState.UNLOADING to setOf(ResourceWorldState.DELETED, ResourceWorldState.FAILED),
            ResourceWorldState.DELETED to emptySet(),
            ResourceWorldState.FAILED to setOf(ResourceWorldState.RESETTING)
        )
    }
}
