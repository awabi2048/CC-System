package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.core.gui.PlayerInventoryTransferGuard;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerInventoryTransferGuardTest {
    @Test
    void 通常操作を許可しつつGUIを横断する操作を拒否する() {
        assertFalse(PlayerInventoryTransferGuard.INSTANCE.blocks(InventoryAction.PICKUP_ALL));
        assertFalse(PlayerInventoryTransferGuard.INSTANCE.blocks(InventoryAction.HOTBAR_SWAP));
        assertTrue(PlayerInventoryTransferGuard.INSTANCE.blocks(InventoryAction.MOVE_TO_OTHER_INVENTORY));
        assertTrue(PlayerInventoryTransferGuard.INSTANCE.blocks(InventoryAction.COLLECT_TO_CURSOR));
        assertTrue(PlayerInventoryTransferGuard.INSTANCE.blocks(InventoryAction.UNKNOWN));
    }
}
