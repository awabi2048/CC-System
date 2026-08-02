package com.awabi2048.ccsystem.api.gui

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuCapabilityContractValidationTest {
    @Test
    fun `unknown capability is invalid when registry validation is enabled`() {
        val violations = MenuContractValidator.validate(
            definition(),
            listOf(MenuActionObservation(4, MenuInteraction.Capability("missing:capability"))),
            MenuContractValidationContext(capabilityService(emptyList())),
        )

        assertTrue(violations.single().message.contains("not registered"))
    }

    @Test
    fun `capability click and safety must match the registry static contract`() {
        val actions = listOf(staticAction("open", MenuCapabilityTrigger.LEFT, MenuActionSafety.NAVIGATION_ONLY))
        val violations = MenuContractValidator.validate(
            definition(),
            listOf(
                MenuActionObservation(
                    4,
                    MenuInteraction.Capability(
                        capabilityId = "test:capability",
                        acceptedClicks = setOf(ClickType.RIGHT),
                        safety = MenuActionSafety.REVERSIBLE,
                        reversibleContract = MenuReversibleContract("audit:test"),
                    ),
                ),
            ),
            MenuContractValidationContext(capabilityService(actions)),
        )

        assertTrue(violations.any { it.message.contains("accepted clicks") })
        assertTrue(violations.any { it.message.contains("safety contract") })
    }

    @Test
    fun `registry validation does not invoke dynamic capability resolution`() {
        var resolveCalls = 0
        val service = capabilityService(
            listOf(staticAction("open", MenuCapabilityTrigger.LEFT, MenuActionSafety.NAVIGATION_ONLY)),
        ) { resolveCalls += 1 }

        val violations = MenuContractValidator.validate(
            definition(),
            listOf(
                MenuActionObservation(
                    4,
                    MenuInteraction.Capability(
                        capabilityId = "test:capability",
                        acceptedClicks = setOf(ClickType.LEFT),
                        safety = MenuActionSafety.NAVIGATION_ONLY,
                        safetyByClick = mapOf(ClickType.LEFT to MenuActionSafety.NAVIGATION_ONLY),
                    ),
                ),
            ),
            MenuContractValidationContext(service),
        )

        assertTrue(violations.isEmpty())
        assertEquals(0, resolveCalls)
    }

    @Test
    fun `capability definition virtual machine error is never converted to a violation`() {
        val service = object : MenuCapabilityService by capabilityService(emptyList()) {
            override fun definition(capabilityId: String): MenuCapabilityDefinition? =
                throw OutOfMemoryError("test")
        }

        assertThrows(OutOfMemoryError::class.java) {
            MenuContractValidator.validate(
                definition(),
                listOf(MenuActionObservation(4, MenuInteraction.Capability("test:capability"))),
                MenuContractValidationContext(service),
            )
        }
    }

    private fun definition(): InventoryMenuDefinition = InventoryMenuDefinition(
        owner = "test",
        id = "contract",
        renderer = InventoryMenuRenderer { error("not used") },
        actions = emptyMap(),
    )

    private fun staticAction(
        id: String,
        trigger: MenuCapabilityTrigger,
        safety: MenuActionSafety,
    ): MenuCapabilityAction = MenuCapabilityAction(
        id = id,
        trigger = trigger,
        textProvider = MenuCapabilityActionTextProvider { "click" },
        handler = MenuCapabilityActionHandler { MenuActionResult.Ignored },
        safety = safety,
    )

    private fun capabilityService(
        actions: List<MenuCapabilityAction>,
        onResolve: () -> Unit = {},
    ): MenuCapabilityService = object : MenuCapabilityService {
        private val definition = actions.takeIf { it.isNotEmpty() }?.let {
            MenuCapabilityDefinition(
                owner = "test",
                id = "capability",
                placement = "test",
                availability = MenuCapabilityAvailability { true },
                presentationProvider = MenuCapabilityPresentationProvider {
                    MenuCapabilityPresentation(
                        GuiItemSpec(Material.STONE, GuiNameSpec.Empty, GuiLoreSpec.None, GuiElementRole.ACTION, 1),
                    )
                },
                actions = it,
            )
        }

        override fun register(definition: MenuCapabilityDefinition) = Unit
        override fun unregisterOwner(owner: String) = Unit
        override fun definition(capabilityId: String): MenuCapabilityDefinition? =
            definition?.takeIf { capabilityId == it.capabilityId }
        override fun definitions(): List<MenuCapabilityDefinition> = listOfNotNull(definition)
        override fun definitions(placement: String): List<MenuCapabilityDefinition> =
            listOfNotNull(definition).filter { it.placement == placement }
        override fun resolve(
            capabilityId: String,
            player: Player,
            arguments: Map<String, String>,
            attributes: Map<String, Any>,
        ): ResolvedMenuCapability? {
            onResolve()
            error("validation must not resolve a capability")
        }

        override fun execute(
            capabilityId: String,
            player: Player,
            click: ClickType,
            arguments: Map<String, String>,
            attributes: Map<String, Any>,
        ): MenuActionResult = MenuActionResult.Ignored
    }
}
