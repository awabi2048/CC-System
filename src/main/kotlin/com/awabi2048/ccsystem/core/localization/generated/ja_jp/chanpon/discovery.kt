package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpChanponDiscoveryCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "chanpon/discovery"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.discovery.title", value = EmbeddedLocalizedValue.Text("ワールドディスカバリー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.discovery.info.name", value = EmbeddedLocalizedValue.Text("ワールドディスカバリー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.discovery.info.lore", value = EmbeddedLocalizedValue.TextList(listOf("まだ訪れたことのないワールドの一覧です", "完成が早い順に並んでいます")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.discovery.no_result", value = EmbeddedLocalizedValue.Text("まだ訪れたことのないワールドはありません"), domain = DOMAIN),
    )

}
