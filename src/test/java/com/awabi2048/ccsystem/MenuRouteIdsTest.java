package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.MenuRouteIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuRouteIdsTest {
    @Test
    void derivesStableSnakeCaseIdsFromHolderNames() {
        assertEquals("rank_menu", MenuRouteIds.INSTANCE.fromHolderName("RankMenuHolder"));
        assertEquals("npc_menu", MenuRouteIds.INSTANCE.fromHolderName("NPCMenuHolder"));
        assertEquals("storage_box_menu", MenuRouteIds.INSTANCE.fromHolderName("StorageBoxMenuInventoryHolder"));
    }
}
