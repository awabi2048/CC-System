package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.MenuRoute;
import com.awabi2048.ccsystem.core.gui.MenuRouteStateStore;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuRouteStateStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void storesRoutesByUuidAndRoundTripsPayload() {
        var file = tempDir.resolve("menu_routes.yml").toFile();
        var store = new MenuRouteStateStore(file);
        var playerId = UUID.randomUUID();
        var route = new MenuRoute("mwm", "world_settings", Map.of(
            "worldUuid", UUID.randomUUID().toString(),
            "page", "2"
        ));

        store.save(Map.of(playerId, route));

        assertEquals(Map.of(playerId, route), store.load());
        assertTrue(file.isFile());
    }

    @Test
    void rejectsUnsafePayloadKeysAndValues() {
        var file = tempDir.resolve("menu_routes.yml").toFile();
        var store = new MenuRouteStateStore(file);
        var playerId = UUID.randomUUID();
        var unsafe = new MenuRoute("mwm", "route", Map.of("payload.injected", "value"));

        store.save(Map.of(playerId, unsafe));

        assertTrue(store.load().isEmpty());
    }
}
