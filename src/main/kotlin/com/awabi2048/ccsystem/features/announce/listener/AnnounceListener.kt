package com.awabi2048.ccsystem.features.announce.listener

import com.awabi2048.ccsystem.api.gui.GuiLoreFrame
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.gui.LoreServiceImpl
import com.awabi2048.ccsystem.util.cancelWithDebug
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import com.awabi2048.ccsystem.features.announce.command.AnnounceCommand
import com.awabi2048.ccsystem.features.announce.manager.AnnouncementManager
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AnnounceListener : Listener {
    companion object {
        private const val MENU_SIZE = 54
        private val LORE_SERVICE = LoreServiceImpl()
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
            val highlightIds = getHighlightAnnouncementIds(player)
            val inventory = createMenuInventory(player, openedFromMenuArgument, highlightIds)
            player.openInventory(inventory)
            PlayerDataManager.set(player.uniqueId, AnnouncementManager.PLAYER_DATA_LAST_CHECKED_AT, Instant.now().toString())
            playOperationSound(player, Sound.UI_BUTTON_CLICK)
        }

        private fun createMenuInventory(
            player: Player,
            openedFromMenuArgument: Boolean,
            highlightAnnouncementIds: Set<String>
        ): Inventory {
            val holder = AnnouncementMenuHolder(openedFromMenuArgument)
            val title = LanguageManager.getRawString(player, "announce.menu_title")
            val inventory = Bukkit.createInventory(holder, MENU_SIZE, LegacyComponentSerializer.legacySection().deserialize(title))
            holder.bind(inventory)

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
                val item = ItemStack(announcement.icon)
                val meta = item.itemMeta ?: continue

                val titleLine = announcement.title.ifBlank {
                    LanguageManager.getRawString(player, "announce.item_default_title")
                }
                meta.displayName(noItalic(deserializeStyledUserText(titleLine)))

                if (highlightAnnouncementIds.contains(announcement.id)) {
                    meta.setEnchantmentGlintOverride(true)
                }

                val contentBlock = mutableListOf<GuiLoreLine>()
                for (line in announcement.contentLines) {
                    if (line.isNotBlank()) {
                        contentBlock.add(rawLine(deserializeStyledUserText(line)))
                    }
                }
                val blocks = mutableListOf<GuiLoreBlock>()
                if (contentBlock.isNotEmpty()) blocks.add(GuiLoreBlock(contentBlock))
                if (hasManagePermission(player)) {
                    val metadataBlock = mutableListOf<GuiLoreLine>()
                    val issuedAt = formatDateTimeForDisplay(player, announcement.issuedAt, formatter)
                    metadataBlock.add(
                        rawLine(
                            LanguageManager.getMessageWithoutPrefix(
                                player,
                                "announce.lore.issued_at",
                                "datetime" to issuedAt
                            )
                        )
                    )
                    val updatedAt = formatDateTimeForDisplay(player, announcement.updatedAt, formatter)
                    metadataBlock.add(
                        rawLine(
                            LanguageManager.getMessageWithoutPrefix(
                                player,
                                "announce.lore.updated_at",
                                "datetime" to updatedAt
                            )
                        )
                    )
                    if (announcement.indefinite) {
                        metadataBlock.add(
                            rawLine(
                                LanguageManager.getMessageWithoutPrefix(player, "announce.lore.indefinite")
                            )
                        )
                    } else {
                        val expiresAt = announcement.expiresAt
                        if (expiresAt != null) {
                            val expiresText = formatDateTimeForDisplay(player, expiresAt, formatter)
                            metadataBlock.add(
                                rawLine(
                                    LanguageManager.getMessageWithoutPrefix(
                                        player,
                                        "announce.lore.expires_at",
                                        "datetime" to expiresText
                                    )
                                )
                            )
                        }
                    }

                    blocks.add(GuiLoreBlock(metadataBlock))
                    blocks.add(GuiLoreBlock(listOf(
                        rawLine(
                            LanguageManager.getMessageWithoutPrefix(player, "announce.lore.help_left_click")
                        ),
                        rawLine(
                            LanguageManager.getMessageWithoutPrefix(player, "announce.lore.help_right_click")
                        )
                    )))
                }

                applyLoreOnlyTooltip(meta)
                meta.lore(LORE_SERVICE.render(if (blocks.isEmpty()) GuiLoreSpec.None else GuiLoreSpec.Blocks(blocks)))
                item.itemMeta = meta
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
            val item = ItemStack(material)
            val meta = item.itemMeta ?: return item
            meta.displayName(noItalic(Component.text(" ")))
            meta.setHideTooltip(true)
            item.itemMeta = meta
            return item
        }

        private fun createMenuCommandItem(player: Player): ItemStack {
            val item = ItemStack(Material.REDSTONE)
            val meta = item.itemMeta ?: return item
            meta.displayName(noItalic(LanguageManager.getMessageWithoutPrefix(player, "announce.menu_command_item_name")))
            meta.lore(
                LORE_SERVICE.render(
                    GuiLoreSpec.Rich(
                        listOf(rawLine(LanguageManager.getMessageWithoutPrefix(player, "announce.menu_command_item_lore"))),
                        GuiLoreFrame.NONE
                    )
                )
            )
            item.itemMeta = meta
            return item
        }

        private fun createAddItem(player: Player): ItemStack {
            val item = ItemStack(Material.WRITABLE_BOOK)
            val meta = item.itemMeta ?: return item
            meta.displayName(noItalic(LanguageManager.getMessageWithoutPrefix(player, "announce.add_item_name")))
            meta.lore(
                LORE_SERVICE.render(
                    GuiLoreSpec.Rich(
                        listOf(rawLine(LanguageManager.getMessageWithoutPrefix(player, "announce.add_item_lore"))),
                        GuiLoreFrame.NONE
                    )
                )
            )
            item.itemMeta = meta
            return item
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

        private fun rawLine(component: Component): GuiLoreLine.Raw = GuiLoreLine.Raw(LEGACY.serialize(component))

        private fun applyLoreOnlyTooltip(meta: org.bukkit.inventory.meta.ItemMeta) {
            val flagsToHide = ItemFlag.values().filterNot {
                it.name.contains("LORE") ||
                    it.name.contains("CUSTOM_NAME") ||
                    it.name.contains("TOOLTIP")
            }
            if (flagsToHide.isNotEmpty()) {
                meta.addItemFlags(*flagsToHide.toTypedArray())
            }
            meta.setHideTooltip(false)
        }

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

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val topHolder = event.view.topInventory.holder

        if (topHolder !is AnnouncementMenuHolder) {
            return
        }

        event.cancelWithDebug("AnnounceListener.onInventoryClick: menu interaction")
        val clickedInventory = event.clickedInventory ?: return

        if (clickedInventory == event.view.topInventory) {
            if (event.slot == 45 && topHolder.openedFromMenuArgument) {
                executeMenuCommand(player)
                return
            }

            if (event.slot == 49 && hasManagePermission(player)) {
                if (AnnouncementManager.getAnnouncementCount() >= AnnouncementManager.MAX_VISIBLE_ANNOUNCEMENTS) {
                    player.sendMessage(
                        LanguageManager.getMessage(
                            player,
                            "announce.add_limit_reached",
                            "max" to AnnouncementManager.MAX_VISIBLE_ANNOUNCEMENTS.toString()
                        )
                    )
                    playOperationSound(player, Sound.BLOCK_NOTE_BLOCK_BASS)
                    return
                }

                pendingIconSelection.add(player.uniqueId)
                player.sendMessage(LanguageManager.getMessage(player, "announce.icon_select_instruction"))
                playOperationSound(player, Sound.ITEM_BOOK_PAGE_TURN)
                return
            }

            if (!hasManagePermission(player)) {
                return
            }

            val announcement = getAnnouncementByMenuSlot(event.slot) ?: return
            when {
                event.isLeftClick -> {
                    pendingIconSelection.remove(player.uniqueId)
                    player.closeInventory()
                    playOperationSound(player, Sound.UI_BUTTON_CLICK)
                    openEditDialog(player, announcement)
                }

                event.isRightClick -> {
                    pendingIconSelection.remove(player.uniqueId)
                    player.closeInventory()
                    playOperationSound(player, Sound.UI_BUTTON_CLICK)
                    openDeleteConfirmDialog(player, announcement)
                }
            }
            return
        }

        if (clickedInventory != player.inventory) {
            return
        }

        if (!pendingIconSelection.contains(player.uniqueId)) {
            return
        }

        val selectedItem = event.currentItem
        if (selectedItem == null || selectedItem.type == Material.AIR) {
            return
        }

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
            return
        }

        pendingIconSelection.remove(player.uniqueId)
        player.closeInventory()
        playOperationSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f)
        openAddDialog(player, selectedItem.type)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val holder = event.view.topInventory.holder
        if (holder is AnnouncementMenuHolder) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.view.topInventory.holder
        if (holder is AnnouncementMenuHolder) {
            pendingIconSelection.remove(event.player.uniqueId)
        }
    }

    private fun executeMenuCommand(player: Player) {
        val menuCommand = ConfigManager.getAnnounceMenuCommand().trim().removePrefix("/")
        if (menuCommand.isBlank()) {
            player.sendMessage(LanguageManager.getMessage(player, "announce.menu_command_not_configured"))
            return
        }

        val success = player.performCommand(menuCommand)
        if (!success) {
            player.sendMessage(LanguageManager.getMessage(player, "announce.menu_command_failed"))
            playOperationSound(player, Sound.BLOCK_NOTE_BLOCK_BASS)
            return
        }
        playOperationSound(player, Sound.UI_BUTTON_CLICK)
    }

    private fun getAnnouncementByMenuSlot(slot: Int): AnnouncementManager.Announcement? {
        val index = INTERIOR_SLOTS.indexOf(slot)
        if (index < 0) {
            return null
        }

        val announcements = AnnouncementManager.getAnnouncementsForMenu()
        return announcements.getOrNull(index)
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
        val inputs = listOf(
            DialogInput.text("title", titleLabel).initial(formState.title).width(310).maxLength(64).build(),
            DialogInput.text("content_1", Component.text("内容1")).initial(formState.content1).width(310).maxLength(128).build(),
            DialogInput.text("content_2", Component.text("内容2")).initial(formState.content2).width(310).maxLength(128).build(),
            DialogInput.text("content_3", Component.text("内容3")).initial(formState.content3).width(310).maxLength(128).build(),
            DialogInput.text("duration", endAtLabel).initial(formState.endAtRaw).width(310).maxLength(128).build(),
            DialogInput.bool("indefinite", Component.text("無期限にする")).initial(formState.indefinite).build()
        )

        val saveAction = DialogAction.customClick(
            { response, audience ->
                val target = audience as? Player ?: return@customClick

                val title = response.getText("title") ?: ""
                val contentLines = listOf(
                    response.getText("content_1") ?: "",
                    response.getText("content_2") ?: "",
                    response.getText("content_3") ?: ""
                )
                val endAtRaw = response.getText("duration")
                val indefinite = response.getBoolean("indefinite") ?: false
                val nextState = AnnounceFormState(
                    title = title,
                    content1 = contentLines.getOrElse(0) { "" },
                    content2 = contentLines.getOrElse(1) { "" },
                    content3 = contentLines.getOrElse(2) { "" },
                    endAtRaw = endAtRaw ?: "",
                    indefinite = indefinite
                )

                when (AnnouncementManager.updateAnnouncement(announcement.id, title, contentLines, endAtRaw, indefinite)) {
                    AnnouncementManager.AddResult.SUCCESS -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.edit_success"))
                        playOperationSound(target, Sound.ENTITY_PLAYER_LEVELUP, 1.2f)
                        openAnnouncementMenu(target, openedFromMenuArgument = false)
                    }

                    AnnouncementManager.AddResult.NOT_FOUND -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.edit_target_not_found"))
                        playOperationSound(target, Sound.BLOCK_NOTE_BLOCK_BASS)
                        openAnnouncementMenu(target, openedFromMenuArgument = false)
                    }

                    AnnouncementManager.AddResult.INVALID_END_AT_FORMAT -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.invalid_duration"))
                        playOperationSound(target, Sound.BLOCK_NOTE_BLOCK_BASS)
                        openEditDialog(
                            target,
                            announcement,
                            nextState,
                            endAtWarningKey = "announce.end_at_warning.invalid_format"
                        )
                    }

                    AnnouncementManager.AddResult.END_AT_NOT_FUTURE -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.end_at_not_future"))
                        playOperationSound(target, Sound.BLOCK_NOTE_BLOCK_BASS)
                        openEditDialog(
                            target,
                            announcement,
                            nextState,
                            endAtWarningKey = "announce.end_at_warning.not_future"
                        )
                    }

                    AnnouncementManager.AddResult.INVALID_TITLE -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.invalid_title"))
                        playOperationSound(target, Sound.BLOCK_NOTE_BLOCK_BASS)
                        openEditDialog(
                            target,
                            announcement,
                            nextState,
                            titleWarningKey = "announce.title_warning.required"
                        )
                    }

                    AnnouncementManager.AddResult.LIMIT_REACHED -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.add_limit_reached", "max" to AnnouncementManager.MAX_VISIBLE_ANNOUNCEMENTS.toString()))
                        playOperationSound(target, Sound.BLOCK_NOTE_BLOCK_BASS)
                    }
                }
            },
            ClickCallback.Options.builder().uses(1).build()
        )

        val yesButton = ActionButton.builder(
            LanguageManager.getMessageWithoutPrefix(player, "announce.edit_dialog_confirm")
        ).action(saveAction).build()

        val noButton = ActionButton.builder(
            LanguageManager.getMessageWithoutPrefix(player, "announce.dialog_cancel")
        ).build()

        val body = LanguageManager.getStringList(player, "announce.edit_dialog_body")
            .ifEmpty { listOf("お知らせを編集します。") }
            .joinToString("\n")

        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(LanguageManager.getMessageWithoutPrefix(player, "announce.edit_dialog_title"))
                        .body(listOf(DialogBody.plainMessage(Component.text(body))))
                        .inputs(inputs)
                        .build()
                )
                .type(DialogType.confirmation(yesButton, noButton))
        }

        player.showDialog(dialog)
    }

    private fun openDeleteConfirmDialog(player: Player, announcement: AnnouncementManager.Announcement) {
        val deleteAction = DialogAction.customClick(
            { _, audience ->
                val target = audience as? Player ?: return@customClick
                val deleted = AnnouncementManager.deleteAnnouncement(announcement.id)
                if (deleted) {
                    target.sendMessage(LanguageManager.getMessage(target, "announce.delete_success"))
                    playOperationSound(target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f)
                } else {
                    target.sendMessage(LanguageManager.getMessage(target, "announce.edit_target_not_found"))
                    playOperationSound(target, Sound.BLOCK_NOTE_BLOCK_BASS)
                }
                openAnnouncementMenu(target, openedFromMenuArgument = false)
            },
            ClickCallback.Options.builder().uses(1).build()
        )

        val yesButton = ActionButton.builder(
            LanguageManager.getMessageWithoutPrefix(player, "announce.delete_dialog_confirm")
        ).action(deleteAction).build()

        val noButton = ActionButton.builder(
            LanguageManager.getMessageWithoutPrefix(player, "announce.delete_dialog_cancel")
        ).build()

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

        val bodyText = body.joinToString("\n")

        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(LanguageManager.getMessageWithoutPrefix(player, "announce.delete_dialog_title"))
                        .body(listOf(DialogBody.plainMessage(Component.text(bodyText))))
                        .build()
                )
                .type(DialogType.confirmation(yesButton, noButton))
        }

        player.showDialog(dialog)
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
        val inputs = listOf(
            DialogInput.text("title", titleLabel).initial(formState.title).width(310).maxLength(64).build(),
            DialogInput.text("content_1", Component.text("内容1")).initial(formState.content1).width(310).maxLength(128).build(),
            DialogInput.text("content_2", Component.text("内容2")).initial(formState.content2).width(310).maxLength(128).build(),
            DialogInput.text("content_3", Component.text("内容3")).initial(formState.content3).width(310).maxLength(128).build(),
            DialogInput.text("duration", endAtLabel).initial(formState.endAtRaw).width(310).maxLength(128).build(),
            DialogInput.bool("indefinite", Component.text("無期限にする")).initial(formState.indefinite).build()
        )

        val saveAction = DialogAction.customClick(
            { response, audience ->
                val target = audience as? Player ?: return@customClick

                val title = response.getText("title") ?: ""
                val contentLines = listOf(
                    response.getText("content_1") ?: "",
                    response.getText("content_2") ?: "",
                    response.getText("content_3") ?: ""
                )
                val durationRaw = response.getText("duration")
                val indefinite = response.getBoolean("indefinite") ?: false
                val nextState = AnnounceFormState(
                    title = title,
                    content1 = contentLines.getOrElse(0) { "" },
                    content2 = contentLines.getOrElse(1) { "" },
                    content3 = contentLines.getOrElse(2) { "" },
                    endAtRaw = durationRaw ?: "",
                    indefinite = indefinite
                )

                when (AnnouncementManager.addAnnouncement(title, iconType, contentLines, durationRaw, indefinite)) {
                    AnnouncementManager.AddResult.SUCCESS -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.add_success"))
                        notifyAnnouncementIssuedToOthers(target, title)
                        playOperationSound(target, Sound.ENTITY_PLAYER_LEVELUP, 1.3f)
                        openAnnouncementMenu(target, openedFromMenuArgument = false)
                    }

                    AnnouncementManager.AddResult.NOT_FOUND -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.edit_target_not_found"))
                        playOperationSound(target, Sound.BLOCK_NOTE_BLOCK_BASS)
                    }

                    AnnouncementManager.AddResult.LIMIT_REACHED -> {
                        target.sendMessage(
                            LanguageManager.getMessage(
                                target,
                                "announce.add_limit_reached",
                                "max" to AnnouncementManager.MAX_VISIBLE_ANNOUNCEMENTS.toString()
                            )
                        )
                        playOperationSound(target, Sound.BLOCK_NOTE_BLOCK_BASS)
                    }

                    AnnouncementManager.AddResult.INVALID_END_AT_FORMAT -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.invalid_duration"))
                        playOperationSound(target, Sound.BLOCK_NOTE_BLOCK_BASS)
                        openAddDialog(
                            target,
                            iconType,
                            nextState,
                            endAtWarningKey = "announce.end_at_warning.invalid_format"
                        )
                    }

                    AnnouncementManager.AddResult.END_AT_NOT_FUTURE -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.end_at_not_future"))
                        playOperationSound(target, Sound.BLOCK_NOTE_BLOCK_BASS)
                        openAddDialog(
                            target,
                            iconType,
                            nextState,
                            endAtWarningKey = "announce.end_at_warning.not_future"
                        )
                    }

                    AnnouncementManager.AddResult.INVALID_TITLE -> {
                        target.sendMessage(LanguageManager.getMessage(target, "announce.invalid_title"))
                        playOperationSound(target, Sound.BLOCK_NOTE_BLOCK_BASS)
                        openAddDialog(
                            target,
                            iconType,
                            nextState,
                            titleWarningKey = "announce.title_warning.required"
                        )
                    }
                }
            },
            ClickCallback.Options.builder().uses(1).build()
        )

        val yesButton = ActionButton.builder(
            LanguageManager.getMessageWithoutPrefix(player, "announce.dialog_confirm")
        ).action(saveAction).build()

        val noButton = ActionButton.builder(
            LanguageManager.getMessageWithoutPrefix(player, "announce.dialog_cancel")
        ).build()

        val body = LanguageManager.getStringList(player, "announce.dialog_body")
            .ifEmpty { listOf("お知らせを追加します。") }
            .joinToString("\n")

        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(LanguageManager.getMessageWithoutPrefix(player, "announce.dialog_title"))
                        .body(listOf(DialogBody.plainMessage(Component.text(body))))
                        .inputs(inputs)
                        .build()
                )
                .type(DialogType.confirmation(yesButton, noButton))
        }

        player.showDialog(dialog)
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

    private class AnnouncementMenuHolder(
        val openedFromMenuArgument: Boolean
    ) : InventoryHolder {
        private var inventory: Inventory? = null

        fun bind(inventory: Inventory) {
            this.inventory = inventory
        }

        override fun getInventory(): Inventory {
            return inventory ?: Bukkit.createInventory(null, MENU_SIZE)
        }
    }
}
