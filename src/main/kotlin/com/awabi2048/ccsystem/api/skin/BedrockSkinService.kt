package com.awabi2048.ccsystem.api.skin

import java.util.concurrent.CompletableFuture

/** 統合版スキンのJava用テクスチャ。署名がない場合もあります。 */
data class BedrockSkin(
    val gamertag: String,
    /** Floodgateがサーバー上のプレイヤー名として使用する名前。オフライン時は接頭辞付きゲーマータグです。 */
    val serverName: String,
    val xuid: String,
    val value: String,
    val signature: String?,
)

/** 取得失敗の理由を呼び出し元が利用者へ具体的に案内できるよう型で表します。 */
sealed interface BedrockSkinLookupResult {
    data class Found(val skin: BedrockSkin) : BedrockSkinLookupResult
    data object FloodgateUnavailable : BedrockSkinLookupResult
    data object PlayerNotFound : BedrockSkinLookupResult
    data object SkinNotFound : BedrockSkinLookupResult
    data object ServiceUnavailable : BedrockSkinLookupResult
    data object InvalidResponse : BedrockSkinLookupResult
}

/** 名前解決と外部通信は非同期で実行し、失敗理由を結果型で返します。 */
interface BedrockSkinService {
    fun fetch(playerName: String): CompletableFuture<BedrockSkinLookupResult>
}
