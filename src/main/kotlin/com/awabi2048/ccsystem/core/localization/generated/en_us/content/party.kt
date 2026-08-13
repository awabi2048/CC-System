package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsContentPartyCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "content/party"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "party.name", value = EmbeddedLocalizedValue.Text("Party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.invite_expired", value = EmbeddedLocalizedValue.Text("The party invitation has expired"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.joined", value = EmbeddedLocalizedValue.Text("You joined the party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.left", value = EmbeddedLocalizedValue.Text("You left the party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.kicked", value = EmbeddedLocalizedValue.Text("You were removed from the party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.disbanded", value = EmbeddedLocalizedValue.Text("The party was disbanded"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.leader_transferred", value = EmbeddedLocalizedValue.Text("Party leadership was transferred"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.chat_prefix", value = EmbeddedLocalizedValue.Text("Party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.feature_disabled", value = EmbeddedLocalizedValue.Text("The party feature is currently disabled"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.created", value = EmbeddedLocalizedValue.Text("Created party \"{party}\" (ID: {id})"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.invited", value = EmbeddedLocalizedValue.Text("{inviter} invited you to \"{party}\" (click to join)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.member_joined", value = EmbeddedLocalizedValue.Text("{player} joined the party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.member_left", value = EmbeddedLocalizedValue.Text("{player} left the party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.member_kicked", value = EmbeddedLocalizedValue.Text("{player} was removed from the party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.invite_sent", value = EmbeddedLocalizedValue.Text("Invitation sent to {player}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.info", value = EmbeddedLocalizedValue.Text("Party \"{party}\" ID={id} leader={leader} members={members} ({count}/{capacity}) recruiting={recruiting}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.recruiting_updated", value = EmbeddedLocalizedValue.Text("Party recruiting changed to {enabled}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.chat", value = EmbeddedLocalizedValue.Text("[{sender}] {message}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.not_in_party", value = EmbeddedLocalizedValue.Text("You are not in a party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.player_not_online", value = EmbeddedLocalizedValue.Text("That player is not online"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.usage", value = EmbeddedLocalizedValue.Text("/party menu|list|create|info|invite|accept|join|leave|kick|leader|disband|recruit|chat|chat-toggle"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.default_name", value = EmbeddedLocalizedValue.Text("{player}'s party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.recruiting_notice", value = EmbeddedLocalizedValue.Text("Party \"{party}\" is recruiting ({count}/{capacity}, click to join)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.list.header", value = EmbeddedLocalizedValue.Text("Recruiting parties"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.list.empty", value = EmbeddedLocalizedValue.Text("No parties are recruiting"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.list.entry", value = EmbeddedLocalizedValue.Text("• {party} ({count}/{capacity}, click to join)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.interaction.busy", value = EmbeddedLocalizedValue.Text("Another left-click action is active, so party invitation cannot start"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.interaction.invite_waiting", value = EmbeddedLocalizedValue.Text("Left-click the player to invite"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.chat_channel.enabled", value = EmbeddedLocalizedValue.Text("Switched to party chat"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.chat_channel.disabled", value = EmbeddedLocalizedValue.Text("Returned to normal chat"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.title", value = EmbeddedLocalizedValue.Text("Party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.state", value = EmbeddedLocalizedValue.Text("State"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.leader_only", value = EmbeddedLocalizedValue.Text("Only the party leader can change this"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.value.none", value = EmbeddedLocalizedValue.Text("None"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.settings.name", value = EmbeddedLocalizedValue.Text("Name and description"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.settings.party_name", value = EmbeddedLocalizedValue.Text("Party name"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.settings.description", value = EmbeddedLocalizedValue.Text("Description"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.settings.action", value = EmbeddedLocalizedValue.Text("Open settings"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.recruiting.name", value = EmbeddedLocalizedValue.Text("Recruitment"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.recruiting.enabled", value = EmbeddedLocalizedValue.Text("Recruiting"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.recruiting.disabled", value = EmbeddedLocalizedValue.Text("Not recruiting"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.recruiting.action", value = EmbeddedLocalizedValue.Text("Toggle recruitment"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.disband.name", value = EmbeddedLocalizedValue.Text("Disband party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.disband.warning", value = EmbeddedLocalizedValue.Text("This action cannot be undone"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.disband.action", value = EmbeddedLocalizedValue.Text("Open disband confirmation"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.chat.name", value = EmbeddedLocalizedValue.Text("Party chat"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.chat.enabled", value = EmbeddedLocalizedValue.Text("Party chat"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.chat.disabled", value = EmbeddedLocalizedValue.Text("Global chat"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.chat.action", value = EmbeddedLocalizedValue.Text("Toggle chat"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.name", value = EmbeddedLocalizedValue.Text("Party information"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.party_name", value = EmbeddedLocalizedValue.Text("Party name"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.description", value = EmbeddedLocalizedValue.Text("Description"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.leader_label", value = EmbeddedLocalizedValue.Text("Leader"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.member_label", value = EmbeddedLocalizedValue.Text("Member"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.capacity_label", value = EmbeddedLocalizedValue.Text("Capacity"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.invite.name", value = EmbeddedLocalizedValue.Text("Invite player"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.invite.full", value = EmbeddedLocalizedValue.Text("The party is full"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.invite.select", value = EmbeddedLocalizedValue.Text("Select a target player"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.invite.input", value = EmbeddedLocalizedValue.Text("Enter a player name"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.body", value = EmbeddedLocalizedValue.Text("Enter the required information"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.submit", value = EmbeddedLocalizedValue.Text("Confirm"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.cancel", value = EmbeddedLocalizedValue.Text("Cancel"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.settings.title", value = EmbeddedLocalizedValue.Text("Party settings"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.settings.name", value = EmbeddedLocalizedValue.Text("Party name"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.settings.description", value = EmbeddedLocalizedValue.Text("Description"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.invite.title", value = EmbeddedLocalizedValue.Text("Invite player"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.invite.target", value = EmbeddedLocalizedValue.Text("Player name"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.disband.title", value = EmbeddedLocalizedValue.Text("Disband party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.disband.body", value = EmbeddedLocalizedValue.Text("Disband this party. This action cannot be undone"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.error.invalid_action", value = EmbeddedLocalizedValue.Text("Invalid party arguments or state"), domain = DOMAIN),
    )

}
