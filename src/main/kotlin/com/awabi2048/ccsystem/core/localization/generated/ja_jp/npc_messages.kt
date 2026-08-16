package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpNpcMessagesCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "npc_messages"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "npc_messages.example_npc", value = EmbeddedLocalizedValue.TextList(listOf("§6[NPC] こんにちは！", "§6[NPC] 何かお手伝いできることはありますか？")), domain = DOMAIN),
    )

}
