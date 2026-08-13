package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object NpcMessagesKeys {
    @JvmField val NPC_MESSAGES_EXAMPLE_NPC: LocalizationKey<List<String>> = LocalizationKey.textList("npc_messages.example_npc")

    internal fun all(): List<LocalizationKey<*>> = listOf(
        NPC_MESSAGES_EXAMPLE_NPC,
    )
}
