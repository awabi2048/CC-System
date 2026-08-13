package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpMyworldGuiDiscoveryCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "myworld/gui_discovery"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "gui.discovery.title", value = EmbeddedLocalizedValue.Text("ワールドディスカバリー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.warp_hint", value = EmbeddedLocalizedValue.Text("このワールドへワープ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.preview_hint", value = EmbeddedLocalizedValue.Text("プレビューを表示"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.member_request_hint", value = EmbeddedLocalizedValue.Text("参加申請を表示"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.favorite_hint_add", value = EmbeddedLocalizedValue.Text("お気に入りに追加"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.favorite_hint_remove", value = EmbeddedLocalizedValue.Text("お気に入りを解除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.favorite_toggle", value = EmbeddedLocalizedValue.Text("お気に入りを切り替え"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.fav_add", value = EmbeddedLocalizedValue.Text("お気に入りに追加"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.fav_remove", value = EmbeddedLocalizedValue.Text("お気に入りを解除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.spotlight_remove", value = EmbeddedLocalizedValue.Text("SPOTLIGHTから削除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_empty.name", value = EmbeddedLocalizedValue.Text("§7« 未登録のSPOTLIGHT »"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_empty.description", value = EmbeddedLocalizedValue.Text("現在このスロットにはワールドが登録されていません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_empty.action.register", value = EmbeddedLocalizedValue.Text("現在のマイワールドを登録"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.display", value = EmbeddedLocalizedValue.Text("§a並び替え方法の選択"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.label", value = EmbeddedLocalizedValue.Text("並び順"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.name", value = EmbeddedLocalizedValue.Text("§a並び替え方法の選択"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.action.previous", value = EmbeddedLocalizedValue.Text("前の並び順へ変更"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.action.next", value = EmbeddedLocalizedValue.Text("次の並び順へ変更"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.action.edit_spotlight", value = EmbeddedLocalizedValue.Text("SPOTLIGHTの説明文を編集"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.type.hot", value = EmbeddedLocalizedValue.Text("§cHOT"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.type.new", value = EmbeddedLocalizedValue.Text("§aNEW"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.type.favorites", value = EmbeddedLocalizedValue.Text("§eFAVORITES"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.type.spotlight", value = EmbeddedLocalizedValue.Text("§6SPOTLIGHT"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.type.random", value = EmbeddedLocalizedValue.Text("§bRANDOM"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.name", value = EmbeddedLocalizedValue.Text("タグフィルター"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.label", value = EmbeddedLocalizedValue.Text("選択中のタグ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.no_selection", value = EmbeddedLocalizedValue.Text("設定なし"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.all", value = EmbeddedLocalizedValue.Text("すべて"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.action.next", value = EmbeddedLocalizedValue.Text("次のタグへ変更"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.action.previous", value = EmbeddedLocalizedValue.Text("前のタグへ変更"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.action.clear", value = EmbeddedLocalizedValue.Text("フィルターを解除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.name", value = EmbeddedLocalizedValue.Text("特殊フィルター"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.label", value = EmbeddedLocalizedValue.Text("選択中の条件"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.action.next", value = EmbeddedLocalizedValue.Text("次の条件へ変更"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.action.previous", value = EmbeddedLocalizedValue.Text("前の条件へ変更"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.action.clear", value = EmbeddedLocalizedValue.Text("フィルターを解除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.type.none", value = EmbeddedLocalizedValue.Text("なし"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.type.unvisited", value = EmbeddedLocalizedValue.Text("訪れたことがないワールド"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_description_dialog.title", value = EmbeddedLocalizedValue.Text("SPOTLIGHT説明文の編集"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_description_dialog.body", value = EmbeddedLocalizedValue.Text("SPOTLIGHTソート中に表示する説明文を入力してください（最大{max}文字）。空欄でデフォルトに戻せます。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_description_dialog.input_label", value = EmbeddedLocalizedValue.Text("説明文"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.stats.name", value = EmbeddedLocalizedValue.Text("§f統計情報"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.stats.sort_label", value = EmbeddedLocalizedValue.Text("並び順"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.stats.tag_label", value = EmbeddedLocalizedValue.Text("タグ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.stats.count_label", value = EmbeddedLocalizedValue.Text("検索結果"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.stats.desc", value = EmbeddedLocalizedValue.Text("§7条件に一致するワールドを表示しています"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort_info.hot", value = EmbeddedLocalizedValue.Text("直近の来訪者が多い順"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort_info.new", value = EmbeddedLocalizedValue.Text("公開日時が新しい順"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort_info.favorites", value = EmbeddedLocalizedValue.Text("お気に入り登録数が多い順"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort_info.spotlight", value = EmbeddedLocalizedValue.Text("ピックアップ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort_info.random", value = EmbeddedLocalizedValue.Text("日替わりランダム"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.no_result", value = EmbeddedLocalizedValue.Text("§7条件に一致するワールドが見つかりませんでした"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_remove_confirm.title", value = EmbeddedLocalizedValue.Text("SPOTLIGHTからの削除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_remove_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fワールド「§a{world}§f」を", "§6SPOTLIGHT§fから削除しますか？")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.spotlight_confirm.title", value = EmbeddedLocalizedValue.Text("SPOTLIGHTへの登録"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.spotlight_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fワールド「§a{world}§f」を", "§6SPOTLIGHT§fに登録しますか？", "§7登録するとディスカバリーメニューの最上位に", "§7優先的に表示されるようになります。")), domain = DOMAIN),
    )

}
