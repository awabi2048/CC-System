package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsContentManagementCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "content/management"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "content_management.no_permission", value = EmbeddedLocalizedValue.Text("&cYou do not have permission."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.usage", value = EmbeddedLocalizedValue.Text("&eUsage: /ccc <status|enable|disable> [feature]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.status_header", value = EmbeddedLocalizedValue.Text("&6=== CC-Content Status ==="), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.status_line", value = EmbeddedLocalizedValue.Text("&7- &f{feature}: {status}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.state.enabled", value = EmbeddedLocalizedValue.Text("&aEnabled"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.state.disabled", value = EmbeddedLocalizedValue.Text("&cDisabled"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.state.warning", value = EmbeddedLocalizedValue.Text("&eEnabled (warning)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.state.failure", value = EmbeddedLocalizedValue.Text("&cEnabled (initialization failed)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.arena", value = EmbeddedLocalizedValue.Text("Arena"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.rank", value = EmbeddedLocalizedValue.Text("Profession and Rank"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.brewery", value = EmbeddedLocalizedValue.Text("Brewery"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.cooking", value = EmbeddedLocalizedValue.Text("Cooking"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.fishing", value = EmbeddedLocalizedValue.Text("Fishing"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.resource_collection", value = EmbeddedLocalizedValue.Text("Resource Collection"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.seasonal", value = EmbeddedLocalizedValue.Text("Seasonal Events"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.sukima_dungeon", value = EmbeddedLocalizedValue.Text("Sukima Dungeon"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.party", value = EmbeddedLocalizedValue.Text("Party"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.minigame", value = EmbeddedLocalizedValue.Text("Minigame"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.unknown_feature", value = EmbeddedLocalizedValue.Text("&cUnknown content: {feature}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature_unavailable", value = EmbeddedLocalizedValue.Text("&c{feature} is currently unavailable. Check /ccc status."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.already_set", value = EmbeddedLocalizedValue.Text("&e{feature} is already {status}."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.enable_dependency_required", value = EmbeddedLocalizedValue.Text("&cEnable these contents before enabling {feature}: {dependencies}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.enable_dependency_unavailable", value = EmbeddedLocalizedValue.Text("&cCannot enable {feature}. Resolve the initialization state of: {dependencies}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.disable_dependent_required", value = EmbeddedLocalizedValue.Text("&cDisable these contents before disabling {feature}: {dependents}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.change_started", value = EmbeddedLocalizedValue.Text("&6Changing {feature} to {status} and reinitializing CC-Content..."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.change_success", value = EmbeddedLocalizedValue.Text("&aChanged {feature} to {status}."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.save_failed", value = EmbeddedLocalizedValue.Text("&cFailed to save the content setting: {reason}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.usage", value = EmbeddedLocalizedValue.Text("&eUsage: /ccc debug <clear_block_placement_data|placement_recording|clear_fishing_grounds|complete_oage_daily> ..."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.unknown_mode", value = EmbeddedLocalizedValue.Text("&cUnknown debug mode: {mode}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.usage", value = EmbeddedLocalizedValue.Text("&eUsage: /ccc debug placement_recording <enable|disable|status>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.unavailable", value = EmbeddedLocalizedValue.Text("&cBlock placement recording control is unavailable."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.status", value = EmbeddedLocalizedValue.Text("&7Block placement recording: {status}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.changed", value = EmbeddedLocalizedValue.Text("&aBlock placement recording is now {status}."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.state.enabled", value = EmbeddedLocalizedValue.Text("&aenabled"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.state.disabled", value = EmbeddedLocalizedValue.Text("&cdisabled"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.fishing_ground.usage", value = EmbeddedLocalizedValue.Text("&eUsage: /ccc debug clear_fishing_grounds"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.fishing_ground.unavailable", value = EmbeddedLocalizedValue.Text("&cNatural fishing ground management is unavailable."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.fishing_ground.cleared", value = EmbeddedLocalizedValue.Text("&aRemoved all current natural fishing grounds."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.no_permission", value = EmbeddedLocalizedValue.Text("&cYou do not have permission to display Display particles."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.usage", value = EmbeddedLocalizedValue.Text("&eUsage: /ccc particle <pattern> [<x> <y> <z>] [<dx> <dy> <dz> <speed> <count> [normal|force]]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.invalid_argument", value = EmbeddedLocalizedValue.Text("&cInvalid particle argument: {detail}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.unknown_pattern", value = EmbeddedLocalizedValue.Text("&cUnknown Display particle preset: {pattern}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.no_viewers", value = EmbeddedLocalizedValue.Text("&eSkipped creation because no player is in view range. Specify force if needed."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.started", value = EmbeddedLocalizedValue.Text("&aDisplayed Display particles: {pattern} x{count}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.rejected", value = EmbeddedLocalizedValue.Text("&cCould not display Display particles: {detail}"), domain = DOMAIN),
    )

}
