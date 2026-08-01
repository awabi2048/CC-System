package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuActionObservation
import com.awabi2048.ccsystem.api.gui.MenuContractValidator
import com.awabi2048.ccsystem.api.gui.MenuContractViolation
import com.awabi2048.ccsystem.api.gui.MenuRenderContext

/**
 * Runtimeがinventoryを作成する前に必ず行う、描画と契約検証の共通処理です。
 * Bukkit APIへ触れないため、open/refreshと同じ契約を単体で検証できます。
 */
internal object MenuRuntimeViewPreparation {
    fun renderValidated(
        definition: InventoryMenuDefinition,
        context: MenuRenderContext,
    ): MenuRuntimePreparedViewResult {
        val view = try {
            definition.renderer.render(context)
        } catch (failure: Throwable) {
            return MenuRuntimePreparedViewResult.RenderFailed(failure.javaClass.name)
        }
        return contractInvalid(definition, view.elements.map { element ->
            MenuActionObservation(element.slot, element.resolvedInteraction())
        }) ?: MenuRuntimePreparedViewResult.Ready(view)
    }

    fun contractInvalid(
        definition: InventoryMenuDefinition,
        observations: List<MenuActionObservation>,
    ): MenuRuntimePreparedViewResult.ContractInvalid? =
        MenuContractValidator.validate(definition, observations)
            .takeIf { violations -> violations.isNotEmpty() }
            ?.let(MenuRuntimePreparedViewResult::ContractInvalid)
}

internal sealed interface MenuRuntimePreparedViewResult {
    data class Ready(val view: InventoryMenuView) : MenuRuntimePreparedViewResult

    data class RenderFailed(val exceptionType: String) : MenuRuntimePreparedViewResult

    data class ContractInvalid(
        val violations: List<MenuContractViolation>,
    ) : MenuRuntimePreparedViewResult
}
