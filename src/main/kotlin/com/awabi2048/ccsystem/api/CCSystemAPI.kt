package com.awabi2048.ccsystem.api

import com.awabi2048.ccsystem.api.config.ConfigSchemaService
import com.awabi2048.ccsystem.api.cosmetic.CosmeticPlatform
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectService
import com.awabi2048.ccsystem.api.gui.GuiElementService
import com.awabi2048.ccsystem.api.gui.GuiLayoutService
import com.awabi2048.ccsystem.api.gui.LoreService
import com.awabi2048.ccsystem.api.gui.MenuNavigationService
import com.awabi2048.ccsystem.api.gui.MenuCapabilityService
import com.awabi2048.ccsystem.api.gui.MenuSoundService
import com.awabi2048.ccsystem.api.gui.MenuRuntimeService
import com.awabi2048.ccsystem.api.gui.MenuDialogService
import com.awabi2048.ccsystem.api.gui.MenuConfirmationService
import com.awabi2048.ccsystem.api.gui.MenuFormService
import com.awabi2048.ccsystem.api.input.PlayerInteractionClaimService
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiService
import com.awabi2048.ccsystem.api.item.ItemGrantService
import com.awabi2048.ccsystem.api.sound.SoundResolutionService
import com.awabi2048.ccsystem.api.action.ContentActionDispatcher
import com.awabi2048.ccsystem.api.time.SharedClockService
import com.awabi2048.ccsystem.api.time.SeasonService
import com.awabi2048.ccsystem.api.resource.ResourceWorldLifecycleService
import com.awabi2048.ccsystem.api.resource.NaturalOriginRegistry
import com.awabi2048.ccsystem.api.world.WorldDirectoryService
import com.awabi2048.ccsystem.api.world.WorldIdentityService
import com.awabi2048.ccsystem.core.queue.model.ChunkTask
import net.kyori.adventure.text.Component
import com.awabi2048.ccsystem.api.localization.LocalizationKey
import org.bukkit.World
import org.bukkit.entity.Player

/**
 * CC-Systemが提供する公開API
 * 他のプラグインがこのインターフェースを経由してCC-Systemの機能を利用します
 */
interface CCSystemAPI {
    /** GUI runtime の公開契約版です。consumer は provider 登録前に要求版との完全一致を確認してください。 */
    val guiRuntimeContractVersion: Int
        get() = GUI_RUNTIME_CONTRACT_VERSION

    /** Gesture GUIは既存Inventory GUIと独立した契約として版管理します。 */
    val gestureGuiContractVersion: Int
        get() = GESTURE_GUI_CONTRACT_VERSION

    companion object {
        /** MenuDialogRequestなどGUIランタイム公開ABIを表す契約版です。公開ABI変更時は必ず更新します。 */
        const val GUI_RUNTIME_CONTRACT_VERSION: Int = 10
        // GestureGuiVisual.Blockへ内側枠の追加描画を追加したため、consumerが旧APIへ
        // 接続しないよう契約版を更新します。
        const val GESTURE_GUI_CONTRACT_VERSION: Int = 12
    }
    /**
     * プレイヤーの言語設定を取得します
     *
     * @param player プレイヤー
     * @return 言語コード (例: "ja_jp", "en_us") またはデフォルト言語
     */
    fun getPlayerLanguage(player: Player): String
    
    /**
     * サポートされている言語コードの一覧を取得します
     *
     * @return サポートされている言語コードのセット
     */
    fun getSupportedLanguages(): Set<String>

    /** キーの型引数に応じた値を返し、文字列とリストの取り違えをコンパイル時に防ぎます。 */
    fun <T> getLocalized(player: Player?, key: LocalizationKey<T>, placeholders: Map<String, Any> = emptyMap()): T

    /** localeを明示する型付き取得です。 */
    fun <T> getLocalized(locale: String, key: LocalizationKey<T>, placeholders: Map<String, Any> = emptyMap()): T

    /** Text型が保証された生成キーからComponentを取得します。 */
    fun getI18nComponent(player: Player?, key: LocalizationKey<String>, placeholders: Map<String, Any> = emptyMap()): Component

    /** TextList型が保証された生成キーからComponent一覧を取得します。 */
    fun getI18nComponentList(player: Player?, key: LocalizationKey<List<String>>, placeholders: Map<String, Any> = emptyMap()): List<Component>

    fun getGuiElementService(): GuiElementService

    fun getGuiLayoutService(): GuiLayoutService

    fun getLoreService(): LoreService

    fun getMenuNavigationService(): MenuNavigationService

    fun getMenuCommandService(): com.awabi2048.ccsystem.api.gui.MenuCommandService

    fun getMenuCapabilityService(): MenuCapabilityService

    fun getMenuReversibleStateProviderRegistry(): com.awabi2048.ccsystem.api.gui.MenuReversibleStateProviderRegistry

    fun getMenuSoundService(): MenuSoundService

    fun getMenuRuntimeService(): MenuRuntimeService

    fun getMenuDialogService(): MenuDialogService

    fun getMenuConfirmationService(): MenuConfirmationService

    fun getMenuFormService(): MenuFormService

    fun getPlayerInteractionClaimService(): PlayerInteractionClaimService

    /** Display Entityと視線ジェスチャーで操作する共有画面サービスです。 */
    fun getGestureGuiService(): GestureGuiService

    fun getConfigSchemaService(): ConfigSchemaService

    fun getItemGrantService(): ItemGrantService

    fun getWorldIdentityService(): WorldIdentityService

    fun getWorldDirectoryService(): WorldDirectoryService

    fun getSoundResolutionService(): SoundResolutionService

    fun getSharedClockService(): SharedClockService

    fun getSeasonService(): SeasonService

    fun getContentActionDispatcher(): ContentActionDispatcher

    fun getResourceWorldLifecycleService(): ResourceWorldLifecycleService

    fun getNaturalOriginRegistry(): NaturalOriginRegistry

    fun getCosmeticPlatform(): CosmeticPlatform

    /** Display Entityを用いたバニラ風エフェクトの実行サービスです。 */
    fun getDisplayEffectService(): DisplayEffectService

    /**
     * 指定ワールドがCC-System管理の資源ワールドか判定します。
     */
    fun isResourceWorld(world: World): Boolean

    // ─── チャンクタスクキューAPI ────────────────────────────────────────

    /**
     * チャンク生成タスクをキューに追加します。
     * 追加後、即座に読み込み試行を行います。
     *
     * @param contentType コンテンツ種別文字列 ("SUKIMA_DUNGEON", "ARENA", "RESOURCE")
     * @param worldName 対象ワールド名
     * @return 追加されたChunkTask、contentTypeが不正な場合はnull
     */
    fun addChunkTask(contentType: String, worldName: String): ChunkTask?

    /**
     * 現在処理中のタスクを取得します。
     *
     * @return 処理中のChunkTask、なければnull
     */
    fun getProcessingChunkTask(): ChunkTask?

    /**
     * 待機中のキュー全体のスナップショットを優先度順で返します。
     *
     * @return 待機中タスクのリスト
     */
    fun getWaitingChunkTaskQueue(): List<ChunkTask>

    /**
     * 指定IDのタスク状態を更新します。
     * 外部プラグインがタスクの完了・失敗を通知するために使用します。
     *
     * @param taskId 対象タスクID
     * @param status 新しい状態文字列 ("WAITING", "PROCESSING", "COMPLETED", "FAILED")
     * @return 更新に成功した場合true、対象タスクが存在しない・無効な状態の場合false
     */
    fun updateChunkTaskStatus(taskId: String, status: String): Boolean

    /**
     * 指定IDのタスクを取得します（待機・処理中・履歴を検索）。
     *
     * @param taskId 対象タスクID
     * @return 対象ChunkTask、存在しない場合null
     */
    fun findChunkTask(taskId: String): ChunkTask?

    /**
     * 完了・失敗タスクの履歴を返します（新しい順・最大100件）。
     *
     * @return タスク履歴リスト
     */
    fun getChunkTaskHistory(): List<ChunkTask>

    /**
     * キューの現在の状態をマップ形式で返します。
     * キーと値の例:
     * - "waitingCount" to Int
     * - "processingTask" to String (taskId or "none")
     * - "processingWorld" to String
     * - "processingContentType" to String
     * - "completedHistoryCount" to Int
     * - "priorityOrder" to List<String>
     * - "readIntervalTicks" to Long
     *
     * @return キュー状態マップ
     */
    fun getChunkTaskQueueStatus(): Map<String, Any>
}
