package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpRentalAreaCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "rental_area"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "rental_ticket_usage", value = EmbeddedLocalizedValue.Text("§c使用法: /cc-system rental-ticket <player> <days> [amount]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_invalid_days", value = EmbeddedLocalizedValue.Text("§cdaysには1以上の整数を指定してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_give_success", value = EmbeddedLocalizedValue.Text("§a%player% にレンタルエリア手形を配布しました。(日数: %days%, 個数: %amount%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_received", value = EmbeddedLocalizedValue.Text("§aレンタルエリア手形を受け取りました。(日数: %days%, 個数: %amount%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_name", value = EmbeddedLocalizedValue.Text("&bレンタルエリア手形"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_days", value = EmbeddedLocalizedValue.Text("レンタル可能日数"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_days_unit", value = EmbeddedLocalizedValue.Text("日"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_action", value = EmbeddedLocalizedValue.Text("レンタルエリアを契約"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_invalid_ticket", value = EmbeddedLocalizedValue.Text("§cこの手形は使用できません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_ticket_missing", value = EmbeddedLocalizedValue.Text("§c手形を持っていないため契約できません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_not_found", value = EmbeddedLocalizedValue.Text("§cレンタルエリアが見つかりません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_already_owned", value = EmbeddedLocalizedValue.Text("§cこのレンタルエリアは契約済みです。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_already_owned_with_days", value = EmbeddedLocalizedValue.Text("§cこのレンタルエリアは契約済みです。残り日数: %days%日"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_already_owned_by_you", value = EmbeddedLocalizedValue.Text("§eこのレンタルエリアはあなたが契約中です。残り日数: %days%日"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_player_already_has_contract", value = EmbeddedLocalizedValue.Text("§c既に別のレンタルエリアを契約しています。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_contract_done", value = EmbeddedLocalizedValue.Text("§aレンタル契約が完了しました。エリア: %area_id% / 期限: %expire_date%"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_contract_confirm_title", value = EmbeddedLocalizedValue.Text("レンタル契約の確認"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_contract_confirm_body", value = EmbeddedLocalizedValue.TextList(listOf("エリアID: %area_id%", "ワールド: %world%", "契約日数: %days%日", "期限: %expire_date%")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_contract_confirm_yes", value = EmbeddedLocalizedValue.Text("契約する"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_contract_confirm_no", value = EmbeddedLocalizedValue.Text("キャンセル"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_remained_items_notification", value = EmbeddedLocalizedValue.Text("§e期限切れレンタルエリアの回収アイテムがあります。(件数: %count%, エリア: %areas%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_remained_items_click", value = EmbeddedLocalizedValue.Text("§b[クリックで受け取る]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_no_remained_items", value = EmbeddedLocalizedValue.Text("§c回収アイテムはありません。"), domain = DOMAIN),
    )

}
