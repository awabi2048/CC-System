package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

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

    /**
     * 監査専用です。現在表示中の slot/click が宣言する可逆操作の状態を取得します。
     * 通常の render / inspect / click 実行では呼ばれません。
     */
    fun captureReversibleState(
        player: Player,
        slot: Int,
        click: org.bukkit.event.inventory.ClickType,
    ): MenuReversibleStateCaptureResult

    /** 監査専用です。token を発行した同一 player だけが復元できます。 */
    fun restoreReversibleState(
        player: Player,
        token: MenuReversibleStateToken,
    ): MenuReversibleStateRestoreResult

    /** 監査専用です。token が束縛された online player を使って復元します。 */
    fun restoreReversibleState(token: MenuReversibleStateToken): MenuReversibleStateRestoreResult

    /** player の未使用可逆 token をすべて無効化します。 */
    fun clearReversibleStates(player: Player)

    /** 指定した仮想遷移文脈で、状態を変更せず完成viewを検査します。 */
    fun inspect(
        player: Player,
        route: MenuRoute,
        mode: MenuRuntimeInspectionMode,
    ): MenuRuntimeInspectionResult

    /** PENDINGでない既存click traceだけを返します。 */
    fun terminalClickTrace(player: Player, runId: String, sequence: Long): MenuRuntimeClickTrace?

    /**
     * 既存のPENDING traceだけを待機登録します。NOT_ATTEMPTEDとTERMINALのtraceは即時完了し、
     * 現在のrunに存在しない識別子は[MenuRuntimeClickTraceAwaitException]で即時失敗します。
     * BukkitメインスレッドでFutureをブロックしてはいけません。
     */
    fun awaitTerminalClickTrace(
        player: Player,
        runId: String,
        sequence: Long,
    ): CompletableFuture<MenuRuntimeClickTrace>

    fun open(player: Player, route: MenuRoute): Boolean

    /** [open]のBoolean互換APIでは失われる詳細な結果を返します。 */
    fun openResult(player: Player, route: MenuRoute): MenuRuntimeOperationResult

    fun replace(player: Player, route: MenuRoute): Boolean

    /** [replace]のBoolean互換APIでは失われる詳細な結果を返します。 */
    fun replaceResult(player: Player, route: MenuRoute): MenuRuntimeOperationResult

    /**
     * 現在のルートを履歴へ積み直さず、open音も再生せずに再表示する。
     * Dialog/Form/チャット入力を挟んで元画面へ復帰する場合に使用する。
     */
    fun reopenCurrent(player: Player): Boolean

    fun reopenCurrentResult(player: Player): MenuRuntimeOperationResult

    fun navigate(player: Player, route: MenuRoute): Boolean

    /** [navigate]のBoolean互換APIでは失われる詳細な結果を返します。 */
    fun navigateResult(player: Player, route: MenuRoute): MenuRuntimeOperationResult

    /**
     * 現在のRouteと履歴を変更せず、一時画面を開く。
     * 現地設定など、閉じた後も親画面の追跡状態を維持する画面で使用する。
     */
    fun openEphemeral(player: Player, route: MenuRoute): Boolean

    fun openEphemeralResult(player: Player, route: MenuRoute): MenuRuntimeOperationResult

    /**
     * ワープや外部画面への移行で現在Inventoryが閉じても、現在Routeとパンくずを保持する。
     * 次に開く画面はreplaceまたはbackで現在Routeを再利用する。
     */
    fun preserveHistoryOnClose(player: Player)

    fun suspendForExternal(player: Player): Boolean

    fun resumeFromExternal(player: Player): Boolean

    fun resumeFromExternalResult(player: Player): MenuRuntimeOperationResult

    /**
     * 外部入力を完了し、保持している現在Routeを次tickに最新状態から再生成します。
     *
     * 対応する [suspendForExternal] が成立している場合だけ一度受理し、
     * 同じ外部入力に対する重複完了では再表示しません。
     *
     * 終端成功した場合だけtrueを返します。次tickで完了する場合はfalseとなるため、
     * 非同期の完了状態が必要な呼び出し側は[finishExternalResult]を使用してください。
     */
    fun finishExternal(player: Player): Boolean

    /**
     * 次tickで完了する再表示は[MenuRuntimeOperationCompletionState.PENDING]を返します。
     * 終端結果は[latestExternalFinishResult]、クリック起点の場合は
     * [awaitTerminalClickTrace]で確認してください。
     */
    fun finishExternalResult(player: Player): MenuRuntimeOperationResult

    fun latestExternalFinishResult(player: Player): MenuRuntimeOperationResult?

    fun completeExternal(player: Player)

    fun present(player: Player, request: ManagedInventoryMenuRequest): Boolean

    fun feedback(player: Player, interaction: ManagedMenuInteraction)

    fun refresh(player: Player): Boolean

    /** [refresh]のBoolean互換APIでは失われる詳細な結果を返します。 */
    fun refreshResult(player: Player): MenuRuntimeOperationResult

    fun back(player: Player): Boolean

    fun backResult(player: Player): MenuRuntimeOperationResult

    fun close(player: Player)

    fun clear(player: Player)

    fun closeOwnedMenus(owner: String): Int
}
