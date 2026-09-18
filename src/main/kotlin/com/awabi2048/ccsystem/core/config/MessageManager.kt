package com.awabi2048.ccsystem.core.config

import com.awabi2048.ccsystem.core.data.PlayerDataManager
import org.bukkit.entity.Player

/**
 * NPCメッセージの表示形式管理マネージャーです。
 * 本文の正データは config/npc_message.yml にあり、言語別に外部設定へ直書きします。
 * 埋め込みローカライズカタログは参照しません。
 */
object MessageManager {

    /**
     * メッセージ設定をロードします。
     * 実読込は ConfigManager が行うため、ここでは特に処理しません。
     */
    fun load() {
        // ConfigManager.load() で npc_message.yml を検証済みで保持します。
    }

    /**
     * メッセージの表示形式を取得します。
     *
     * @param player プレイヤー（形式は言語共通のため未使用、互換用）
     * @param id メッセージID
     * @return 表示形式 ("random", "order", "batch")、未定義時は "batch"
     */
    fun getStyle(player: Player?, id: String): String {
        // 設定読込時に検証済みのため、未定義時のみ安全側の batch を返します。
        return ConfigManager.getNpcMessageStyle(id) ?: "batch"
    }

    /**
     * 順序付きメッセージのインデックスを取得します。
     */
    fun getOrderIndex(player: Player, id: String, max: Int): Int {
        if (max <= 0) return 0

        val key = "npc_message_index.$id"
        var index = PlayerDataManager.getInt(player.uniqueId, key, 0)

        // インデックスが範囲内か確認（リストサイズが変更された場合）
        if (index >= max || index < 0) {
            index = 0
        }

        // 次回使用用のインデックスを計算
        val nextIndex = (index + 1) % max
        PlayerDataManager.set(player.uniqueId, key, nextIndex)

        return index
    }

    /**
     * メッセージIDの一覧を取得します。
     */
    fun getMessageIds(player: Player?): Set<String> {
        // ID一覧は言語非依存のため、player は互換用に残します。
        return ConfigManager.getNpcMessageIds()
    }

    /**
     * 対象プレイヤーの言語に合わせた本文を取得します。
     * 不足ロケールは既定言語へフォールバックし、未定義時は空を返します。
     */
    fun getMessageTexts(player: Player?, id: String): List<String> {
        val requestedLocale = resolveRequestedLocale(player)
        return ConfigManager.getNpcMessageTexts(id, requestedLocale)
    }

    // LanguageManager 未初期化時（単体試験等）でも落とさず、既定言語で解決します。
    private fun resolveRequestedLocale(player: Player?): String {
        return runCatching { LanguageManager.getPlayerLanguageCode(player) }
            .getOrElse { ConfigManager.getDefaultLanguage() }
    }
}
