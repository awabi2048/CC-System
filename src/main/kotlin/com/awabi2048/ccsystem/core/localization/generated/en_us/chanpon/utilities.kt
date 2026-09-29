package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsChanponUtilitiesCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "chanpon/utilities"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon_utilities.beplayerhead.usage", value = EmbeddedLocalizedValue.Text("&cUsage: /beplayerhead <player name>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.beplayerhead.loading", value = EmbeddedLocalizedValue.Text("&7Fetching Bedrock skin…"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.beplayerhead.inventory_full", value = EmbeddedLocalizedValue.Text("&cYour inventory is full."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.beplayerhead.success", value = EmbeddedLocalizedValue.Text("&aReceived {name}'s head."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.beplayerhead.floodgate_unavailable", value = EmbeddedLocalizedValue.Text("&cFloodgate is not running, so Bedrock players cannot be looked up."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.beplayerhead.player_not_found", value = EmbeddedLocalizedValue.Text("&cNo Bedrock player was found with that name. Check the gamertag."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.beplayerhead.skin_not_found", value = EmbeddedLocalizedValue.Text("&cThe player was found, but Geyser has no retrievable skin for them."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.beplayerhead.service_unavailable", value = EmbeddedLocalizedValue.Text("&cFloodgate or Geyser skin service could not be reached. Please try again later."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.beplayerhead.invalid_response", value = EmbeddedLocalizedValue.Text("&cThe skin service returned no usable texture. Try again after the player has joined the server."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.common.prefix", value = EmbeddedLocalizedValue.Text("&7[&bChanpon-Utilities&7] "), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.usage", value = EmbeddedLocalizedValue.Text("&cUsage: /cu status|enable <module>|disable <module>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.no_permission", value = EmbeddedLocalizedValue.Text("&cYou do not have permission to use this command."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.unknown_module", value = EmbeddedLocalizedValue.Text("&cUnknown module: {id}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.feature_disabled", value = EmbeddedLocalizedValue.Text("&eModule {id} is currently disabled."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.changed", value = EmbeddedLocalizedValue.Text("&aModule {id} changed to {state}."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.unchanged", value = EmbeddedLocalizedValue.Text("&7Module {id} is already {state}."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.failed", value = EmbeddedLocalizedValue.Text("&cFailed to change module {id}. The previous setting was restored."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.status.header", value = EmbeddedLocalizedValue.Text("&bChanpon-Utilities module status"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.status.entry", value = EmbeddedLocalizedValue.Text("&7- &f{id}&7: {state}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.state.enabled", value = EmbeddedLocalizedValue.Text("&aenabled"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.state.disabled", value = EmbeddedLocalizedValue.Text("&cdisabled"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.state.unavailable", value = EmbeddedLocalizedValue.Text("&eenabled (dependency unavailable)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.unlink.success", value = EmbeddedLocalizedValue.Text("&aUnlinked from the lectern"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.unlink.already", value = EmbeddedLocalizedValue.Text("&eThis bookshelf is not linked"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.started", value = EmbeddedLocalizedValue.TextList(listOf("&bFreeCam started! \\ &7Press Shift twice or use /freecam stop to finish")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.previous_camera_too_far", value = EmbeddedLocalizedValue.TextList(listOf("&7The last camera position was too far away, so FreeCam started from your current position")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.recovered", value = EmbeddedLocalizedValue.Text("&eRecovered your body from an unfinished FreeCam session."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.unavailable", value = EmbeddedLocalizedValue.Text("&cFreeCam cannot be started right now."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.not_active", value = EmbeddedLocalizedValue.Text("&cFreeCam is not active."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.usage", value = EmbeddedLocalizedValue.Text("&cUsage: /freecam [start|stop|status|toggle_shortcut|reset_distance]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.shortcut.enabled", value = EmbeddedLocalizedValue.Text("&aEnabled the FreeCam shortcut."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.shortcut.disabled", value = EmbeddedLocalizedValue.Text("&eDisabled the FreeCam shortcut."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.reset_distance.usage", value = EmbeddedLocalizedValue.Text("&cUsage: /freecam reset_distance <0-30000000 | reset>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.reset_distance.changed", value = EmbeddedLocalizedValue.Text("&aSet the camera reset distance to {distance} blocks."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.reset_distance.reset", value = EmbeddedLocalizedValue.Text("&aReset the camera reset distance to the default of {distance} blocks."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.status.active", value = EmbeddedLocalizedValue.Text("&aFreeCam is active."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.status.inactive", value = EmbeddedLocalizedValue.Text("&7FreeCam is not active."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.body.damaged", value = EmbeddedLocalizedValue.Text("&cFreeCam stopped because your body was attacked."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.item.name", value = EmbeddedLocalizedValue.Text("&bFreeCam Activator"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.item.lore", value = EmbeddedLocalizedValue.Text("&7Use to start FreeCam."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.safety.enabled", value = EmbeddedLocalizedValue.Text("&aTour safety mode is now on."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.safety.disabled", value = EmbeddedLocalizedValue.Text("&eTour safety mode is now off."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.safety.blocked", value = EmbeddedLocalizedValue.Text("&7Tour safety mode is currently on. Use &e/anzen&7 to turn it off."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.command_safeguard.title", value = EmbeddedLocalizedValue.Text("Command Safeguard"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.command_safeguard.description", value = EmbeddedLocalizedValue.Text("Do you want to execute this command?"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.command_safeguard.command_label", value = EmbeddedLocalizedValue.Text("Command to execute"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.command_safeguard.confirm", value = EmbeddedLocalizedValue.Text("Execute"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.command_safeguard.cancel", value = EmbeddedLocalizedValue.Text("Cancel"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.command_safeguard.ui_unavailable", value = EmbeddedLocalizedValue.Text("The command was cancelled because the confirmation screen could not be shown."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.usage", value = EmbeddedLocalizedValue.Text("&cUsage: /guide start [route] | stop | status"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.started", value = EmbeddedLocalizedValue.Text("&bGuide started: {route}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.start_replaced", value = EmbeddedLocalizedValue.Text("&eEnded the current guide and started a new one: {route}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.stopped", value = EmbeddedLocalizedValue.Text("&eGuide stopped."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.not_active", value = EmbeddedLocalizedValue.Text("&cNo guide is active."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.route_not_found", value = EmbeddedLocalizedValue.Text("&cGuide route not found: {route}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.no_routes", value = EmbeddedLocalizedValue.Text("&cNo guide routes are available."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.completed", value = EmbeddedLocalizedValue.Text("&bGuide completed: {route}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.waypoint_completed", value = EmbeddedLocalizedValue.Text("&aWaypoint {index}/{total} completed."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.status.active", value = EmbeddedLocalizedValue.Text("&bGuiding: {route} (waypoint {index}/{total})"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.status.inactive", value = EmbeddedLocalizedValue.Text("&7No guide is active."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.world_left", value = EmbeddedLocalizedValue.Text("&eYou left the guide's world, so the guide ended."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.follow_prompt", value = EmbeddedLocalizedValue.Text("&eYou are off course. The allay is pointing toward the next waypoint."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.returned", value = EmbeddedLocalizedValue.Text("&aBack on course."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.allay_name", value = EmbeddedLocalizedValue.Text("&bGuide"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.player_only", value = EmbeddedLocalizedValue.Text("&cOnly players can run this command."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.admin.usage", value = EmbeddedLocalizedValue.Text("&cUsage: /guide admin setpoint <route> <index> | reload"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.admin.setpoint_saved", value = EmbeddedLocalizedValue.Text("&aSaved waypoint {index} of route {route}."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.admin.route_created", value = EmbeddedLocalizedValue.Text("&bCreated new route {route}."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.admin.reloaded", value = EmbeddedLocalizedValue.Text("&aReloaded guide routes ({count} routes)."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.guide.admin.invalid_index", value = EmbeddedLocalizedValue.Text("&cInvalid waypoint index: {index}"), domain = DOMAIN),
    )

}
