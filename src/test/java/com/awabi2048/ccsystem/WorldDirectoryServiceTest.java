package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.world.WorldDirectoryState;
import com.awabi2048.ccsystem.core.world.WorldDirectoryServiceImpl;
import java.nio.file.Files;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldDirectoryServiceTest {
    @TempDir
    java.nio.file.Path root;

    @Test
    void resolvesPaper26DimensionDirectory() {
        var service = new WorldDirectoryServiceImpl(root, "world");
        assertEquals(
            root.resolve("world/dimensions/minecraft/example").toAbsolutePath().normalize(),
            service.dimensionDirectory(NamespacedKey.minecraft("example"))
        );
    }

    @Test
    void distinguishesCurrentLegacyConflictAndMissing() throws Exception {
        var service = new WorldDirectoryServiceImpl(root, "world");
        var key = NamespacedKey.minecraft("my_world.1234");
        assertEquals(WorldDirectoryState.MISSING, service.inspect(key, "my_world.1234").getState());

        Files.createDirectories(root.resolve("my_world.1234"));
        assertEquals(WorldDirectoryState.LEGACY, service.inspect(key, "my_world.1234").getState());

        Files.createDirectories(service.dimensionDirectory(key));
        assertEquals(WorldDirectoryState.CONFLICT, service.inspect(key, "my_world.1234").getState());
    }

    @Test
    void rejectsUnsafeLegacyNames() {
        var service = new WorldDirectoryServiceImpl(root, "world");
        var resolution = service.inspect(NamespacedKey.minecraft("safe"), "../outside");
        assertEquals(WorldDirectoryState.UNSAFE, resolution.getState());
        assertThrows(
            IllegalArgumentException.class,
            () -> new WorldDirectoryServiceImpl(root, "../world")
        );
    }

    @Test
    void listsOnlyMatchingDimensionDirectories() throws Exception {
        var service = new WorldDirectoryServiceImpl(root, "world");
        Files.createDirectories(service.dimensionDirectory(NamespacedKey.minecraft("resource.a")));
        Files.createDirectories(service.dimensionDirectory(NamespacedKey.minecraft("resource.b")));
        Files.createDirectories(service.dimensionDirectory(NamespacedKey.minecraft("arena.pool.1")));
        var entries = service.listByKeyPrefix("minecraft", "resource.");
        assertEquals(2, entries.size());
        assertTrue(entries.stream().allMatch(entry -> entry.getKey().getKey().startsWith("resource.")));
    }

    @Test
    void listsLegacyDirectoriesWithinWorldContainer() throws Exception {
        var service = new WorldDirectoryServiceImpl(root, "world");
        Files.createDirectories(root.resolve("my_world.11111111-1111-1111-1111-111111111111"));
        Files.createDirectories(root.resolve("unrelated"));
        assertEquals(1, service.listLegacyByNamePrefix("my_world.").size());
    }

    @Test
    void doesNotFollowSymbolicLinks() throws Exception {
        var service = new WorldDirectoryServiceImpl(root, "world");
        var target = Files.createDirectories(root.resolve("target"));
        var link = root.resolve("my_world.link");
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | java.io.IOException ignored) {
            return;
        }
        assertEquals(0, service.listLegacyByNamePrefix("my_world.").size());
    }
}
