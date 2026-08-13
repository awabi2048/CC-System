package com.awabi2048.ccsystem.api

import com.awabi2048.ccsystem.api.gui.MenuReversibleInteractionContext
import com.awabi2048.ccsystem.api.gui.MenuReversibleProviderState
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateRestoreContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CCSystemApiContractVersionTest {
    @Test
    fun `GUI runtime contract version is stable and publicly visible`() {
        assertEquals(8, CCSystemAPI.GUI_RUNTIME_CONTRACT_VERSION)
        assertEquals(3, CCSystemAPI.GESTURE_GUI_CONTRACT_VERSION)
        assertEquals(Int::class.javaPrimitiveType, CCSystemAPI::class.java.getMethod("getGuiRuntimeContractVersion").returnType)
        assertEquals(MenuReversibleProviderState::class.java, MenuReversibleStateRestoreContext::class.java.getMethod("getState").returnType)
        assertEquals(MenuReversibleInteractionContext::class.java, MenuReversibleStateRestoreContext::class.java.getMethod("getInteraction").returnType)
    }
}
