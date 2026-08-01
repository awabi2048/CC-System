package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player

interface MenuRuntimeService {
    fun register(definition: InventoryMenuDefinition)

    fun unregister(owner: String, id: String)

    fun unregisterOwner(owner: String)

    fun definitions(): List<InventoryMenuDefinition>

    fun definition(owner: String, id: String): InventoryMenuDefinition?

    /** 現在Runtimeが表示している画面の事実を、状態を変更せずに複写します。 */
    fun snapshot(player: Player): MenuRuntimeSnapshot?

    /** click traceを新しい診断runへ切り替え、run IDを返します。 */
    fun startClickTraceRun(player: Player): String

    /** 呼出し側が識別子を指定してclick traceを新しい診断runへ切り替えます。 */
    fun startClickTraceRun(player: Player, runId: String): String

    /** playerに対して記録された有界click traceを古い順で返します。 */
    fun clickTraces(player: Player): List<MenuRuntimeClickTrace>

    fun latestClickTrace(player: Player): MenuRuntimeClickTrace?

    /** GUI状態を変えず、playerの診断traceだけを削除します。 */
    fun clearClickTraces(player: Player)

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

    /**
     * 外部入力を完了し、保持している現在Routeを次tickに最新状態から再生成します。
     *
     * 対応する [suspendForExternal] が成立している場合だけ一度受理し、
     * 同じ外部入力に対する重複完了では再表示しません。
     */
    fun finishExternal(player: Player): Boolean

    fun completeExternal(player: Player)

    fun present(player: Player, request: ManagedInventoryMenuRequest): Boolean

    fun feedback(player: Player, interaction: ManagedMenuInteraction)

    fun refresh(player: Player): Boolean

    fun back(player: Player): Boolean

    fun close(player: Player)

    fun clear(player: Player)

    fun closeOwnedMenus(owner: String): Int
}
