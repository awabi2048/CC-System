package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsChanponDiscoveryCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "chanpon/discovery"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.discovery.title", value = EmbeddedLocalizedValue.Text("World Discovery"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.discovery.info.name", value = EmbeddedLocalizedValue.Text("World Discovery"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.discovery.info.lore", value = EmbeddedLocalizedValue.TextList(listOf("A list of worlds you have not visited yet.", "Worlds are ordered by earliest completion.")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.discovery.no_result", value = EmbeddedLocalizedValue.Text("There are no worlds you have not visited yet."), domain = DOMAIN),
    )

}
