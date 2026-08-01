package com.awabi2048.ccsystem.api.gui

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuRuntimeLegacyAbiTest {
    @Test
    fun `former data classes retain copy default component and value methods`() {
        assertLegacyDataMethods(MenuInteraction.Action::class.java, 9)
        assertLegacyDataMethods(MenuInteraction.Capability::class.java, 9)
        assertLegacyDataMethods(MenuInteraction.Branches::class.java, 2)
        assertLegacyDataMethods(MenuInteraction.ClickBranches::class.java, 1)
        assertLegacyDataMethods(MenuActionBranch::class.java, 5)
    }

    private fun assertLegacyDataMethods(type: Class<*>, components: Int) {
        val names = type.declaredMethods.map { it.name }.toSet()
        assertTrue("copy" in names, "${type.name} copy descriptor is missing")
        assertTrue("copy\$default" in names, "${type.name} copy default descriptor is missing")
        (1..components).forEach { assertTrue("component$it" in names, "${type.name} component$it is missing") }
        listOf("equals", "hashCode", "toString").forEach { assertTrue(it in names, "${type.name} $it is missing") }
    }
}
