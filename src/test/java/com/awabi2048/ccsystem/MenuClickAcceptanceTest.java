package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.core.gui.MenuClickAcceptance;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuClickAcceptanceTest {
    @Test
    void acceptsPrimaryAndSecondaryClicks() {
        assertTrue(MenuClickAcceptance.INSTANCE.accepts(ClickType.LEFT));
        assertTrue(MenuClickAcceptance.INSTANCE.accepts(ClickType.RIGHT));
        assertTrue(MenuClickAcceptance.INSTANCE.accepts(ClickType.SHIFT_LEFT));
        assertTrue(MenuClickAcceptance.INSTANCE.accepts(ClickType.SHIFT_RIGHT));
    }

    @Test
    void rejectsInventoryTransferAndCreativeClicks() {
        assertFalse(MenuClickAcceptance.INSTANCE.accepts(ClickType.NUMBER_KEY));
        assertFalse(MenuClickAcceptance.INSTANCE.accepts(ClickType.SWAP_OFFHAND));
        assertFalse(MenuClickAcceptance.INSTANCE.accepts(ClickType.DOUBLE_CLICK));
        assertFalse(MenuClickAcceptance.INSTANCE.accepts(ClickType.DROP));
        assertFalse(MenuClickAcceptance.INSTANCE.accepts(ClickType.CONTROL_DROP));
        assertFalse(MenuClickAcceptance.INSTANCE.accepts(ClickType.CREATIVE));
        assertFalse(MenuClickAcceptance.INSTANCE.accepts(ClickType.MIDDLE));
    }
}
