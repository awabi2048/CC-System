package com.awabi2048.ccsystem.core.gesturegui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GestureGuiTextMetricsTest {
    @Test
    fun `logical text sizes are converted into visible display scales`() {
        assertEquals(0.5f, GestureGuiTextMetrics.toDisplayScale(0.0125), 1.0e-6f)
        assertEquals(0.24f, GestureGuiTextMetrics.toDisplayScale(0.006), 1.0e-6f)
    }
}
