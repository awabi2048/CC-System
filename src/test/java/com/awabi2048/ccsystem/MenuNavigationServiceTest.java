package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.MenuRoute;
import com.awabi2048.ccsystem.core.gui.MenuNavigationServiceImpl;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuNavigationServiceTest {
    @Test
    void openingRootClearsPreviousBreadcrumbs() {
        var service = new MenuNavigationServiceImpl();
        var player = player(UUID.randomUUID());
        var root = route("root");
        var child = route("child");
        var otherRoot = route("other-root");

        for (var route : new MenuRoute[]{root, child, otherRoot}) {
            service.registerOpener(route.getOwner(), route.getId(), (target, openedRoute) -> true);
        }

        assertTrue(service.openRoot(player, root));
        assertTrue(service.pushAndOpen(player, root, child));
        assertEquals(java.util.List.of(root), service.breadcrumbs(player));

        assertTrue(service.openRoot(player, otherRoot));
        assertTrue(service.breadcrumbs(player).isEmpty());
        assertEquals(otherRoot, service.currentRoute(player));
    }

    private static Player player(UUID playerId) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> playerId;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "TestPlayer[" + playerId + "]";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static MenuRoute route(String id) {
        return new MenuRoute("test", id, Map.of());
    }
}
