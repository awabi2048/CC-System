package com.awabi2048.ccsystem.core.localization

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmbeddedLocalizationCatalogTest {
    @Test
    fun `全localeのキー・型・プレースホルダー契約が一致する`() {
        val result = EmbeddedLocalizationCatalog.validate()
        assertTrue(result.isValid, result.errors.joinToString("\n"))
        val generatedKeyErrors = EmbeddedLocalizationCatalog.validateGeneratedKeys()
        assertTrue(generatedKeyErrors.isEmpty(), generatedKeyErrors.joinToString("\n"))
    }

    @Test
    fun `キー不足はビルド時検証を失敗させる`() {
        val result = EmbeddedLocalizationCatalog.validateEntries(
            mapOf(
                "ja_jp" to listOf(entry("required.key", "{name}")),
                "en_us" to emptyList(),
            ),
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.joinToString("\n").contains("required.key がありません"))
    }

    @Test
    fun `値型とプレースホルダーの不一致はビルド時検証を失敗させる`() {
        val typeMismatch = EmbeddedLocalizationCatalog.validateEntries(
            mapOf(
                "ja_jp" to listOf(entry("typed.key", "{name}")),
                "en_us" to listOf(listEntry("typed.key", listOf("{name}"))),
            ),
        )
        assertFalse(typeMismatch.isValid)

        val placeholderMismatch = EmbeddedLocalizationCatalog.validateEntries(
            mapOf(
                "ja_jp" to listOf(entry("placeholder.key", "{name}")),
                "en_us" to listOf(entry("placeholder.key", "{player}")),
            ),
        )
        assertFalse(placeholderMismatch.isValid)
    }

    @Test
    fun `重複キーはビルド時検証を失敗させる`() {
        val duplicate = entry("duplicate.key", "value")
        val result = EmbeddedLocalizationCatalog.validateEntries(
            mapOf("ja_jp" to listOf(duplicate, duplicate), "en_us" to listOf(duplicate)),
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.joinToString("\n").contains("重複"))
    }

    private fun entry(key: String, value: String) = EmbeddedLocalizationEntry(
        key,
        EmbeddedLocalizedValue.Text(value),
        "test",
    )

    private fun listEntry(key: String, value: List<String>) = EmbeddedLocalizationEntry(
        key,
        EmbeddedLocalizedValue.TextList(value),
        "test",
    )
}
