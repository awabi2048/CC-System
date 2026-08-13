package com.awabi2048.ccsystem.core.localization

import com.awabi2048.ccsystem.core.localization.generated.GeneratedLocalizationCatalogIndex
import com.awabi2048.ccsystem.api.localization.LocalizationKey
import com.awabi2048.ccsystem.api.localization.generated.GeneratedLocalizationKeyIndex

/**
 * ローカライズ値の取り得る型です。
 *
 * YAMLのような汎用データ型を持ち込まず、利用APIが返せる2種類だけを明示します。
 */
sealed interface EmbeddedLocalizedValue {
    data class Text(val value: String) : EmbeddedLocalizedValue

    data class TextList(val values: List<String>) : EmbeddedLocalizedValue
}

/** カタログ内の1項目と、その文言を所有する機能領域を表します。 */
data class EmbeddedLocalizationEntry(
    val key: String,
    val value: EmbeddedLocalizedValue,
    val domain: String,
)

data class EmbeddedLocalizationValidationResult(
    val errors: List<String>,
    val errorsByDomain: Map<String, List<String>>,
) {
    val isValid: Boolean = errors.isEmpty()
}

/**
 * コンパイル済みKotlinコードだけから構成される不変のローカライズカタログです。
 *
 * 構築時に重複、locale間のキー・値型・プレースホルダー契約を検査するため、
 * 個別画面を開くまで欠損が潜伏することはありません。
 */
object EmbeddedLocalizationCatalog {
    private val sourceEntriesByLocale = GeneratedLocalizationCatalogIndex.entriesByLocale()
    private val entriesByLocale: Map<String, Map<String, EmbeddedLocalizationEntry>> =
        sourceEntriesByLocale.mapValues { (locale, entries) ->
            require(locale.isNotBlank()) { "空のlocaleは登録できません" }
            require(entries.none { it.key.isBlank() }) { "空のローカライズキーがあります: locale=$locale" }
            val duplicateKeys = entries.groupingBy(EmbeddedLocalizationEntry::key).eachCount().filterValues { it > 1 }
            require(duplicateKeys.isEmpty()) {
                "ローカライズキーが重複しています: locale=$locale keys=${duplicateKeys.keys.sorted()}"
            }
            entries.associateBy(EmbeddedLocalizationEntry::key)
        }

    val locales: Set<String>
        get() = entriesByLocale.keys

    fun validate(): EmbeddedLocalizationValidationResult = validateEntries(sourceEntriesByLocale)

    /** 生成済みキーの型引数に対応する実データ型が全localeで一致することを検査します。 */
    fun validateGeneratedKeys(): List<String> {
        val errors = mutableListOf<String>()
        val keys = GeneratedLocalizationKeyIndex.all()
        val duplicates = keys.groupingBy(LocalizationKey<*>::id).eachCount().filterValues { it > 1 }
        duplicates.keys.forEach { errors += "生成済み型付きキーが重複しています: $it" }
        for (key in keys) {
            for (locale in locales) {
                val value = value(locale, key.id)
                val matches = when (key.valueType) {
                    LocalizationKey.ValueType.TEXT -> value is EmbeddedLocalizedValue.Text
                    LocalizationKey.ValueType.TEXT_LIST -> value is EmbeddedLocalizedValue.TextList
                }
                if (!matches) {
                    errors += "型付きキーとカタログ値型が一致しません: locale=$locale key=${key.id} expected=${key.valueType}"
                }
            }
        }
        val catalogKeys = entriesByLocale["ja_jp"].orEmpty().keys
        val generatedIds = keys.mapTo(linkedSetOf(), LocalizationKey<*>::id)
        (catalogKeys - generatedIds).forEach { errors += "型付きキーが生成されていません: $it" }
        (generatedIds - catalogKeys).forEach { errors += "カタログに存在しない型付きキーがあります: $it" }
        return errors
    }

    /** 負例を含むビルド時契約テストでも、本番と同じ検証器を使用します。 */
    internal fun validateEntries(
        source: Map<String, List<EmbeddedLocalizationEntry>>,
    ): EmbeddedLocalizationValidationResult {
        val errorsByDomain = linkedMapOf<String, MutableList<String>>()
        val referenceLocale = "ja_jp"
        val duplicateErrors = mutableListOf<String>()
        val indexed = linkedMapOf<String, Map<String, EmbeddedLocalizationEntry>>()
        for ((locale, entries) in source) {
            if (locale.isBlank()) duplicateErrors += "空のlocaleは登録できません"
            entries.filter { it.key.isBlank() }.forEach { duplicateErrors += "空のローカライズキーがあります: locale=$locale" }
            val duplicates = entries.groupingBy(EmbeddedLocalizationEntry::key).eachCount().filterValues { it > 1 }
            duplicates.keys.forEach { key -> duplicateErrors += "ローカライズキーが重複しています: locale=$locale key=$key" }
            indexed[locale] = entries.associateBy(EmbeddedLocalizationEntry::key)
        }
        if (duplicateErrors.isNotEmpty()) {
            return EmbeddedLocalizationValidationResult(
                errors = duplicateErrors,
                errorsByDomain = mapOf("catalog" to duplicateErrors),
            )
        }
        val reference = indexed[referenceLocale]
        if (reference == null) {
            return EmbeddedLocalizationValidationResult(
                errors = listOf("必須localeがありません: $referenceLocale"),
                errorsByDomain = mapOf("cc-system" to listOf("必須localeがありません: $referenceLocale")),
            )
        }

        for ((locale, actual) in indexed) {
            if (locale == referenceLocale) continue
            for ((key, expectedEntry) in reference) {
                val actualEntry = actual[key]
                val errors = errorsByDomain.getOrPut(expectedEntry.domain) { mutableListOf() }
                if (actualEntry == null) {
                    errors += "locale=$locale key=$key がありません"
                    continue
                }
                if (expectedEntry.value::class != actualEntry.value::class) {
                    errors += "locale=$locale key=$key の値型が一致しません"
                    continue
                }
                val expectedPlaceholders = placeholders(expectedEntry.value)
                val actualPlaceholders = placeholders(actualEntry.value)
                if (expectedPlaceholders != actualPlaceholders) {
                    errors += "locale=$locale key=$key のプレースホルダーが一致しません: expected=$expectedPlaceholders actual=$actualPlaceholders"
                }
            }
            for ((key, entry) in actual) {
                if (key !in reference) {
                    errorsByDomain.getOrPut(entry.domain) { mutableListOf() } +=
                        "locale=$locale に基準localeへ存在しないキーがあります: $key"
                }
            }
        }

        val immutable = errorsByDomain.mapValues { it.value.toList() }
        return EmbeddedLocalizationValidationResult(
            errors = immutable.flatMap { (domain, errors) -> errors.map { "[$domain] $it" } },
            errorsByDomain = immutable,
        )
    }

    fun value(locale: String, key: String): EmbeddedLocalizedValue? = entriesByLocale[locale]?.get(key)?.value

    fun contains(locale: String, key: String): Boolean = entriesByLocale[locale]?.containsKey(key) == true

    fun childKeys(locale: String, section: String): Set<String> {
        val prefix = "$section."
        return entriesByLocale[locale].orEmpty().keys.asSequence()
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix).substringBefore('.') }
            .toCollection(linkedSetOf())
    }

    private fun placeholders(value: EmbeddedLocalizedValue): Set<String> {
        val texts = when (value) {
            is EmbeddedLocalizedValue.Text -> listOf(value.value)
            is EmbeddedLocalizedValue.TextList -> value.values
        }
        return texts.flatMapTo(linkedSetOf()) { text ->
            PLACEHOLDER.findAll(text).map { match -> match.groups[1]?.value ?: match.groups[2]!!.value }.toList()
        }
    }

    private val PLACEHOLDER = Regex("\\{([A-Za-z0-9_.-]+)}|%([A-Za-z0-9_.-]+)%")
}
