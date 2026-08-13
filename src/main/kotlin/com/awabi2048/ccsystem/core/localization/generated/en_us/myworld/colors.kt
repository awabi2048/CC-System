package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsMyworldColorsCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "myworld/colors"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "colors.white", value = EmbeddedLocalizedValue.Text("white"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.silver", value = EmbeddedLocalizedValue.Text("light gray"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.gray", value = EmbeddedLocalizedValue.Text("gray"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.black", value = EmbeddedLocalizedValue.Text("black"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.red", value = EmbeddedLocalizedValue.Text("red"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.maroon", value = EmbeddedLocalizedValue.Text("dark red"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.yellow", value = EmbeddedLocalizedValue.Text("yellow"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.olive", value = EmbeddedLocalizedValue.Text("dark yellow"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.lime", value = EmbeddedLocalizedValue.Text("yellow green"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.green", value = EmbeddedLocalizedValue.Text("green"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.aqua", value = EmbeddedLocalizedValue.Text("sky blue"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.teal", value = EmbeddedLocalizedValue.Text("light blue"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.blue", value = EmbeddedLocalizedValue.Text("blue"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.navy", value = EmbeddedLocalizedValue.Text("dark blue"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.fuchsia", value = EmbeddedLocalizedValue.Text("reddish purple"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.purple", value = EmbeddedLocalizedValue.Text("violet"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.orange", value = EmbeddedLocalizedValue.Text("orange color"), domain = DOMAIN),
    )

}
