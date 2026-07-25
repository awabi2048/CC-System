package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player

interface MenuRuntimeService {
    fun register(definition: InventoryMenuDefinition)

    fun unregister(owner: String, id: String)

    fun unregisterOwner(owner: String)

    fun definitions(): List<InventoryMenuDefinition>

    fun definition(owner: String, id: String): InventoryMenuDefinition?

    fun open(player: Player, route: MenuRoute): Boolean

    fun replace(player: Player, route: MenuRoute): Boolean

    fun navigate(player: Player, route: MenuRoute): Boolean

    /**
     * 現在のRouteと履歴を変更せず、一時画面を開く。
     * 現地設定など、閉じた後も親画面の追跡状態を維持する画面で使用する。
     */
    fun openEphemeral(player: Player, route: MenuRoute): Boolean

    fun present(player: Player, request: ManagedInventoryMenuRequest): Boolean

    fun feedback(player: Player, interaction: ManagedMenuInteraction)

    fun refresh(player: Player): Boolean

    fun back(player: Player): Boolean

    fun close(player: Player)

    fun closeOwnedMenus(owner: String): Int
}
