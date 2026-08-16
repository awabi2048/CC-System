package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsMyworldWorldTagCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "myworld/world_tag"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "world_tag.shop", value = EmbeddedLocalizedValue.Text("Shop"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "world_tag.minigame", value = EmbeddedLocalizedValue.Text("Minigame"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "world_tag.building", value = EmbeddedLocalizedValue.Text("Building"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "world_tag.facility", value = EmbeddedLocalizedValue.Text("Facility"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "world_tag.streaming", value = EmbeddedLocalizedValue.Text("Streaming/Recording Welcome"), domain = DOMAIN),
    )

}
