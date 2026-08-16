package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsChanponMemberLoreCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "chanpon/member_lore"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.last_online_label", value = EmbeddedLocalizedValue.Text("Last Online"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.online", value = EmbeddedLocalizedValue.Text("Online"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.unknown", value = EmbeddedLocalizedValue.Text("Unknown"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.tool_permission_title", value = EmbeddedLocalizedValue.Text("Tool Permissions"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.none", value = EmbeddedLocalizedValue.Text("None"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.title", value = EmbeddedLocalizedValue.Text("Member Permissions"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.state_label", value = EmbeddedLocalizedValue.Text("State"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.enabled", value = EmbeddedLocalizedValue.Text("On"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.disabled", value = EmbeddedLocalizedValue.Text("Off"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.toggle_action", value = EmbeddedLocalizedValue.Text("Toggle"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.back", value = EmbeddedLocalizedValue.Text("Back"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.role.display", value = EmbeddedLocalizedValue.Text("Member / Moderator"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.role.state_label", value = EmbeddedLocalizedValue.Text("Current role"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.role.member", value = EmbeddedLocalizedValue.Text("Member"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.role.moderator", value = EmbeddedLocalizedValue.Text("Moderator"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.role.action", value = EmbeddedLocalizedValue.Text("Change role"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.click_edit_action", value = EmbeddedLocalizedValue.Text("Open menu"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.tool_permission_operation", value = EmbeddedLocalizedValue.Text("Configure permissions"), domain = DOMAIN),
    )

}
