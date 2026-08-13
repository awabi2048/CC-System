package com.awabi2048.ccsystem.api.gesturegui

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiModelsTest {
    @Test
    fun `access policy separates owner allowlist and public operation`() {
        val owner = UUID.randomUUID()
        val allowed = UUID.randomUUID()
        val stranger = UUID.randomUUID()

        assertFalse(screen(GestureGuiAccess.OWNER_ONLY).canOperate(owner, stranger))
        assertTrue(screen(GestureGuiAccess.ALLOWLIST, setOf(allowed)).canOperate(owner, owner))
        assertTrue(screen(GestureGuiAccess.ALLOWLIST, setOf(allowed)).canOperate(owner, allowed))
        assertFalse(screen(GestureGuiAccess.ALLOWLIST, setOf(allowed)).canOperate(owner, stranger))
        assertTrue(screen(GestureGuiAccess.PUBLIC).canOperate(owner, stranger))
    }

    @Test
    fun `element ids must be stable and unique in a screen`() {
        val element = GestureGuiElement("action", GestureGuiBounds(-0.1, -0.1, 0.1, 0.1))
        assertThrows(IllegalArgumentException::class.java) {
            GestureGuiScreenDefinition("screen", listOf(element, element))
        }
    }

    @Test
    fun `bounds include their visible edge`() {
        val bounds = GestureGuiBounds(-0.75, -0.375, 0.75, 0.375)
        assertTrue(bounds.contains(-0.75, 0.375))
        assertFalse(bounds.contains(0.751, 0.0))
    }

    private fun screen(access: GestureGuiAccess, allowlist: Set<UUID> = emptySet()) =
        GestureGuiScreenDefinition("screen", emptyList(), access, allowlist)
}
