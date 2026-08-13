package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object ResourceKeys {
    @JvmField val BROADCAST_SUCCESS: LocalizationKey<String> = LocalizationKey.text("broadcast_success")
    @JvmField val CONSOLE_SUCCESS: LocalizationKey<String> = LocalizationKey.text("console_success")
    @JvmField val PREGEN_PRIORITY_COMPLETE: LocalizationKey<String> = LocalizationKey.text("pregen_priority_complete")
    @JvmField val PREGEN_ALL_COMPLETE: LocalizationKey<String> = LocalizationKey.text("pregen_all_complete")
    @JvmField val RESOURCE_USAGE_GENERATE: LocalizationKey<String> = LocalizationKey.text("resource.usage.generate")
    @JvmField val RESOURCE_USAGE_TELEPORT: LocalizationKey<String> = LocalizationKey.text("resource.usage.teleport")
    @JvmField val RESOURCE_USAGE_PAUSE_PREGEN: LocalizationKey<String> = LocalizationKey.text("resource.usage.pause_pregen")
    @JvmField val RESOURCE_USAGE_CLOSE: LocalizationKey<String> = LocalizationKey.text("resource.usage.close")
    @JvmField val RESOURCE_INVALID_FORMAT: LocalizationKey<String> = LocalizationKey.text("resource.invalid_format")
    @JvmField val RESOURCE_GENERATION_START: LocalizationKey<String> = LocalizationKey.text("resource.generation_start")
    @JvmField val RESOURCE_GENERATION_SUCCESS: LocalizationKey<String> = LocalizationKey.text("resource.generation_success")
    @JvmField val RESOURCE_GENERATION_FAILED: LocalizationKey<String> = LocalizationKey.text("resource.generation_failed")
    @JvmField val RESOURCE_DELETE_INCOMPLETE_ABORT: LocalizationKey<String> = LocalizationKey.text("resource.delete_incomplete_abort")
    @JvmField val RESOURCE_DELETE_INCOMPLETE_ABORT_BROADCAST: LocalizationKey<String> = LocalizationKey.text("resource.delete_incomplete_abort_broadcast")
    @JvmField val RESOURCE_WORLD_CREATE_FAILED: LocalizationKey<String> = LocalizationKey.text("resource.world_create_failed")
    @JvmField val RESOURCE_EVACUATED_FOR_REGENERATION: LocalizationKey<String> = LocalizationKey.text("resource.evacuated_for_regeneration")
    @JvmField val RESOURCE_WORLD_NOT_FOUND: LocalizationKey<String> = LocalizationKey.text("resource.world_not_found")
    @JvmField val RESOURCE_WORLD_NOT_READY: LocalizationKey<String> = LocalizationKey.text("resource.world_not_ready")
    @JvmField val RESOURCE_TELEPORT_SUCCESS: LocalizationKey<String> = LocalizationKey.text("resource.teleport_success")
    @JvmField val RESOURCE_RETURNED_ON_CLOSE: LocalizationKey<String> = LocalizationKey.text("resource.returned_on_close")
    @JvmField val RESOURCE_RETURNED_FROM_WORLD: LocalizationKey<String> = LocalizationKey.text("resource.returned_from_world")
    @JvmField val RESOURCE_NORMAL_NAME: LocalizationKey<String> = LocalizationKey.text("resource.normal.name")
    @JvmField val RESOURCE_NETHER_NAME: LocalizationKey<String> = LocalizationKey.text("resource.nether.name")
    @JvmField val RESOURCE_END_NAME: LocalizationKey<String> = LocalizationKey.text("resource.end.name")
    @JvmField val RESOURCE_RELOAD_SUCCESS: LocalizationKey<String> = LocalizationKey.text("resource.reload_success")
    @JvmField val RESOURCE_PAUSE_SUCCESS: LocalizationKey<String> = LocalizationKey.text("resource.pause_success")
    @JvmField val RESOURCE_PAUSE_FAILED: LocalizationKey<String> = LocalizationKey.text("resource.pause_failed")
    @JvmField val RESOURCE_CLOSE_SUCCESS: LocalizationKey<String> = LocalizationKey.text("resource.close_success")
    @JvmField val RESOURCE_CLOSE_FAILED: LocalizationKey<String> = LocalizationKey.text("resource.close_failed")

    internal fun all(): List<LocalizationKey<*>> = listOf(
        BROADCAST_SUCCESS,
        CONSOLE_SUCCESS,
        PREGEN_PRIORITY_COMPLETE,
        PREGEN_ALL_COMPLETE,
        RESOURCE_USAGE_GENERATE,
        RESOURCE_USAGE_TELEPORT,
        RESOURCE_USAGE_PAUSE_PREGEN,
        RESOURCE_USAGE_CLOSE,
        RESOURCE_INVALID_FORMAT,
        RESOURCE_GENERATION_START,
        RESOURCE_GENERATION_SUCCESS,
        RESOURCE_GENERATION_FAILED,
        RESOURCE_DELETE_INCOMPLETE_ABORT,
        RESOURCE_DELETE_INCOMPLETE_ABORT_BROADCAST,
        RESOURCE_WORLD_CREATE_FAILED,
        RESOURCE_EVACUATED_FOR_REGENERATION,
        RESOURCE_WORLD_NOT_FOUND,
        RESOURCE_WORLD_NOT_READY,
        RESOURCE_TELEPORT_SUCCESS,
        RESOURCE_RETURNED_ON_CLOSE,
        RESOURCE_RETURNED_FROM_WORLD,
        RESOURCE_NORMAL_NAME,
        RESOURCE_NETHER_NAME,
        RESOURCE_END_NAME,
        RESOURCE_RELOAD_SUCCESS,
        RESOURCE_PAUSE_SUCCESS,
        RESOURCE_PAUSE_FAILED,
        RESOURCE_CLOSE_SUCCESS,
        RESOURCE_CLOSE_FAILED,
    )
}
