package com.awabi2048.ccsystem.features.misc.inputmacro

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InputMacroModeTest {
    @Test
    fun `parses console mode case-insensitively`() {
        assertEquals(InputMacroMode.CONSOLE, InputMacroMode.from("console"))
        assertEquals(InputMacroMode.CONSOLE, InputMacroMode.from("CONSOLE"))
        assertEquals(InputMacroMode.CONSOLE, InputMacroMode.from("Console"))
    }

    @Test
    fun `parses player mode case-insensitively`() {
        assertEquals(InputMacroMode.PLAYER, InputMacroMode.from("player"))
        assertEquals(InputMacroMode.PLAYER, InputMacroMode.from("PLAYER"))
        assertEquals(InputMacroMode.PLAYER, InputMacroMode.from("Player"))
    }

    @Test
    fun `rejects invalid and missing modes`() {
        assertNull(InputMacroMode.from("admin"))
        assertNull(InputMacroMode.from(""))
        assertNull(InputMacroMode.from(null))
    }

    @Test
    fun `player and console permissions are separated`() {
        assertEquals("cc-system.input-macro.player", InputMacroMode.PLAYER.permission)
        assertEquals("cc-system.input-macro.console", InputMacroMode.CONSOLE.permission)
        assertNotEquals(InputMacroMode.PLAYER.permission, InputMacroMode.CONSOLE.permission)
    }
}
