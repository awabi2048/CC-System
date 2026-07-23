package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.ManagedMenuTransition;
import com.awabi2048.ccsystem.api.gui.MenuRoute;
import com.awabi2048.ccsystem.core.gui.ManagedPresentationClosePolicy;
import com.awabi2048.ccsystem.core.gui.ManagedTransitionResolver;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedTransitionResolverTest {
    @Test
    void automaticStartsAtRootWithoutCurrentRoute() {
        assertSame(
            ManagedMenuTransition.ROOT,
            ManagedTransitionResolver.INSTANCE.resolve(ManagedMenuTransition.AUTOMATIC, null, route("list"))
        );
    }

    @Test
    void automaticReplacesSameRoute() {
        var route = route("list");
        assertSame(
            ManagedMenuTransition.REPLACE,
            ManagedTransitionResolver.INSTANCE.resolve(ManagedMenuTransition.AUTOMATIC, route, route)
        );
    }

    @Test
    void automaticNavigatesToDifferentRoute() {
        assertSame(
            ManagedMenuTransition.NAVIGATE,
            ManagedTransitionResolver.INSTANCE.resolve(
                ManagedMenuTransition.AUTOMATIC,
                route("list"),
                route("detail")
            )
        );
    }

    @Test
    void explicitTransitionIsPreserved() {
        assertSame(
            ManagedMenuTransition.PRESERVE_HISTORY,
            ManagedTransitionResolver.INSTANCE.resolve(
                ManagedMenuTransition.PRESERVE_HISTORY,
                route("list"),
                route("detail")
            )
        );
    }

    @Test
    void closeClearsOnlyThePlayersCurrentUnreplacedRoute() {
        var playerId = UUID.randomUUID();
        var route = route("list");
        assertTrue(ManagedPresentationClosePolicy.INSTANCE.shouldClear(playerId, playerId, null, route, route));
        assertFalse(ManagedPresentationClosePolicy.INSTANCE.shouldClear(
            playerId,
            playerId,
            route("detail"),
            route("detail"),
            route
        ));
        assertFalse(ManagedPresentationClosePolicy.INSTANCE.shouldClear(
            UUID.randomUUID(),
            playerId,
            null,
            route,
            route
        ));
    }

    private static MenuRoute route(String id) {
        return new MenuRoute("test", id, Map.of());
    }
}
