package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsMyworldGuiMeetCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "myworld/gui_meet"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "gui.meet.title", value = EmbeddedLocalizedValue.Text("Visit {player}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.title_list", value = EmbeddedLocalizedValue.Text("Go to Player's Place"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.empty_message", value = EmbeddedLocalizedValue.Text("§7No players are available to display."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.current_world", value = EmbeddedLocalizedValue.Text("Current World"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.same_world", value = EmbeddedLocalizedValue.Text("Same world"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.status", value = EmbeddedLocalizedValue.Text("Status"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.online_state", value = EmbeddedLocalizedValue.Text("Connection"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.online", value = EmbeddedLocalizedValue.Text("Online"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.offline", value = EmbeddedLocalizedValue.Text("Offline"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.click_visit", value = EmbeddedLocalizedValue.Text("Warp to this world"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.world_item.click_request", value = EmbeddedLocalizedValue.Text("Send warp request"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.status_button.display", value = EmbeddedLocalizedValue.Text("§a{player}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.status_button.current", value = EmbeddedLocalizedValue.Text("Current Status"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.status_button.next", value = EmbeddedLocalizedValue.Text("Next Status"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.meet.status_button.action", value = EmbeddedLocalizedValue.Text("toggle"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_confirm.title", value = EmbeddedLocalizedValue.Text("Member Request Confirmation"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_confirm.confirm", value = EmbeddedLocalizedValue.Text("§aSend Request"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_confirm.cancel", value = EmbeddedLocalizedValue.Text("§cCancel"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fSend a §dmember request§f to", "§fthe owner of \"§a{world}§f\"?", "§7If the owner approves, you will become a member.")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_owner_confirm.title", value = EmbeddedLocalizedValue.Text("Member Request Approval"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_owner_confirm.confirm", value = EmbeddedLocalizedValue.Text("§aApprove"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_owner_confirm.reject", value = EmbeddedLocalizedValue.Text("§cReject"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_request_owner_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fApprove the §dmember request§f from", "§fplayer \"§a{player}§f\"?", "§7If approved, they will become a member of this world.")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_invite_accept_confirm.title", value = EmbeddedLocalizedValue.Text("Member Invite Confirmation"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_invite_accept_confirm.confirm", value = EmbeddedLocalizedValue.Text("§aAccept"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_invite_accept_confirm.cancel", value = EmbeddedLocalizedValue.Text("§cDecline"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.member_invite_accept_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fAccept the §dmember invite§f from", "§fplayer \"§a{player}§f\"?", "§7Target world: §a{world}")), domain = DOMAIN),
    )

}
