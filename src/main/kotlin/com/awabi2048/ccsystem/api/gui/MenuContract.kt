package com.awabi2048.ccsystem.api.gui

import org.bukkit.event.inventory.ClickType

/** Runtime handlerが表示側へ提供する宣言的な契約。 */
data class MenuActionContract(
    val acceptedClicks: Set<ClickType> = emptySet(),
    val requiredPayloadKeys: Set<String> = emptySet(),
) {
    init {
        require(requiredPayloadKeys.none { it.isBlank() }) {
            "required payload keys must not be blank"
        }
    }
}

data class MenuContractViolation(
    val routeId: String,
    val slot: Int?,
    val actionId: String?,
    val message: String,
)

data class MenuActionObservation(
    val slot: Int,
    val interaction: MenuInteraction,
)

/** RuntimeがCapability registryと照合して画面契約を検証するための文脈です。 */
data class MenuContractValidationContext(
    val capabilities: MenuCapabilityService,
)

/**
 * 画面を開く前に、表示された操作と登録済みHandlerの契約を検査する。
 * Handler本体の条件分岐は解析せず、宣言されたクリック集合だけを比較する。
 */
object MenuContractValidator {
    fun validate(
        definition: InventoryMenuDefinition,
        view: InventoryMenuView,
        context: MenuContractValidationContext? = null,
    ): List<MenuContractViolation> = validate(
        definition,
        view.elements.map { MenuActionObservation(it.slot, it.resolvedInteraction()) },
        context,
    )

    fun validate(
        definition: InventoryMenuDefinition,
        observations: List<MenuActionObservation>,
        context: MenuContractValidationContext? = null,
    ): List<MenuContractViolation> {
        val violations = mutableListOf<MenuContractViolation>()
        val acceptedByAction = mutableMapOf<String, MutableSet<ClickType>>()

        observations.forEach { observation ->
            when (val interaction = observation.interaction) {
                is MenuInteraction.Action -> validateAction(
                    definition,
                    observation.slot,
                    interaction.actionId,
                    interaction.payload,
                    interaction.acceptedClicks,
                    acceptedByAction,
                    violations,
                )
                is MenuInteraction.Branches -> interaction.branches.forEach { branch ->
                    validateAction(
                    definition,
                        observation.slot,
                        branch.actionId,
                        branch.payload,
                        branch.acceptedClicks,
                        acceptedByAction,
                        violations,
                    )
                }
                is MenuInteraction.ClickBranches -> interaction.branches.forEach { branch ->
                    validateInteraction(
                        definition,
                        observation.slot,
                        branch.interaction,
                        context,
                        acceptedByAction,
                        violations,
                    )
                }
                is MenuInteraction.Capability -> {
                    validateCapability(definition, observation.slot, interaction, context, violations)
                }
                is MenuInteraction.Back,
                is MenuInteraction.Unavailable,
                MenuInteraction.DisplayOnly -> Unit
            }
        }

        acceptedByAction.forEach { (actionId, acceptedClicks) ->
            val declared = definition.actionContracts[actionId]?.acceptedClicks.orEmpty()
            if (declared.isNotEmpty() && declared != acceptedClicks) {
                violations += MenuContractViolation(
                    definition.routeId,
                    null,
                    actionId,
                    "accepted click contract differs: declared=$declared rendered=$acceptedClicks",
                )
            }
        }
        return violations
    }

    fun requireValid(definition: InventoryMenuDefinition, view: InventoryMenuView) {
        val violations = validate(definition, view)
        require(violations.isEmpty()) {
            violations.joinToString("; ") { violation ->
                "${violation.routeId} slot=${violation.slot} action=${violation.actionId}: ${violation.message}"
            }
        }
    }

    private fun validateAction(
        definition: InventoryMenuDefinition,
        slot: Int,
        actionId: String,
        payload: Map<String, String>,
        acceptedClicks: Set<ClickType>,
        acceptedByAction: MutableMap<String, MutableSet<ClickType>>,
        violations: MutableList<MenuContractViolation>,
    ) {
        if (actionId !in definition.actions) {
            violations += MenuContractViolation(
                definition.routeId,
                slot,
                actionId,
                "no handler is registered for action id",
            )
            return
        }
        acceptedByAction.getOrPut(actionId) { linkedSetOf() } += acceptedClicks
        val contract = definition.actionContracts[actionId] ?: return
        val missing = contract.requiredPayloadKeys - payload.keys
        if (missing.isNotEmpty()) {
            violations += MenuContractViolation(
                definition.routeId,
                slot,
                actionId,
                "required payload is missing: $missing",
            )
        }
    }

    private fun validateInteraction(
        definition: InventoryMenuDefinition,
        slot: Int,
        interaction: MenuInteraction,
        context: MenuContractValidationContext?,
        acceptedByAction: MutableMap<String, MutableSet<ClickType>>,
        violations: MutableList<MenuContractViolation>,
    ) {
        when (interaction) {
            is MenuInteraction.Action -> validateAction(
                definition,
                slot,
                interaction.actionId,
                interaction.payload,
                interaction.acceptedClicks,
                acceptedByAction,
                violations,
            )
            is MenuInteraction.Capability ->
                validateCapability(definition, slot, interaction, context, violations)
            is MenuInteraction.Unavailable,
            is MenuInteraction.Back,
            MenuInteraction.DisplayOnly -> Unit
            is MenuInteraction.Branches,
            is MenuInteraction.ClickBranches -> error("interaction branches must resolve to a final interaction")
        }
    }

    private fun validateCapability(
        definition: InventoryMenuDefinition,
        slot: Int,
        interaction: MenuInteraction.Capability,
        context: MenuContractValidationContext?,
        violations: MutableList<MenuContractViolation>,
    ) {
        if (interaction.capabilityId.isBlank()) {
            violations += MenuContractViolation(
                definition.routeId,
                slot,
                null,
                "capability interaction must retain a capability id",
            )
            return
        }
        context ?: return
        val capability = try {
            context.capabilities.definition(interaction.capabilityId)
        } catch (failure: Throwable) {
            failure.rethrowIfUnrecoverableMenuRuntimeFailure()
            violations += MenuContractViolation(
                definition.routeId,
                slot,
                null,
                "capability definition lookup failed: ${failure.javaClass.name}",
            )
            return
        }
        if (capability == null) {
            violations += MenuContractViolation(
                definition.routeId,
                slot,
                null,
                "capability is not registered: ${interaction.capabilityId}",
            )
            return
        }
        val staticContract = capability.staticContract()
        val unsupported = interaction.acceptedClicks - staticContract.acceptedClicks
        if (unsupported.isNotEmpty()) {
            violations += MenuContractViolation(
                definition.routeId,
                slot,
                null,
                "capability accepted clicks are not declared by registry: $unsupported",
            )
        }
        val expectedSafetyByClick = interaction.acceptedClicks.associateWith { staticContract.safetyByClick[it] }
        val safetyMismatch = expectedSafetyByClick.any { (click, safety) ->
            safety == null || interaction.safetyFor(click) != safety
        }
        val expectedSafety = expectedSafetyByClick.values.filterNotNull().distinct().singleOrNull()
            ?: MenuActionSafety.UNSPECIFIED
        if (safetyMismatch || interaction.safety != expectedSafety) {
            violations += MenuContractViolation(
                definition.routeId,
                slot,
                null,
                "capability safety contract differs from registry static contract",
            )
        }
    }
}
