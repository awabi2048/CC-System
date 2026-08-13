package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsMyworldRoleCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "myworld/role"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "role.owner", value = EmbeddedLocalizedValue.Text("Owner"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "role.moderator", value = EmbeddedLocalizedValue.Text("Moderator"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "role.member", value = EmbeddedLocalizedValue.Text("Member"), domain = DOMAIN),
    )

}
