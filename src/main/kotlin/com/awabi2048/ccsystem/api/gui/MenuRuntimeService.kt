package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player

interface MenuRuntimeService {
    fun register(definition: InventoryMenuDefinition)

    fun unregister(owner: String, id: String)

    fun unregisterOwner(owner: String)

    fun definitions(): List<InventoryMenuDefinition>

    fun definition(owner: String, id: String): InventoryMenuDefinition?

    fun open(player: Player, route: MenuRoute): Boolean

    fun refresh(player: Player): Boolean

    fun back(player: Player): Boolean

    fun closeOwnedMenus(owner: String): Int
}
