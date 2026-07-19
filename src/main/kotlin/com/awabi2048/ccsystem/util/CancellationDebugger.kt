package com.awabi2048.ccsystem.util

import org.bukkit.event.inventory.InventoryClickEvent

fun InventoryClickEvent.cancelWithDebug(@Suppress("UNUSED_PARAMETER") source: String) {
    this.isCancelled = true
}
