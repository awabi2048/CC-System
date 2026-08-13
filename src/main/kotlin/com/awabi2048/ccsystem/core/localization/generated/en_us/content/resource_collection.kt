package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsContentResourceCollectionCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "content/resource_collection"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.mineral", value = EmbeddedLocalizedValue.Text("Vein appraisal"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.forest", value = EmbeddedLocalizedValue.Text("Forest product survey"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.vegetation", value = EmbeddedLocalizedValue.Text("Vegetation survey"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.chisel_result", value = EmbeddedLocalizedValue.Text("Chisel mining"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.mineral_batch_result", value = EmbeddedLocalizedValue.Text("Batch mining"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.forest_batch_result", value = EmbeddedLocalizedValue.Text("Batch felling"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.woodworking_result", value = EmbeddedLocalizedValue.Text("Woodworking"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.harvest_result", value = EmbeddedLocalizedValue.Text("Area harvest"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.heading.tilling_result", value = EmbeddedLocalizedValue.Text("Area tilling"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.altitude", value = EmbeddedLocalizedValue.Text("Altitude"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.region", value = EmbeddedLocalizedValue.Text("Region"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.candidate_count", value = EmbeddedLocalizedValue.Text("Candidates"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.collectible_items", value = EmbeddedLocalizedValue.Text("Collectible hints"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.season", value = EmbeddedLocalizedValue.Text("Season"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.uses", value = EmbeddedLocalizedValue.Text("Uses"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.vegetation_group", value = EmbeddedLocalizedValue.Text("Vegetation group"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.amount", value = EmbeddedLocalizedValue.Text("Amount"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.processed_count", value = EmbeddedLocalizedValue.Text("Processed"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.data.special_materials", value = EmbeddedLocalizedValue.Text("Special materials"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.companion_minerals", value = EmbeddedLocalizedValue.Text("Companion minerals"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.forest_products", value = EmbeddedLocalizedValue.Text("Bark, resin, nuts, and similar products"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.seasonal_plants", value = EmbeddedLocalizedValue.Text("Plants available in the current season"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.vegetation_none", value = EmbeddedLocalizedValue.Text("There does not seem to be anything noteworthy around here."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.forest_none", value = EmbeddedLocalizedValue.Text("There does not seem to be anything to gather from this tree"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.hint.forest_harvested", value = EmbeddedLocalizedValue.Text("There does not seem to be anything left to gather from this tree"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.display.list_separator", value = EmbeddedLocalizedValue.Text(", "), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.chisel.completed", value = EmbeddedLocalizedValue.Text("§aChisel mining is complete"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.altitude.high", value = EmbeddedLocalizedValue.Text("High"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.altitude.shallow", value = EmbeddedLocalizedValue.Text("Shallow"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.altitude.middle", value = EmbeddedLocalizedValue.Text("Mid-depth"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.altitude.deep", value = EmbeddedLocalizedValue.Text("Deep"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.temperate", value = EmbeddedLocalizedValue.Text("Temperate"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.cold", value = EmbeddedLocalizedValue.Text("Cold"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.dry", value = EmbeddedLocalizedValue.Text("Dry"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.wet", value = EmbeddedLocalizedValue.Text("Wet"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.mountain", value = EmbeddedLocalizedValue.Text("Mountain"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.inspection.biome.nether", value = EmbeddedLocalizedValue.Text("Nether"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.woodworking.completed", value = EmbeddedLocalizedValue.Text("§aWoodworking complete"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.woodworking.not_ready", value = EmbeddedLocalizedValue.Text("§7It seems this timber is not ready to be worked yet"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.batch.completed", value = EmbeddedLocalizedValue.Text("§aBatch operation complete"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.completed", value = EmbeddedLocalizedValue.Text("§aGathered plants"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.surface_depleted", value = EmbeddedLocalizedValue.Text("§7It seems there is nothing to gather here for a while"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.season.spring", value = EmbeddedLocalizedValue.Text("Spring"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.season.summer", value = EmbeddedLocalizedValue.Text("Summer"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.season.autumn", value = EmbeddedLocalizedValue.Text("Autumn"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.gathering.season.winter", value = EmbeddedLocalizedValue.Text("Winter"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.forest.harvested", value = EmbeddedLocalizedValue.Text("§7There does not seem to be anything left to gather from this tree"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.forest.completed", value = EmbeddedLocalizedValue.Text("§aGathered forest products"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.cultivation.harvested", value = EmbeddedLocalizedValue.Text("§aHarvested crops"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource_collection.cultivation.tilled", value = EmbeddedLocalizedValue.Text("§aTilled the soil"), domain = DOMAIN),
    )

}
