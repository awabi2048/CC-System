package com.awabi2048.ccsystem.api

import com.awabi2048.ccsystem.core.config.LanguageManager
import org.bukkit.entity.Player

/**
 * CC-System APIの実装クラス
 * LanguageManagerをラップして他のプラグインに機能を提供します
 */
internal class CCSystemAPIImpl : CCSystemAPI {
    
    override fun getPlayerLanguage(player: Player): String {
        return LanguageManager.getRawString(player, "") // プレイヤーの言語を取得
            .let { 
                // プレイヤーのデータから言語設定を直接取得
                com.awabi2048.ccsystem.core.data.PlayerDataManager.getString(
                    player.uniqueId, 
                    "lang", 
                    "ja_jp"
                ) ?: "ja_jp"
            }
    }
    
    override fun setPlayerLanguage(player: Player, language: String) {
        LanguageManager.setPlayerLang(player, language)
    }
    
    override fun getSupportedLanguages(): Set<String> {
        // サポートされている言語は ja_jp と en_us
        return setOf("ja_jp", "en_us")
    }
}
