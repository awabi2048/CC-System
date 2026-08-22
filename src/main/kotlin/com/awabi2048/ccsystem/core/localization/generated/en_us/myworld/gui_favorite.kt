package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsMyworldGuiFavoriteCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "myworld/gui_favorite"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "gui.favorite.title", value = EmbeddedLocalizedValue.Text("Favorite Worlds"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.empty_message", value = EmbeddedLocalizedValue.Text("§7No favorites match the current filter."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.empty_message_no_favorites", value = EmbeddedLocalizedValue.Text("§7No favorites yet."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.current_world.name", value = EmbeddedLocalizedValue.Text("§bCurrent World"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.current_world.unmanaged", value = EmbeddedLocalizedValue.Text("§eThis world is not managed as a MyWorld."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.warp", value = EmbeddedLocalizedValue.Text("Warp to this world"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.preview", value = EmbeddedLocalizedValue.Text("Preview this world"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.unfavorite", value = EmbeddedLocalizedValue.Text("Remove from favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.open_actions", value = EmbeddedLocalizedValue.Text("Open world actions"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.direct_warp_unavailable", value = EmbeddedLocalizedValue.Text("§eThe current access settings do not allow a direct warp to this world."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.world_item.archived_label", value = EmbeddedLocalizedValue.Text("§c§l[ARCHIVED]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.info.display", value = EmbeddedLocalizedValue.Text("§fStatistics"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.player_icon.name", value = EmbeddedLocalizedValue.Text("§b{player}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.player_icon.lore_count", value = EmbeddedLocalizedValue.Text("Favorited Worlds"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.other_worlds.name", value = EmbeddedLocalizedValue.Text("§aOther Worlds of this Player"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.other_worlds.lore", value = EmbeddedLocalizedValue.TextList(listOf("§7View other worlds owned by", "§7the owner of this world.")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.other_worlds.action", value = EmbeddedLocalizedValue.Text("View other worlds"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.name_add", value = EmbeddedLocalizedValue.Text("§bAdd to Favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.name_remove", value = EmbeddedLocalizedValue.Text("§cRemove from Favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.name_restricted", value = EmbeddedLocalizedValue.Text("§7Cannot Favorite"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.lore_add", value = EmbeddedLocalizedValue.Text("§7Add this world to your favorites."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.lore_remove", value = EmbeddedLocalizedValue.Text("§7Remove this world from your favorites."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.lore_restricted_owner", value = EmbeddedLocalizedValue.Text("§cYou cannot favorite your own world."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.lore_restricted_not_managed", value = EmbeddedLocalizedValue.Text("§cThis location cannot be favorited."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.lore_restricted_member", value = EmbeddedLocalizedValue.Text("§cYou cannot favorite a world you are a member of."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.favorite_menu.toggle.action", value = EmbeddedLocalizedValue.Text("Toggle favorite"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.remove_confirm.title", value = EmbeddedLocalizedValue.Text("Confirmation of cancellation of favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.remove_confirm.lore", value = EmbeddedLocalizedValue.TextList(listOf("§fWorld \"§a{world}§f\"", "§cRemove from favorites§f?", "If you cancel §7, it will be removed from your favorites list.")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.remove_confirm.confirm", value = EmbeddedLocalizedValue.Text("§aRemove Favorite"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.title", value = EmbeddedLocalizedValue.Text("Actions for {world}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.warp", value = EmbeddedLocalizedValue.Text("§aWarp to World"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.preview", value = EmbeddedLocalizedValue.Text("§bPreview World"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.invite", value = EmbeddedLocalizedValue.Text("§eInvite Nearby Players to This World"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.invite_description", value = EmbeddedLocalizedValue.TextList(listOf("Invite eligible players in your current world", "to this favorite world.")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.invite_recipient_count", value = EmbeddedLocalizedValue.Text("Eligible Players"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.invite_unavailable", value = EmbeddedLocalizedValue.Text("§cGroup invitations are unavailable for this world."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.invite_no_recipients", value = EmbeddedLocalizedValue.Text("§eThere are no eligible players in your current world."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.actions.unfavorite", value = EmbeddedLocalizedValue.Text("§cRemove from Favorites"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.invite_confirm.title", value = EmbeddedLocalizedValue.Text("Confirm Group Invitation"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.invite_confirm.recipient_count", value = EmbeddedLocalizedValue.Text("Players to Invite"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "gui.favorite.invite_confirm.confirm", value = EmbeddedLocalizedValue.Text("§aSend Invitations"), domain = DOMAIN),
    )

}
