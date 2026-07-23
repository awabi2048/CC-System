package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.MenuRoute;
import com.awabi2048.ccsystem.core.gui.MenuSessionClosePolicy;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuSessionClosePolicyTest {
    @Test
    void closingPreviousRouteDoesNotRemoveReplacementSession() {
        var previous = route("list");
        var replacement = route("detail");

        assertFalse(MenuSessionClosePolicy.INSTANCE.shouldRemove(previous, replacement, replacement));
    }

    @Test
    void closingCurrentRouteRemovesItsSession() {
        var current = route("list");

        assertTrue(MenuSessionClosePolicy.INSTANCE.shouldRemove(current, null, current));
    }

    @Test
    void reopeningSameRouteKeepsNewSession() {
        var route = route("list");

        assertFalse(MenuSessionClosePolicy.INSTANCE.shouldRemove(route, route, route));
    }

    private static MenuRoute route(String id) {
        return new MenuRoute("test", id, Map.of());
    }
}
