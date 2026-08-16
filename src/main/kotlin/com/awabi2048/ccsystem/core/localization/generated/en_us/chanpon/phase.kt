package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsChanponPhaseCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "chanpon/phase"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.display", value = EmbeddedLocalizedValue.Text("Building permission"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.current", value = EmbeddedLocalizedValue.Text("Current"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.allowed", value = EmbeddedLocalizedValue.Text("Allowed"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.denied", value = EmbeddedLocalizedValue.Text("Denied"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.allowed_description", value = EmbeddedLocalizedValue.Text("Non-administrator players can build."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.denied_description", value = EmbeddedLocalizedValue.Text("Non-administrator players cannot build."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.allow_action", value = EmbeddedLocalizedValue.Text("Allow building"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.deny_action", value = EmbeddedLocalizedValue.Text("Deny building"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.display", value = EmbeddedLocalizedValue.Text("World creation availability"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.current", value = EmbeddedLocalizedValue.Text("Current"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.next_action", value = EmbeddedLocalizedValue.Text("Next setting"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.previous_action", value = EmbeddedLocalizedValue.Text("Previous setting"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.cycle_action", value = EmbeddedLocalizedValue.Text("Change setting"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.denied.display", value = EmbeddedLocalizedValue.Text("Deny all"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.denied.description", value = EmbeddedLocalizedValue.Text("Standard and production worlds cannot be created."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.non_production.display", value = EmbeddedLocalizedValue.Text("Allow non-production"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.non_production.description", value = EmbeddedLocalizedValue.Text("Only standard worlds can be created."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.production.display", value = EmbeddedLocalizedValue.Text("Allow production"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.production.description", value = EmbeddedLocalizedValue.Text("Standard and production worlds can be created."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.no_permission", value = EmbeddedLocalizedValue.Text("§cChanpon administrator permission is required."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.player_only", value = EmbeddedLocalizedValue.Text("§cThis command can only be used by a player."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.unknown_subcommand", value = EmbeddedLocalizedValue.Text("§cUnknown subcommand."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.usage_get_linked_portal", value = EmbeddedLocalizedValue.Text("§eUsage: /chanpon-mwm get_linked_portal <player>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.player_not_found", value = EmbeddedLocalizedValue.Text("§cThe target player is not online."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.target_not_in_world", value = EmbeddedLocalizedValue.Text("§cThe target player is not in a My World."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.portal_failed", value = EmbeddedLocalizedValue.Text("§cThe world portal could not be obtained."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.portal_received", value = EmbeddedLocalizedValue.Text("§aWorld portal obtained."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.usage_unzip_archive", value = EmbeddedLocalizedValue.Text("§eUsage: /chanpon-mwm unzip_archive <archive>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.archive_unzipped", value = EmbeddedLocalizedValue.Text("§aArchive extracted: §f{archive}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.archive_unzip_failed", value = EmbeddedLocalizedValue.Text("§cArchive extraction failed: §f{reason}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation_denied", value = EmbeddedLocalizedValue.Text("§cWorld creation is currently unavailable."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.production_denied", value = EmbeddedLocalizedValue.Text("§cProduction world creation and switching are currently unavailable."), domain = DOMAIN),
    )

}
