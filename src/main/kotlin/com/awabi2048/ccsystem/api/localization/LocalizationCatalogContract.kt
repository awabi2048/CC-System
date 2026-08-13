package com.awabi2048.ccsystem.api.localization

import com.awabi2048.ccsystem.api.localization.generated.GeneratedLocalizationKeyIndex

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
}
