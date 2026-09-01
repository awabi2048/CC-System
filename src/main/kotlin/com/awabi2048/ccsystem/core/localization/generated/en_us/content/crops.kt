package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsContentCropsCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "content/crops"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "crops.soybean_seed.name", value = EmbeddedLocalizedValue.Text("&aSoybean Seed"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.soybean_seed.lore", value = EmbeddedLocalizedValue.TextList(listOf("&7Plant on a support to grow")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.soybean.name", value = EmbeddedLocalizedValue.Text("&aSoybean"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.soybean.lore", value = EmbeddedLocalizedValue.TextList(listOf("&7Harvested soybean")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.support.place", value = EmbeddedLocalizedValue.Text("&7Placed a support"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.support.break", value = EmbeddedLocalizedValue.Text("&7Recovered a support"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.plant", value = EmbeddedLocalizedValue.Text("&7Planted soybean"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.harvest", value = EmbeddedLocalizedValue.Text("&aHarvested soybean"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.not_ready", value = EmbeddedLocalizedValue.Text("&cNot ready to harvest"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.already_planted", value = EmbeddedLocalizedValue.Text("&cA crop is already planted here"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.invalid_ground", value = EmbeddedLocalizedValue.Text("&cCannot be placed here"), domain = DOMAIN),
    )
}
