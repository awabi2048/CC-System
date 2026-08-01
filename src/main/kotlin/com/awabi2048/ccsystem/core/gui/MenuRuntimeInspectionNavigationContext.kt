package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionMode

/** inspect用に遷移操作後の履歴を純粋に組み立てます。 */
internal data class MenuRuntimeInspectionNavigationContext(
    val breadcrumbs: List<MenuRoute>,
) {
    val canGoBack: Boolean
        get() = breadcrumbs.isNotEmpty()
}

internal object MenuRuntimeInspectionNavigationContextResolver {
    fun resolve(
        mode: MenuRuntimeInspectionMode,
        current: MenuRoute?,
        breadcrumbs: List<MenuRoute>,
        target: MenuRoute,
    ): MenuRuntimeInspectionNavigationContext? {
        val virtualBreadcrumbs = when (mode) {
            MenuRuntimeInspectionMode.ROOT -> emptyList()
            MenuRuntimeInspectionMode.CURRENT -> {
                if (current != target) return null
                breadcrumbs
            }
            MenuRuntimeInspectionMode.REPLACE,
            MenuRuntimeInspectionMode.EPHEMERAL -> breadcrumbs
            MenuRuntimeInspectionMode.NAVIGATE -> current?.let { breadcrumbs + it }.orEmpty()
        }
        return MenuRuntimeInspectionNavigationContext(virtualBreadcrumbs)
    }
}
