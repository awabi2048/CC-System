package com.awabi2048.ccsystem.features.misc.inputmacro

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InputMacroArgumentTest {
    private fun parse(args: List<String>): InputMacroArgumentResult = InputMacroArgumentParser.parse(args)

    private fun success(args: List<String>): InputMacroArgumentResult.Success =
        parse(args) as InputMacroArgumentResult.Success

    private fun failureReason(args: List<String>): InputMacroArgumentResult.Failure.Reason =
        (parse(args) as InputMacroArgumentResult.Failure).reason

    @Test
    fun `parses player name template and console mode`() {
        val result = success(listOf("Alice", "'/say", "%player_input%'", "console"))
        assertEquals("Alice", result.playerName)
        assertEquals(listOf("'/say", "%player_input%'"), result.templateParts)
        assertEquals(InputMacroMode.CONSOLE, result.mode)
    }

    @Test
    fun `parses player mode case-insensitively`() {
        val result = success(listOf("Alice", "'/say", "hi'", "PLAYER"))
        assertEquals(InputMacroMode.PLAYER, result.mode)
        assertEquals(listOf("'/say", "hi'"), result.templateParts)
    }

    @Test
    fun `defaults to console when mode is omitted`() {
        val result = success(listOf("Alice", "'/say", "hi'"))
        assertEquals(InputMacroMode.CONSOLE, result.mode)
        assertEquals(listOf("'/say", "hi'"), result.templateParts)
    }

    @Test
    fun `non-mode last argument stays in the template`() {
        // 最後の引数が console/player に一致しない場合は、モード省略とみなしてテンプレートへ含める。
        val result = success(listOf("Alice", "'/say", "hello'"))
        assertEquals(InputMacroMode.CONSOLE, result.mode)
        assertEquals(listOf("'/say", "hello'"), result.templateParts)
    }

    @Test
    fun `single token template with mode is parsed`() {
        val result = success(listOf("Alice", "say", "console"))
        assertEquals("Alice", result.playerName)
        assertEquals(listOf("say"), result.templateParts)
        assertEquals(InputMacroMode.CONSOLE, result.mode)
    }

    @Test
    fun `rejects missing player name`() {
        assertEquals(
            InputMacroArgumentResult.Failure.Reason.MISSING_PLAYER,
            failureReason(emptyList())
        )
    }

    @Test
    fun `rejects missing template`() {
        assertEquals(
            InputMacroArgumentResult.Failure.Reason.MISSING_TEMPLATE,
            failureReason(listOf("Alice"))
        )
    }

    @Test
    fun `rejects mode-only without template`() {
        assertEquals(
            InputMacroArgumentResult.Failure.Reason.MISSING_TEMPLATE,
            failureReason(listOf("Alice", "console"))
        )
    }
}
