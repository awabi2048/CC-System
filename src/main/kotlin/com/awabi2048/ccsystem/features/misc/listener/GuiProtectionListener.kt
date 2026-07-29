package com.awabi2048.ccsystem.features.misc.listener

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.api.gui.MenuNavigationService
import com.awabi2048.ccsystem.api.gui.PlayerInventoryInteraction
import com.awabi2048.ccsystem.core.gui.GuiItemMarker
import com.awabi2048.ccsystem.core.gui.PlayerInventoryTransferGuard
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryPickupItemEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.Inventory

/**
 * 共通GUI生成物がGUI外へ流出する経路を閉じる。
 * 入力GUIの通常アイテムはPDCマーカーを持たないため、このlistenerでは削除しない。
 */
class GuiProtectionListener(private val navigation: MenuNavigationService) : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        markManagedInventory(event.inventory)
        removeLeakedPlayerItems(player, "インベントリを開いた際")
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        markManagedInventory(event.view.topInventory)
        val currentIsGuiBeforeCleanup = GuiItemMarker.isMarked(event.currentItem)
        val cursorIsGuiBeforeCleanup = GuiItemMarker.isMarked(event.cursor)
        val removedBefore = removeLeakedPlayerItems(player, "GUI操作前")
        if (removedBefore > 0) event.isCancelled = true

        val clickedTop = event.rawSlot in 0 until event.view.topInventory.size
        val policy = navigation.inventoryPolicy(event.view.topInventory)
        if (cursorIsGuiBeforeCleanup) {
            event.isCancelled = true
            player.setItemOnCursor(null)
            warn(player, "カーソル", 1)
        }
        if (!clickedTop && currentIsGuiBeforeCleanup) {
            event.isCancelled = true
        }
        if (!clickedTop && policy != null) {
            when (policy.playerInventoryInteraction) {
                PlayerInventoryInteraction.BLOCKED,
                PlayerInventoryInteraction.SELECTION -> event.isCancelled = true
                PlayerInventoryInteraction.INTERACTIVE -> {
                    if (PlayerInventoryTransferGuard.blocks(event.action)) event.isCancelled = true
                }
            }
        }
        if (clickedTop && !currentIsGuiBeforeCleanup && policy != null && !policy.acceptsTopSlot(event.rawSlot)) {
            event.isCancelled = true
        }
        if (clickedTop && currentIsGuiBeforeCleanup &&
            (event.hotbarButton >= 0 || event.click == ClickType.SWAP_OFFHAND)) {
            event.isCancelled = true
        }

        val hotbarSlot = event.hotbarButton
        if (hotbarSlot >= 0 && GuiItemMarker.isMarked(player.inventory.getItem(hotbarSlot))) {
            event.isCancelled = true
            removeLeakedPlayerItems(player, "ホットバー操作")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        val guiInCursor = GuiItemMarker.isMarked(event.oldCursor) || GuiItemMarker.isMarked(event.cursor)
        val guiInDraggedItems = event.newItems.values.any { GuiItemMarker.isMarked(it) }
        val removed = removeLeakedPlayerItems(player, "GUIドラッグ前")
        val topSize = event.view.topInventory.size
        val policy = navigation.inventoryPolicy(event.view.topInventory)
        val policyViolation = policy != null && event.rawSlots.any { slot ->
            if (slot < topSize) !policy.acceptsTopSlot(slot)
            else policy.playerInventoryInteraction != PlayerInventoryInteraction.INTERACTIVE
        }
        if (!guiInCursor && !guiInDraggedItems && removed == 0 && !policyViolation) return

        event.isCancelled = true
        if (guiInCursor) {
            player.setItemOnCursor(null)
            warn(player, "カーソル", 1)
        }
        if (removed > 0) warn(player, "プレイヤーインベントリ", removed)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryMove(event: InventoryMoveItemEvent) {
        if (!GuiItemMarker.isMarked(event.item)) return

        event.isCancelled = true
        val removed = removeLeakedInventoryItems(event.source) +
            if (event.destination !== event.source) removeLeakedInventoryItems(event.destination) else 0
        if (removed > 0) {
            CCSystem.instance.logger.warning(
                "GUIアイテムがホッパー等のInventoryMoveで流出したため削除しました: count=$removed"
            )
        } else {
            CCSystem.instance.logger.warning("GUIアイテムのInventoryMoveを阻止しました")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryPickup(event: InventoryPickupItemEvent) {
        if (!GuiItemMarker.isMarked(event.item.itemStack)) return
        event.isCancelled = true
        event.item.remove()
        CCSystem.instance.logger.warning("GUIアイテムがInventoryPickupへ流出したため削除しました")
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onEntityPickup(event: EntityPickupItemEvent) {
        if (!GuiItemMarker.isMarked(event.item.itemStack)) return
        event.isCancelled = true
        event.item.remove()
        CCSystem.instance.logger.warning("GUIアイテムがEntityPickupへ流出したため削除しました")
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onPlayerDrop(event: PlayerDropItemEvent) {
        if (!GuiItemMarker.isMarked(event.itemDrop.itemStack)) return
        event.isCancelled = true
        event.itemDrop.remove()
        removeLeakedPlayerItems(event.player, "ドロップ操作")
        warn(event.player, "ドロップ", 1)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onSwapHandItems(event: PlayerSwapHandItemsEvent) {
        if (!GuiItemMarker.isMarked(event.mainHandItem) && !GuiItemMarker.isMarked(event.offHandItem)) return
        event.isCancelled = true
        removeLeakedPlayerItems(event.player, "オフハンド操作")
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        removeLeakedPlayerItems(event.player, "ログイン時")
    }

    private fun removeLeakedPlayerItems(player: Player, context: String): Int {
        var removed = 0
        for (slot in 0 until player.inventory.size) {
            if (GuiItemMarker.isMarked(player.inventory.getItem(slot))) {
                player.inventory.setItem(slot, null)
                removed++
            }
        }
        if (GuiItemMarker.isMarked(player.itemOnCursor)) {
            player.setItemOnCursor(null)
            removed++
        }
        if (removed > 0) warn(player, context, removed)
        return removed
    }

    private fun markManagedInventory(inventory: Inventory) {
        val policy = navigation.inventoryPolicy(inventory) ?: return
        for (slot in 0 until inventory.size) {
            // 入力スロットにはプレイヤー所有アイテムが入るため、GUI生成物として印を付けない。
            if (policy.acceptsTopSlot(slot)) continue
            val item = inventory.getItem(slot) ?: continue
            if (!GuiItemMarker.isMarked(item)) {
                CCSystem.getAPI().getGuiElementService().mark(item)
                inventory.setItem(slot, item)
            }
        }
    }

    private fun removeLeakedInventoryItems(inventory: Inventory): Int {
        if (navigation.isManagedInventory(inventory)) return 0
        var removed = 0
        for (slot in 0 until inventory.size) {
            if (GuiItemMarker.isMarked(inventory.getItem(slot))) {
                inventory.setItem(slot, null)
                removed++
            }
        }
        return removed
    }

    private fun warn(player: Player, context: String, count: Int) {
        CCSystem.instance.logger.warning(
            "GUIアイテムが${context}GUI外へ流出したため削除しました: player=${player.name} uuid=${player.uniqueId} count=$count"
        )
    }
}
