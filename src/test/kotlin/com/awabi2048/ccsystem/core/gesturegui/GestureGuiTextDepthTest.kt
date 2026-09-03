package com.awabi2048.ccsystem.core.gesturegui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GestureGuiTextDepthTest {
    @Test
    fun `text depth keeps the content surface and halves the distance above it`() {
        assertEquals(4.0, GestureGuiTextDepth.effectiveLayer(4), 1.0e-9)
        assertEquals(5.0, GestureGuiTextDepth.effectiveLayer(6), 1.0e-9)
        assertEquals(12.0, GestureGuiTextDepth.effectiveLayer(20), 1.0e-9)
    }

    @Test
    fun `text depth does not move layers at or behind the content surface`() {
        assertEquals(2.0, GestureGuiTextDepth.effectiveLayer(2), 1.0e-9)
        assertEquals(4.0, GestureGuiTextDepth.effectiveLayer(4), 1.0e-9)
    }
}
