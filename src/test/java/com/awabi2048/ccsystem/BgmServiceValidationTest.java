package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.bgm.BgmRequest;
import com.awabi2048.ccsystem.api.bgm.BgmSource;
import com.awabi2048.ccsystem.core.bgm.BgmServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BgmServiceValidationTest {
    @Test
    void acceptsValidRequest() {
        BgmRequest request = new BgmRequest("kota_server:ost_3.sukima_dungeon", 3840L, 0.8409F, 1.0F, null, false);
        assertEquals("kota_server:ost_3.sukima_dungeon", request.getSoundKey());
        assertEquals(3840L, request.getLoopTicks());
    }

    @Test
    void rejectsBlankOrMalformedSoundKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new BgmRequest("   ", 100L, 1.0F, 1.0F, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new BgmRequest("invalid sound id", 100L, 1.0F, 1.0F, null, false));
    }

    @Test
    void rejectsNonPositiveLoopTicks() {
        assertThrows(IllegalArgumentException.class,
                () -> new BgmRequest("minecraft:music.game", 0L, 1.0F, 1.0F, null, false));
    }

    @Test
    void rejectsOutOfRangePitchAndVolume() {
        assertThrows(IllegalArgumentException.class,
                () -> new BgmRequest("minecraft:music.game", 100L, 2.1F, 1.0F, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new BgmRequest("minecraft:music.game", 100L, 0.05F, 1.0F, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new BgmRequest("minecraft:music.game", 100L, 1.0F, 0.0F, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new BgmRequest("minecraft:music.game", 100L, 1.0F, Float.POSITIVE_INFINITY, null, false));
    }

    @Test
    void resolvesHighestPrioritySource() {
        assertNull(BgmServiceImpl.resolveActiveSource(List.of()));
        assertEquals(
            BgmSource.WORLD,
            BgmServiceImpl.resolveActiveSource(EnumSet.of(BgmSource.WORLD))
        );
        assertEquals(
            BgmSource.ARENA_LOBBY,
            BgmServiceImpl.resolveActiveSource(EnumSet.of(BgmSource.WORLD, BgmSource.SUKIMA, BgmSource.ARENA_LOBBY))
        );
        assertEquals(
            BgmSource.ARENA_COMBAT,
            BgmServiceImpl.resolveActiveSource(EnumSet.allOf(BgmSource.class))
        );
    }

    @Test
    void sourcePrioritiesAreStrictlyOrdered() {
        assertTrue(BgmSource.WORLD.getPriority() < BgmSource.SUKIMA.getPriority());
        assertTrue(BgmSource.SUKIMA.getPriority() < BgmSource.ARENA_LOBBY.getPriority());
        assertTrue(BgmSource.ARENA_LOBBY.getPriority() < BgmSource.ARENA_NORMAL.getPriority());
        assertTrue(BgmSource.ARENA_NORMAL.getPriority() < BgmSource.ARENA_COMBAT.getPriority());
    }
}
