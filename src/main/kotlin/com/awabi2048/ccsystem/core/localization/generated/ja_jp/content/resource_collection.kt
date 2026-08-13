package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpContentResourceCollectionCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "content/resource_collection"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.mineral", value = EmbeddedLocalizedValue.Text("鉱脈の鑑定"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.forest", value = EmbeddedLocalizedValue.Text("林産物の調査"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.vegetation", value = EmbeddedLocalizedValue.Text("植生の調査"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.chisel_result", value = EmbeddedLocalizedValue.Text("ノミ採掘"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.mineral_batch_result", value = EmbeddedLocalizedValue.Text("一括採掘"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.forest_batch_result", value = EmbeddedLocalizedValue.Text("一括伐採"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.woodworking_result", value = EmbeddedLocalizedValue.Text("木材加工"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.harvest_result", value = EmbeddedLocalizedValue.Text("範囲収穫"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.tilling_result", value = EmbeddedLocalizedValue.Text("範囲耕作"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.altitude", value = EmbeddedLocalizedValue.Text("高度"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.region", value = EmbeddedLocalizedValue.Text("地域"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.candidate_count", value = EmbeddedLocalizedValue.Text("候補数"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.collectible_items", value = EmbeddedLocalizedValue.Text("収集候補"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.season", value = EmbeddedLocalizedValue.Text("季節"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.uses", value = EmbeddedLocalizedValue.Text("用途"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.vegetation_group", value = EmbeddedLocalizedValue.Text("植生群"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.amount", value = EmbeddedLocalizedValue.Text("個数"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.processed_count", value = EmbeddedLocalizedValue.Text("処理数"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.special_materials", value = EmbeddedLocalizedValue.Text("特殊素材"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.companion_minerals", value = EmbeddedLocalizedValue.Text("随伴鉱物"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.forest_products", value = EmbeddedLocalizedValue.Text("樹皮・樹脂・木の実など"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.seasonal_plants", value = EmbeddedLocalizedValue.Text("現在の季節に採れる植物"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.vegetation_none", value = EmbeddedLocalizedValue.Text("このあたりには、めぼしいものはなさそうです"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.forest_none", value = EmbeddedLocalizedValue.Text("この木には採れそうなものはなさそうです"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.forest_harvested", value = EmbeddedLocalizedValue.Text("この木から採れるものはもうなさそうです"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.list_separator", value = EmbeddedLocalizedValue.Text("、"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.chisel.completed", value = EmbeddedLocalizedValue.Text("§aノミ採掘が完了しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.altitude.high", value = EmbeddedLocalizedValue.Text("高所"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.altitude.shallow", value = EmbeddedLocalizedValue.Text("浅層"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.altitude.middle", value = EmbeddedLocalizedValue.Text("中層"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.altitude.deep", value = EmbeddedLocalizedValue.Text("深層"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.temperate", value = EmbeddedLocalizedValue.Text("温帯"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.cold", value = EmbeddedLocalizedValue.Text("寒冷"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.dry", value = EmbeddedLocalizedValue.Text("乾燥"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.wet", value = EmbeddedLocalizedValue.Text("湿潤"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.mountain", value = EmbeddedLocalizedValue.Text("山岳"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.nether", value = EmbeddedLocalizedValue.Text("ネザー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.woodworking.completed", value = EmbeddedLocalizedValue.Text("§a木材加工が完了しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.woodworking.not_ready", value = EmbeddedLocalizedValue.Text("§7この木材は、まだ加工できないようです"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.batch.completed", value = EmbeddedLocalizedValue.Text("§a一括処理が完了しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.completed", value = EmbeddedLocalizedValue.Text("§a植物を採集しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.surface_depleted", value = EmbeddedLocalizedValue.Text("§7しばらくは、ここでは何も採れなさそうです"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.season.spring", value = EmbeddedLocalizedValue.Text("春"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.season.summer", value = EmbeddedLocalizedValue.Text("夏"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.season.autumn", value = EmbeddedLocalizedValue.Text("秋"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.season.winter", value = EmbeddedLocalizedValue.Text("冬"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.forest.harvested", value = EmbeddedLocalizedValue.Text("§7この木から採れるものはもうなさそうです"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.forest.completed", value = EmbeddedLocalizedValue.Text("§a林産物を採集しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.cultivation.harvested", value = EmbeddedLocalizedValue.Text("§a作物を収穫しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.cultivation.tilled", value = EmbeddedLocalizedValue.Text("§a土を耕しました"), domain = DOMAIN),
    )

}
