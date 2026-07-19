package com.awabi2048.ccsystem.core.resource

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.StructureGrowEvent

class NaturalOriginListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChunkLoad(event: ChunkLoadEvent) {
        val generation = ResourceWorldLifecycleRuntime.service.getGeneration(event.world.key) ?: return
        NaturalOriginRuntime.registry.markGeneratedChunk(
            generation.generationId,
            event.world.key,
            event.chunk.x,
            event.chunk.z
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        NaturalOriginRuntime.registry.markPlayerPlaced(event.blockPlaced)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) {
        NaturalOriginRuntime.registry.markPlayerPlaced(event.blocks.map { it.block })
    }
}
