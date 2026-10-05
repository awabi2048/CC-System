package com.awabi2048.ccsystem.core.bedrock

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.geysermc.floodgate.api.FloodgateApi

/**
 * floodgate API による統合版プレイヤー判定の共通実装です。
 *
 * 判定はプレイヤー名ではなく floodgate が発行する UUID で行うため、
 * 複数プロキシで接頭辞設定が異なる構成でも正しく動作します。
 * プロキシ経由構成では、プロキシ側 floodgate の send-floodgate-data: true と
 * 各インスタンス共通の key.pem が前提となります。
 *
 * floodgate 未導入時や API 呼び出しに失敗した場合は常に false を返します。
 */
internal object BedrockPlayerLookup {
    fun isBedrockPlayer(player: Player): Boolean =
        Bukkit.getPluginManager().isPluginEnabled("floodgate") &&
            runCatching { FloodgateApi.getInstance().isFloodgatePlayer(player.uniqueId) }
                .getOrDefault(false)
}
