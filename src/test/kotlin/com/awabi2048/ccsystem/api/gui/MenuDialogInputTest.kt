package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MenuDialogInputTest {
    @Test
    fun `single option response exposes selected id`() {
        val response = MenuDialogResponse(
            text = emptyMap(),
            booleans = emptyMap(),
            selections = mapOf("display_type" to "hologram"),
        )

        assertEquals("hologram", response.selectedValue("display_type"))
    }

    @Test
    fun `single option rejects duplicate ids`() {
        assertThrows(IllegalArgumentException::class.java) {
            MenuDialogInput.SingleOption(
                id = "display_type",
                label = Component.text("表示方法"),
                options = listOf(
                    MenuDialogInput.SingleOption.Option("sign", Component.text("看板")),
                    MenuDialogInput.SingleOption.Option("sign", Component.text("ホログラム")),
                ),
            )
        }
    }

    @Test
    fun `single option rejects multiple initial values`() {
        assertThrows(IllegalArgumentException::class.java) {
            MenuDialogInput.SingleOption(
                id = "display_type",
                label = Component.text("表示方法"),
                options = listOf(
                    MenuDialogInput.SingleOption.Option("sign", Component.text("看板"), true),
                    MenuDialogInput.SingleOption.Option("hologram", Component.text("ホログラム"), true),
                ),
            )
        }
    }

    @Test
    fun `dialog rejects non-positive multi action columns`() {
        val button = MenuDialogButton(
            Component.text("決定"),
            MenuDialogHandler { _, _ -> MenuActionResult.Success() },
        )
        assertThrows(IllegalArgumentException::class.java) {
            MenuDialogRequest(
                owner = "test",
                id = "multi-action",
                title = Component.text("確認"),
                body = emptyList(),
                confirm = button,
                cancel = button,
                additionalActions = listOf(button),
                columns = 0,
            )
        }
    }

    @Test
    fun `dialog allows escape close by default`() {
        val button = MenuDialogButton(
            Component.text("決定"),
            MenuDialogHandler { _, _ -> MenuActionResult.Success() },
        )
        val request = MenuDialogRequest(
            owner = "test",
            id = "escape-default",
            title = Component.text("確認"),
            body = emptyList(),
            confirm = button,
            cancel = button,
        )
        assertEquals(true, request.canCloseWithEscape)
    }

    @Test
    fun `dialog can opt out of escape close`() {
        val button = MenuDialogButton(
            Component.text("決定"),
            MenuDialogHandler { _, _ -> MenuActionResult.Success() },
        )
        val request = MenuDialogRequest(
            owner = "test",
            id = "escape-off",
            title = Component.text("確認"),
            body = emptyList(),
            confirm = button,
            cancel = button,
            canCloseWithEscape = false,
        )
        assertEquals(false, request.canCloseWithEscape)
    }
}
