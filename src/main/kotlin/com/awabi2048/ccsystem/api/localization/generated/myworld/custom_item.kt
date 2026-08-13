package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object MyworldCustomItemKeys {
    @JvmField val CUSTOM_ITEM_EMPTY_BIOME_BOTTLE_NAME: LocalizationKey<String> = LocalizationKey.text("custom_item.empty_biome_bottle.name")
    @JvmField val CUSTOM_ITEM_EMPTY_BIOME_BOTTLE_LORE: LocalizationKey<List<String>> = LocalizationKey.textList("custom_item.empty_biome_bottle.lore")
    @JvmField val CUSTOM_ITEM_BOTTLED_BIOME_AIR_NAME: LocalizationKey<String> = LocalizationKey.text("custom_item.bottled_biome_air.name")
    @JvmField val CUSTOM_ITEM_BOTTLED_BIOME_AIR_LORE: LocalizationKey<List<String>> = LocalizationKey.textList("custom_item.bottled_biome_air.lore")
    @JvmField val CUSTOM_ITEM_MOON_STONE_NAME: LocalizationKey<String> = LocalizationKey.text("custom_item.moon_stone.name")
    @JvmField val CUSTOM_ITEM_MOON_STONE_LORE: LocalizationKey<List<String>> = LocalizationKey.textList("custom_item.moon_stone.lore")
    @JvmField val CUSTOM_ITEM_WORLD_SEED_NAME: LocalizationKey<String> = LocalizationKey.text("custom_item.world_seed.name")
    @JvmField val CUSTOM_ITEM_WORLD_SEED_DESCRIPTION: LocalizationKey<List<String>> = LocalizationKey.textList("custom_item.world_seed.description")
    @JvmField val CUSTOM_ITEM_WORLD_SEED_ACTION: LocalizationKey<String> = LocalizationKey.text("custom_item.world_seed.action")
    @JvmField val CUSTOM_ITEM_TOUR_SIGN_NAME: LocalizationKey<String> = LocalizationKey.text("custom_item.tour_sign.name")
    @JvmField val CUSTOM_ITEM_TOUR_SIGN_DESCRIPTION: LocalizationKey<List<String>> = LocalizationKey.textList("custom_item.tour_sign.description")
    @JvmField val CUSTOM_ITEM_TOUR_SIGN_ACTION: LocalizationKey<String> = LocalizationKey.text("custom_item.tour_sign.action")

    internal fun all(): List<LocalizationKey<*>> = listOf(
        CUSTOM_ITEM_EMPTY_BIOME_BOTTLE_NAME,
        CUSTOM_ITEM_EMPTY_BIOME_BOTTLE_LORE,
        CUSTOM_ITEM_BOTTLED_BIOME_AIR_NAME,
        CUSTOM_ITEM_BOTTLED_BIOME_AIR_LORE,
        CUSTOM_ITEM_MOON_STONE_NAME,
        CUSTOM_ITEM_MOON_STONE_LORE,
        CUSTOM_ITEM_WORLD_SEED_NAME,
        CUSTOM_ITEM_WORLD_SEED_DESCRIPTION,
        CUSTOM_ITEM_WORLD_SEED_ACTION,
        CUSTOM_ITEM_TOUR_SIGN_NAME,
        CUSTOM_ITEM_TOUR_SIGN_DESCRIPTION,
        CUSTOM_ITEM_TOUR_SIGN_ACTION,
    )
}
