package com.awabi2048.ccsystem.util

import com.awabi2048.ccsystem.CCSystem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

fun InventoryClickEvent.cancelWithDebug(source: String) {
    val player = this.whoClicked as? Player ?: return
    val parts = source.split(": ", limit = 2)
    val path = parts[0]
    val reason = parts.getOrElse(1) { "unspecified" }
    CCSystem.instance.logger.info("[ClickCancel] player=${player.name} | source=${path} | reason=${reason} | title=${this.view.title()} | slot=${this.slot} | click=${this.click} | action=${this.action}")
    this.isCancelled = true
}
