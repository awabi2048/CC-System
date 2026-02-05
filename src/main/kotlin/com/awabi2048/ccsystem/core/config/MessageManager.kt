package com.awabi2048.ccsystem.core.config

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player

/**
 * メッセージスタイル管理マネージャー
 */
object MessageManager {
    
    /**
     * メッセージ設定をロード（互換性のために残す）
     */
    fun load() {
        // 現在はLanguageManagerがメッセージを管理するため、特に処理なし
    }
    
    /**
     * メッセージスタイルを取得
     * 
     * @param player プレイヤー（nullの場合はデフォルト言語）
     * @param id メッセージID
     * @return スタイル ("random", "order", "batch")
     */
    fun getStyle(player: Player?, id: String): String {
        return LanguageManager.getCustomMessageStyle(player, id)
    }
    
    /**
     * 順序付きメッセージのインデックスを取得
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
     * メッセージIDの一覧を取得
     */
    fun getMessageIds(player: Player?): Set<String> {
        return LanguageManager.getCustomMessageIds(player)
    }
    
    /**
     * メッセージテキストリストを取得
     */
    fun getMessageTexts(player: Player?, id: String): List<String> {
        return LanguageManager.getCustomMessageTexts(player, id)
    }
}