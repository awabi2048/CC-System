package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object RentalAreaKeys {
    @JvmField val RENTAL_TICKET_USAGE: LocalizationKey<String> = LocalizationKey.text("rental_ticket_usage", setOf())
    @JvmField val RENTAL_TICKET_INVALID_DAYS: LocalizationKey<String> = LocalizationKey.text("rental_ticket_invalid_days", setOf())
    @JvmField val RENTAL_TICKET_GIVE_SUCCESS: LocalizationKey<String> = LocalizationKey.text("rental_ticket_give_success", setOf("amount", "days", "player"))
    @JvmField val RENTAL_TICKET_RECEIVED: LocalizationKey<String> = LocalizationKey.text("rental_ticket_received", setOf("amount", "days"))
    @JvmField val RENTAL_TICKET_NAME: LocalizationKey<String> = LocalizationKey.text("rental_ticket_name", setOf())
    @JvmField val RENTAL_TICKET_DAYS: LocalizationKey<String> = LocalizationKey.text("rental_ticket_days", setOf())
    @JvmField val RENTAL_TICKET_DAYS_UNIT: LocalizationKey<String> = LocalizationKey.text("rental_ticket_days_unit", setOf())
    @JvmField val RENTAL_TICKET_ACTION: LocalizationKey<String> = LocalizationKey.text("rental_ticket_action", setOf())
    @JvmField val RENTAL_AREA_INVALID_TICKET: LocalizationKey<String> = LocalizationKey.text("rental_area_invalid_ticket", setOf())
    @JvmField val RENTAL_AREA_TICKET_MISSING: LocalizationKey<String> = LocalizationKey.text("rental_area_ticket_missing", setOf())
    @JvmField val RENTAL_AREA_NOT_FOUND: LocalizationKey<String> = LocalizationKey.text("rental_area_not_found", setOf())
    @JvmField val RENTAL_AREA_ALREADY_OWNED: LocalizationKey<String> = LocalizationKey.text("rental_area_already_owned", setOf())
    @JvmField val RENTAL_AREA_ALREADY_OWNED_WITH_DAYS: LocalizationKey<String> = LocalizationKey.text("rental_area_already_owned_with_days", setOf("days"))
    @JvmField val RENTAL_AREA_ALREADY_OWNED_BY_YOU: LocalizationKey<String> = LocalizationKey.text("rental_area_already_owned_by_you", setOf("days"))
    @JvmField val RENTAL_AREA_PLAYER_ALREADY_HAS_CONTRACT: LocalizationKey<String> = LocalizationKey.text("rental_area_player_already_has_contract", setOf())
    @JvmField val RENTAL_AREA_CONTRACT_DONE: LocalizationKey<String> = LocalizationKey.text("rental_area_contract_done", setOf("area_id", "expire_date"))
    @JvmField val RENTAL_AREA_CONTRACT_CONFIRM_TITLE: LocalizationKey<String> = LocalizationKey.text("rental_area_contract_confirm_title", setOf())
    @JvmField val RENTAL_AREA_CONTRACT_CONFIRM_BODY: LocalizationKey<List<String>> = LocalizationKey.textList("rental_area_contract_confirm_body", setOf("area_id", "days", "expire_date", "world"))
    @JvmField val RENTAL_AREA_CONTRACT_CONFIRM_YES: LocalizationKey<String> = LocalizationKey.text("rental_area_contract_confirm_yes", setOf())
    @JvmField val RENTAL_AREA_CONTRACT_CONFIRM_NO: LocalizationKey<String> = LocalizationKey.text("rental_area_contract_confirm_no", setOf())
    @JvmField val RENTAL_AREA_REMAINED_ITEMS_NOTIFICATION: LocalizationKey<String> = LocalizationKey.text("rental_area_remained_items_notification", setOf("areas", "count"))
    @JvmField val RENTAL_AREA_REMAINED_ITEMS_CLICK: LocalizationKey<String> = LocalizationKey.text("rental_area_remained_items_click", setOf())
    @JvmField val RENTAL_AREA_NO_REMAINED_ITEMS: LocalizationKey<String> = LocalizationKey.text("rental_area_no_remained_items", setOf())

    internal fun all(): List<LocalizationKey<*>> = listOf(
        RENTAL_TICKET_USAGE,
        RENTAL_TICKET_INVALID_DAYS,
        RENTAL_TICKET_GIVE_SUCCESS,
        RENTAL_TICKET_RECEIVED,
        RENTAL_TICKET_NAME,
        RENTAL_TICKET_DAYS,
        RENTAL_TICKET_DAYS_UNIT,
        RENTAL_TICKET_ACTION,
        RENTAL_AREA_INVALID_TICKET,
        RENTAL_AREA_TICKET_MISSING,
        RENTAL_AREA_NOT_FOUND,
        RENTAL_AREA_ALREADY_OWNED,
        RENTAL_AREA_ALREADY_OWNED_WITH_DAYS,
        RENTAL_AREA_ALREADY_OWNED_BY_YOU,
        RENTAL_AREA_PLAYER_ALREADY_HAS_CONTRACT,
        RENTAL_AREA_CONTRACT_DONE,
        RENTAL_AREA_CONTRACT_CONFIRM_TITLE,
        RENTAL_AREA_CONTRACT_CONFIRM_BODY,
        RENTAL_AREA_CONTRACT_CONFIRM_YES,
        RENTAL_AREA_CONTRACT_CONFIRM_NO,
        RENTAL_AREA_REMAINED_ITEMS_NOTIFICATION,
        RENTAL_AREA_REMAINED_ITEMS_CLICK,
        RENTAL_AREA_NO_REMAINED_ITEMS,
    )
}
