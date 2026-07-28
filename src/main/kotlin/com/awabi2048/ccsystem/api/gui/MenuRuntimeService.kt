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

    /**
     * 現在のルートを履歴へ積み直さず、open音も再生せずに再表示する。
     * Dialog/Form/チャット入力を挟んで元画面へ復帰する場合に使用する。
     */
    fun reopenCurrent(player: Player): Boolean

    fun navigate(player: Player, route: MenuRoute): Boolean

    /**
     * 現在のRouteと履歴を変更せず、一時画面を開く。
     * 現地設定など、閉じた後も親画面の追跡状態を維持する画面で使用する。
     */
    fun openEphemeral(player: Player, route: MenuRoute): Boolean

    /**
     * ワープや外部画面への移行で現在Inventoryが閉じても、現在Routeとパンくずを保持する。
     * 次に開く画面はreplaceまたはbackで現在Routeを再利用する。
     */
    fun preserveHistoryOnClose(player: Player)

    fun suspendForExternal(player: Player): Boolean

    fun resumeFromExternal(player: Player): Boolean

    fun completeExternal(player: Player)

    fun present(player: Player, request: ManagedInventoryMenuRequest): Boolean

    fun feedback(player: Player, interaction: ManagedMenuInteraction)

    fun refresh(player: Player): Boolean

    fun back(player: Player): Boolean

    fun close(player: Player)

    fun clear(player: Player)

    fun closeOwnedMenus(owner: String): Int
}
