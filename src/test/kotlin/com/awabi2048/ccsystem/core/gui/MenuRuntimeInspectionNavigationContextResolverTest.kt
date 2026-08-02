package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuRuntimeInspectionNavigationContextResolverTest {
    private val root = MenuRoute("test", "root")
    private val current = MenuRoute("test", "current")
    private val target = MenuRoute("test", "target")

    @Test
    fun `root never inherits a player's existing history`() {
        val context = MenuRuntimeInspectionNavigationContextResolver.resolve(
            MenuRuntimeInspectionMode.ROOT,
            current,
            listOf(root),
            target,
        )!!

        assertFalse(context.canGoBack)
        assertEquals(emptyList<MenuRoute>(), context.breadcrumbs)
    }

    @Test
    fun `navigate adds current route while replace and ephemeral preserve only existing history`() {
        val history = listOf(root)
        val navigate = MenuRuntimeInspectionNavigationContextResolver.resolve(
            MenuRuntimeInspectionMode.NAVIGATE,
            current,
            history,
            target,
        )!!
        val replace = MenuRuntimeInspectionNavigationContextResolver.resolve(
            MenuRuntimeInspectionMode.REPLACE,
            current,
            history,
            target,
        )!!
        val ephemeral = MenuRuntimeInspectionNavigationContextResolver.resolve(
            MenuRuntimeInspectionMode.EPHEMERAL,
            current,
            history,
            target,
        )!!

        assertEquals(listOf(root, current), navigate.breadcrumbs)
        assertTrue(navigate.canGoBack)
        assertEquals(history, replace.breadcrumbs)
        assertEquals(history, ephemeral.breadcrumbs)
    }

    @Test
    fun `current rejects an unrelated inspect route instead of borrowing history`() {
        assertNull(
            MenuRuntimeInspectionNavigationContextResolver.resolve(
                MenuRuntimeInspectionMode.CURRENT,
                current,
                listOf(root),
                target,
            ),
        )
    }
}
