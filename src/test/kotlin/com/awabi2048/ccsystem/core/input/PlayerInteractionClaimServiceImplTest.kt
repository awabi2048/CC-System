package com.awabi2048.ccsystem.core.input

import com.awabi2048.ccsystem.api.input.PlayerInteractionChannel
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerInteractionClaimServiceImplTest {
    private val playerId = UUID.randomUUID()
    private val service = PlayerInteractionClaimServiceImpl()

    @Test
    fun `異なる入力チャネルは別ownerが同時取得できる`() {
        assertNotNull(service.claim(playerId, PlayerInteractionChannel.PRIMARY, "gesture-gui"))
        assertNotNull(service.claim(playerId, PlayerInteractionChannel.SWAP_HAND, "freecam"))
    }

    @Test
    fun `同じ入力チャネルは異なるownerへ渡さない`() {
        assertNotNull(service.claim(playerId, PlayerInteractionChannel.SWAP_HAND, "gesture-gui"))
        assertNull(service.claim(playerId, PlayerInteractionChannel.SWAP_HAND, "freecam"))
        assertEquals("gesture-gui", service.ownerOf(playerId, PlayerInteractionChannel.SWAP_HAND))
    }

    @Test
    fun `同一ownerの複数handleは最後のcloseまで所有権を保つ`() {
        val first = requireNotNull(service.claim(playerId, PlayerInteractionChannel.PRIMARY, "gesture-gui"))
        val second = requireNotNull(service.claim(playerId, PlayerInteractionChannel.PRIMARY, "gesture-gui"))

        first.close()
        assertTrue(service.isClaimedBy(playerId, PlayerInteractionChannel.PRIMARY, "gesture-gui"))
        second.close()
        assertFalse(service.isClaimed(playerId, PlayerInteractionChannel.PRIMARY))
        second.close()
        assertFalse(second.active)
    }

    @Test
    fun `owner指定releaseAllは他ownerのチャネルを維持する`() {
        service.claim(playerId, PlayerInteractionChannel.PRIMARY, "gesture-gui")
        service.claim(playerId, PlayerInteractionChannel.SWAP_HAND, "freecam")

        service.releaseAll(playerId, "gesture-gui")

        assertFalse(service.isClaimed(playerId, PlayerInteractionChannel.PRIMARY))
        assertTrue(service.isClaimedBy(playerId, PlayerInteractionChannel.SWAP_HAND, "freecam"))
    }
}
