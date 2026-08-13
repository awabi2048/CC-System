package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsRentalAreaCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "rental_area"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "rental_ticket_usage", value = EmbeddedLocalizedValue.Text("§cUsage: /cc-system rental-ticket <player> <days> [amount]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_invalid_days", value = EmbeddedLocalizedValue.Text("§cPlease specify an integer of 1 or greater for days."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_give_success", value = EmbeddedLocalizedValue.Text("§aGave rental area ticket(s) to %player%. (days: %days%, amount: %amount%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_received", value = EmbeddedLocalizedValue.Text("§aYou received rental area ticket(s). (days: %days%, amount: %amount%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_name", value = EmbeddedLocalizedValue.Text("&bRental Area Ticket"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_days", value = EmbeddedLocalizedValue.Text("Rental days"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_days_unit", value = EmbeddedLocalizedValue.Text(" days"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_ticket_action", value = EmbeddedLocalizedValue.Text("contract the rental area"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_invalid_ticket", value = EmbeddedLocalizedValue.Text("§cThis ticket cannot be used."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_ticket_missing", value = EmbeddedLocalizedValue.Text("§cTicket not found. Contract failed."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_not_found", value = EmbeddedLocalizedValue.Text("§cRental area not found."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_already_owned", value = EmbeddedLocalizedValue.Text("§cThis rental area is already contracted."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_already_owned_with_days", value = EmbeddedLocalizedValue.Text("§cThis rental area is already contracted. Remaining days: %days%"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_already_owned_by_you", value = EmbeddedLocalizedValue.Text("§eYou already own this rental area. Remaining days: %days%"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_player_already_has_contract", value = EmbeddedLocalizedValue.Text("§cYou already have another active rental area."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_contract_done", value = EmbeddedLocalizedValue.Text("§aContract completed. Area: %area_id% / Expire: %expire_date%"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_contract_confirm_title", value = EmbeddedLocalizedValue.Text("Confirm Rental Contract"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_contract_confirm_body", value = EmbeddedLocalizedValue.TextList(listOf("Area ID: %area_id%", "World: %world%", "Days: %days%", "Expire: %expire_date%")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_contract_confirm_yes", value = EmbeddedLocalizedValue.Text("Confirm"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_contract_confirm_no", value = EmbeddedLocalizedValue.Text("Cancel"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_remained_items_notification", value = EmbeddedLocalizedValue.Text("§eYou have items from expired rental areas. (Count: %count%, Areas: %areas%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_remained_items_click", value = EmbeddedLocalizedValue.Text("§b[Click to receive]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "rental_area_no_remained_items", value = EmbeddedLocalizedValue.Text("§cNo items to receive."), domain = DOMAIN),
    )

}
