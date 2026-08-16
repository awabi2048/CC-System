package com.awabi2048.ccsystem.api.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KantanLocalizationContractTest {
    @Test
    fun `Kantan GUI contract retains every typed key`() {
        val keys = LocalizationCatalogContract.keys()
            .filter { it.startsWith("kantan_commander_clean.") }

        assertEquals(442, keys.size)
        assertEquals(
            "ea14c14266e453194bc8057063bc9a63578e5e2c54207b20282688c7b52aafda",
            LocalizationCatalogContract.fingerprint("kantan_commander_clean"),
        )
        assertEquals(
            LocalizationKey.ValueType.TEXT_LIST,
            LocalizationCatalogContract.valueType("kantan_commander_clean.gui.editor.add_description"),
        )
    }
}
