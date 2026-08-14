package com.awabi2048.ccsystem.core.localization

import com.awabi2048.ccsystem.api.localization.generated.MyworldBiomesKeys
import java.lang.reflect.Modifier
import org.bukkit.block.Biome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmbeddedLocalizationCatalogTest {
    @Test
    fun `Paperの全biome IDが生成済みカタログと一致する`() {
        // 実サーバーで新biomeに初めて触れた時ではなく、Paper API更新時のビルドで差分を検出します。
        val registryIds = Biome::class.java.fields
            .filter { Modifier.isStatic(it.modifiers) && it.type == Biome::class.java && it.name != "CUSTOM" }
            .map { it.name.lowercase() }
            .toSet()
        val catalogIds = MyworldBiomesKeys.all()
            .map { it.id.removePrefix("biomes.") }
            .toSet()

        assertEquals(registryIds, catalogIds)
    }

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
