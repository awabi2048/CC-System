package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicy
import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicyProvider
import com.awabi2048.ccsystem.api.gui.MenuRoute
import java.util.UUID
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

internal class MenuRuntimeHolder(
    val playerId: UUID,
    val route: MenuRoute,
    private val policy: GuiInventoryPolicy,
    val preserveHistory: Boolean = false,
) : InventoryHolder, GuiInventoryPolicyProvider {
    lateinit var backingInventory: Inventory

    override fun getInventory(): Inventory = backingInventory

    override fun guiInventoryPolicy(): GuiInventoryPolicy = policy
}
