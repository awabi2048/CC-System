package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.MenuRoute;
import com.awabi2048.ccsystem.core.gui.MenuNavigationHistory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuNavigationHistoryTest {
    @Test
    void duplicateCurrentRouteIsNotStacked() {
        var playerId = UUID.randomUUID();
        var history = new MenuNavigationHistory();
        var route = route("mwm", "player_world", Map.of("page", "0"));

        history.push(playerId, route);
        history.push(playerId, route);

        assertEquals(List.of(route), history.snapshot(playerId));
    }

    @Test
    void staleRoutesAreSkippedUntilOpenableRouteIsFound() {
        var playerId = UUID.randomUUID();
        var history = new MenuNavigationHistory();
        var playerWorld = route("mwm", "player_world", Map.of("page", "0"));
        var removedWorldSettings = route("mwm", "world_settings", Map.of("world", "missing"));
        var attempted = new ArrayList<String>();

        history.push(playerId, playerWorld);
        history.push(playerId, removedWorldSettings);

        var opened = history.popPrevious(playerId, route -> {
            attempted.add(route.getId());
            return route.getId().equals("player_world");
        });

        assertEquals(playerWorld, opened);
        assertEquals(List.of("world_settings", "player_world"), attempted);
        assertTrue(history.snapshot(playerId).isEmpty());
    }

    @Test
    void oldestRoutesAreDiscardedWhenLimitIsExceeded() {
        var playerId = UUID.randomUUID();
        var history = new MenuNavigationHistory(2);
        var first = route("mwm", "first", Map.of());
        var second = route("mwm", "second", Map.of());
        var third = route("mwm", "third", Map.of());

        history.push(playerId, first);
        history.push(playerId, second);
        history.push(playerId, third);

        assertEquals(List.of(second, third), history.snapshot(playerId));
    }

    @Test
    void unregisteringOwnerRemovesItsStackedRoutes() {
        var playerId = UUID.randomUUID();
        var history = new MenuNavigationHistory();
        var mwmRoute = route("mwm", "player_world", Map.of());
        var chanponRoute = route("mwm-chanpon", "admin", Map.of());

        history.push(playerId, mwmRoute);
        history.push(playerId, chanponRoute);
        history.removeOwner("mwm-chanpon");

        assertEquals(List.of(mwmRoute), history.snapshot(playerId));
    }

    @Test
    void snapshotKeepsBreadcrumbOrderFromRootToCurrent() {
        var playerId = UUID.randomUUID();
        var history = new MenuNavigationHistory();
        var root = route("mwm", "player_world", Map.of("page", "0"));
        var dialogSource = route("mwm", "creation_dialog_source", Map.of());

        history.push(playerId, root);
        history.push(playerId, dialogSource);

        assertEquals(List.of(root, dialogSource), history.snapshot(playerId));
    }

    private static MenuRoute route(String owner, String id, Map<String, String> payload) {
        return new MenuRoute(owner, id, payload);
    }
}
