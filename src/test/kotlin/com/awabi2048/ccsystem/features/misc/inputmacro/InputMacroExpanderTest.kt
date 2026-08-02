package com.awabi2048.ccsystem.features.misc.inputmacro

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InputMacroExpanderTest {
    @Test
    fun `expands all three macros`() {
        assertEquals(
            "/say hello Alice 00000000-0000-0000-0000-000000000000",
            InputMacroExpander.expand(
                "/say %player_input% %player_name% %player_uuid%",
                "hello",
                "Alice",
                "00000000-0000-0000-0000-000000000000",
            )
        )
    }

    @Test
    fun `expands the same macro multiple times to the same value`() {
        assertEquals(
            "/tell Alice hello /tell Alice hello",
            InputMacroExpander.expand(
                "/tell %player_name% %player_input% /tell %player_name% %player_input%",
                "hello",
                "Alice",
                "uuid",
            )
        )
    }

    @Test
    fun `does not re-expand macro string contained in the input`() {
        // player_input に %player_name% が含まれていても、挿入後の再展開は行わない。
        assertEquals(
            "/say %player_name%",
            InputMacroExpander.expand("/say %player_input%", "%player_name%", "Alice", "uuid")
        )
    }

    @Test
    fun `keeps unknown percent placeholders unchanged`() {
        assertEquals(
            "/say %unknown%",
            InputMacroExpander.expand("/say %unknown%", "input", "Alice", "uuid")
        )
    }

    @Test
    fun `macro names are case-sensitive`() {
        assertEquals(
            "/say %PLAYER_INPUT% %Player_Name%",
            InputMacroExpander.expand("/say %PLAYER_INPUT% %Player_Name%", "input", "Alice", "uuid")
        )
    }

    @Test
    fun `expands without macros as-is`() {
        assertEquals("/say hello", InputMacroExpander.expand("/say hello", "input", "Alice", "uuid"))
    }
}
