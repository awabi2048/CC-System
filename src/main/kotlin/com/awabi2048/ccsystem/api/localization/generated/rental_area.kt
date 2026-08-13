package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object RentalAreaKeys {
    @JvmField val RENTAL_TICKET_USAGE: LocalizationKey<String> = LocalizationKey.text("rental_ticket_usage")
    @JvmField val RENTAL_TICKET_INVALID_DAYS: LocalizationKey<String> = LocalizationKey.text("rental_ticket_invalid_days")
    @JvmField val RENTAL_TICKET_GIVE_SUCCESS: LocalizationKey<String> = LocalizationKey.text("rental_ticket_give_success")
    @JvmField val RENTAL_TICKET_RECEIVED: LocalizationKey<String> = LocalizationKey.text("rental_ticket_received")
    @JvmField val RENTAL_TICKET_NAME: LocalizationKey<String> = LocalizationKey.text("rental_ticket_name")
    @JvmField val RENTAL_TICKET_DAYS: LocalizationKey<String> = LocalizationKey.text("rental_ticket_days")
    @JvmField val RENTAL_TICKET_DAYS_UNIT: LocalizationKey<String> = LocalizationKey.text("rental_ticket_days_unit")
    @JvmField val RENTAL_TICKET_ACTION: LocalizationKey<String> = LocalizationKey.text("rental_ticket_action")
    @JvmField val RENTAL_AREA_INVALID_TICKET: LocalizationKey<String> = LocalizationKey.text("rental_area_invalid_ticket")
    @JvmField val RENTAL_AREA_TICKET_MISSING: LocalizationKey<String> = LocalizationKey.text("rental_area_ticket_missing")
    @JvmField val RENTAL_AREA_NOT_FOUND: LocalizationKey<String> = LocalizationKey.text("rental_area_not_found")
    @JvmField val RENTAL_AREA_ALREADY_OWNED: LocalizationKey<String> = LocalizationKey.text("rental_area_already_owned")
    @JvmField val RENTAL_AREA_ALREADY_OWNED_WITH_DAYS: LocalizationKey<String> = LocalizationKey.text("rental_area_already_owned_with_days")
    @JvmField val RENTAL_AREA_ALREADY_OWNED_BY_YOU: LocalizationKey<String> = LocalizationKey.text("rental_area_already_owned_by_you")
    @JvmField val RENTAL_AREA_PLAYER_ALREADY_HAS_CONTRACT: LocalizationKey<String> = LocalizationKey.text("rental_area_player_already_has_contract")
    @JvmField val RENTAL_AREA_CONTRACT_DONE: LocalizationKey<String> = LocalizationKey.text("rental_area_contract_done")
    @JvmField val RENTAL_AREA_CONTRACT_CONFIRM_TITLE: LocalizationKey<String> = LocalizationKey.text("rental_area_contract_confirm_title")
    @JvmField val RENTAL_AREA_CONTRACT_CONFIRM_BODY: LocalizationKey<List<String>> = LocalizationKey.textList("rental_area_contract_confirm_body")
    @JvmField val RENTAL_AREA_CONTRACT_CONFIRM_YES: LocalizationKey<String> = LocalizationKey.text("rental_area_contract_confirm_yes")
    @JvmField val RENTAL_AREA_CONTRACT_CONFIRM_NO: LocalizationKey<String> = LocalizationKey.text("rental_area_contract_confirm_no")
    @JvmField val RENTAL_AREA_REMAINED_ITEMS_NOTIFICATION: LocalizationKey<String> = LocalizationKey.text("rental_area_remained_items_notification")
    @JvmField val RENTAL_AREA_REMAINED_ITEMS_CLICK: LocalizationKey<String> = LocalizationKey.text("rental_area_remained_items_click")
    @JvmField val RENTAL_AREA_NO_REMAINED_ITEMS: LocalizationKey<String> = LocalizationKey.text("rental_area_no_remained_items")

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
