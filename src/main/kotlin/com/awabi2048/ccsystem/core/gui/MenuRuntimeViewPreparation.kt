package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuActionObservation
import com.awabi2048.ccsystem.api.gui.MenuCapabilityService
import com.awabi2048.ccsystem.api.gui.MenuContractValidationContext
import com.awabi2048.ccsystem.api.gui.MenuContractValidator
import com.awabi2048.ccsystem.api.gui.MenuContractViolation
import com.awabi2048.ccsystem.api.gui.MenuRenderContext
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateProviderRegistry
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionResult
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperation
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationFailureReason
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationResult
import com.awabi2048.ccsystem.api.gui.rethrowIfUnrecoverableMenuRuntimeFailure

/**
 * Runtimeがinventoryを作成する前に必ず行う、描画と契約検証の共通処理です。
 * Bukkit APIへ触れないため、open/refreshと同じ契約を単体で検証できます。
 */
internal object MenuRuntimeViewPreparation {
    fun renderValidated(
        definition: InventoryMenuDefinition,
        context: MenuRenderContext,
        capabilities: MenuCapabilityService? = null,
        reversibleProviders: MenuReversibleStateProviderRegistry? = null,
    ): MenuRuntimePreparedViewResult {
        val view = try {
            definition.renderer.render(context)
        } catch (failure: Throwable) {
            failure.rethrowIfUnrecoverableMenuRuntimeFailure()
            // 原因例外を保持して呼び出し側へ渡します。型名だけに縮退すると、
            // 本番で同じ描画失敗が起きた際に原因行を特定できなくなります。
            return MenuRuntimePreparedViewResult.RenderFailed(failure.javaClass.name, failure)
        }
        return contractInvalid(
            definition,
            view.elements.map { element -> MenuActionObservation(element.slot, element.resolvedInteraction()) },
            capabilities?.let { MenuContractValidationContext(it, reversibleProviders) },
        ) ?: MenuRuntimePreparedViewResult.Ready(view)
    }

    fun contractInvalid(
        definition: InventoryMenuDefinition,
        observations: List<MenuActionObservation>,
        context: MenuContractValidationContext? = null,
    ): MenuRuntimePreparedViewResult.ContractInvalid? =
        MenuContractValidator.validate(definition, observations, context)
            .takeIf { violations -> violations.isNotEmpty() }
            ?.let(MenuRuntimePreparedViewResult::ContractInvalid)

    fun inspect(
        definition: InventoryMenuDefinition,
        context: MenuRenderContext,
        capabilities: MenuCapabilityService,
        reversibleProviders: MenuReversibleStateProviderRegistry? = null,
        snapshot: (InventoryMenuView) -> MenuRuntimeInspectionSnapshot,
    ): MenuRuntimeInspectionResult = when (
        val prepared = renderValidated(definition, context, capabilities, reversibleProviders)
    ) {
        is MenuRuntimePreparedViewResult.Ready -> MenuRuntimeInspectionResult(
            MenuRuntimeOperationResult.succeeded(MenuRuntimeOperation.INSPECT, context.route),
            snapshot(prepared.view),
        )
        is MenuRuntimePreparedViewResult.RenderFailed -> MenuRuntimeInspectionResult(
            MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.INSPECT,
                context.route,
                MenuRuntimeOperationFailureReason.RENDER_FAILED,
                exceptionType = prepared.exceptionType,
            ),
        )
        is MenuRuntimePreparedViewResult.ContractInvalid -> MenuRuntimeInspectionResult(
            MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.INSPECT,
                context.route,
                MenuRuntimeOperationFailureReason.CONTRACT_INVALID,
                contractViolations = prepared.violations,
            ),
        )
    }
}

internal sealed interface MenuRuntimePreparedViewResult {
    data class Ready(val view: InventoryMenuView) : MenuRuntimePreparedViewResult

    data class RenderFailed(
        val exceptionType: String,
        val cause: Throwable,
    ) : MenuRuntimePreparedViewResult

    data class ContractInvalid(
        val violations: List<MenuContractViolation>,
    ) : MenuRuntimePreparedViewResult
}
