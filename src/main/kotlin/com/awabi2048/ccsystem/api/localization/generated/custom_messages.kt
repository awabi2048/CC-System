package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object CustomMessagesKeys {
    @JvmField val CUSTOM_MESSAGES_EXAMPLE_MESSAGE_STYLE: LocalizationKey<String> = LocalizationKey.text("custom_messages.example_message.style")
    @JvmField val CUSTOM_MESSAGES_EXAMPLE_MESSAGE_TEXTS: LocalizationKey<List<String>> = LocalizationKey.textList("custom_messages.example_message.texts")

    internal fun all(): List<LocalizationKey<*>> = listOf(
        CUSTOM_MESSAGES_EXAMPLE_MESSAGE_STYLE,
        CUSTOM_MESSAGES_EXAMPLE_MESSAGE_TEXTS,
    )
}
