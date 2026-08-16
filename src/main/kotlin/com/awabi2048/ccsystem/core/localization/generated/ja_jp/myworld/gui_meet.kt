package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpMyworldGuiMeetCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "myworld/gui_meet"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "gui.meet.title", value = EmbeddedLocalizedValue.Text("{player}のところに行く"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.title_list", value = EmbeddedLocalizedValue.Text("プレイヤーのところに行く"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.empty_message", value = EmbeddedLocalizedValue.Text("§7表示できるプレイヤーはいません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.current_world", value = EmbeddedLocalizedValue.Text("現在のワールド"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.same_world", value = EmbeddedLocalizedValue.Text("このワールド"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.status", value = EmbeddedLocalizedValue.Text("ステータス"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.online_state", value = EmbeddedLocalizedValue.Text("接続状態"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.online", value = EmbeddedLocalizedValue.Text("オンライン"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.offline", value = EmbeddedLocalizedValue.Text("オフライン"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.click_visit", value = EmbeddedLocalizedValue.Text("このワールドへワープ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.click_request", value = EmbeddedLocalizedValue.Text("ワープ申請を送信"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.status_button.display", value = EmbeddedLocalizedValue.Text("§e{player}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.status_button.current", value = EmbeddedLocalizedValue.Text("現在のステータス"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.status_button.next", value = EmbeddedLocalizedValue.Text("次のステータス"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.status_button.action", value = EmbeddedLocalizedValue.Text("切り替え"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_confirm.title", value = EmbeddedLocalizedValue.Text("メンバー参加申請の確認"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_confirm.confirm", value = EmbeddedLocalizedValue.Text("§a申請を送信"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_confirm.cancel", value = EmbeddedLocalizedValue.Text("§cキャンセル"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fワールド「§a{world}§f」のオーナーに", "§dメンバー参加申請§fを送信しますか？", "§7オーナーが承認すると、メンバーとして参加できます。")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_owner_confirm.title", value = EmbeddedLocalizedValue.Text("メンバー参加申請の承認"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_owner_confirm.confirm", value = EmbeddedLocalizedValue.Text("§a承認する"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_owner_confirm.reject", value = EmbeddedLocalizedValue.Text("§c却下する"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_owner_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fプレイヤー「§a{player}§f」からの", "§dメンバー参加申請§fを承認しますか？", "§7承認すると、このプレイヤーはワールドのメンバーになります。")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_invite_accept_confirm.title", value = EmbeddedLocalizedValue.Text("メンバー招待の確認"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_invite_accept_confirm.confirm", value = EmbeddedLocalizedValue.Text("§a承諾する"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_invite_accept_confirm.cancel", value = EmbeddedLocalizedValue.Text("§c辞退する"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_invite_accept_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fプレイヤー「§a{player}§f」からの", "§dメンバー招待§fを承諾しますか？", "§7対象ワールド: §a{world}")), domain = DOMAIN),
    )

}
