package com.awabi2048.ccsystem.core.resource

import com.awabi2048.ccsystem.api.resource.NaturalOriginRegistry
import com.awabi2048.ccsystem.api.resource.ResourceWorldLifecycleService
import com.awabi2048.ccsystem.api.resource.ResourceWorldState
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.util.UUID

class NaturalOriginRegistryImpl(
    private val storagePath: Path,
    private val lifecycleService: ResourceWorldLifecycleService
) : NaturalOriginRegistry {
    private data class BlockPosition(val x: Int, val y: Int, val z: Int)
    private data class ChunkPosition(val x: Int, val z: Int)
    private data class GenerationState(
        val worldKey: NamespacedKey,
        val generatedChunks: MutableSet<ChunkPosition> = mutableSetOf(),
        val nonNaturalBlocks: MutableSet<BlockPosition> = mutableSetOf()
    )

    private val states = load()
    private var placementRecordingEnabled = true

    init {
        lifecycleService.subscribe("cc-system:natural-origin") { change ->
            if (change.current.state == ResourceWorldState.DELETED ||
                change.current.state == ResourceWorldState.FAILED
            ) {
                clearGeneration(change.current.generationId)
            }
        }
    }

    @Synchronized
    override fun isPlacementRecordingEnabled(): Boolean = placementRecordingEnabled

    @Synchronized
    override fun setPlacementRecordingEnabled(enabled: Boolean) {
        placementRecordingEnabled = enabled
    }

    @Synchronized
    override fun isNatural(worldKey: NamespacedKey, x: Int, y: Int, z: Int): Boolean {
        val generation = lifecycleService.getGeneration(worldKey)
            ?.takeIf { it.state == ResourceWorldState.READY }
            ?: return false
        val state = states[generation.generationId] ?: return false
        if (state.worldKey != worldKey) return false
        val chunk = ChunkPosition(Math.floorDiv(x, 16), Math.floorDiv(z, 16))
        return chunk in state.generatedChunks && BlockPosition(x, y, z) !in state.nonNaturalBlocks
    }

    @Synchronized
    override fun markPlayerPlaced(worldKey: NamespacedKey, x: Int, y: Int, z: Int) {
        if (!placementRecordingEnabled) return
        val generation = lifecycleService.getGeneration(worldKey) ?: return
        val state = states.getOrPut(generation.generationId) { GenerationState(worldKey) }
        state.nonNaturalBlocks += BlockPosition(x, y, z)
        save()
    }

    @Synchronized
    override fun markPlayerPlaced(blocks: Collection<Block>) {
        if (!placementRecordingEnabled) return
        var changed = false
        blocks.forEach { block ->
            val generation = lifecycleService.getGeneration(block.world.key) ?: return@forEach
            val state = states.getOrPut(generation.generationId) { GenerationState(block.world.key) }
            changed = state.nonNaturalBlocks.add(BlockPosition(block.x, block.y, block.z)) || changed
        }
        if (changed) save()
    }

    @Synchronized
    override fun markGeneratedChunk(
        generationId: UUID,
        worldKey: NamespacedKey,
        chunkX: Int,
        chunkZ: Int
    ) {
        val current = lifecycleService.getGeneration(worldKey) ?: return
        require(current.generationId == generationId) {
            "generation $generationId is not current for $worldKey"
        }
        val state = states.getOrPut(generationId) { GenerationState(worldKey) }
        require(state.worldKey == worldKey) { "generation $generationId belongs to ${state.worldKey}" }
        if (state.generatedChunks.add(ChunkPosition(chunkX, chunkZ))) save()
    }

    @Synchronized
    override fun clearGeneration(generationId: UUID) {
        if (states.remove(generationId) != null) save()
    }

    private fun load(): MutableMap<UUID, GenerationState> {
        if (!Files.exists(storagePath)) return mutableMapOf()
        val properties = Properties().also { values ->
            Files.newInputStream(storagePath).use(values::load)
        }
        val result = mutableMapOf<UUID, GenerationState>()
        properties.stringPropertyNames()
            .filter { it.endsWith(".world") }
            .forEach { key ->
                val generationId = UUID.fromString(key.removePrefix("generation.").removeSuffix(".world"))
                val worldKey = requireNotNull(NamespacedKey.fromString(properties.getProperty(key)))
                result[generationId] = GenerationState(worldKey)
            }
        result.forEach { (generationId, state) ->
            properties.getProperty("generation.$generationId.chunks", "")
                .split(';')
                .filter(String::isNotBlank)
                .mapTo(state.generatedChunks) { encoded ->
                    val parts = encoded.split(',')
                    ChunkPosition(parts[0].toInt(), parts[1].toInt())
                }
            properties.getProperty("generation.$generationId.excluded", "")
                .split(';')
                .filter(String::isNotBlank)
                .mapTo(state.nonNaturalBlocks) { encoded ->
                    val parts = encoded.split(',')
                    BlockPosition(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                }
        }
        return result
    }

    private fun save() {
        storagePath.parent?.let(Files::createDirectories)
        val properties = Properties()
        states.toSortedMap(compareBy(UUID::toString)).forEach { (generationId, state) ->
            val prefix = "generation.$generationId"
            properties.setProperty("$prefix.world", state.worldKey.toString())
            properties.setProperty(
                "$prefix.chunks",
                state.generatedChunks.sortedWith(compareBy(ChunkPosition::x, ChunkPosition::z))
                    .joinToString(";") { "${it.x},${it.z}" }
            )
            properties.setProperty(
                "$prefix.excluded",
                state.nonNaturalBlocks.sortedWith(compareBy(BlockPosition::x, BlockPosition::y, BlockPosition::z))
                    .joinToString(";") { "${it.x},${it.y},${it.z}" }
            )
        }
        val temporary = storagePath.resolveSibling("${storagePath.fileName}.tmp")
        Files.newOutputStream(temporary).use { properties.store(it, "CC-System natural origin registry") }
        runCatching {
            Files.move(
                temporary,
                storagePath,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        }.getOrElse {
            Files.move(temporary, storagePath, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
