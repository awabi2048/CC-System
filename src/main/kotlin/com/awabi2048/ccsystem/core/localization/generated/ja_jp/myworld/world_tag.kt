package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpMyworldWorldTagCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "myworld/world_tag"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "world_tag.shop", value = EmbeddedLocalizedValue.Text("ショップ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "world_tag.minigame", value = EmbeddedLocalizedValue.Text("ミニゲーム"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "world_tag.building", value = EmbeddedLocalizedValue.Text("建築"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "world_tag.facility", value = EmbeddedLocalizedValue.Text("共用施設"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "world_tag.streaming", value = EmbeddedLocalizedValue.Text("配信・撮影歓迎"), domain = DOMAIN),
    )

}
