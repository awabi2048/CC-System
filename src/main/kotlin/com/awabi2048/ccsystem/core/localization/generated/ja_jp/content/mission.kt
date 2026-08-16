package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpContentMissionCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "content/mission"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "mission.accept.success", value = EmbeddedLocalizedValue.Text("&aミッションを受注しました！"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "mission.complete.success", value = EmbeddedLocalizedValue.Text("&a&lクエスト達成！"), domain = DOMAIN),
    )

}
