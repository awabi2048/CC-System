package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

/** 副作用なしのRuntime inspectで返す、完成viewの論理snapshotです。 */
data class MenuRuntimeInspectionSnapshot(
    val route: MenuRuntimeRouteSnapshot,
    val breadcrumbs: List<MenuRuntimeRouteSnapshot>,
    val canGoBack: Boolean,
    val title: Component,
    val size: Int,
    val revision: Long,
    val slots: List<MenuRuntimeInspectionSlotSnapshot>,
)

/** inspect時点の要素slotです。標準frameの未配置slotは含めません。 */
data class MenuRuntimeInspectionSlotSnapshot(
    val slot: Int,
    val material: Material,
    val amount: Int,
    val name: Component?,
    val lore: List<Component>,
    val glint: Boolean,
    val role: GuiElementRole,
    val enabled: Boolean,
    val interaction: MenuRuntimeInspectionInteractionSnapshot,
)

/** 実行時に解決されるinteraction契約を、attributesを含めて保持する診断型です。 */
data class MenuRuntimeInspectionInteractionSnapshot(
    val kind: MenuRuntimeInteractionKind,
    val actionId: String? = null,
    val capabilityId: String? = null,
    val arguments: Map<String, String> = emptyMap(),
    val attributes: Map<String, Any> = emptyMap(),
    val acceptedClicks: Set<ClickType> = emptySet(),
    val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
    val safetyByClick: Map<ClickType, MenuActionSafety> = emptyMap(),
    val branches: List<MenuRuntimeInspectionInteractionBranchSnapshot> = emptyList(),
)

data class MenuRuntimeInspectionInteractionBranchSnapshot(
    val acceptedClicks: Set<ClickType>,
    val interaction: MenuRuntimeInspectionInteractionSnapshot,
)

/** inspectの詳細結果です。失敗時は[operationResult]のfailureに診断を保持します。 */
data class MenuRuntimeInspectionResult(
    val operationResult: MenuRuntimeOperationResult,
    val snapshot: MenuRuntimeInspectionSnapshot? = null,
) {
    init {
        require(operationResult.operation == MenuRuntimeOperation.INSPECT) {
            "inspection result must use INSPECT operation"
        }
        require(operationResult.successful == (snapshot != null)) {
            "successful inspection must have a snapshot"
        }
    }
}
