package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsMyworldPublishLevelCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "myworld/publish_level"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "publish_level.public", value = EmbeddedLocalizedValue.Text("Public"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.friend", value = EmbeddedLocalizedValue.Text("Limited"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.private", value = EmbeddedLocalizedValue.Text("Private"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.locked", value = EmbeddedLocalizedValue.Text("Locked"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.description.public", value = EmbeddedLocalizedValue.Text("§fAnyone can visit. Listed in the discovery menu."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.description.friend", value = EmbeddedLocalizedValue.Text("§fAnyone can visit, but not listed in discovery menu."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.description.private", value = EmbeddedLocalizedValue.Text("§fOnly invited players can visit."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "publish_level.description.locked", value = EmbeddedLocalizedValue.Text("§fNon-members cannot visit, and portal warp is disabled."), domain = DOMAIN),
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
