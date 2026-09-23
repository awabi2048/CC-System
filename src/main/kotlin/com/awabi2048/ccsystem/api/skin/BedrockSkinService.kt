package com.awabi2048.ccsystem.api.skin

import java.util.concurrent.CompletableFuture

/** 統合版スキンのJava用テクスチャ。署名がない場合もあります。 */
data class BedrockSkin(val gamertag: String, val xuid: String, val value: String, val signature: String?)

/** 名前解決と外部通信は非同期で実行します。見つからない場合はnullを返します。 */
interface BedrockSkinService {
    fun fetch(playerName: String): CompletableFuture<BedrockSkin?>
}
