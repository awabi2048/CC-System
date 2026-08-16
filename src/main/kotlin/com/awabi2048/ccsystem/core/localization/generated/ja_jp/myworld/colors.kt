package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpMyworldColorsCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "myworld/colors"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "colors.white", value = EmbeddedLocalizedValue.Text("白色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.silver", value = EmbeddedLocalizedValue.Text("薄灰色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.gray", value = EmbeddedLocalizedValue.Text("灰色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.black", value = EmbeddedLocalizedValue.Text("黒"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.red", value = EmbeddedLocalizedValue.Text("赤"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.maroon", value = EmbeddedLocalizedValue.Text("濃い赤色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.yellow", value = EmbeddedLocalizedValue.Text("黄色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.olive", value = EmbeddedLocalizedValue.Text("濃い黄色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.lime", value = EmbeddedLocalizedValue.Text("黄緑色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.green", value = EmbeddedLocalizedValue.Text("緑色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.aqua", value = EmbeddedLocalizedValue.Text("空色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.teal", value = EmbeddedLocalizedValue.Text("水色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.blue", value = EmbeddedLocalizedValue.Text("青"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.navy", value = EmbeddedLocalizedValue.Text("紺色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.fuchsia", value = EmbeddedLocalizedValue.Text("赤紫色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.purple", value = EmbeddedLocalizedValue.Text("紫色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "colors.orange", value = EmbeddedLocalizedValue.Text("オレンジ色"), domain = DOMAIN),
    )

}
