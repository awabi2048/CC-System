package com.awabi2048.ccsystem.api.resource

import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import java.util.UUID

interface NaturalOriginRegistry {
    fun isNatural(block: Block): Boolean =
        isNatural(block.world.key, block.x, block.y, block.z)

    fun isNatural(worldKey: NamespacedKey, x: Int, y: Int, z: Int): Boolean

    fun markPlayerPlaced(block: Block) =
        markPlayerPlaced(block.world.key, block.x, block.y, block.z)

    fun markPlayerPlaced(worldKey: NamespacedKey, x: Int, y: Int, z: Int)

    fun markPlayerPlaced(blocks: Collection<Block>) {
        blocks.forEach(::markPlayerPlaced)
    }

    fun markGeneratedRegion(generationId: UUID, chunk: Chunk) =
        markGeneratedChunk(generationId, chunk.world.key, chunk.x, chunk.z)

    fun markGeneratedChunk(generationId: UUID, worldKey: NamespacedKey, chunkX: Int, chunkZ: Int)

    fun clearGeneration(generationId: UUID)
}
