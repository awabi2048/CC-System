package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpMyworldGuiPortalCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "myworld/gui_portal"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "gui.portal.title", value = EmbeddedLocalizedValue.Text("ポータルの設定"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.id_label", value = EmbeddedLocalizedValue.Text("PORTAL_ID"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.toggle_text.name", value = EmbeddedLocalizedValue.Text("§a浮遊テキストの表示"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.toggle_text.current_label", value = EmbeddedLocalizedValue.Text("現在の設定"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.toggle_text.action", value = EmbeddedLocalizedValue.Text("浮遊テキスト表示を切り替え"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.destination_label", value = EmbeddedLocalizedValue.Text("§7行き先 "), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.color.name", value = EmbeddedLocalizedValue.Text("§eパーティクルの色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.color.current_label", value = EmbeddedLocalizedValue.Text("現在の色"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.color.previous", value = EmbeddedLocalizedValue.Text("§7« {color}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.color.next", value = EmbeddedLocalizedValue.Text("§7{color} »"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.color.action", value = EmbeddedLocalizedValue.Text("パーティクルの色を切り替え"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.remove.name", value = EmbeddedLocalizedValue.Text("§cポータルを撤去する"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.remove.description", value = EmbeddedLocalizedValue.Text("設置されたポータルを撤去します。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.remove.action", value = EmbeddedLocalizedValue.Text("ポータルを撤去"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal_item.name", value = EmbeddedLocalizedValue.Text("§bワールドポータル"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal_item.destination", value = EmbeddedLocalizedValue.Text("リンク先"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal_item.action.link", value = EmbeddedLocalizedValue.Text("マイワールドをリンク"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal_item.action.place", value = EmbeddedLocalizedValue.Text("ワールドポータルを設置"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal_item.action.unlink", value = EmbeddedLocalizedValue.Text("リンクを解除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.world_gate_item.name", value = EmbeddedLocalizedValue.Text("§3ワールドゲート"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.world_gate_item.destination", value = EmbeddedLocalizedValue.Text("リンク先"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.world_gate_item.action.link", value = EmbeddedLocalizedValue.Text("マイワールドをリンク"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.world_gate_item.action.relink", value = EmbeddedLocalizedValue.Text("リンク先を再設定"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.world_gate_item.action.select_area", value = EmbeddedLocalizedValue.Text("範囲指定の2点を選択"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.title", value = EmbeddedLocalizedValue.Text("ポータルの遠隔管理"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.name", value = EmbeddedLocalizedValue.Text("§bポータル: §f{id}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.owner", value = EmbeddedLocalizedValue.Text("所有者"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.world", value = EmbeddedLocalizedValue.Text("設置ワールド"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.coordinates", value = EmbeddedLocalizedValue.Text("座標"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.action.teleport", value = EmbeddedLocalizedValue.Text("ポータルの位置にテレポート"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.action.remove", value = EmbeddedLocalizedValue.Text("ポータルを撤去"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.sort.display", value = EmbeddedLocalizedValue.Text("§e並び替え方法"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.sort.label", value = EmbeddedLocalizedValue.Text("並び順"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.sort.created_desc", value = EmbeddedLocalizedValue.Text("設置日（新しい順）"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.sort.created_asc", value = EmbeddedLocalizedValue.Text("設置日（古い順）"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.invite.target_head.click_invite", value = EmbeddedLocalizedValue.Text("プレイヤーを招待"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.input.title", value = EmbeddedLocalizedValue.Text("訪問先プレイヤーの入力"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.input.label", value = EmbeddedLocalizedValue.Text("プレイヤー名"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.input.placeholder", value = EmbeddedLocalizedValue.Text("プレイヤー名を入力"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.title", value = EmbeddedLocalizedValue.Text("{player}のワールド"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.world_item.warp", value = EmbeddedLocalizedValue.Text("このワールドへワープ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.world_item.fav_add", value = EmbeddedLocalizedValue.Text("お気に入りに追加"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.world_item.fav_remove", value = EmbeddedLocalizedValue.Text("お気に入りを解除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.input.title", value = EmbeddedLocalizedValue.Text("訪問ワールド検索"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.input.label", value = EmbeddedLocalizedValue.Text("検索キーワード"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.input.placeholder", value = EmbeddedLocalizedValue.Text("ワールド名で検索"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.title", value = EmbeddedLocalizedValue.Text("ワールド検索: {query}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.info.name", value = EmbeddedLocalizedValue.Text("§6検索情報"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.info.query_label", value = EmbeddedLocalizedValue.Text("検索キーワード"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.info.hit_label", value = EmbeddedLocalizedValue.Text("検索結果"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.info.shown_label", value = EmbeddedLocalizedValue.Text("表示件数"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.info.page_label", value = EmbeddedLocalizedValue.Text("ページ"), domain = DOMAIN),
    )

}
