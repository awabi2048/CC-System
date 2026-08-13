package com.awabi2048.ccsystem.core.displayeffect.paper

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId
import org.bukkit.entity.Display
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PaperDisplayEffectAssetResolverTest {
    private val resolver = PaperMaterialAssetResolver()

    @Test
    fun `standard resolver rejects custom namespace before material lookup`() {
        assertThrows(IllegalArgumentException::class.java) {
            resolver.resolveItem(DisplayEffectAssetId("cc-content:custom_particle"))
        }
    }

    @Test
    fun `render config rejects values outside Paper interpolation range`() {
        assertThrows(IllegalArgumentException::class.java) {
            PaperDisplayEffectRenderConfig(interpolationDurationTicks = 60)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PaperDisplayEffectRenderConfig(viewRange = Float.NaN)
        }
    }

    @Test
    fun `render config keeps particle friendly fixed defaults`() {
        val config = PaperDisplayEffectRenderConfig()

        assertEquals(0, config.interpolationDurationTicks)
        assertEquals(0, config.teleportDurationTicks)
        assertEquals(Display.Billboard.FIXED, config.billboard)
        assertEquals(
            org.bukkit.entity.ItemDisplay.ItemDisplayTransform.NONE,
            config.itemDisplayTransform
        )
        assertEquals(false, config.persistent)
    }
}
