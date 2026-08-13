package com.awabi2048.ccsystem.api.localization

/**
 * 取得結果の型をコンパイル時に固定するローカライズキーです。
 * コンストラクタを閉じ、生成器だけが使用するfactoryから値型を明示します。
 */
class LocalizationKey<T> private constructor(
    val id: String,
    val valueType: ValueType,
) {
    enum class ValueType { TEXT, TEXT_LIST }

    companion object {
        fun text(id: String): LocalizationKey<String> = LocalizationKey(id, ValueType.TEXT)

        fun textList(id: String): LocalizationKey<List<String>> = LocalizationKey(id, ValueType.TEXT_LIST)
    }
}
