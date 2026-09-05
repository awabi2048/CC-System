package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object ContentCropsKeys {
    @JvmField val CROPS_SOYBEAN_SEED_NAME: LocalizationKey<String> = LocalizationKey.text("crops.soybean_seed.name", setOf())
    @JvmField val CROPS_SOYBEAN_SEED_LORE: LocalizationKey<List<String>> = LocalizationKey.textList("crops.soybean_seed.lore", setOf())
    @JvmField val CROPS_SOYBEAN_NAME: LocalizationKey<String> = LocalizationKey.text("crops.soybean.name", setOf())
    @JvmField val CROPS_SOYBEAN_LORE: LocalizationKey<List<String>> = LocalizationKey.textList("crops.soybean.lore", setOf())
    @JvmField val CROPS_SUPPORT_PLACE: LocalizationKey<String> = LocalizationKey.text("crops.support.place", setOf())
    @JvmField val CROPS_SUPPORT_BREAK: LocalizationKey<String> = LocalizationKey.text("crops.support.break", setOf())
    @JvmField val CROPS_PLANT: LocalizationKey<String> = LocalizationKey.text("crops.plant", setOf())
    @JvmField val CROPS_HARVEST: LocalizationKey<String> = LocalizationKey.text("crops.harvest", setOf())
    @JvmField val CROPS_NOT_READY: LocalizationKey<String> = LocalizationKey.text("crops.not_ready", setOf())
    @JvmField val CROPS_ALREADY_PLANTED: LocalizationKey<String> = LocalizationKey.text("crops.already_planted", setOf())
    @JvmField val CROPS_INVALID_GROUND: LocalizationKey<String> = LocalizationKey.text("crops.invalid_ground", setOf())

    internal fun all(): List<LocalizationKey<*>> = listOf(
        CROPS_SOYBEAN_SEED_NAME,
        CROPS_SOYBEAN_SEED_LORE,
        CROPS_SOYBEAN_NAME,
        CROPS_SOYBEAN_LORE,
        CROPS_SUPPORT_PLACE,
        CROPS_SUPPORT_BREAK,
        CROPS_PLANT,
        CROPS_HARVEST,
        CROPS_NOT_READY,
        CROPS_ALREADY_PLANTED,
        CROPS_INVALID_GROUND,
    )
}
