package com.awabi2048.ccsystem.api.gesturegui

import java.util.UUID
import java.lang.reflect.Proxy
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiModelsTest {
    @Test
    fun `default panel doubles the legacy area and uses cyan terracotta frame`() {
        val panel = GestureGuiPanel()
        assertEquals(2.0 * 1.5 * 0.75, panel.width * panel.height, 1.0e-9)
        assertEquals(2.0, panel.width / panel.height, 1.0e-9)
        assertEquals(Material.CYAN_TERRACOTTA, panel.frameMaterial)
    }

    @Test
    fun `child screen accepts arbitrary finite size through its view panel`() {
        val view = GestureGuiView(
            GestureGuiScreenDefinition("dialog", emptyList()),
            emptyList(),
            GestureGuiPanel(width = 0.8, height = 0.45),
        ) {}
        val options = GestureGuiChildOptions("parent", offsetX = 0.2, offsetY = -0.1)

        assertEquals(0.8, view.panel.width)
        assertFalse(options.allowParentInteraction)
    }

    @Test
    fun `open options can select non-contiguous vertical slots`() {
        val options = GestureGuiOpenOptions(
            verticalSlots = listOf(GestureGuiVerticalSlot.TOP, GestureGuiVerticalSlot.MIDDLE),
        )

        assertEquals(
            listOf(GestureGuiVerticalSlot.TOP, GestureGuiVerticalSlot.MIDDLE),
            options.verticalSlots,
        )
        assertTrue(options.secondaryInputEnabled)
        assertFalse(options.suppressWorldClicks)
        assertThrows(IllegalArgumentException::class.java) {
            GestureGuiOpenOptions(
                layout = GestureGuiScreenLayout.HORIZONTAL,
                verticalSlots = listOf(GestureGuiVerticalSlot.TOP),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            GestureGuiOpenOptions(
                verticalSlots = listOf(GestureGuiVerticalSlot.TOP, GestureGuiVerticalSlot.TOP),
            )
        }
    }
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
    fun `dynamic access policy is evaluated after static public access`() {
        val owner = UUID.randomUUID()
        val operator = UUID.randomUUID()
        var enabled = false
        val screen = screen(
            GestureGuiAccess.PUBLIC,
            accessPolicy = GestureGuiAccessPolicy { ownerId, actorId ->
                actorId == ownerId || (actorId == operator && enabled)
            },
        )

        assertTrue(screen.canOperate(owner, owner))
        assertFalse(screen.canOperate(owner, operator))
        enabled = true
        assertTrue(screen.canOperate(owner, operator))
        enabled = false
        assertFalse(screen.canOperate(owner, operator))
    }

    @Test
    fun `visibility policy can keep a screen visible while operation is unavailable`() {
        val owner = UUID.randomUUID()
        val viewer = UUID.randomUUID()
        val screen = screen(
            GestureGuiAccess.PUBLIC,
            accessPolicy = GestureGuiAccessPolicy { _, _ -> false },
            visibilityPolicy = GestureGuiVisibilityPolicy { _, viewerId -> viewerId == viewer },
        )

        assertFalse(screen.canOperate(owner, viewer))
        assertTrue(screen.canView(owner, viewer))
        assertFalse(screen.canView(owner, UUID.randomUUID()))
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

    @Test
    fun `hover text accepts arbitrary finite screen coordinates`() {
        val hover = GestureGuiHoverText(
            Component.text("hover"),
            0.61,
            -0.31,
            layer = 38,
            replacesVisualId = "description",
        )
        assertTrue(hover.x == 0.61 && hover.y == -0.31 && hover.layer == 38)
        assertEquals("description", hover.replacesVisualId)
        assertThrows(IllegalArgumentException::class.java) {
            GestureGuiHoverText(Component.text("hover"), 0.0, 0.0, replacesVisualId = " ")
        }
    }

    @Test
    fun `text visual can own hover and click interaction`() {
        val textVisual = GestureGuiVisual.Text("title", 0.23, 0.17, Component.text("Title"))
        val element = GestureGuiElement(
            "title-action",
            GestureGuiBounds(0.0, 0.0, 0.46, 0.34),
            setOf(GestureGuiGesture.PRIMARY),
            GestureGuiHoverText(Component.text("Open"), 0.23, 0.05),
            targetVisualId = "title",
        )
        val view = GestureGuiView(
            GestureGuiScreenDefinition("screen", listOf(element)),
            listOf(textVisual),
        ) {}

        assertTrue(view.definition.elements.single().targetVisualId == textVisual.visualId)
    }

    @Test
    fun `element guard is evaluated again for the same rendered element`() {
        var enabled = false
        val element = GestureGuiElement(
            elementId = "dynamic-action",
            bounds = GestureGuiBounds(-0.1, -0.1, 0.1, 0.1),
            acceptedGestures = setOf(GestureGuiGesture.PRIMARY),
            gestureGuard = { _, _ -> enabled },
        )
        val player = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
        ) { _, _, _ -> null } as Player

        assertFalse(element.acceptsGesture(player, GestureGuiGesture.PRIMARY))
        enabled = true
        assertTrue(element.acceptsGesture(player, GestureGuiGesture.PRIMARY))
    }

    @Test
    fun `element cannot target a missing visual`() {
        val element = GestureGuiElement(
            "missing",
            GestureGuiBounds(-0.1, -0.1, 0.1, 0.1),
            targetVisualId = "unknown",
        )
        assertThrows(IllegalArgumentException::class.java) {
            GestureGuiView(GestureGuiScreenDefinition("screen", listOf(element)), emptyList()) {}
        }
    }

    @Test
    fun `hover cannot replace a missing visual`() {
        val element = GestureGuiElement(
            "hover-missing",
            GestureGuiBounds(-0.1, -0.1, 0.1, 0.1),
            hoverText = GestureGuiHoverText(
                Component.text("hover"),
                0.0,
                0.0,
                replacesVisualId = "unknown",
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            GestureGuiView(GestureGuiScreenDefinition("screen", listOf(element)), emptyList()) {}
        }
    }

    @Test
    fun `view rejects duplicate visual ids independently of display type`() {
        val visual = GestureGuiVisual.Text("title", 0.0, 0.0, Component.text("test"))
        assertThrows(IllegalArgumentException::class.java) {
            GestureGuiView(screen(GestureGuiAccess.OWNER_ONLY), listOf(visual, visual)) {}
        }
    }

    @Test
    fun `visual position must be finite`() {
        assertThrows(IllegalArgumentException::class.java) {
            GestureGuiVisual.Text("title", Double.NaN, 0.0, Component.empty())
        }
    }

    private fun screen(
        access: GestureGuiAccess,
        allowlist: Set<UUID> = emptySet(),
        accessPolicy: GestureGuiAccessPolicy? = null,
        visibilityPolicy: GestureGuiVisibilityPolicy? = null,
    ) = GestureGuiScreenDefinition("screen", emptyList(), access, allowlist, accessPolicy, visibilityPolicy)
}
