package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpChanponMemberLoreCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "chanpon/member_lore"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.last_online_label", value = EmbeddedLocalizedValue.Text("最終オンライン"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.online", value = EmbeddedLocalizedValue.Text("オンライン"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.unknown", value = EmbeddedLocalizedValue.Text("不明"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.tool_permission_title", value = EmbeddedLocalizedValue.Text("ツールの使用権限"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.none", value = EmbeddedLocalizedValue.Text("なし"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.title", value = EmbeddedLocalizedValue.Text("メンバーの権限設定"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.state_label", value = EmbeddedLocalizedValue.Text("状態"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.enabled", value = EmbeddedLocalizedValue.Text("オン"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.disabled", value = EmbeddedLocalizedValue.Text("オフ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.toggle_action", value = EmbeddedLocalizedValue.Text("切り替え"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.back", value = EmbeddedLocalizedValue.Text("戻る"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.role.display", value = EmbeddedLocalizedValue.Text("メンバー・モデレーター"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.role.state_label", value = EmbeddedLocalizedValue.Text("現在の権限"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.role.member", value = EmbeddedLocalizedValue.Text("メンバー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.role.moderator", value = EmbeddedLocalizedValue.Text("モデレーター"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.permission_menu.role.action", value = EmbeddedLocalizedValue.Text("権限を切り替える"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.click_edit_action", value = EmbeddedLocalizedValue.Text("メニューを開く"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.member_lore.tool_permission_operation", value = EmbeddedLocalizedValue.Text("権限を設定する"), domain = DOMAIN),
    )

}
