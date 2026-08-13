package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object ContentMissionKeys {
    @JvmField val MISSION_ACCEPT_SUCCESS: LocalizationKey<String> = LocalizationKey.text("mission.accept.success")
    @JvmField val MISSION_COMPLETE_SUCCESS: LocalizationKey<String> = LocalizationKey.text("mission.complete.success")

    internal fun all(): List<LocalizationKey<*>> = listOf(
        MISSION_ACCEPT_SUCCESS,
        MISSION_COMPLETE_SUCCESS,
    )
}
