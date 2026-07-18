package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.MenuTargetPolicy;
import com.awabi2048.ccsystem.api.gui.PublicMenuDefinition;
import com.awabi2048.ccsystem.api.item.ItemGrantDefinition;
import com.awabi2048.ccsystem.api.item.ItemGrantProvider;
import com.awabi2048.ccsystem.api.item.ItemGrantRequest;
import com.awabi2048.ccsystem.api.item.ItemGrantResult;
import com.awabi2048.ccsystem.core.gui.MenuCommandServiceImpl;
import com.awabi2048.ccsystem.core.item.ItemGrantServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagementRegistryTest {
    @Test
    void itemProvidersRejectDuplicateIdsAndCanBeUnregisteredByOwner() {
        var service = new ItemGrantServiceImpl();
        service.register(provider("first", "sample.item"));
        assertThrows(IllegalArgumentException.class, () -> service.register(provider("second", "sample.item")));
        service.unregister("first");
        assertNull(service.definition("sample.item"));
    }

    @Test
    void publicMenusRejectDuplicateRoutesAndCanBeUnregisteredByOwner() {
        var service = new MenuCommandServiceImpl();
        var definition = new PublicMenuDefinition(
            "sample",
            "menu",
            null,
            MenuTargetPolicy.SELF_ONLY,
            Set.of(),
            (player, arguments) -> true
        );
        service.register(definition);
        assertThrows(IllegalArgumentException.class, () -> service.register(definition));
        service.unregisterOwner("sample");
        assertEquals(0, service.definitions().size());
    }

    private ItemGrantProvider provider(String owner, String id) {
        return new ItemGrantProvider() {
            @Override
            public String getOwner() {
                return owner;
            }

            @Override
            public List<ItemGrantDefinition> definitions() {
                return List.of(new ItemGrantDefinition(id, null, 64, ignored -> List.of()));
            }

            @Override
            public ItemGrantResult grant(ItemGrantRequest request) {
                return new ItemGrantResult(true, request.getAmount(), 0, null);
            }
        };
    }
}
