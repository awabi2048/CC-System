package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object MyworldWorldTagKeys {
    @JvmField val WORLD_TAG_SHOP: LocalizationKey<String> = LocalizationKey.text("world_tag.shop", setOf())
    @JvmField val WORLD_TAG_MINIGAME: LocalizationKey<String> = LocalizationKey.text("world_tag.minigame", setOf())
    @JvmField val WORLD_TAG_BUILDING: LocalizationKey<String> = LocalizationKey.text("world_tag.building", setOf())
    @JvmField val WORLD_TAG_FACILITY: LocalizationKey<String> = LocalizationKey.text("world_tag.facility", setOf())
    @JvmField val WORLD_TAG_STREAMING: LocalizationKey<String> = LocalizationKey.text("world_tag.streaming", setOf())

    internal fun all(): List<LocalizationKey<*>> = listOf(
        WORLD_TAG_SHOP,
        WORLD_TAG_MINIGAME,
        WORLD_TAG_BUILDING,
        WORLD_TAG_FACILITY,
        WORLD_TAG_STREAMING,
    )
}
