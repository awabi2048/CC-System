package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsMyworldGuiPortalCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "myworld/gui_portal"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "gui.portal.title", value = EmbeddedLocalizedValue.Text("Portal Settings"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.id_label", value = EmbeddedLocalizedValue.Text("PORTAL_ID"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.toggle_text.name", value = EmbeddedLocalizedValue.Text("§aToggle Floating Text"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.toggle_text.current_label", value = EmbeddedLocalizedValue.Text("Current setting"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.toggle_text.action", value = EmbeddedLocalizedValue.Text("Toggle floating text"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.destination_label", value = EmbeddedLocalizedValue.Text("§7Destination: "), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.color.name", value = EmbeddedLocalizedValue.Text("§eParticle Color"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.color.current_label", value = EmbeddedLocalizedValue.Text("Current color"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.color.previous", value = EmbeddedLocalizedValue.Text("§7« {color}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.color.next", value = EmbeddedLocalizedValue.Text("§7{color} »"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.color.action", value = EmbeddedLocalizedValue.Text("Cycle particle color"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.remove.name", value = EmbeddedLocalizedValue.Text("§cRemove Portal"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.remove.description", value = EmbeddedLocalizedValue.Text("Remove the installed portal."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal.remove.action", value = EmbeddedLocalizedValue.Text("Remove portal"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal_item.name", value = EmbeddedLocalizedValue.Text("§bWorld Portal"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal_item.destination", value = EmbeddedLocalizedValue.Text("Destination"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal_item.action.link", value = EmbeddedLocalizedValue.Text("Link the current My World"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal_item.action.place", value = EmbeddedLocalizedValue.Text("Place a World Portal"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.portal_item.action.unlink", value = EmbeddedLocalizedValue.Text("Unlink"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.world_gate_item.name", value = EmbeddedLocalizedValue.Text("§3World Gate"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.world_gate_item.destination", value = EmbeddedLocalizedValue.Text("Destination"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.world_gate_item.action.link", value = EmbeddedLocalizedValue.Text("Link the current My World"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.world_gate_item.action.relink", value = EmbeddedLocalizedValue.Text("Reconfigure the link"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.world_gate_item.action.select_area", value = EmbeddedLocalizedValue.Text("Select two points for the area"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.title", value = EmbeddedLocalizedValue.Text("Remote portal management"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.name", value = EmbeddedLocalizedValue.Text("§b Portal: §f{id}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.owner", value = EmbeddedLocalizedValue.Text("Owner"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.world", value = EmbeddedLocalizedValue.Text("Placed world"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.coordinates", value = EmbeddedLocalizedValue.Text("Coordinates"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.action.teleport", value = EmbeddedLocalizedValue.Text("Teleport to portal location"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.portal_item.action.remove", value = EmbeddedLocalizedValue.Text("Remove portal"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.sort.display", value = EmbeddedLocalizedValue.Text("§e Sorting method"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.sort.label", value = EmbeddedLocalizedValue.Text("Sort order"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.sort.created_desc", value = EmbeddedLocalizedValue.Text("Installation date (newest first)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.admin_portals.sort.created_asc", value = EmbeddedLocalizedValue.Text("Installation date (oldest first)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.invite.target_head.click_invite", value = EmbeddedLocalizedValue.Text("Invite this player"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.input.title", value = EmbeddedLocalizedValue.Text("Visit Target Player"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.input.label", value = EmbeddedLocalizedValue.Text("Player Name"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.input.placeholder", value = EmbeddedLocalizedValue.Text("Enter player name"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.title", value = EmbeddedLocalizedValue.Text("{player}'s world"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.world_item.warp", value = EmbeddedLocalizedValue.Text("Warp to this world"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.world_item.fav_add", value = EmbeddedLocalizedValue.Text("Add to favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visit.world_item.fav_remove", value = EmbeddedLocalizedValue.Text("Remove from favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.input.title", value = EmbeddedLocalizedValue.Text("Visit World Search"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.input.label", value = EmbeddedLocalizedValue.Text("Search Keyword"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.input.placeholder", value = EmbeddedLocalizedValue.Text("Search by world name"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.title", value = EmbeddedLocalizedValue.Text("World Search: {query}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.info.name", value = EmbeddedLocalizedValue.Text("§6Search Info"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.info.query_label", value = EmbeddedLocalizedValue.Text("Query"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.info.hit_label", value = EmbeddedLocalizedValue.Text("Results"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.info.shown_label", value = EmbeddedLocalizedValue.Text("Shown"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.visitworld.info.page_label", value = EmbeddedLocalizedValue.Text("Page"), domain = DOMAIN),
    )

}
