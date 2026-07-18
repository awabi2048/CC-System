package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.resource.ResourceWorldState;
import com.awabi2048.ccsystem.core.resource.ResourceWorldGenerationStore;
import com.awabi2048.ccsystem.core.resource.ResourceWorldLifecycleServiceImpl;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceWorldLifecycleServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void publishesValidTransitionsAndPersistsGenerationId() {
        var file = temporaryDirectory.resolve("generations.properties");
        var failures = new ArrayList<String>();
        var states = new ArrayList<ResourceWorldState>();
        var service = new ResourceWorldLifecycleServiceImpl(
            new ResourceWorldGenerationStore(file),
            () -> Instant.parse("2026-07-19T00:00:00Z"),
            (owner, failure) -> {
                failures.add(owner);
                return kotlin.Unit.INSTANCE;
            }
        );
        var key = NamespacedKey.minecraft("resource.normal.20260719");
        service.subscribe("broken", change -> { throw new IllegalStateException("boom"); });
        service.subscribe("observer", change -> states.add(change.getCurrent().getState()));

        var created = service.beginGeneration(key, key.getKey(), "normal", "default");
        service.transition(key, ResourceWorldState.PREGENERATING);
        service.transition(key, ResourceWorldState.READY);

        assertEquals(java.util.List.of(ResourceWorldState.CREATING, ResourceWorldState.PREGENERATING, ResourceWorldState.READY), states);
        assertEquals(3, failures.size());
        assertTrue(service.isReady(key));

        var reloaded = new ResourceWorldLifecycleServiceImpl(new ResourceWorldGenerationStore(file));
        assertEquals(created.getGenerationId(), reloaded.getGeneration(key).getGenerationId());
        assertEquals(ResourceWorldState.READY, reloaded.getGeneration(key).getState());
    }

    @Test
    void rejectsInvalidTransitionAndCreatesNewIdForNewGeneration() {
        var service = new ResourceWorldLifecycleServiceImpl(
            new ResourceWorldGenerationStore(temporaryDirectory.resolve("generations.properties"))
        );
        var key = NamespacedKey.minecraft("resource.normal.20260719");
        var first = service.beginGeneration(key, key.getKey(), "normal", "default");

        assertThrows(IllegalArgumentException.class, () -> service.transition(key, ResourceWorldState.READY));

        var second = service.beginGeneration(key, key.getKey(), "normal", "default");
        assertNotEquals(first.getGenerationId(), second.getGenerationId());
    }
}
