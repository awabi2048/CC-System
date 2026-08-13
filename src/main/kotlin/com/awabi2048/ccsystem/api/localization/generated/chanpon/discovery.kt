package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object ChanponDiscoveryKeys {
    @JvmField val CHANPON_DISCOVERY_TITLE: LocalizationKey<String> = LocalizationKey.text("chanpon.discovery.title", setOf())
    @JvmField val CHANPON_DISCOVERY_INFO_NAME: LocalizationKey<String> = LocalizationKey.text("chanpon.discovery.info.name", setOf())
    @JvmField val CHANPON_DISCOVERY_INFO_LORE: LocalizationKey<List<String>> = LocalizationKey.textList("chanpon.discovery.info.lore", setOf())
    @JvmField val CHANPON_DISCOVERY_NO_RESULT: LocalizationKey<String> = LocalizationKey.text("chanpon.discovery.no_result", setOf())

    internal fun all(): List<LocalizationKey<*>> = listOf(
        CHANPON_DISCOVERY_TITLE,
        CHANPON_DISCOVERY_INFO_NAME,
        CHANPON_DISCOVERY_INFO_LORE,
        CHANPON_DISCOVERY_NO_RESULT,
    )
}
