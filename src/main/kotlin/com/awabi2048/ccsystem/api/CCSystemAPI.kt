package com.awabi2048.ccsystem.api

import org.bukkit.entity.Player

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
     * プレイヤーの言語設定を変更します
     *
     * @param player プレイヤー
     * @param language 言語コード (例: "ja_jp", "en_us")
     */
    fun setPlayerLanguage(player: Player, language: String)
    
    /**
     * サポートされている言語コードの一覧を取得します
     *
     * @return サポートされている言語コードのセット
     */
    fun getSupportedLanguages(): Set<String>
}
