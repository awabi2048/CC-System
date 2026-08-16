package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsSystemInputMacroCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "system/input_macro"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "input_macro.usage", value = EmbeddedLocalizedValue.Text("§cUsage: /cc-system input_macro <player> '<command template>' [console|player]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.invalid_template", value = EmbeddedLocalizedValue.Text("§cThe command template is invalid."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.empty_input", value = EmbeddedLocalizedValue.Text("§cPlease enter a string."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.forbidden_input", value = EmbeddedLocalizedValue.Text("§cThe input contains characters that cannot be used (such as newlines). Please enter it again."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.no_permission", value = EmbeddedLocalizedValue.Text("§cYou do not have permission to execute this feature."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.dialog.input_label", value = EmbeddedLocalizedValue.Text("The entered string is expanded into a command and executed."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.dialog.confirm", value = EmbeddedLocalizedValue.Text("Execute"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "input_macro.dialog.cancel", value = EmbeddedLocalizedValue.Text("Cancel"), domain = DOMAIN),
    )

}
