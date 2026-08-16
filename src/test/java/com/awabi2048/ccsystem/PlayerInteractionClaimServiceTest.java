package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.input.PlayerInteractionChannel;
import com.awabi2048.ccsystem.core.input.PlayerInteractionClaimServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerInteractionClaimServiceTest {
    @Test
    void onlyOneOwnerCanClaimInputChannelUntilReleased() {
        var service = new PlayerInteractionClaimServiceImpl();
        var player = UUID.randomUUID();

        var party = service.claim(player, PlayerInteractionChannel.SWAP_HAND, "party");
        assertNotNull(party);
        assertNull(service.claim(player, PlayerInteractionChannel.SWAP_HAND, "macro"));
        assertEquals("party", service.ownerOf(player, PlayerInteractionChannel.SWAP_HAND));
        assertFalse(service.release(player, PlayerInteractionChannel.SWAP_HAND, "macro"));
        assertTrue(service.release(player, PlayerInteractionChannel.SWAP_HAND, "party"));
        assertNotNull(service.claim(player, PlayerInteractionChannel.SWAP_HAND, "macro"));
        service.releaseAll(player);
        assertNull(service.ownerOf(player, PlayerInteractionChannel.SWAP_HAND));
    }
}
