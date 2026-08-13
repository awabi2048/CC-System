package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object SystemInputMacroKeys {
    @JvmField val INPUT_MACRO_USAGE: LocalizationKey<String> = LocalizationKey.text("input_macro.usage")
    @JvmField val INPUT_MACRO_INVALID_TEMPLATE: LocalizationKey<String> = LocalizationKey.text("input_macro.invalid_template")
    @JvmField val INPUT_MACRO_EMPTY_INPUT: LocalizationKey<String> = LocalizationKey.text("input_macro.empty_input")
    @JvmField val INPUT_MACRO_FORBIDDEN_INPUT: LocalizationKey<String> = LocalizationKey.text("input_macro.forbidden_input")
    @JvmField val INPUT_MACRO_NO_PERMISSION: LocalizationKey<String> = LocalizationKey.text("input_macro.no_permission")
    @JvmField val INPUT_MACRO_DIALOG_INPUT_LABEL: LocalizationKey<String> = LocalizationKey.text("input_macro.dialog.input_label")
    @JvmField val INPUT_MACRO_DIALOG_CONFIRM: LocalizationKey<String> = LocalizationKey.text("input_macro.dialog.confirm")
    @JvmField val INPUT_MACRO_DIALOG_CANCEL: LocalizationKey<String> = LocalizationKey.text("input_macro.dialog.cancel")

    internal fun all(): List<LocalizationKey<*>> = listOf(
        INPUT_MACRO_USAGE,
        INPUT_MACRO_INVALID_TEMPLATE,
        INPUT_MACRO_EMPTY_INPUT,
        INPUT_MACRO_FORBIDDEN_INPUT,
        INPUT_MACRO_NO_PERMISSION,
        INPUT_MACRO_DIALOG_INPUT_LABEL,
        INPUT_MACRO_DIALOG_CONFIRM,
        INPUT_MACRO_DIALOG_CANCEL,
    )
}
