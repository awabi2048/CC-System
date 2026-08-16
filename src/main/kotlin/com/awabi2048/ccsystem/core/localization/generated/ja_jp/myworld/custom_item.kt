package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpMyworldCustomItemCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "myworld/custom_item"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "custom_item.empty_biome_bottle.name", value = EmbeddedLocalizedValue.Text("§f空のバイオームの瓶(仮)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.empty_biome_bottle.lore", value = EmbeddedLocalizedValue.TextList(listOf("§7特定のバイオームで使用すると、そのバイオームの空気を詰めることができます！", "§7瓶済みした空気は、マイワールドの§6バイオーム設定§7に使うことができます。")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.bottled_biome_air.name", value = EmbeddedLocalizedValue.Text("§fバイオームの雰囲気入りの瓶【{biome}】"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.bottled_biome_air.lore", value = EmbeddedLocalizedValue.TextList(listOf("§7§o濃厚なバイオームの空気が詰まっている", "§7マイワールドの§6バイオーム設定§7に使うことができます！")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.moon_stone.name", value = EmbeddedLocalizedValue.Text("§f月の石"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.moon_stone.lore", value = EmbeddedLocalizedValue.TextList(listOf("§7§o一見普通の石に見えても、紛れもない月の石である", "§7マイワールドの§b重力設定§7に使うことができます！")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.world_seed.name", value = EmbeddedLocalizedValue.Text("§dワールドの種"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.world_seed.description", value = EmbeddedLocalizedValue.TextList(listOf("§7使用することで、§bマイワールドスロット§7を", "§71つ拡張することができます！")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.world_seed.action", value = EmbeddedLocalizedValue.Text("使用"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.tour_sign.name", value = EmbeddedLocalizedValue.Text("§bTour看板"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.tour_sign.description", value = EmbeddedLocalizedValue.TextList(listOf("§7ワールドツアーのマーカーになる看板です。", "§7マイワールド内に設置できます。")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "custom_item.tour_sign.action", value = EmbeddedLocalizedValue.Text("設置"), domain = DOMAIN),
    )

}
