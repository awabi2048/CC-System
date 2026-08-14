package com.awabi2048.ccsystem.api.localization

import com.awabi2048.ccsystem.api.localization.generated.GeneratedLocalizationKeyIndex
import java.security.MessageDigest

/**
 * 埋め込みカタログの公開契約です。
 * 翻訳本文は公開せず、子システムがビルド時・設定読込時に必要とするキー情報だけを提供します。
 */
object LocalizationCatalogContract {
    private val keysById = GeneratedLocalizationKeyIndex.all().associateBy(LocalizationKey<*>::id)

    fun contains(key: String): Boolean = key in keysById

    fun valueType(key: String): LocalizationKey.ValueType? = keysById[key]?.valueType

    fun placeholders(key: String): Set<String>? = keysById[key]?.placeholders

    fun keys(): Set<String> = keysById.keys

    /** 外部設定のTextキー参照を、検証済みの型付きキーへ変換します。 */
    @JvmStatic
    fun resolveText(key: String): LocalizationKey<String> =
        requireTypedKey(key, LocalizationKey.ValueType.TEXT)

    /** 外部設定のTextListキー参照を、検証済みの型付きキーへ変換します。 */
    @JvmStatic
    fun resolveTextList(key: String): LocalizationKey<List<String>> =
        requireTypedKey(key, LocalizationKey.ValueType.TEXT_LIST)

    @Suppress("UNCHECKED_CAST")
    private fun <T> requireTypedKey(key: String, expected: LocalizationKey.ValueType): LocalizationKey<T> {
        require(key.isNotBlank()) { "localization key must not be blank" }
        val resolved = requireNotNull(keysById[key]) { "unknown localization key: $key" }
        require(resolved.valueType == expected) {
            "localization key type mismatch: key=$key expected=$expected actual=${resolved.valueType}"
        }
        return resolved as LocalizationKey<T>
    }

    /**
     * 指定prefix配下のキーID・値型・placeholder集合から互換性fingerprintを生成します。
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
