package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object ChanponUtilitiesKeys {
    @JvmField val CHANPON_UTILITIES_COMMON_PREFIX: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.common.prefix", setOf())
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_USAGE: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.usage", setOf())
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_NO_PERMISSION: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.no_permission", setOf())
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_UNKNOWN_MODULE: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.unknown_module", setOf("id"))
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_FEATURE_DISABLED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.feature_disabled", setOf("id"))
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_CHANGED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.changed", setOf("id", "state"))
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_UNCHANGED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.unchanged", setOf("id", "state"))
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_FAILED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.failed", setOf("id"))
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_STATUS_HEADER: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.status.header", setOf())
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_STATUS_ENTRY: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.status.entry", setOf("id", "state"))
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_STATE_ENABLED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.state.enabled", setOf())
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_STATE_DISABLED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.state.disabled", setOf())
    @JvmField val CHANPON_UTILITIES_MANAGEMENT_STATE_UNAVAILABLE: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.management.state.unavailable", setOf())
    @JvmField val CHANPON_UTILITIES_UNLINK_SUCCESS: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.unlink.success", setOf())
    @JvmField val CHANPON_UTILITIES_UNLINK_ALREADY: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.unlink.already", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_STARTED: LocalizationKey<List<String>> = LocalizationKey.textList("chanpon_utilities.freecam.started", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_PREVIOUS_CAMERA_TOO_FAR: LocalizationKey<List<String>> = LocalizationKey.textList("chanpon_utilities.freecam.previous_camera_too_far", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_RECOVERED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.recovered", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_UNAVAILABLE: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.unavailable", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_NOT_ACTIVE: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.not_active", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_USAGE: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.usage", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_SHORTCUT_ENABLED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.shortcut.enabled", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_SHORTCUT_DISABLED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.shortcut.disabled", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_RESET_DISTANCE_USAGE: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.reset_distance.usage", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_RESET_DISTANCE_CHANGED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.reset_distance.changed", setOf("distance"))
    @JvmField val CHANPON_UTILITIES_FREECAM_RESET_DISTANCE_RESET: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.reset_distance.reset", setOf("distance"))
    @JvmField val CHANPON_UTILITIES_FREECAM_STATUS_ACTIVE: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.status.active", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_STATUS_INACTIVE: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.status.inactive", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_BODY_DAMAGED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.body.damaged", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_ITEM_NAME: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.item.name", setOf())
    @JvmField val CHANPON_UTILITIES_FREECAM_ITEM_LORE: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.freecam.item.lore", setOf())
    @JvmField val CHANPON_UTILITIES_SAFETY_ENABLED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.safety.enabled", setOf())
    @JvmField val CHANPON_UTILITIES_SAFETY_DISABLED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.safety.disabled", setOf())
    @JvmField val CHANPON_UTILITIES_SAFETY_BLOCKED: LocalizationKey<String> = LocalizationKey.text("chanpon_utilities.safety.blocked", setOf())

    internal fun all(): List<LocalizationKey<*>> = listOf(
        CHANPON_UTILITIES_COMMON_PREFIX,
        CHANPON_UTILITIES_MANAGEMENT_USAGE,
        CHANPON_UTILITIES_MANAGEMENT_NO_PERMISSION,
        CHANPON_UTILITIES_MANAGEMENT_UNKNOWN_MODULE,
        CHANPON_UTILITIES_MANAGEMENT_FEATURE_DISABLED,
        CHANPON_UTILITIES_MANAGEMENT_CHANGED,
        CHANPON_UTILITIES_MANAGEMENT_UNCHANGED,
        CHANPON_UTILITIES_MANAGEMENT_FAILED,
        CHANPON_UTILITIES_MANAGEMENT_STATUS_HEADER,
        CHANPON_UTILITIES_MANAGEMENT_STATUS_ENTRY,
        CHANPON_UTILITIES_MANAGEMENT_STATE_ENABLED,
        CHANPON_UTILITIES_MANAGEMENT_STATE_DISABLED,
        CHANPON_UTILITIES_MANAGEMENT_STATE_UNAVAILABLE,
        CHANPON_UTILITIES_UNLINK_SUCCESS,
        CHANPON_UTILITIES_UNLINK_ALREADY,
        CHANPON_UTILITIES_FREECAM_STARTED,
        CHANPON_UTILITIES_FREECAM_PREVIOUS_CAMERA_TOO_FAR,
        CHANPON_UTILITIES_FREECAM_RECOVERED,
        CHANPON_UTILITIES_FREECAM_UNAVAILABLE,
        CHANPON_UTILITIES_FREECAM_NOT_ACTIVE,
        CHANPON_UTILITIES_FREECAM_USAGE,
        CHANPON_UTILITIES_FREECAM_SHORTCUT_ENABLED,
        CHANPON_UTILITIES_FREECAM_SHORTCUT_DISABLED,
        CHANPON_UTILITIES_FREECAM_RESET_DISTANCE_USAGE,
        CHANPON_UTILITIES_FREECAM_RESET_DISTANCE_CHANGED,
        CHANPON_UTILITIES_FREECAM_RESET_DISTANCE_RESET,
        CHANPON_UTILITIES_FREECAM_STATUS_ACTIVE,
        CHANPON_UTILITIES_FREECAM_STATUS_INACTIVE,
        CHANPON_UTILITIES_FREECAM_BODY_DAMAGED,
        CHANPON_UTILITIES_FREECAM_ITEM_NAME,
        CHANPON_UTILITIES_FREECAM_ITEM_LORE,
        CHANPON_UTILITIES_SAFETY_ENABLED,
        CHANPON_UTILITIES_SAFETY_DISABLED,
        CHANPON_UTILITIES_SAFETY_BLOCKED,
    )
}
