package com.awabi2048.ccsystem

import com.awabi2048.ccsystem.api.gui.MenuUpdate
import com.awabi2048.ccsystem.core.gui.MenuStaleUpdatePolicy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuStaleUpdatePolicyTest {
    @Test
    fun `applies update only to the originating presentation`() {
        assertTrue(MenuStaleUpdatePolicy.shouldApply(MenuUpdate.Close, 10, 10))
        assertFalse(MenuStaleUpdatePolicy.shouldApply(MenuUpdate.Close, 10, 11))
        assertFalse(MenuStaleUpdatePolicy.shouldApply(MenuUpdate.Back, 10, 11))
        assertFalse(MenuStaleUpdatePolicy.shouldApply(MenuUpdate.Refresh, 10, 11))
    }

    @Test
    fun `none remains safe after a presentation change`() {
        assertTrue(MenuStaleUpdatePolicy.shouldApply(MenuUpdate.None, 10, 11))
    }
}
