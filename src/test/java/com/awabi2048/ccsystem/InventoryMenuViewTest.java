package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.InventoryMenuView;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryMenuViewTest {
    @Test
    void 入力アイテムは宣言済み入力スロットだけに配置できる() {
        Map<Integer, ItemStack> validItems = new HashMap<>();
        validItems.put(0, null);
        new InventoryMenuView(
            9,
            Component.text("input"),
            List.of(),
            true,
            Set.of(0),
            validItems,
            false
        );

        Map<Integer, ItemStack> invalidItems = new HashMap<>();
        invalidItems.put(1, null);
        assertThrows(IllegalArgumentException.class, () -> new InventoryMenuView(
            9,
            Component.text("invalid"),
            List.of(),
            true,
            Set.of(0),
            invalidItems,
            false
        ));
    }
}
