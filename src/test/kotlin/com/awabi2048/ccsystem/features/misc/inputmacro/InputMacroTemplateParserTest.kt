package com.awabi2048.ccsystem.features.misc.inputmacro

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InputMacroTemplateParserTest {
    private fun parse(parts: List<String>): InputMacroTemplateParseResult = InputMacroTemplateParser.parse(parts)

    private fun successCommand(parts: List<String>): String =
        (parse(parts) as InputMacroTemplateParseResult.Success).command

    private fun failureReason(parts: List<String>): InputMacroTemplateParseResult.Failure.Reason =
        (parse(parts) as InputMacroTemplateParseResult.Failure).reason

    @Test
    fun `parses single-quoted template`() {
        // Bukkit は空白で分割するため、引用符は分割後のトークンへ付いたまま届く。
        assertEquals(
            "say %player_input% %player_name% %player_uuid%",
            successCommand(listOf("'/say", "%player_input%", "%player_name%", "%player_uuid%'"))
        )
    }

    @Test
    fun `parses double-quoted template`() {
        assertEquals(
            "say %player_input%",
            successCommand(listOf("\"/say", "%player_input%\""))
        )
    }

    @Test
    fun `parses unquoted single token`() {
        assertEquals("say", successCommand(listOf("/say")))
    }

    @Test
    fun `rejects mismatched quotes`() {
        assertEquals(
            InputMacroTemplateParseResult.Failure.Reason.UNPAIRED_QUOTE,
            failureReason(listOf("'/say", "x"))
        )
        assertEquals(
            InputMacroTemplateParseResult.Failure.Reason.UNPAIRED_QUOTE,
            failureReason(listOf("/say", "x'"))
        )
    }

    @Test
    fun `rejects empty template`() {
        assertEquals(
            InputMacroTemplateParseResult.Failure.Reason.EMPTY,
            failureReason(listOf("''"))
        )
        assertEquals(
            InputMacroTemplateParseResult.Failure.Reason.EMPTY,
            failureReason(emptyList())
        )
    }

    @Test
    fun `rejects slash-only template`() {
        assertEquals(
            InputMacroTemplateParseResult.Failure.Reason.EMPTY,
            failureReason(listOf("/"))
        )
    }

    @Test
    fun `rejects cr lf and nul in template`() {
        assertEquals(
            InputMacroTemplateParseResult.Failure.Reason.FORBIDDEN_CONTROL,
            failureReason(listOf("/say", "a\rb"))
        )
        assertEquals(
            InputMacroTemplateParseResult.Failure.Reason.FORBIDDEN_CONTROL,
            failureReason(listOf("/say", "a\nb"))
        )
        assertEquals(
            InputMacroTemplateParseResult.Failure.Reason.FORBIDDEN_CONTROL,
            failureReason(listOf("/say", "a\u0000b"))
        )
    }

    @Test
    fun `removes only one leading slash before dispatch`() {
        assertEquals("/say", successCommand(listOf("//say")))
        assertEquals("say", successCommand(listOf("/say")))
    }

    @Test
    fun `keeps command without leading slash even when quoted`() {
        assertEquals("say x", successCommand(listOf("'/say", "x'")))
    }

    @Test
    fun `single token with matching quotes is unquoted`() {
        assertEquals("say", successCommand(listOf("\"say\"")))
    }

    @Test
    fun `success result carries fully prepared command`() {
        val result = parse(listOf("'/say", "hello'"))
        assertTrue(result is InputMacroTemplateParseResult.Success)
        assertEquals("say hello", (result as InputMacroTemplateParseResult.Success).command)
    }
}
