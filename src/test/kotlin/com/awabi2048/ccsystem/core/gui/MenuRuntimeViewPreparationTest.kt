package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuRenderer
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuActionObservation
import com.awabi2048.ccsystem.api.gui.MenuCapabilityDefinition
import com.awabi2048.ccsystem.api.gui.MenuCapabilityService
import com.awabi2048.ccsystem.api.gui.MenuCapabilityAvailability
import com.awabi2048.ccsystem.api.gui.MenuCapabilityPresentationProvider
import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationFailureReason
import com.awabi2048.ccsystem.api.gui.MenuRuntimeRouteSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRenderContext
import com.awabi2048.ccsystem.api.gui.MenuRoute
import java.lang.reflect.Proxy
import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
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

    @Test
    fun `inspect uses common preparation and does not mutate caller state`() {
        val state = mutableListOf("current-route", "history", "revision")
        val before = state.toList()
        val result = MenuRuntimeViewPreparation.inspect(
            InventoryMenuDefinition(
                owner = "test",
                id = "inspect",
                renderer = InventoryMenuRenderer { emptyView() },
                actions = emptyMap(),
            ),
            MenuRenderContext(player(), MenuRoute("test", "inspect")),
            capabilityService(),
        ) {
            MenuRuntimeInspectionSnapshot(
                MenuRuntimeRouteSnapshot("test", "inspect", emptyMap()),
                emptyList(),
                false,
                it.title,
                it.size,
                7L,
                emptyList(),
            )
        }

        assertEquals(before, state)
        assertEquals(true, result.operationResult.successful)
        assertEquals(7L, result.snapshot?.revision)
    }

    @Test
    fun `inspect distinguishes render and contract failures`() {
        val renderFailure = MenuRuntimeViewPreparation.inspect(
            InventoryMenuDefinition(
                owner = "test",
                id = "inspect-render-failure",
                renderer = InventoryMenuRenderer { throw IllegalStateException("boom") },
                actions = emptyMap(),
            ),
            MenuRenderContext(player(), MenuRoute("test", "inspect-render-failure")),
            capabilityService(),
        ) { error("snapshot must not run") }
        assertEquals(
            MenuRuntimeOperationFailureReason.RENDER_FAILED,
            renderFailure.operationResult.failure?.reason,
        )

        val contractFailure = MenuRuntimeViewPreparation.contractInvalid(
            InventoryMenuDefinition(
                owner = "test",
                id = "inspect-contract-failure",
                renderer = InventoryMenuRenderer { emptyView() },
                actions = emptyMap(),
            ),
            listOf(MenuActionObservation(0, com.awabi2048.ccsystem.api.gui.MenuInteraction.Action("missing-handler"))),
        )
        assertEquals(
            "missing-handler",
            contractFailure?.violations?.single()?.actionId,
        )
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

    private fun capabilityService(): MenuCapabilityService = object : MenuCapabilityService {
        override fun register(definition: MenuCapabilityDefinition) = Unit
        override fun unregisterOwner(owner: String) = Unit
        override fun definition(capabilityId: String): MenuCapabilityDefinition? = null
        override fun definitions(): List<MenuCapabilityDefinition> = emptyList()
        override fun definitions(placement: String): List<MenuCapabilityDefinition> = emptyList()
        override fun resolve(
            capabilityId: String,
            player: Player,
            arguments: Map<String, String>,
            attributes: Map<String, Any>,
        ) = null
        override fun execute(
            capabilityId: String,
            player: Player,
            click: ClickType,
            arguments: Map<String, String>,
            attributes: Map<String, Any>,
        ) = MenuActionResult.Ignored
    }
}
