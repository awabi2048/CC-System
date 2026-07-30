package com.awabi2048.ccsystem.features.announce.listener

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiItemSpec
import com.awabi2048.ccsystem.api.gui.GuiLoreFrame
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiNameSpec
import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks
import com.awabi2048.ccsystem.api.gui.MenuActionHandler
import com.awabi2048.ccsystem.api.gui.MenuDialogButton
import com.awabi2048.ccsystem.api.gui.MenuDialogHandler
import com.awabi2048.ccsystem.api.gui.MenuDialogInput
import com.awabi2048.ccsystem.api.gui.MenuDialogRequest
import com.awabi2048.ccsystem.api.gui.MenuDialogResponse
import com.awabi2048.ccsystem.api.gui.MenuElement
import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRuntimeActions
import com.awabi2048.ccsystem.api.gui.MenuSound
import com.awabi2048.ccsystem.api.gui.MenuSoundPolicy
import com.awabi2048.ccsystem.api.gui.MenuUpdate
import com.awabi2048.ccsystem.api.gui.PlayerInventoryInteraction
import com.awabi2048.ccsystem.core.gui.GuiItemMarker
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import com.awabi2048.ccsystem.features.announce.command.AnnounceCommand
import com.awabi2048.ccsystem.features.announce.manager.AnnouncementManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AnnounceListener {
    init {
        CCSystem.getAPI().getMenuRuntimeService().register(
            InventoryMenuDefinition(
                owner = MENU_OWNER,
                id = MENU_ID,
                renderer = { context -> createMenuView(context.player, context.route) },
                actions = mapOf(
                    "menu-command" to MenuActionHandler { context -> executeMenuCommandAction(context.player) },
                    "select-icon" to MenuActionHandler { context -> beginIconSelection(context.player) },
                    "announcement" to MenuActionHandler { context ->
                        openAnnouncementAction(context.player, context.payload["id"], context.click)
                    },
                    MenuRuntimeActions.PLAYER_INVENTORY_CLICK to MenuActionHandler { context ->
                        selectAnnouncementIcon(context.player, context.item)
                    },
                ),
                onClose = { context -> pendingIconSelection.remove(context.player.uniqueId) },
            )
        )
    }

    companion object {
        private const val MENU_OWNER = "cc-system"
        private const val MENU_ID = "announcement"
        private const val MENU_SIZE = 54
        private val LEGACY = LegacyComponentSerializer.legacySection()
        private val pendingIconSelection = mutableSetOf<UUID>()

        private val INTERIOR_SLOTS: List<Int> = buildList {
            for (row in 1..4) {
                for (col in 1..7) {
                    add((row * 9) + col)
                }
            }
        }

        fun openAnnouncementMenu(player: Player, openedFromMenuArgument: Boolean) {
            pendingIconSelection.remove(player.uniqueId)
            CCSystem.getAPI().getMenuRuntimeService().open(
                player,
                MenuRoute(MENU_OWNER, MENU_ID, mapOf("fromMenu" to openedFromMenuArgument.toString()))
            )
        }

        private fun createMenuView(player: Player, route: MenuRoute): InventoryMenuView {
            val openedFromMenuArgument = route.payload["fromMenu"].toBoolean()
            val highlightIds = getHighlightAnnouncementIds(player)
            val inventory = createMenuInventory(player, openedFromMenuArgument, highlightIds)
            val announcements = AnnouncementManager.getAnnouncementsForMenu()
            val elements = buildList {
                for (slot in 0 until inventory.size) {
                    val item = inventory.getItem(slot) ?: continue
                    val announcement = announcements.getOrNull(INTERIOR_SLOTS.indexOf(slot))
                    val actionId = when {
                        slot == 45 && openedFromMenuArgument -> "menu-command"
                        slot == 49 && hasManagePermission(player) -> "select-icon"
                        announcement != null && hasManagePermission(player) -> "announcement"
                        else -> null
                    }
                    add(
                        MenuElement(
                            slot = slot,
                            item = item,
                            role = GuiItemMarker.role(item) ?: GuiElementRole.CONTENT,
                            actionId = actionId,
                            actionPayload = if (actionId == "announcement") {
                                mapOf("id" to requireNotNull(announcement).id)
                            } else emptyMap()
                        )
                    )
                }
            }
            PlayerDataManager.set(
                player.uniqueId,
                AnnouncementManager.PLAYER_DATA_LAST_CHECKED_AT,
                Instant.now().toString()
            )
            return InventoryMenuView(
                size = MENU_SIZE,
                title = LEGACY.deserialize(LanguageManager.getRawString(player, "announce.menu_title")),
                elements = elements,
                standardFrame = false,
                playerInventoryInteraction = PlayerInventoryInteraction.SELECTION
            )
        }

        private class MenuItems(val size: Int) {
            private val items = arrayOfNulls<ItemStack>(size)
            fun setItem(slot: Int, item: ItemStack?) {
                items[slot] = item
            }
            fun getItem(slot: Int): ItemStack? = items.getOrNull(slot)
        }

        private fun createMenuInventory(
            player: Player,
            openedFromMenuArgument: Boolean,
            highlightAnnouncementIds: Set<String>
        ): MenuItems {
            val inventory = MenuItems(MENU_SIZE)

            val blackPane = createPane(Material.BLACK_STAINED_GLASS_PANE)
            val grayPane = createPane(Material.GRAY_STAINED_GLASS_PANE)
            val whitePane = createPane(Material.WHITE_STAINED_GLASS_PANE)

            for (slot in 0..8) {
                inventory.setItem(slot, blackPane)
            }
            for (slot in 45..53) {
                inventory.setItem(slot, blackPane)
            }

            for (row in 1..4) {
                inventory.setItem(row * 9, grayPane)
                inventory.setItem((row * 9) + 8, grayPane)
            }

            INTERIOR_SLOTS.forEach { slot ->
                inventory.setItem(slot, whitePane)
            }

            val announcements = AnnouncementManager.getAnnouncementsForMenu()
            val formatter = createDateTimeFormatter(player)
            for ((index, announcement) in announcements.withIndex()) {
                val slot = INTERIOR_SLOTS[index]
                val titleLine = announcement.title.ifBlank {
                    LanguageManager.getRawString(player, "announce.item_default_title")
                }

                val contentBlock = mutableListOf<GuiLoreLine>()
                for (line in announcement.contentLines) {
                    if (line.isNotBlank()) {
                        contentBlock.add(GuiLoreLine.UserText(line))
                    }
                }
                val blocks = mutableListOf<GuiLoreBlock>()
                if (contentBlock.isNotEmpty()) blocks.add(GuiLoreBlock(contentBlock))
                if (hasManagePermission(player)) {
                    val metadataBlock = mutableListOf<GuiLoreLine>()
                    val issuedAt = formatDateTimeForDisplay(player, announcement.issuedAt, formatter)
                    metadataBlock.add(GuiLoreLine.Metadata(rawText(player, "announce.lore.issued_at"), issuedAt))
                    val updatedAt = formatDateTimeForDisplay(player, announcement.updatedAt, formatter)
                    metadataBlock.add(GuiLoreLine.Metadata(rawText(player, "announce.lore.updated_at"), updatedAt))
                    if (announcement.indefinite) {
                        metadataBlock.add(GuiLoreLine.Metadata(
                            rawText(player, "announce.lore.expires_at"),
                            rawText(player, "announce.lore.indefinite")
                        ))
                    } else {
                        val expiresAt = announcement.expiresAt
                        if (expiresAt != null) {
                            val expiresText = formatDateTimeForDisplay(player, expiresAt, formatter)
                            metadataBlock.add(GuiLoreLine.Metadata(rawText(player, "announce.lore.expires_at"), expiresText))
                        }
                    }

                    blocks.add(GuiLoreBlock(metadataBlock))
                    blocks.add(GuiLoreBlock(listOf(
                        GuiLoreLine.Interaction(player, MenuAcceptedClicks.LEFT, rawText(player, "announce.lore.edit")),
                        GuiLoreLine.Interaction(player, MenuAcceptedClicks.RIGHT, rawText(player, "announce.lore.delete"))
                    )))
                }

                val item = CCSystem.getAPI().getGuiElementService().item(
                    GuiItemSpec(
                        material = announcement.icon,
                        name = GuiNameSpec.Component(noItalic(deserializeStyledUserText(titleLine))),
                        lore = if (blocks.isEmpty()) GuiLoreSpec.None else GuiLoreSpec.Blocks(blocks),
                        role = GuiElementRole.CONTENT,
                        amount = 1
                    )
                )
                if (highlightAnnouncementIds.contains(announcement.id)) {
                    item.editMeta { meta -> meta.setEnchantmentGlintOverride(true) }
                }
                inventory.setItem(slot, item)
            }

            if (openedFromMenuArgument) {
                inventory.setItem(45, createMenuCommandItem(player))
            }

            if (hasManagePermission(player)) {
                inventory.setItem(49, createAddItem(player))
            }

            return inventory
        }

        private fun createPane(material: Material): ItemStack {
            return CCSystem.getAPI().getGuiElementService().item(
                GuiItemSpec(
                    material = material,
                    name = GuiNameSpec.Component(noItalic(Component.text(" "))),
                    lore = GuiLoreSpec.None,
                    role = GuiElementRole.DECORATION,
                    amount = 1
                )
            )
        }

        private fun createMenuCommandItem(player: Player): ItemStack {
            return CCSystem.getAPI().getGuiElementService().item(
                GuiItemSpec(
                    material = Material.REDSTONE,
                    name = GuiNameSpec.Component(
                        noItalic(LanguageManager.getMessageWithoutPrefix(player, "announce.menu_command_item_name"))
                    ),
                    lore = GuiLoreSpec.Rich(
                        listOf(singleClickLine(player, "announce.menu_command_item_lore")),
                        GuiLoreFrame.NONE
                    ),
                    role = GuiElementRole.ACTION,
                    amount = 1
                )
            )
        }

        private fun createAddItem(player: Player): ItemStack {
            return CCSystem.getAPI().getGuiElementService().item(
                GuiItemSpec(
                    material = Material.WRITABLE_BOOK,
                    name = GuiNameSpec.Component(
                        noItalic(LanguageManager.getMessageWithoutPrefix(player, "announce.add_item_name"))
                    ),
                    lore = GuiLoreSpec.Rich(
                        listOf(singleClickLine(player, "announce.add_item_lore")),
                        GuiLoreFrame.NONE
                    ),
                    role = GuiElementRole.ACTION,
                    amount = 1
                )
            )
        }

        private fun hasManagePermission(player: Player): Boolean {
            return player.hasPermission(AnnounceCommand.MANAGE_PERMISSION) ||
                player.hasPermission("cc-system.admin") ||
                player.hasPermission("cc-system.*") ||
                player.isOp
        }

        private fun getHighlightAnnouncementIds(player: Player): Set<String> {
            val lastLogoutAt = AnnouncementManager.parseInstant(
                PlayerDataManager.getString(player.uniqueId, AnnouncementManager.PLAYER_DATA_LAST_LOGOUT_AT)
            )
            val lastCheckedAt = AnnouncementManager.parseInstant(
                PlayerDataManager.getString(player.uniqueId, AnnouncementManager.PLAYER_DATA_LAST_CHECKED_AT)
            )

            return AnnouncementManager.getNotificationTargetAnnouncements(lastLogoutAt, lastCheckedAt)
                .map { it.id }
                .toSet()
        }

        private fun createDateTimeFormatter(player: Player): DateTimeFormatter {
            val patternRaw = LanguageManager.getRawString(player, "announce.datetime_format")
            val pattern =
                if (patternRaw == "announce.datetime_format" || patternRaw.isBlank()) {
                    "yyyy/MM/dd HH:mm"
                } else {
                    patternRaw
                }

            val lang = getPlayerLangCode(player)
            val locale = if (lang.startsWith("en")) Locale.US else Locale.JAPAN

            return try {
                DateTimeFormatter.ofPattern(pattern, locale)
            } catch (_: IllegalArgumentException) {
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", locale)
            }
        }

        private fun formatDateTimeForDisplay(player: Player, instant: Instant, formatter: DateTimeFormatter): String {
            val formatted = formatter.format(instant.atZone(ZoneId.of("Asia/Tokyo")))
            return if (isJapaneseLanguage(player)) {
                formatted
            } else {
                "$formatted (JST)"
            }
        }

        private fun isJapaneseLanguage(player: Player): Boolean {
            return getPlayerLangCode(player).startsWith("ja")
        }

        private fun getPlayerLangCode(player: Player): String {
            val defaultLang = ConfigManager.getDefaultLanguage()
            return PlayerDataManager.getString(player.uniqueId, "lang", defaultLang)?.lowercase() ?: defaultLang
        }

        private fun deserializeStyledUserText(raw: String): Component {
            val normalized = raw.replace('§', '&')
            return LanguageManager.deserializeLegacy("&7$normalized")
        }

        private fun noItalic(component: Component): Component {
            return component
                .colorIfAbsent(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
        }

        private fun rawText(player: Player?, key: String): String = LanguageManager.getRawString(player, key)

        private fun singleClickLine(player: Player, actionKey: String): GuiLoreLine.Interaction =
            GuiLoreLine.Interaction(
                player,
                MenuAcceptedClicks.STANDARD,
                rawText(player, actionKey),
            )

        private fun playOperationSound(player: Player, sound: Sound, pitch: Float = 1.0f) {
            player.playSound(player.location, sound, 0.8f, pitch)
        }

        private fun notifyAnnouncementIssuedToOthers(issuer: Player, title: String) {
            for (target in Bukkit.getOnlinePlayers()) {
                if (target.uniqueId == issuer.uniqueId) {
                    continue
                }
                target.sendMessage(
                    LanguageManager.getMessage(
                        target,
                        "announce.notify.issued_title",
                        "title" to title
                    )
                )
                target.sendMessage(LanguageManager.getMessage(target, "announce.notify.toast_description"))
            }
        }
    }

    private fun selectAnnouncementIcon(player: Player, selectedItem: ItemStack): MenuActionResult {
        if (!isAnnouncementRoute(player) || !pendingIconSelection.contains(player.uniqueId)) {
            return MenuActionResult.Ignored
        }
        if (selectedItem.type == Material.AIR) return MenuActionResult.Ignored

        if (AnnouncementManager.getAnnouncementCount() >= AnnouncementManager.MAX_VISIBLE_ANNOUNCEMENTS) {
            pendingIconSelection.remove(player.uniqueId)
            player.sendMessage(
                LanguageManager.getMessage(
                    player,
                    "announce.add_limit_reached",
                    "max" to AnnouncementManager.MAX_VISIBLE_ANNOUNCEMENTS.toString()
                )
            )
            playOperationSound(player, Sound.BLOCK_NOTE_BLOCK_BASS)
            return MenuActionResult.Rejected()
        }

        pendingIconSelection.remove(player.uniqueId)
        playOperationSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f)
        openAddDialog(player, selectedItem.type)
        return MenuActionResult.Success(MenuUpdate.Close, MenuSoundPolicy.Silent)
    }

    private fun isAnnouncementRoute(player: Player): Boolean {
        val route = CCSystem.getAPI().getMenuNavigationService().currentRoute(player) ?: return false
        return route.owner == MENU_OWNER && route.id == MENU_ID
    }

    private fun beginIconSelection(player: Player): MenuActionResult {
        if (AnnouncementManager.getAnnouncementCount() >= AnnouncementManager.MAX_VISIBLE_ANNOUNCEMENTS) {
            return MenuActionResult.Rejected(
                LanguageManager.getMessage(
                    player,
                    "announce.add_limit_reached",
                    "max" to AnnouncementManager.MAX_VISIBLE_ANNOUNCEMENTS.toString()
                ),
                MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
            )
        }
        pendingIconSelection.add(player.uniqueId)
        player.sendMessage(LanguageManager.getMessage(player, "announce.icon_select_instruction"))
        return MenuActionResult.Success(
            MenuUpdate.None,
            MenuSoundPolicy.Custom(MenuSound("ITEM_BOOK_PAGE_TURN"))
        )
    }

    private fun openAnnouncementAction(
        player: Player,
        announcementId: String?,
        click: org.bukkit.event.inventory.ClickType
    ): MenuActionResult {
        if (!hasManagePermission(player)) return MenuActionResult.Rejected()
        val announcement = announcementId?.let(AnnouncementManager::getAnnouncementById)
            ?: return MenuActionResult.Rejected()
        pendingIconSelection.remove(player.uniqueId)
        CCSystem.getAPI().getMenuRuntimeService().close(player)
        if (click.isRightClick) openDeleteConfirmDialog(player, announcement)
        else openEditDialog(player, announcement)
        return MenuActionResult.Success(MenuUpdate.None)
    }

    private fun executeMenuCommandAction(player: Player): MenuActionResult {
        val menuCommand = ConfigManager.getAnnounceMenuCommand().trim().removePrefix("/")
        if (menuCommand.isBlank()) {
            return MenuActionResult.Rejected(
                LanguageManager.getMessage(player, "announce.menu_command_not_configured")
            )
        }
        if (!player.performCommand(menuCommand)) {
            return MenuActionResult.Rejected(
                LanguageManager.getMessage(player, "announce.menu_command_failed"),
                MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
            )
        }
        return MenuActionResult.Success(MenuUpdate.Close)
    }

    private fun openEditDialog(
        player: Player,
        announcement: AnnouncementManager.Announcement,
        formState: AnnounceFormState = AnnounceFormState(
            title = announcement.title,
            content1 = announcement.contentLines.getOrElse(0) { "" },
            content2 = announcement.contentLines.getOrElse(1) { "" },
            content3 = announcement.contentLines.getOrElse(2) { "" },
            endAtRaw = announcement.endAtRaw ?: "",
            indefinite = announcement.indefinite
        ),
        endAtWarningKey: String? = null,
        titleWarningKey: String? = null
    ) {
        val titleLabel = buildTitleLabel(player, titleWarningKey)
        val endAtLabel = buildEndAtLabel(player, endAtWarningKey)
        val inputs = createAnnouncementInputs(titleLabel, endAtLabel, formState)

        val body = LanguageManager.getStringList(player, "announce.edit_dialog_body")
            .ifEmpty { listOf("お知らせを編集します。") }
            .joinToString("\n")
        CCSystem.getAPI().getMenuDialogService().show(
            player,
            MenuDialogRequest(
                owner = MENU_OWNER,
                id = "announcement-edit",
                title = LanguageManager.getMessageWithoutPrefix(player, "announce.edit_dialog_title"),
                body = listOf(Component.text(body)),
                inputs = inputs,
                confirm = MenuDialogButton(
                    LanguageManager.getMessageWithoutPrefix(player, "announce.edit_dialog_confirm"),
                    MenuDialogHandler { target, response ->
                        val nextState = response.toAnnounceFormState()
                        val contentLines = nextState.contentLines()
                        when (AnnouncementManager.updateAnnouncement(
                            announcement.id,
                            nextState.title,
                            contentLines,
                            nextState.endAtRaw,
                            nextState.indefinite
                        )) {
                    AnnouncementManager.AddResult.SUCCESS -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.edit_success"))
                        openAnnouncementMenu(target, openedFromMenuArgument = false)
                                MenuActionResult.Success(
                                    MenuUpdate.None,
                                    MenuSoundPolicy.Custom(MenuSound("ENTITY_PLAYER_LEVELUP", pitch = 1.2f))
                                )
                    }

                    AnnouncementManager.AddResult.NOT_FOUND -> {
                        openAnnouncementMenu(target, openedFromMenuArgument = false)
                                MenuActionResult.Rejected(
                                    LanguageManager.getMessage(target, "announce.edit_target_not_found"),
                                    MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
                                )
                    }

                    AnnouncementManager.AddResult.INVALID_END_AT_FORMAT -> {
                        openEditDialog(
                            target,
                            announcement,
                            nextState,
                            endAtWarningKey = "announce.end_at_warning.invalid_format"
                        )
                                MenuActionResult.Rejected(
                                    LanguageManager.getMessage(target, "announce.invalid_duration"),
                                    MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
                                )
                    }

                    AnnouncementManager.AddResult.END_AT_NOT_FUTURE -> {
                        openEditDialog(
                            target,
                            announcement,
                            nextState,
                            endAtWarningKey = "announce.end_at_warning.not_future"
                        )
                                MenuActionResult.Rejected(
                                    LanguageManager.getMessage(target, "announce.end_at_not_future"),
                                    MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
                                )
                    }

                    AnnouncementManager.AddResult.INVALID_TITLE -> {
                        openEditDialog(
                            target,
                            announcement,
                            nextState,
                            titleWarningKey = "announce.title_warning.required"
                        )
                                MenuActionResult.Rejected(
                                    LanguageManager.getMessage(target, "announce.invalid_title"),
                                    MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
                                )
                    }

                    AnnouncementManager.AddResult.LIMIT_REACHED -> {
                                MenuActionResult.Rejected(
                                    LanguageManager.getMessage(target, "announce.add_limit_reached", "max" to AnnouncementManager.MAX_VISIBLE_ANNOUNCEMENTS.toString()),
                                    MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
                                )
                    }
                }
                    },
                ),
                cancel = closeDialogButton(player, "announce.dialog_cancel"),
            )
        )
    }

    private fun openDeleteConfirmDialog(player: Player, announcement: AnnouncementManager.Announcement) {
        val body = LanguageManager.getStringListWithPlaceholders(
            player,
            "announce.delete_dialog_body",
            "title" to announcement.title
        ).ifEmpty { listOf("このお知らせを削除します。") }
            .toMutableList()

        body.add(LanguageManager.getRawString(player, "announce.delete_dialog_content_label"))
        val contentLines = announcement.contentLines.filter { it.isNotBlank() }
        if (contentLines.isEmpty()) {
            body.add(LanguageManager.getRawString(player, "announce.delete_dialog_content_empty"))
        } else {
            body.addAll(contentLines)
        }

        CCSystem.getAPI().getMenuDialogService().show(
            player,
            MenuDialogRequest(
                owner = MENU_OWNER,
                id = "announcement-delete",
                title = LanguageManager.getMessageWithoutPrefix(player, "announce.delete_dialog_title"),
                body = listOf(Component.text(body.joinToString("\n"))),
                confirm = MenuDialogButton(
                    LanguageManager.getMessageWithoutPrefix(player, "announce.delete_dialog_confirm"),
                    MenuDialogHandler { target, _ ->
                        val deleted = AnnouncementManager.deleteAnnouncement(announcement.id)
                        if (deleted) {
                            target.sendMessage(LanguageManager.getMessage(target, "announce.delete_success"))
                        }
                        openAnnouncementMenu(target, openedFromMenuArgument = false)
                        if (deleted) {
                            MenuActionResult.Success(
                                MenuUpdate.None,
                                MenuSoundPolicy.Custom(MenuSound("ENTITY_EXPERIENCE_ORB_PICKUP", pitch = 0.9f))
                            )
                        } else {
                            MenuActionResult.Rejected(
                                LanguageManager.getMessage(target, "announce.edit_target_not_found"),
                                MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
                            )
                        }
                    },
                ),
                cancel = closeDialogButton(player, "announce.delete_dialog_cancel"),
            )
        )
    }

    private fun openAddDialog(
        player: Player,
        iconType: Material,
        formState: AnnounceFormState = AnnounceFormState(),
        endAtWarningKey: String? = null,
        titleWarningKey: String? = null
    ) {
        val titleLabel = buildTitleLabel(player, titleWarningKey)
        val endAtLabel = buildEndAtLabel(player, endAtWarningKey)
        val inputs = createAnnouncementInputs(titleLabel, endAtLabel, formState)

        val body = LanguageManager.getStringList(player, "announce.dialog_body")
            .ifEmpty { listOf("お知らせを追加します。") }
            .joinToString("\n")
        CCSystem.getAPI().getMenuDialogService().show(
            player,
            MenuDialogRequest(
                owner = MENU_OWNER,
                id = "announcement-add",
                title = LanguageManager.getMessageWithoutPrefix(player, "announce.dialog_title"),
                body = listOf(Component.text(body)),
                inputs = inputs,
                confirm = MenuDialogButton(
                    LanguageManager.getMessageWithoutPrefix(player, "announce.dialog_confirm"),
                    MenuDialogHandler { target, response ->
                        val nextState = response.toAnnounceFormState()
                        val contentLines = nextState.contentLines()
                        when (AnnouncementManager.addAnnouncement(
                            nextState.title,
                            iconType,
                            contentLines,
                            nextState.endAtRaw,
                            nextState.indefinite
                        )) {
                    AnnouncementManager.AddResult.SUCCESS -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.add_success"))
                                notifyAnnouncementIssuedToOthers(target, nextState.title)
                        openAnnouncementMenu(target, openedFromMenuArgument = false)
                                MenuActionResult.Success(
                                    MenuUpdate.None,
                                    MenuSoundPolicy.Custom(MenuSound("ENTITY_PLAYER_LEVELUP", pitch = 1.3f))
                                )
                    }

                    AnnouncementManager.AddResult.NOT_FOUND -> {
                                MenuActionResult.Rejected(
                                    LanguageManager.getMessage(target, "announce.edit_target_not_found"),
                                    MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
                                )
                    }

                    AnnouncementManager.AddResult.LIMIT_REACHED -> {
                                MenuActionResult.Rejected(
                            LanguageManager.getMessage(
                                target,
                                "announce.add_limit_reached",
                                "max" to AnnouncementManager.MAX_VISIBLE_ANNOUNCEMENTS.toString()
                                    ),
                                    MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
                        )
                    }

                    AnnouncementManager.AddResult.INVALID_END_AT_FORMAT -> {
                        openAddDialog(
                            target,
                            iconType,
                            nextState,
                            endAtWarningKey = "announce.end_at_warning.invalid_format"
                        )
                                MenuActionResult.Rejected(
                                    LanguageManager.getMessage(target, "announce.invalid_duration"),
                                    MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
                                )
                    }

                    AnnouncementManager.AddResult.END_AT_NOT_FUTURE -> {
                        openAddDialog(
                            target,
                            iconType,
                            nextState,
                            endAtWarningKey = "announce.end_at_warning.not_future"
                        )
                                MenuActionResult.Rejected(
                                    LanguageManager.getMessage(target, "announce.end_at_not_future"),
                                    MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
                                )
                    }

                    AnnouncementManager.AddResult.INVALID_TITLE -> {
                        openAddDialog(
                            target,
                            iconType,
                            nextState,
                            titleWarningKey = "announce.title_warning.required"
                        )
                                MenuActionResult.Rejected(
                                    LanguageManager.getMessage(target, "announce.invalid_title"),
                                    MenuSoundPolicy.Custom(MenuSound("BLOCK_NOTE_BLOCK_BASS"))
                                )
                    }
                }
                    },
                ),
                cancel = closeDialogButton(player, "announce.dialog_cancel"),
            )
        )
    }

    private fun buildTitleLabel(player: Player, warningKey: String?): Component {
        val base = "タイトル"
        if (warningKey.isNullOrBlank()) {
            return Component.text(base)
        }

        val warning = LanguageManager.getRawString(player, warningKey)
        return Component.text(base)
            .append(Component.text(" "))
            .append(LanguageManager.deserializeLegacy(warning))
    }

    private fun createAnnouncementInputs(
        titleLabel: Component,
        endAtLabel: Component,
        state: AnnounceFormState,
    ): List<MenuDialogInput> = listOf(
        MenuDialogInput.Text("title", titleLabel, state.title, width = 310, maxLength = 64),
        MenuDialogInput.Text("content_1", Component.text("内容1"), state.content1, width = 310, maxLength = 128),
        MenuDialogInput.Text("content_2", Component.text("内容2"), state.content2, width = 310, maxLength = 128),
        MenuDialogInput.Text("content_3", Component.text("内容3"), state.content3, width = 310, maxLength = 128),
        MenuDialogInput.Text("duration", endAtLabel, state.endAtRaw, width = 310, maxLength = 128),
        MenuDialogInput.BooleanInput("indefinite", Component.text("無期限にする"), state.indefinite),
    )

    private fun MenuDialogResponse.toAnnounceFormState(): AnnounceFormState = AnnounceFormState(
        title = textValue("title"),
        content1 = textValue("content_1"),
        content2 = textValue("content_2"),
        content3 = textValue("content_3"),
        endAtRaw = textValue("duration"),
        indefinite = booleanValue("indefinite"),
    )

    private fun AnnounceFormState.contentLines(): List<String> = listOf(content1, content2, content3)

    private fun closeDialogButton(player: Player, labelKey: String): MenuDialogButton = MenuDialogButton(
        LanguageManager.getMessageWithoutPrefix(player, labelKey),
        MenuDialogHandler { _, _ -> MenuActionResult.Success(MenuUpdate.Close) },
    )

    private fun buildEndAtLabel(player: Player, warningKey: String?): Component {
        val base = "終了時刻"
        if (warningKey.isNullOrBlank()) {
            return Component.text(base)
        }

        val warning = LanguageManager.getRawString(player, warningKey)
        return Component.text(base)
            .append(Component.text(" "))
            .append(LanguageManager.deserializeLegacy(warning))
    }

    private data class AnnounceFormState(
        val title: String = "",
        val content1: String = "",
        val content2: String = "",
        val content3: String = "",
        val endAtRaw: String = "",
        val indefinite: Boolean = false
    )

}
