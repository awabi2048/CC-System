package com.awabi2048.ccsystem.api.gui

import java.lang.reflect.Proxy
import java.util.UUID
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuCapabilityContractValidationTest {
    @Test
    fun `unknown capability is invalid when registry validation is enabled`() {
        val violations = MenuContractValidator.validate(
            definition(),
            listOf(MenuActionObservation(4, MenuInteraction.Capability("missing:capability"))),
            MenuContractValidationContext(player(), capabilityService(emptyMap())),
        )

        assertTrue(violations.single().message.contains("not registered"))
    }

    @Test
    fun `capability click and safety must match the registry resolution`() {
        val capability = resolvedCapability(
            acceptedClicks = setOf(ClickType.LEFT),
            safety = MenuActionSafety.NAVIGATION_ONLY,
        )
        val violations = MenuContractValidator.validate(
            definition(),
            listOf(
                MenuActionObservation(
                    4,
                    MenuInteraction.Capability(
                        capabilityId = capability.capabilityId,
                        acceptedClicks = setOf(ClickType.RIGHT),
                        safety = MenuActionSafety.REVERSIBLE,
                    ),
                ),
            ),
            MenuContractValidationContext(player(), capabilityService(mapOf(capability.capabilityId to capability))),
        )

        assertTrue(violations.any { it.message.contains("accepted clicks") })
        assertTrue(violations.any { it.message.contains("safety contract") })
    }

    private fun definition(): InventoryMenuDefinition = InventoryMenuDefinition(
        owner = "test",
        id = "contract",
        renderer = InventoryMenuRenderer { error("not used") },
        actions = emptyMap(),
    )

    private fun resolvedCapability(
        acceptedClicks: Set<ClickType>,
        safety: MenuActionSafety,
    ): ResolvedMenuCapability = ResolvedMenuCapability(
        capabilityId = "test:capability",
        presentation = MenuCapabilityPresentation(
            GuiItemSpec(Material.STONE, GuiNameSpec.Empty, GuiLoreSpec.None, GuiElementRole.ACTION, 1),
        ),
        actions = listOf(
            ResolvedMenuCapabilityAction(
                "open",
                MenuCapabilityTrigger.LEFT,
                "クリック",
                safety,
            ),
        ),
    ).let { resolved ->
        if (resolved.acceptedClicks == acceptedClicks) resolved else ResolvedMenuCapability(
            resolved.capabilityId,
            resolved.presentation,
            listOf(
                ResolvedMenuCapabilityAction(
                    "alternate",
                    MenuCapabilityTrigger.RIGHT,
                    "クリック",
                    safety,
                ),
            ),
        )
    }

    private fun capabilityService(
        resolved: Map<String, ResolvedMenuCapability>,
    ): MenuCapabilityService = object : MenuCapabilityService {
        override fun register(definition: MenuCapabilityDefinition) = Unit
        override fun unregisterOwner(owner: String) = Unit
        override fun definition(capabilityId: String): MenuCapabilityDefinition? =
            resolved[capabilityId]?.let { MenuCapabilityDefinition(
                owner = "test",
                id = "capability",
                placement = "test",
                availability = MenuCapabilityAvailability { true },
                presentationProvider = MenuCapabilityPresentationProvider { it1 -> it1.let { error("not used") } },
                actions = emptyList(),
            ) }
        override fun definitions(): List<MenuCapabilityDefinition> = emptyList()
        override fun definitions(placement: String): List<MenuCapabilityDefinition> = emptyList()
        override fun resolve(
            capabilityId: String,
            player: Player,
            arguments: Map<String, String>,
            attributes: Map<String, Any>,
        ): ResolvedMenuCapability? = resolved[capabilityId]
        override fun execute(
            capabilityId: String,
            player: Player,
            click: ClickType,
            arguments: Map<String, String>,
            attributes: Map<String, Any>,
        ): MenuActionResult = MenuActionResult.Ignored
    }

    private fun player(): Player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { proxy, method, arguments ->
        when (method.name) {
            "getUniqueId" -> UUID.randomUUID()
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments?.singleOrNull()
            else -> throw UnsupportedOperationException(method.name)
        }
    } as Player
}
