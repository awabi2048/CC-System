package com.awabi2048.ccsystem.core.localization

import com.awabi2048.ccsystem.api.localization.LocalizationCatalogContract
import com.awabi2048.ccsystem.api.localization.LocalizationKey
import com.awabi2048.ccsystem.api.localization.generated.ContentCropsKeys
import com.awabi2048.ccsystem.core.localization.generated.GeneratedLocalizationCatalogIndex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentCropsLocalizationContractTest {
    @Test
    fun `Cropsの公開キーとja en生成カタログが全件一致する`() {
        val expectedTypes = linkedMapOf(
            "crops.soybean_seed.name" to LocalizationKey.ValueType.TEXT,
            "crops.soybean_seed.lore" to LocalizationKey.ValueType.TEXT_LIST,
            "crops.soybean.name" to LocalizationKey.ValueType.TEXT,
            "crops.soybean.lore" to LocalizationKey.ValueType.TEXT_LIST,
            "crops.support.place" to LocalizationKey.ValueType.TEXT,
            "crops.support.break" to LocalizationKey.ValueType.TEXT,
            "crops.plant" to LocalizationKey.ValueType.TEXT,
            "crops.harvest" to LocalizationKey.ValueType.TEXT,
            "crops.not_ready" to LocalizationKey.ValueType.TEXT,
            "crops.already_planted" to LocalizationKey.ValueType.TEXT,
            "crops.invalid_ground" to LocalizationKey.ValueType.TEXT,
        )

        // 公開APIのキー索引に全件があり、型付きキーのall()から欠落していないことを同時に検証します。
        assertEquals(expectedTypes.keys, ContentCropsKeys.all().map { it.id }.toSet())
        assertEquals(expectedTypes.keys, LocalizationCatalogContract.keys().filter { it.startsWith("crops.") }.toSet())
        expectedTypes.forEach { (key, valueType) ->
            assertEquals(valueType, LocalizationCatalogContract.valueType(key), key)
        }

        // locale索引を直接確認し、片方だけに登録された生成カタログを許しません。
        val indexedEntries = GeneratedLocalizationCatalogIndex.entriesByLocale()
        for (locale in listOf("ja_jp", "en_us")) {
            val cropEntries = indexedEntries.getValue(locale).filter { it.domain == "content/crops" }
            assertEquals(expectedTypes.keys, cropEntries.map { it.key }.toSet(), "locale=$locale")
            expectedTypes.keys.forEach { key ->
                val value = EmbeddedLocalizationCatalog.value(locale, key)
                assertNotNull(value, "locale=$locale key=$key")
                val matchesType = when (expectedTypes.getValue(key)) {
                    LocalizationKey.ValueType.TEXT -> value is EmbeddedLocalizedValue.Text
                    LocalizationKey.ValueType.TEXT_LIST -> value is EmbeddedLocalizedValue.TextList
                }
                assertTrue(matchesType, "locale=$locale key=$key の値型が不正です")
            }
        }
    }
}
