package com.awabi2048.ccsystem.api.localization

import com.awabi2048.ccsystem.api.localization.generated.GeneratedLocalizationKeyIndex
import java.security.MessageDigest

/**
 * 他モジュールのビルド時参照検査へ、埋込カタログのキーと値型だけを公開します。
 * 翻訳本文や内部カタログ実装を公開APIへ漏らさないための境界です。
 */
object LocalizationCatalogContract {
    private val keysById = GeneratedLocalizationKeyIndex.all().associateBy(LocalizationKey<*>::id)

    fun contains(key: String): Boolean = key in keysById

    fun valueType(key: String): LocalizationKey.ValueType? = keysById[key]?.valueType

    fun placeholders(key: String): Set<String>? = keysById[key]?.placeholders

    fun keys(): Set<String> = keysById.keys

    /**
     * 指定prefix以下のキーID・値型・placeholder集合から、配備世代を照合する安定fingerprintを生成します。
     * 子プラグインはこの値を起動時にも確認し、互換性のないカタログでは機能登録前に停止できます。
     */
    fun fingerprint(prefix: String): String {
        require(prefix.isNotBlank()) { "localization contract prefix must not be blank" }
        val normalizedPrefix = prefix.trimEnd('.')
        val canonical = keysById.values
            .asSequence()
            .filter { it.id == normalizedPrefix || it.id.startsWith("$normalizedPrefix.") }
            .sortedBy(LocalizationKey<*>::id)
            .joinToString("\n") { key ->
                listOf(
                    key.id,
                    key.valueType.name,
                    key.placeholders.sorted().joinToString(","),
                ).joinToString("\u001f")
            }
        require(canonical.isNotEmpty()) { "localization contract domain is empty: prefix=$normalizedPrefix" }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
