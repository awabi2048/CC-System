package com.awabi2048.ccsystem.api

import com.awabi2048.ccsystem.core.queue.model.ChunkTask
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * CC-Systemが提供する公開API
 * 他のプラグインがこのインターフェースを経由してCC-Systemの機能を利用します
 */
interface CCSystemAPI {
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

    fun getI18nString(player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): String

    fun getI18nString(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): String

    fun getI18nString(locale: String, key: String, placeholders: Map<String, Any> = emptyMap()): String

    fun getI18nString(sourceId: String, locale: String, key: String, placeholders: Map<String, Any> = emptyMap()): String

    fun getI18nStringList(player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): List<String>

    fun getI18nStringList(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): List<String>

    fun getI18nStringList(locale: String, key: String, placeholders: Map<String, Any> = emptyMap()): List<String>

    fun getI18nStringList(sourceId: String, locale: String, key: String, placeholders: Map<String, Any> = emptyMap()): List<String>

    fun getI18nComponent(player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): Component

    fun getI18nComponent(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): Component

    fun getI18nComponentList(player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): List<Component>

    fun getI18nComponentList(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): List<Component>

    fun hasI18nKey(key: String): Boolean

    fun hasI18nKey(sourceId: String, key: String): Boolean

    fun isI18nKeyMatch(title: String, key: String): Boolean

    fun isI18nKeyMatch(sourceId: String, title: String, key: String): Boolean

    fun isI18nKeyStartWith(title: String, key: String): Boolean

    fun isI18nKeyStartWith(sourceId: String, title: String, key: String): Boolean

    fun validateI18nSource(sourcePlugin: JavaPlugin, featureByFile: Map<String, String> = emptyMap()): I18nValidationResult

    fun registerI18nSource(sourceId: String, sourcePlugin: JavaPlugin, fileNames: Set<String> = emptySet())

    fun unregisterI18nSource(sourceId: String)

    fun createLoreSeparator(lines: Collection<String> = emptyList()): String

    fun createLoreSeparatorComponent(lines: Collection<Component> = emptyList()): Component

    fun createLoreDataLine(label: String, value: Any?, valueColor: String = "§e"): String

    fun createLoreSubDataLine(label: String, value: Any?): String

    fun createLoreActionLine(operation: String, action: String): String

    fun createLoreWarningLine(content: String): String

    fun buildLore(lines: List<String>, closingSeparator: Boolean = true): List<Component>

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
