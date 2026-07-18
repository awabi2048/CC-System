package com.awabi2048.ccsystem.features.rentalarea.listener

import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.data.PlacedBlockLedgerManager
import com.awabi2048.ccsystem.core.item.CustomItemFactory
import com.awabi2048.ccsystem.features.rentalarea.manager.RentalAreaManager
import com.awabi2048.ccsystem.features.rentalarea.storage.RemainedItemManager
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.Material
import java.time.LocalDate

class RentalAreaListener : Listener {
    companion object {
        private const val BYPASS_PERMISSION = "cc-system.rental.bypass"

        private val IMPACT_SENSITIVE_BLOCKS: Set<Material> = setOf(
            Material.SAND,
            Material.RED_SAND,
            Material.GRAVEL,
            Material.DRAGON_EGG,
            Material.ANVIL,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL,
            Material.PISTON,
            Material.STICKY_PISTON,
            Material.MOVING_PISTON,
            Material.SLIME_BLOCK,
            Material.HONEY_BLOCK,
            Material.TNT,
            Material.WHITE_CONCRETE_POWDER,
            Material.ORANGE_CONCRETE_POWDER,
            Material.MAGENTA_CONCRETE_POWDER,
            Material.LIGHT_BLUE_CONCRETE_POWDER,
            Material.YELLOW_CONCRETE_POWDER,
            Material.LIME_CONCRETE_POWDER,
            Material.PINK_CONCRETE_POWDER,
            Material.GRAY_CONCRETE_POWDER,
            Material.LIGHT_GRAY_CONCRETE_POWDER,
            Material.CYAN_CONCRETE_POWDER,
            Material.PURPLE_CONCRETE_POWDER,
            Material.BLUE_CONCRETE_POWDER,
            Material.BROWN_CONCRETE_POWDER,
            Material.GREEN_CONCRETE_POWDER,
            Material.RED_CONCRETE_POWDER,
            Material.BLACK_CONCRETE_POWDER
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInteract(event: PlayerInteractEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }

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
                worldName = area.worldKey,
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
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }

        val player = event.player
        val hasBypass = player.hasPermission(BYPASS_PERMISSION) || player.isOp

        if (RentalAreaManager.isOwnerAt(player.uniqueId, event.block.location)) {
            if (isImpactSensitiveBlock(event.block.type)) {
                event.isCancelled = true
                return
            }

            val placed = event.blockPlaced
            PlacedBlockLedgerManager.registerPlacement(
                worldId = placed.world.uid,
                x = placed.x,
                y = placed.y,
                z = placed.z
            )
            return
        }

        if (!hasBypass && RentalAreaManager.isProtectedFor(player.uniqueId, event.block.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }

        val player = event.player
        if (player.hasPermission(BYPASS_PERMISSION) || player.isOp) {
            return
        }
        if (RentalAreaManager.isProtectedFor(player.uniqueId, event.block.location)) {
            event.isCancelled = true
            return
        }

        val block = event.block
        PlacedBlockLedgerManager.unregisterPlacement(
            worldId = block.world.uid,
            x = block.x,
            y = block.y,
            z = block.z
        )
    }

    private fun isImpactSensitiveBlock(type: Material): Boolean {
        return type in IMPACT_SENSITIVE_BLOCKS
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }

        val player = event.player
        if (player.hasPermission(BYPASS_PERMISSION) || player.isOp) {
            return
        }

        val targetLocation = event.blockClicked.getRelative(event.blockFace).location
        if (RentalAreaManager.isProtectedFor(player.uniqueId, targetLocation)) {
            event.isCancelled = true
            return
        }

        if (RentalAreaManager.isOwnerAt(player.uniqueId, targetLocation)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }
        clearLedgerAt(event.block.location)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockFade(event: BlockFadeEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }
        clearLedgerAt(event.block.location)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockForm(event: BlockFormEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }
        clearLedgerAt(event.block.location)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockSpread(event: BlockSpreadEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }
        clearLedgerAt(event.block.location)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockFromTo(event: BlockFromToEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }
        clearLedgerAt(event.toBlock.location)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }
        clearLedgerAt(event.block.location)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }
        event.blockList().forEach { block ->
            clearLedgerAt(block.location)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }
        event.blockList().forEach { block ->
            clearLedgerAt(block.location)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }
        event.blocks.forEach { block ->
            clearLedgerAt(block.location)
            val moved = block.location.clone().add(
                event.direction.modX.toDouble(),
                event.direction.modY.toDouble(),
                event.direction.modZ.toDouble()
            )
            clearLedgerAt(moved)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }
        event.blocks.forEach { block ->
            clearLedgerAt(block.location)
            val moved = block.location.clone().add(
                event.direction.modX.toDouble(),
                event.direction.modY.toDouble(),
                event.direction.modZ.toDouble()
            )
            clearLedgerAt(moved)
        }
    }

    private fun clearLedgerAt(location: org.bukkit.Location) {
        val world = location.world ?: return
        PlacedBlockLedgerManager.unregisterPlacement(
            worldId = world.uid,
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ
        )
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

    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }

        val player = event.player
        if (!RemainedItemManager.hasRemainedItems(player.uniqueId)) {
            return
        }

        val areaIds = RemainedItemManager.getRemainedAreaIds(player.uniqueId)
        val totalCount = RemainedItemManager.getTotalItemCount(player.uniqueId)

        val areaList = areaIds.joinToString(", ")
        val message = LanguageManager.getMessage(
            player,
            "rental_area_remained_items_notification",
            "count" to totalCount.toString(),
            "areas" to areaList
        )

        val clickText = LanguageManager.deserializeLegacy(
            LanguageManager.getRawString(player, "rental_area_remained_items_click")
        ).clickEvent(ClickEvent.runCommand("/rental-receive"))

        val clickableMessage = message
            .append(Component.space())
            .append(clickText)

        player.sendMessage(clickableMessage)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!ConfigManager.isRentalAreaEnabled()) {
            return
        }

        val player = event.player
        if (player !is org.bukkit.entity.Player) {
            return
        }

        if (!RemainedItemManager.isOpenedStorage(player.uniqueId)) {
            return
        }

        val title = event.view.title()
        if (!title.toString().startsWith("§8回収アイテム:")) {
            return
        }

        RemainedItemManager.onInventoryClose(player, event.inventory)
    }
}
