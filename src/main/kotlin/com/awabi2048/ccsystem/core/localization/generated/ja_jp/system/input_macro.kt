package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpSystemInputMacroCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "system/input_macro"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "input_macro.usage", value = EmbeddedLocalizedValue.Text("§c使用法: /cc-system input_macro <プレイヤー名> '<コマンドテンプレート>' [console|player]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.invalid_template", value = EmbeddedLocalizedValue.Text("§cコマンドテンプレートが不正です。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.empty_input", value = EmbeddedLocalizedValue.Text("§c文字列を入力してください"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.forbidden_input", value = EmbeddedLocalizedValue.Text("§c入力に使用できない文字（改行など）が含まれています。もう一度入力してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.no_permission", value = EmbeddedLocalizedValue.Text("§cこの機能を実行する権限がありません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.dialog.input_label", value = EmbeddedLocalizedValue.Text("入力した文字列をもとにコマンドを実行します。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.dialog.confirm", value = EmbeddedLocalizedValue.Text("実行"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.dialog.cancel", value = EmbeddedLocalizedValue.Text("キャンセル"), domain = DOMAIN),
    )

}
