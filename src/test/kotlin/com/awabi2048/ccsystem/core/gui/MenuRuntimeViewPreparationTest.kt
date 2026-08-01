package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuRenderer
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuActionObservation
import com.awabi2048.ccsystem.api.gui.MenuRenderContext
import com.awabi2048.ccsystem.api.gui.MenuRoute
import java.lang.reflect.Proxy
import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class MenuRuntimeViewPreparationTest {
    @Test
    fun `open and refresh contexts apply the same contract validation`() {
        val definition = InventoryMenuDefinition(
            owner = "test",
            id = "invalid",
            renderer = InventoryMenuRenderer { emptyView() },
            actions = emptyMap(),
        )

        listOf(false, true).forEach { canGoBack ->
            val result = MenuRuntimeViewPreparation.renderValidated(
                definition,
                MenuRenderContext(player(), MenuRoute("test", "invalid"), canGoBack),
            )
            assertInstanceOf(MenuRuntimePreparedViewResult.Ready::class.java, result)
        }
    }

    @Test
    fun `common preparation reports contract invalid from the same validator used by rendered views`() {
        val definition = InventoryMenuDefinition(
            owner = "test",
            id = "invalid-contract",
            renderer = InventoryMenuRenderer { emptyView() },
            actions = emptyMap(),
        )

        val invalid = MenuRuntimeViewPreparation.contractInvalid(
            definition,
            listOf(MenuActionObservation(4, com.awabi2048.ccsystem.api.gui.MenuInteraction.Action("missing-handler"))),
        )

        assertEquals("missing-handler", invalid?.violations?.single()?.actionId)
    }

    @Test
    fun `renderer failure is distinguished from contract invalid`() {
        val definition = InventoryMenuDefinition(
            owner = "test",
            id = "render-failure",
            renderer = InventoryMenuRenderer { throw IllegalStateException("boom") },
            actions = emptyMap(),
        )

        val result = MenuRuntimeViewPreparation.renderValidated(
            definition,
            MenuRenderContext(player(), MenuRoute("test", "render-failure")),
        )

        val failed = assertInstanceOf(MenuRuntimePreparedViewResult.RenderFailed::class.java, result)
        assertEquals(IllegalStateException::class.java.name, failed.exceptionType)
    }

    private fun player(): Player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { proxy, method, arguments ->
        when (method.name) {
            "getUniqueId" -> UUID.randomUUID()
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments?.singleOrNull()
            "toString" -> "TestPlayer"
            else -> throw UnsupportedOperationException(method.name)
        }
    } as Player

    private fun emptyView(): InventoryMenuView = InventoryMenuView(
        size = 9,
        title = Component.text("Test"),
        elements = emptyList(),
    )
}
