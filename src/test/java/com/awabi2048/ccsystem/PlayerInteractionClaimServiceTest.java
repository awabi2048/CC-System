package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.core.input.PlayerInteractionClaimServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerInteractionClaimServiceTest {
    @Test
    void onlyOneOwnerCanClaimPlayerInputUntilReleased() {
        var service = new PlayerInteractionClaimServiceImpl();
        var player = UUID.randomUUID();

        assertTrue(service.tryClaim(player, "party"));
        assertTrue(service.tryClaim(player, "party"));
        assertFalse(service.tryClaim(player, "macro"));
        assertEquals("party", service.ownerOf(player));
        assertFalse(service.release(player, "macro"));
        assertTrue(service.release(player, "party"));
        assertTrue(service.tryClaim(player, "macro"));
        service.releaseAll(player);
        assertNull(service.ownerOf(player));
    }
}
