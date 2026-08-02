package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.MenuRoute;
import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicy;
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationFailureReason;
import com.awabi2048.ccsystem.core.gui.MenuNavigationServiceImpl;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void navigatingToTheCurrentRouteDoesNotAddItToBreadcrumbs() {
        var service = new MenuNavigationServiceImpl();
        var player = player(UUID.randomUUID());
        var route = route("same-route");
        service.registerOpener(route.getOwner(), route.getId(), (target, openedRoute) -> true);

        assertTrue(service.openRoot(player, route));
        assertTrue(service.pushAndOpen(player, route, route));

        assertTrue(service.breadcrumbs(player).isEmpty());
        assertEquals(route, service.currentRoute(player));
    }

    @Test
    void inventoryInstancePolicyIsRegisteredAndRemovedByIdentity() {
        var service = new MenuNavigationServiceImpl();
        var inventory = inventory();
        var policy = new GuiInventoryPolicy(java.util.Set.of(4), true);

        service.registerInventory("test", inventory, policy);
        assertEquals(policy, service.inventoryPolicy(inventory));

        service.unregisterInventory(inventory);
        assertNull(service.inventoryPolicy(inventory));
    }

    @Test
    void resultOpenerPreservesThrownExceptionWithoutChangingLegacyBooleanSam() {
        var service = new MenuNavigationServiceImpl();
        var player = player(UUID.randomUUID());
        var route = route("throws");
        service.registerResultOpener(route.getOwner(), route.getId(), (target, openedRoute) -> {
            throw new IllegalStateException("intentional test failure");
        });

        var result = service.openResult(player, route);

        assertTrue(!result.getSuccessful());
        assertEquals(MenuRuntimeOperationFailureReason.OPENER_EXCEPTION, result.getFailure().getReason());
        assertEquals(IllegalStateException.class.getName(), result.getFailure().getExceptionType());
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

    private static Inventory inventory() {
        return (Inventory) Proxy.newProxyInstance(
                Inventory.class.getClassLoader(),
                new Class<?>[]{Inventory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getHolder" -> null;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "TestInventory";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
