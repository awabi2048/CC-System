package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpContentCropsCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "content/crops"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "crops.soybean_seed.name", value = EmbeddedLocalizedValue.Text("&a大豆の種"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.soybean_seed.lore", value = EmbeddedLocalizedValue.TextList(listOf("&7支柱に植えて栽培する")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.soybean.name", value = EmbeddedLocalizedValue.Text("&a大豆"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.soybean.lore", value = EmbeddedLocalizedValue.TextList(listOf("&7収穫した大豆")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.support.place", value = EmbeddedLocalizedValue.Text("&7支柱を設置した"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.support.break", value = EmbeddedLocalizedValue.Text("&7支柱を回収した"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.plant", value = EmbeddedLocalizedValue.Text("&7大豆を植えた"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.harvest", value = EmbeddedLocalizedValue.Text("&a大豆を収穫した"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.not_ready", value = EmbeddedLocalizedValue.Text("&cまだ収穫できない"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.already_planted", value = EmbeddedLocalizedValue.Text("&cすでに作物が植えられている"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "crops.invalid_ground", value = EmbeddedLocalizedValue.Text("&cここには設置できない"), domain = DOMAIN),
    )
}
