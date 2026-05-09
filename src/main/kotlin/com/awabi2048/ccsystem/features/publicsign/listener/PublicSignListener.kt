package com.awabi2048.ccsystem.features.publicsign.listener

import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.features.publicsign.manager.PublicSignManager
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PublicSignListener : Listener {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onSignChange(event: SignChangeEvent) {
        if (!ConfigManager.isPublicSignEnabled()) {
            return
        }

        val worldName = event.block.world.name
        val location = PublicSignManager.toLocation(
            worldName,
            event.block.x,
            event.block.y,
            event.block.z
        )

        val firstLine = PlainTextComponentSerializer.plainText().serialize(event.line(0) ?: Component.empty()).trim()
        if (!firstLine.equals(PublicSignManager.MARKER_TEXT, ignoreCase = true)) {
            return
        }

        event.line(0, Component.text(PublicSignManager.ENABLED_MARKER_TEXT))

        if (PublicSignManager.isRegistered(location)) {
            return
        }

        val expireDate = LocalDate.now().plusDays(ConfigManager.getPublicSignDefaultExpireDays().toLong())
        PublicSignManager.register(
            location,
            PublicSignManager.createDefaultContent(),
            expireDate
        )

        event.player.sendMessage(
            LanguageManager.getMessage(
                event.player,
                "public_sign_enabled",
                "expire_date" to expireDate.format(dateFormatter)
            )
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onRightClickSign(event: PlayerInteractEvent) {
        if (!ConfigManager.isPublicSignEnabled()) {
            return
        }

        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clicked = event.clickedBlock ?: return
        val state = clicked.state
        if (state !is Sign) {
            return
        }

        val location = PublicSignManager.toLocation(clicked.world.name, clicked.x, clicked.y, clicked.z)
        val data = PublicSignManager.get(location) ?: return

        event.isCancelled = true

        if (data.owner == null || event.player.uniqueId == data.owner) {
            openEditDialog(event.player, location, data)
            event.player.sendMessage(LanguageManager.getMessage(event.player, "public_sign_open_editor"))
            return
        }

        openContentDialog(event.player, data.content)
        event.player.sendMessage(LanguageManager.getMessage(event.player, "public_sign_show_dialog"))
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSignBreak(event: BlockBreakEvent) {
        if (!ConfigManager.isPublicSignEnabled()) {
            return
        }

        val location = PublicSignManager.toLocation(
            event.block.world.name,
            event.block.x,
            event.block.y,
            event.block.z
        )

        if (!PublicSignManager.isRegistered(location)) {
            return
        }

        PublicSignManager.remove(location)
    }

    private fun openContentDialog(player: Player, lines: List<String>) {
        val body = lines
            .map { if (it.isBlank()) " " else it }
            .joinToString("\n")

        val book = ItemStack(Material.WRITTEN_BOOK)
        val meta = book.itemMeta as? BookMeta ?: return
        meta.setTitle("PublicSign")
        meta.setAuthor("CC-System")
        meta.pages(listOf(Component.text(body.ifBlank { "(empty)" })))
        book.itemMeta = meta
        player.openBook(book)
    }

    private fun openEditDialog(
        player: Player,
        location: PublicSignManager.SignLocation,
        data: PublicSignManager.PublicSignData
    ) {
        val inputCount = ConfigManager.getPublicSignContentLines()
        val inputs = (0 until inputCount).map { index ->
            DialogInput.text("content_${index + 1}", Component.text("内容${index + 1}"))
                .initial(data.content.getOrElse(index) { "" })
                .maxLength(128)
                .build()
        }

        val saveAction = DialogAction.customClick(
            { response, audience ->
                val editPlayer = audience as? Player ?: return@customClick
                val values = (0 until inputCount).map { index ->
                    response.getText("content_${index + 1}") ?: ""
                }
                saveDialogInput(editPlayer, location, values)
            },
            ClickCallback.Options.builder().uses(1).build()
        )

        val yesButton = ActionButton.builder(Component.text("保存"))
            .action(saveAction)
            .build()
        val noButton = ActionButton.builder(Component.text("キャンセル")).build()

        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(Component.text("PublicSign編集"))
                        .body(listOf(DialogBody.plainMessage(Component.text("PublicSignの内容を編集します"))))
                        .inputs(inputs)
                        .build()
                )
                .type(DialogType.confirmation(yesButton, noButton))
        }

        player.showDialog(dialog)
    }

    private fun saveDialogInput(
        player: Player,
        location: PublicSignManager.SignLocation,
        content: List<String>
    ) {
        val current = PublicSignManager.get(location) ?: return

        if (current.owner != null && current.owner != player.uniqueId) {
            return
        }

        if (current.owner == null) {
            PublicSignManager.setOwner(location, player.uniqueId)
        }

        PublicSignManager.updateContent(location, content)

        val world = org.bukkit.Bukkit.getWorld(location.world)
        if (world != null) {
            val state = world.getBlockAt(location.x, location.y, location.z).state
            if (state is Sign) {
                state.getSide(Side.FRONT).line(0, Component.text(PublicSignManager.ENABLED_MARKER_TEXT))
                state.update(true, false)
            }
        }

        player.sendMessage(LanguageManager.getMessage(player, "public_sign_edit_saved"))
    }
}
