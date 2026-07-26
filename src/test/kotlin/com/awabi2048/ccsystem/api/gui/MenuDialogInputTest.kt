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
}
