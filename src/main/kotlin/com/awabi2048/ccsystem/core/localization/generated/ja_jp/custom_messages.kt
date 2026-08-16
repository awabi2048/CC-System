package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpCustomMessagesCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "custom_messages"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "custom_messages.example_message.style", value = EmbeddedLocalizedValue.Text("random"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_messages.example_message.texts", value = EmbeddedLocalizedValue.TextList(listOf("§aメッセージ例1", "§bメッセージ例2", "§cメッセージ例3")), domain = DOMAIN),
    )

}
