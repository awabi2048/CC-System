package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.*
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MenuCapabilityAvailabilityReasonTest {
    @Test
    fun `reasoned availability is evaluated once and propagated`() {
        val evaluations = AtomicInteger()
        val reason = Component.text("利用できません")
        val service = MenuCapabilityServiceImpl()
        service.register(definition(MenuCapabilityAvailability.reasoned {
            evaluations.incrementAndGet()
            MenuAvailabilityResult.Unavailable(reason)
        }))

        val resolved = requireNotNull(service.resolve("test:reasoned", player()))
        assertEquals(1, evaluations.get())
        assertEquals(reason, resolved.unavailableReason)
        assertFalse(resolved.actionable)
        assertTrue(resolved.actions.isEmpty())
    }

    @Test
    fun `legacy boolean availability remains distinguishable without invented reason`() {
        val service = MenuCapabilityServiceImpl()
        service.register(definition(MenuCapabilityAvailability { false }))

        val resolved = requireNotNull(service.resolve("test:reasoned", player()))
        assertEquals(MenuAvailabilityResult.UnavailableUnknown, resolved.availabilityResult)
        assertNull(resolved.unavailableReason)
        assertFalse(resolved.actionable)
    }

    private fun definition(availability: MenuCapabilityAvailability) = MenuCapabilityDefinition(
        owner = "test",
        id = "reasoned",
        placement = "test",
        availability = availability,
        presentationProvider = MenuCapabilityPresentationProvider {
            MenuCapabilityPresentation(
                GuiItemSpec(
                    Material.BARRIER,
                    GuiNameSpec.FixedLabel(Component.text("Test")),
                    GuiLoreSpec.None,
                    GuiElementRole.CONTENT,
                    1,
                ),
            )
        },
        actions = emptyList(),
    )

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
}
