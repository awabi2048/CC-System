package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsAnnounceCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "announce"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "announce_usage", value = EmbeddedLocalizedValue.Text("§cUsage: /announcement [-menu]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.menu_title", value = EmbeddedLocalizedValue.Text("§0Announcements"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.item_default_title", value = EmbeddedLocalizedValue.Text("Announcement"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.datetime_format", value = EmbeddedLocalizedValue.Text("MM/dd/yyyy hh:mm a"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.issued_at", value = EmbeddedLocalizedValue.Text("Issued"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.updated_at", value = EmbeddedLocalizedValue.Text("Updated"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.expires_at", value = EmbeddedLocalizedValue.Text("Expires"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.indefinite", value = EmbeddedLocalizedValue.Text("Indefinite"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.edit", value = EmbeddedLocalizedValue.Text("edit this announcement"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.delete", value = EmbeddedLocalizedValue.Text("delete this announcement"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.notify.issued_title", value = EmbeddedLocalizedValue.Text("§aNew announcement: §f%title%"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.notify.toast_title", value = EmbeddedLocalizedValue.Text("§aYou have %count% new announcements!"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.notify.toast_description", value = EmbeddedLocalizedValue.Text("§7Use /an to open"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.menu_command_item_name", value = EmbeddedLocalizedValue.Text("§cOpen menu"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.menu_command_item_lore", value = EmbeddedLocalizedValue.Text("execute the menu command"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.add_item_name", value = EmbeddedLocalizedValue.Text("§fAdd announcement"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.add_item_lore", value = EmbeddedLocalizedValue.Text("create a new announcement"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.add_limit_reached", value = EmbeddedLocalizedValue.Text("§cYou can only have up to %max% announcements."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.icon_select_instruction", value = EmbeddedLocalizedValue.Text("§eClick an item in your own inventory to use it as the announcement icon."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.menu_command_not_configured", value = EmbeddedLocalizedValue.Text("§cannounce_menu_command is not configured."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.menu_command_failed", value = EmbeddedLocalizedValue.Text("§cFailed to execute the menu command."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.add_success", value = EmbeddedLocalizedValue.Text("§aAnnouncement added."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.edit_success", value = EmbeddedLocalizedValue.Text("§aAnnouncement updated."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_success", value = EmbeddedLocalizedValue.Text("§aAnnouncement deleted."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.edit_target_not_found", value = EmbeddedLocalizedValue.Text("§cTarget announcement was not found."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.invalid_title", value = EmbeddedLocalizedValue.Text("§cTitle is required."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.invalid_duration", value = EmbeddedLocalizedValue.Text("§cInvalid end time format. (e.g. 2026/03/10-10:30 / 2026/03/10 / 10:30)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.end_at_not_future", value = EmbeddedLocalizedValue.Text("§cPlease specify an end time later than now."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.title_warning.required", value = EmbeddedLocalizedValue.Text("§c§nTitle is required"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.end_at_warning.invalid_format", value = EmbeddedLocalizedValue.Text("§c§nInvalid date/time format"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.end_at_warning.not_future", value = EmbeddedLocalizedValue.Text("§c§nEnd time must be later than now"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.dialog_title", value = EmbeddedLocalizedValue.Text("Add Announcement"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.dialog_body", value = EmbeddedLocalizedValue.TextList(listOf("Fill in required title, 3 content lines, end time, and indefinite option.", "End time examples: 2026/03/10-10:30 / 2026/03/10 / 10:30", "When indefinite=true, end time will be ignored.")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.dialog_confirm", value = EmbeddedLocalizedValue.Text("Add"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.dialog_cancel", value = EmbeddedLocalizedValue.Text("Cancel"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.edit_dialog_title", value = EmbeddedLocalizedValue.Text("Edit Announcement"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.edit_dialog_body", value = EmbeddedLocalizedValue.TextList(listOf("You can update title, contents, end time, and indefinite option.")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.edit_dialog_confirm", value = EmbeddedLocalizedValue.Text("Update"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_title", value = EmbeddedLocalizedValue.Text("Delete Announcement"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_body", value = EmbeddedLocalizedValue.TextList(listOf("The following announcement will be deleted:", "%title%")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_content_label", value = EmbeddedLocalizedValue.Text("Contents:"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_content_empty", value = EmbeddedLocalizedValue.Text("(no content)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_confirm", value = EmbeddedLocalizedValue.Text("Delete"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_cancel", value = EmbeddedLocalizedValue.Text("Cancel"), domain = DOMAIN),
    )

}
