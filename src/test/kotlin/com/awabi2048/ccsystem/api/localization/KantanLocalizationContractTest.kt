package com.awabi2048.ccsystem.api.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KantanLocalizationContractTest {
    @Test
    fun `Kantan GUI contract retains every typed key`() {
        val keys = LocalizationCatalogContract.keys()
            .filter { it.startsWith("kantan_commander_clean.") }

        assertEquals(438, keys.size)
        assertEquals(
            "cbff780f12f1894767b80a4f46a00a736e1f210633a67a89b25dc4cdf7d59a64",
            LocalizationCatalogContract.fingerprint("kantan_commander_clean"),
        )
        assertEquals(
            LocalizationKey.ValueType.TEXT_LIST,
            LocalizationCatalogContract.valueType("kantan_commander_clean.gui.editor.add_description"),
        )
    }
}
