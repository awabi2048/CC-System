package com.awabi2048.ccsystem.api.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LocalizationCatalogResolverTest {
    @Test
    fun `external text reference resolves to its generated key contract`() {
        val key = LocalizationCatalogContract.resolveText("general.player_only")
        assertEquals("general.player_only", key.id)
        assertEquals(LocalizationKey.ValueType.TEXT, key.valueType)
    }

    @Test
    fun `unknown and mismatched external references fail during loading`() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalizationCatalogContract.resolveText("missing.external.key")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LocalizationCatalogContract.resolveText("kantan_commander_clean.gui.editor.add_description")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LocalizationCatalogContract.resolveTextList("general.player_only")
        }
    }
}
