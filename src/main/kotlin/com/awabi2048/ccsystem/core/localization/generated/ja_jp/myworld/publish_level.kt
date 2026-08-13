package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpMyworldPublishLevelCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "myworld/publish_level"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "publish_level.public", value = EmbeddedLocalizedValue.Text("公開"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.friend", value = EmbeddedLocalizedValue.Text("限定公開"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.private", value = EmbeddedLocalizedValue.Text("非公開"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.locked", value = EmbeddedLocalizedValue.Text("封鎖中"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.description.public", value = EmbeddedLocalizedValue.Text("§f誰でも訪問可能です。ディスカバリーメニューに掲載されます。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.description.friend", value = EmbeddedLocalizedValue.Text("§f誰でも訪問可能ですが、ディスカバリーメニューには掲載されません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.description.private", value = EmbeddedLocalizedValue.Text("§fメンバーが招待した人のみ訪問可能です。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.description.locked", value = EmbeddedLocalizedValue.Text("§fメンバー以外は訪問できず、ポータルからのワープもできません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.public", value = EmbeddedLocalizedValue.Text("§a"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.friend", value = EmbeddedLocalizedValue.Text("§e"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.private", value = EmbeddedLocalizedValue.Text("§c"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.locked", value = EmbeddedLocalizedValue.Text("§c"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.owner", value = EmbeddedLocalizedValue.Text("§6"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.moderator", value = EmbeddedLocalizedValue.Text("§b"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.member", value = EmbeddedLocalizedValue.Text("§f"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.online", value = EmbeddedLocalizedValue.Text("§a"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.offline", value = EmbeddedLocalizedValue.Text("§c"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.active", value = EmbeddedLocalizedValue.Text("§a"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.inactive", value = EmbeddedLocalizedValue.Text("§7"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.uuid", value = EmbeddedLocalizedValue.Text("§8"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.color.debug", value = EmbeddedLocalizedValue.Text("§8"), domain = DOMAIN),
    )

}
