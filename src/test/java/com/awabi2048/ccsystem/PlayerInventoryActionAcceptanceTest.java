package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.core.gui.PlayerInventoryActionAcceptance;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerInventoryActionAcceptanceTest {
    @Test
    void 許可済みプレイヤーインベントリの通常クリックだけをActionへ渡す() {
        assertTrue(PlayerInventoryActionAcceptance.INSTANCE.accepts(true, true, true, ClickType.LEFT));
        assertTrue(PlayerInventoryActionAcceptance.INSTANCE.accepts(true, true, true, ClickType.SHIFT_RIGHT));
        assertFalse(PlayerInventoryActionAcceptance.INSTANCE.accepts(false, true, true, ClickType.LEFT));
        assertFalse(PlayerInventoryActionAcceptance.INSTANCE.accepts(true, false, true, ClickType.LEFT));
        assertFalse(PlayerInventoryActionAcceptance.INSTANCE.accepts(true, true, false, ClickType.LEFT));
        assertFalse(PlayerInventoryActionAcceptance.INSTANCE.accepts(true, true, true, ClickType.DROP));
    }
}
