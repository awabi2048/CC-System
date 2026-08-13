package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsMyworldCustomItemCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "myworld/custom_item"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "custom_item.empty_biome_bottle.name", value = EmbeddedLocalizedValue.Text("§fEmpty Biome Bottle (WIP)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.empty_biome_bottle.lore", value = EmbeddedLocalizedValue.TextList(listOf("§7Use in a specific biome to capture its air!", "§7The bottled air can be used for §6Biome Settings§7 in MyWorld.")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.bottled_biome_air.name", value = EmbeddedLocalizedValue.Text("§fBottled Biome Atmosphere【{biome}】"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.bottled_biome_air.lore", value = EmbeddedLocalizedValue.TextList(listOf("§7§oFilled with dense biome air.", "§7Can be used for §6Biome Settings§7 in MyWorld!")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.moon_stone.name", value = EmbeddedLocalizedValue.Text("§fMoon Stone"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.moon_stone.lore", value = EmbeddedLocalizedValue.TextList(listOf("§7§oA seemingly normal stone, yet undeniably from the moon.", "§7Can be used for §bGravity Settings§7 in MyWorld!")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.world_seed.name", value = EmbeddedLocalizedValue.Text("§dWorld Seed"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.world_seed.description", value = EmbeddedLocalizedValue.TextList(listOf("By using §7, §bMy World Slot§7", "§7 can be extended by one!")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.world_seed.action", value = EmbeddedLocalizedValue.Text("Use"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.tour_sign.name", value = EmbeddedLocalizedValue.Text("§bTour Sign"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.tour_sign.description", value = EmbeddedLocalizedValue.TextList(listOf("§7A sign used as a world tour marker.", "§7Can be placed in your MyWorld.")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.tour_sign.action", value = EmbeddedLocalizedValue.Text("Place"), domain = DOMAIN),
    )

}
