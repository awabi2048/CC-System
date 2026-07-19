package com.awabi2048.ccsystem.api.resource

import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import java.util.UUID

interface NaturalOriginRegistry {
    /**
     * プレイヤー操作による配置を自然由来判定から除外する記録が有効か返す。
     *
     * この状態はデバッグ用途の一時設定であり、再起動後は有効へ戻る。
     */
    fun isPlacementRecordingEnabled(): Boolean

    /**
     * プレイヤー操作による配置記録を一時的に有効化または無効化する。
     *
     * 既に保存されている配置記録は変更しない。
     */
    fun setPlacementRecordingEnabled(enabled: Boolean)

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
