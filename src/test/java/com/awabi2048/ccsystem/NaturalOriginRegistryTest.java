package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.resource.ResourceWorldGeneration;
import com.awabi2048.ccsystem.api.resource.ResourceWorldLifecycleService;
import com.awabi2048.ccsystem.api.resource.ResourceWorldState;
import com.awabi2048.ccsystem.api.resource.ResourceWorldStateListener;
import com.awabi2048.ccsystem.api.resource.ResourceWorldStateSubscription;
import com.awabi2048.ccsystem.core.resource.NaturalOriginRegistryImpl;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalOriginRegistryTest {
    @TempDir
    Path tempDirectory;

    @Test
    void generatedChunkAndPlayerPlacedExclusionSurviveReload() {
        NamespacedKey worldKey = NamespacedKey.minecraft("resource");
        UUID generationId = UUID.randomUUID();
        FakeLifecycle lifecycle = new FakeLifecycle(new ResourceWorldGeneration(
            worldKey,
            "resource",
            "overworld",
            "default",
            generationId,
            ResourceWorldState.READY
        ));
        Path store = tempDirectory.resolve("natural.properties");
        NaturalOriginRegistryImpl registry = new NaturalOriginRegistryImpl(store, lifecycle);

        assertFalse(registry.isNatural(worldKey, 20, 64, -2));
        registry.markGeneratedChunk(generationId, worldKey, 1, -1);
        assertTrue(registry.isNatural(worldKey, 20, 64, -2));
        registry.markPlayerPlaced(worldKey, 20, 64, -2);
        assertFalse(registry.isNatural(worldKey, 20, 64, -2));
        assertTrue(registry.isNatural(worldKey, 21, 64, -2));

        NaturalOriginRegistryImpl reloaded = new NaturalOriginRegistryImpl(store, lifecycle);
        assertFalse(reloaded.isNatural(worldKey, 20, 64, -2));
        assertTrue(reloaded.isNatural(worldKey, 21, 64, -2));
    }

    @Test
    void generationBoundaryPreventsStaleChunkReuse() {
        NamespacedKey worldKey = NamespacedKey.minecraft("resource");
        UUID first = UUID.randomUUID();
        FakeLifecycle lifecycle = new FakeLifecycle(new ResourceWorldGeneration(
            worldKey, "resource", "overworld", "default", first, ResourceWorldState.READY
        ));
        NaturalOriginRegistryImpl registry = new NaturalOriginRegistryImpl(
            tempDirectory.resolve("natural.properties"), lifecycle
        );
        registry.markGeneratedChunk(first, worldKey, 0, 0);
        assertTrue(registry.isNatural(worldKey, 1, 64, 1));

        UUID second = UUID.randomUUID();
        lifecycle.generation = new ResourceWorldGeneration(
            worldKey, "resource", "overworld", "default", second, ResourceWorldState.READY
        );
        assertFalse(registry.isNatural(worldKey, 1, 64, 1));
        assertThrows(IllegalArgumentException.class,
            () -> registry.markGeneratedChunk(first, worldKey, 0, 0));
        registry.markGeneratedChunk(second, worldKey, 0, 0);
        assertTrue(registry.isNatural(worldKey, 1, 64, 1));
        registry.clearGeneration(second);
        assertFalse(registry.isNatural(worldKey, 1, 64, 1));
    }

    @Test
    void placementRecordingCanBeTemporarilyDisabledWithoutDiscardingExistingRecords() {
        NamespacedKey worldKey = NamespacedKey.minecraft("resource");
        UUID generationId = UUID.randomUUID();
        FakeLifecycle lifecycle = new FakeLifecycle(new ResourceWorldGeneration(
            worldKey, "resource", "overworld", "default", generationId, ResourceWorldState.READY
        ));
        Path store = tempDirectory.resolve("natural.properties");
        NaturalOriginRegistryImpl registry = new NaturalOriginRegistryImpl(store, lifecycle);
        registry.markGeneratedChunk(generationId, worldKey, 0, 0);
        registry.markPlayerPlaced(worldKey, 1, 64, 1);

        assertTrue(registry.isPlacementRecordingEnabled());
        assertFalse(registry.isNatural(worldKey, 1, 64, 1));

        registry.setPlacementRecordingEnabled(false);
        registry.markPlayerPlaced(worldKey, 2, 64, 2);
        assertFalse(registry.isPlacementRecordingEnabled());
        assertTrue(registry.isNatural(worldKey, 2, 64, 2));
        assertFalse(registry.isNatural(worldKey, 1, 64, 1));

        registry.setPlacementRecordingEnabled(true);
        registry.markPlayerPlaced(worldKey, 3, 64, 3);
        assertFalse(registry.isNatural(worldKey, 3, 64, 3));

        NaturalOriginRegistryImpl reloaded = new NaturalOriginRegistryImpl(store, lifecycle);
        assertTrue(reloaded.isPlacementRecordingEnabled());
        assertTrue(reloaded.isNatural(worldKey, 2, 64, 2));
        assertFalse(reloaded.isNatural(worldKey, 3, 64, 3));
    }

    private static final class FakeLifecycle implements ResourceWorldLifecycleService {
        private ResourceWorldGeneration generation;

        private FakeLifecycle(ResourceWorldGeneration generation) {
            this.generation = generation;
        }

        @Override
        public ResourceWorldGeneration getGeneration(NamespacedKey worldKey) {
            return generation.getWorldKey().equals(worldKey) ? generation : null;
        }

        @Override
        public List<ResourceWorldGeneration> getGenerations() {
            return List.of(generation);
        }

        @Override
        public ResourceWorldStateSubscription subscribe(String owner, ResourceWorldStateListener listener) {
            return () -> { };
        }

        @Override
        public void unsubscribeOwner(String owner) {
        }
    }
}
