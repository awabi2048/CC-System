package com.awabi2048.ccsystem.features.rentalarea.listener

import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.item.CustomItemFactory
import com.awabi2048.ccsystem.features.rentalarea.manager.RentalAreaManager
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.time.LocalDate

class RentalAreaListener : Listener {
    companion object {
        private const val BYPASS_PERMISSION = "cc-system.rental.bypass"
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) {
            return
        }

        val player = event.player
        val today = LocalDate.now()
        val areaLocation = when (event.action) {
            Action.RIGHT_CLICK_BLOCK -> event.clickedBlock?.location
            Action.RIGHT_CLICK_AIR -> player.location
            else -> null
        } ?: return

        val area = RentalAreaManager.getAreaAt(areaLocation) ?: return

        val item = event.item
        if (CustomItemFactory.isRentalTicket(item)) {
            val days = CustomItemFactory.getRentalDays(item)
            if (days == null || days <= 0) {
                event.isCancelled = true
                player.sendMessage(LanguageManager.getMessage(player, "rental_area_invalid_ticket"))
                return
            }

            val ownerArea = RentalAreaManager.getOwnedArea(player.uniqueId, today)
            if (ownerArea != null && ownerArea.id != area.id) {
                event.isCancelled = true
                player.sendMessage(LanguageManager.getMessage(player, "rental_area_player_already_has_contract"))
                return
            }

            if (area.owner != null && area.owner != player.uniqueId) {
                event.isCancelled = true
                val remainingDays = RentalAreaManager.remainingDays(area, today)
                player.sendMessage(
                    LanguageManager.getMessage(
                        player,
                        "rental_area_already_owned_with_days",
                        "days" to remainingDays.toString()
                    )
                )
                return
            }

            if (area.owner == player.uniqueId) {
                event.isCancelled = true
                val remainingDays = RentalAreaManager.remainingDays(area, today)
                player.sendMessage(
                    LanguageManager.getMessage(
                        player,
                        "rental_area_already_owned_by_you",
                        "days" to remainingDays.toString()
                    )
                )
                return
            }

            event.isCancelled = true
            openContractDialog(
                player = player,
                areaId = area.id,
                worldName = area.world,
                days = days,
                hand = event.hand ?: EquipmentSlot.HAND
            )
            return
        }

        if (player.hasPermission(BYPASS_PERMISSION) || player.isOp) {
            return
        }

        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            val clicked = event.clickedBlock ?: return
            if (clicked.state is org.bukkit.block.Container) {
                if (RentalAreaManager.isProtectedFor(player.uniqueId, clicked.location, today)) {
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (player.hasPermission(BYPASS_PERMISSION) || player.isOp) {
            return
        }
        if (RentalAreaManager.isProtectedFor(player.uniqueId, event.block.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        if (player.hasPermission(BYPASS_PERMISSION) || player.isOp) {
            return
        }
        if (RentalAreaManager.isProtectedFor(player.uniqueId, event.block.location)) {
            event.isCancelled = true
        }
    }

    private fun openContractDialog(
        player: org.bukkit.entity.Player,
        areaId: String,
        worldName: String,
        days: Int,
        hand: EquipmentSlot
    ) {
        val expireDate = LocalDate.now().plusDays(days.toLong()).toString()

        val confirmAction = DialogAction.customClick(
            { _, audience ->
                val dialogPlayer = audience as? org.bukkit.entity.Player ?: return@customClick
                processContract(dialogPlayer, areaId, days, hand)
            },
            ClickCallback.Options.builder().uses(1).build()
        )

        val yesButton = ActionButton.builder(
            LanguageManager.getMessage(player, "rental_area_contract_confirm_yes")
        ).action(confirmAction).build()

        val noButton = ActionButton.builder(
            LanguageManager.getMessage(player, "rental_area_contract_confirm_no")
        ).build()

        val title = LanguageManager.getRawString(player, "rental_area_contract_confirm_title")
        val bodyLines = LanguageManager.getStringListWithPlaceholders(
            player,
            "rental_area_contract_confirm_body",
            "area_id" to areaId,
            "world" to worldName,
            "days" to days.toString(),
            "expire_date" to expireDate
        )
        val bodyText = bodyLines.joinToString("\n")

        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(Component.text(title))
                        .body(listOf(DialogBody.plainMessage(Component.text(bodyText))))
                        .build()
                )
                .type(DialogType.confirmation(yesButton, noButton))
        }

        player.showDialog(dialog)
    }

    private fun processContract(player: org.bukkit.entity.Player, areaId: String, days: Int, hand: EquipmentSlot) {
        val area = RentalAreaManager.getArea(areaId)
        if (area == null) {
            player.sendMessage(LanguageManager.getMessage(player, "rental_area_not_found"))
            return
        }

        if (area.owner != null && area.owner != player.uniqueId) {
            val remainingDays = RentalAreaManager.remainingDays(area, LocalDate.now())
            player.sendMessage(
                LanguageManager.getMessage(
                    player,
                    "rental_area_already_owned_with_days",
                    "days" to remainingDays.toString()
                )
            )
            return
        }

        val ownerArea = RentalAreaManager.getOwnedArea(player.uniqueId)
        if (ownerArea != null && ownerArea.id != areaId) {
            player.sendMessage(LanguageManager.getMessage(player, "rental_area_player_already_has_contract"))
            return
        }

        if (!consumeTicket(player, hand, days)) {
            player.sendMessage(LanguageManager.getMessage(player, "rental_area_ticket_missing"))
            return
        }

        when (val result = RentalAreaManager.contractArea(areaId, player.uniqueId, days)) {
            RentalAreaManager.ContractResult.SUCCESS -> {
                val area = RentalAreaManager.getArea(areaId)
                val expireDate = area?.expireDate?.toString() ?: LocalDate.now().plusDays(days.toLong()).toString()
                player.sendMessage(
                    LanguageManager.getMessage(
                        player,
                        "rental_area_contract_done",
                        "area_id" to areaId,
                        "expire_date" to expireDate
                    )
                )
            }

            RentalAreaManager.ContractResult.AREA_ALREADY_OWNED -> {
                player.sendMessage(LanguageManager.getMessage(player, "rental_area_already_owned"))
            }

            RentalAreaManager.ContractResult.PLAYER_ALREADY_HAS_AREA -> {
                player.sendMessage(LanguageManager.getMessage(player, "rental_area_player_already_has_contract"))
            }

            RentalAreaManager.ContractResult.AREA_NOT_FOUND -> {
                player.sendMessage(LanguageManager.getMessage(player, "rental_area_not_found"))
            }

            RentalAreaManager.ContractResult.INVALID_DAYS -> {
                player.sendMessage(LanguageManager.getMessage(player, "rental_area_invalid_ticket"))
            }
        }
    }

    private fun consumeTicket(player: org.bukkit.entity.Player, hand: EquipmentSlot, expectedDays: Int): Boolean {
        val item = when (hand) {
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            else -> player.inventory.itemInMainHand
        }

        val days = CustomItemFactory.getRentalDays(item)
        if (!CustomItemFactory.isRentalTicket(item) || days != expectedDays) {
            return false
        }

        if (item.amount <= 1) {
            if (hand == EquipmentSlot.OFF_HAND) {
                player.inventory.setItemInOffHand(null)
            } else {
                player.inventory.setItemInMainHand(null)
            }
        } else {
            item.amount = item.amount - 1
            if (hand == EquipmentSlot.OFF_HAND) {
                player.inventory.setItemInOffHand(item)
            } else {
                player.inventory.setItemInMainHand(item)
            }
        }

        return true
    }
}
