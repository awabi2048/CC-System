package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpMyworldGuiFavoriteCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "myworld/gui_favorite"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "gui.favorite.title", value = EmbeddedLocalizedValue.Text("お気に入りリスト"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.empty_message", value = EmbeddedLocalizedValue.Text("§7フィルターに一致するお気に入りはありません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.empty_message_no_favorites", value = EmbeddedLocalizedValue.Text("§7まだお気に入りがありません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.current_world.name", value = EmbeddedLocalizedValue.Text("§b現在のワールド"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.current_world.unmanaged", value = EmbeddedLocalizedValue.Text("§eマイワールド管理対象外のワールドです"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.warp", value = EmbeddedLocalizedValue.Text("このワールドへワープ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.preview", value = EmbeddedLocalizedValue.Text("このワールドをプレビュー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.unfavorite", value = EmbeddedLocalizedValue.Text("お気に入りを解除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.open_actions", value = EmbeddedLocalizedValue.Text("操作メニューを開く"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.direct_warp_unavailable", value = EmbeddedLocalizedValue.Text("§e公開設定などにより、このワールドへ直接ワープできません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.archived_label", value = EmbeddedLocalizedValue.Text("§c【アーカイブされています】"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.info.display", value = EmbeddedLocalizedValue.Text("§f統計情報"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.player_icon.name", value = EmbeddedLocalizedValue.Text("§b{player}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.player_icon.lore_count", value = EmbeddedLocalizedValue.Text("お気に入りしたワールド数"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.other_worlds.name", value = EmbeddedLocalizedValue.Text("§aこのプレイヤーのほかのワールド"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.other_worlds.lore", value = EmbeddedLocalizedValue.TextList(listOf("§7このワールドのオーナーが所有する", "§7ほかのワールド一覧を表示します。")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.other_worlds.action", value = EmbeddedLocalizedValue.Text("ほかのワールド一覧を表示"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.name_add", value = EmbeddedLocalizedValue.Text("§bお気に入りに追加"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.name_remove", value = EmbeddedLocalizedValue.Text("§cお気に入りから削除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.name_restricted", value = EmbeddedLocalizedValue.Text("§7お気に入り登録不可"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.lore_add", value = EmbeddedLocalizedValue.Text("§7このワールドをお気に入りリストに追加します。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.lore_remove", value = EmbeddedLocalizedValue.Text("§7このワールドをお気に入りリストから削除します。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.lore_restricted_owner", value = EmbeddedLocalizedValue.Text("§c自分のワールドはお気に入り登録できません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.lore_restricted_not_managed", value = EmbeddedLocalizedValue.Text("§cこの場所はお気に入り登録できません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.lore_restricted_member", value = EmbeddedLocalizedValue.Text("§cメンバーとして参加中のワールドはお気に入り登録できません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.action", value = EmbeddedLocalizedValue.Text("お気に入りを切り替え"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.remove_confirm.title", value = EmbeddedLocalizedValue.Text("お気に入り解除の確認"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.remove_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fワールド「§a{world}§f」を", "§cお気に入りから解除§fしますか？", "§7解除すると、お気に入りリストから削除されます。")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.remove_confirm.confirm", value = EmbeddedLocalizedValue.Text("§aお気に入り解除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.title", value = EmbeddedLocalizedValue.Text("{world} の操作"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.warp", value = EmbeddedLocalizedValue.Text("§aワールドにワープ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.preview", value = EmbeddedLocalizedValue.Text("§bワールドをプレビュー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.invite", value = EmbeddedLocalizedValue.Text("§eこのワールドに他のプレイヤーを招待"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.invite_description", value = EmbeddedLocalizedValue.TextList(listOf("現在同じワールドにいるプレイヤーを", "このお気に入りワールドへ招待します。")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.invite_recipient_count", value = EmbeddedLocalizedValue.Text("招待できるプレイヤー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.invite_unavailable", value = EmbeddedLocalizedValue.Text("§cこのワールドへ一括招待できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.invite_no_recipients", value = EmbeddedLocalizedValue.Text("§e現在招待できるプレイヤーはいません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.unfavorite", value = EmbeddedLocalizedValue.Text("§cお気に入りを解除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.invite_confirm.title", value = EmbeddedLocalizedValue.Text("一括招待の確認"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.invite_confirm.recipient_count", value = EmbeddedLocalizedValue.Text("招待するプレイヤー数"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.invite_confirm.confirm", value = EmbeddedLocalizedValue.Text("§a招待を送信"), domain = DOMAIN),
    )

}
