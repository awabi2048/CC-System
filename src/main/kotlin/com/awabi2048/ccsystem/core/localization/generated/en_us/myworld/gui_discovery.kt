package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsMyworldGuiDiscoveryCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "myworld/gui_discovery"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "gui.discovery.title", value = EmbeddedLocalizedValue.Text("Discovery"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.warp_hint", value = EmbeddedLocalizedValue.Text("Warp to this world"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.preview_hint", value = EmbeddedLocalizedValue.Text("Show preview"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.member_request_hint", value = EmbeddedLocalizedValue.Text("Show membership request"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.favorite_hint_add", value = EmbeddedLocalizedValue.Text("Add to favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.favorite_hint_remove", value = EmbeddedLocalizedValue.Text("Remove from favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.favorite_toggle", value = EmbeddedLocalizedValue.Text("Toggle favorite"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.fav_add", value = EmbeddedLocalizedValue.Text("Add to favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.fav_remove", value = EmbeddedLocalizedValue.Text("Remove from favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.world_item.spotlight_remove", value = EmbeddedLocalizedValue.Text("Remove from SPOTLIGHT"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_empty.name", value = EmbeddedLocalizedValue.Text("§7[Empty slot]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_empty.description", value = EmbeddedLocalizedValue.Text("This slot is currently vacant."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_empty.action.register", value = EmbeddedLocalizedValue.Text("Register the current MyWorld"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.display", value = EmbeddedLocalizedValue.Text("§aSelecting the sorting method"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.label", value = EmbeddedLocalizedValue.Text("Sort order"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.name", value = EmbeddedLocalizedValue.Text("§aSelect Sort Order"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.action.previous", value = EmbeddedLocalizedValue.Text("Select previous sort order"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.action.next", value = EmbeddedLocalizedValue.Text("Select next sort order"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.action.edit_spotlight", value = EmbeddedLocalizedValue.Text("Edit SPOTLIGHT description"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.type.hot", value = EmbeddedLocalizedValue.Text("Popular"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.type.new", value = EmbeddedLocalizedValue.Text("New"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.type.favorites", value = EmbeddedLocalizedValue.Text("Favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.type.spotlight", value = EmbeddedLocalizedValue.Text("Spotlight"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort.type.random", value = EmbeddedLocalizedValue.Text("Random"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.name", value = EmbeddedLocalizedValue.Text("Tag Filter"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.label", value = EmbeddedLocalizedValue.Text("Selected tag"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.no_selection", value = EmbeddedLocalizedValue.Text("No selection"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.all", value = EmbeddedLocalizedValue.Text("All"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.action.next", value = EmbeddedLocalizedValue.Text("Select next tag"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.action.previous", value = EmbeddedLocalizedValue.Text("Select previous tag"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.tag_filter.action.clear", value = EmbeddedLocalizedValue.Text("Clear filter"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.name", value = EmbeddedLocalizedValue.Text("Special Filter"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.label", value = EmbeddedLocalizedValue.Text("Selected condition"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.action.next", value = EmbeddedLocalizedValue.Text("Select next condition"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.action.previous", value = EmbeddedLocalizedValue.Text("Select previous condition"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.action.clear", value = EmbeddedLocalizedValue.Text("Clear filter"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.type.none", value = EmbeddedLocalizedValue.Text("None"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.special_filter.type.unvisited", value = EmbeddedLocalizedValue.Text("Unvisited worlds"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_description_dialog.title", value = EmbeddedLocalizedValue.Text("Edit SPOTLIGHT Description"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_description_dialog.body", value = EmbeddedLocalizedValue.Text("Enter the description shown while SPOTLIGHT sort is selected (max {max} chars). Leave empty to restore default."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_description_dialog.input_label", value = EmbeddedLocalizedValue.Text("Description"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.stats.name", value = EmbeddedLocalizedValue.Text("§fStatistics"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.stats.sort_label", value = EmbeddedLocalizedValue.Text("Sort"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.stats.tag_label", value = EmbeddedLocalizedValue.Text("Tag"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.stats.count_label", value = EmbeddedLocalizedValue.Text("Results"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.stats.desc", value = EmbeddedLocalizedValue.Text("§7 Displaying worlds that match the conditions"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort_info.hot", value = EmbeddedLocalizedValue.Text("Sorted by recent visitors"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort_info.new", value = EmbeddedLocalizedValue.Text("Sorted by publication date"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort_info.favorites", value = EmbeddedLocalizedValue.Text("Sorted by number of favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort_info.spotlight", value = EmbeddedLocalizedValue.Text("Featured worlds"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.sort_info.random", value = EmbeddedLocalizedValue.Text("Daily random"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.no_result", value = EmbeddedLocalizedValue.Text("§7No worlds found matching the criteria"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_remove_confirm.title", value = EmbeddedLocalizedValue.Text("Remove from SPOTLIGHT"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.discovery.spotlight_remove_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fDo you want to remove", "§fworld \"§a{world}§f\" from §6SPOTLIGHT§f?")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.spotlight_confirm.title", value = EmbeddedLocalizedValue.Text("Register to SPOTLIGHT"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.spotlight_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fDo you want to register", "§fworld \"§a{world}§f\" to §6SPOTLIGHT§f?", "§7Registered worlds will be displayed")), domain = DOMAIN),
    )

}
