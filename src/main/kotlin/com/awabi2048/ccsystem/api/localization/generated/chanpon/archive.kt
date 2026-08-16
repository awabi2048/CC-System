package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object ChanponArchiveKeys {
    @JvmField val CHANPON_ARCHIVE_PROMPT_NAME_INPUT: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.prompt.name_input", setOf())
    @JvmField val CHANPON_ARCHIVE_PROMPT_PASSWORD_INPUT: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.prompt.password_input", setOf())
    @JvmField val CHANPON_ARCHIVE_PROMPT_CANCEL_HINT: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.prompt.cancel_hint", setOf())
    @JvmField val CHANPON_ARCHIVE_PROMPT_CANCEL_HOVER: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.prompt.cancel_hover", setOf())
    @JvmField val CHANPON_ARCHIVE_CONFIRM_NAME: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.confirm.name", setOf("name"))
    @JvmField val CHANPON_ARCHIVE_CONFIRM_PROCEED: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.confirm.proceed", setOf())
    @JvmField val CHANPON_ARCHIVE_CONFIRM_PROCEED_HOVER: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.confirm.proceed_hover", setOf())
    @JvmField val CHANPON_ARCHIVE_CONFIRM_FINAL: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.confirm.final", setOf("name"))
    @JvmField val CHANPON_ARCHIVE_CONFIRM_EXECUTE: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.confirm.execute", setOf())
    @JvmField val CHANPON_ARCHIVE_CONFIRM_EXECUTE_HOVER: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.confirm.execute_hover", setOf())
    @JvmField val CHANPON_ARCHIVE_ERROR_EMPTY_NAME: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.error.empty_name", setOf())
    @JvmField val CHANPON_ARCHIVE_ERROR_WRONG_PASSWORD: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.error.wrong_password", setOf())
    @JvmField val CHANPON_ARCHIVE_ERROR_USE_BUTTON: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.error.use_button", setOf())
    @JvmField val CHANPON_ARCHIVE_ERROR_ALREADY_IN_PROGRESS: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.error.already_in_progress", setOf())
    @JvmField val CHANPON_ARCHIVE_MESSAGE_EXECUTING: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.message.executing", setOf())
    @JvmField val CHANPON_ARCHIVE_MESSAGE_SUCCESS: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.message.success", setOf("name"))
    @JvmField val CHANPON_ARCHIVE_MESSAGE_FAILED: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.message.failed", setOf("reason"))
    @JvmField val CHANPON_ARCHIVE_MESSAGE_CANCELLED: LocalizationKey<String> = LocalizationKey.text("chanpon.archive.message.cancelled", setOf())

    internal fun all(): List<LocalizationKey<*>> = listOf(
        CHANPON_ARCHIVE_PROMPT_NAME_INPUT,
        CHANPON_ARCHIVE_PROMPT_PASSWORD_INPUT,
        CHANPON_ARCHIVE_PROMPT_CANCEL_HINT,
        CHANPON_ARCHIVE_PROMPT_CANCEL_HOVER,
        CHANPON_ARCHIVE_CONFIRM_NAME,
        CHANPON_ARCHIVE_CONFIRM_PROCEED,
        CHANPON_ARCHIVE_CONFIRM_PROCEED_HOVER,
        CHANPON_ARCHIVE_CONFIRM_FINAL,
        CHANPON_ARCHIVE_CONFIRM_EXECUTE,
        CHANPON_ARCHIVE_CONFIRM_EXECUTE_HOVER,
        CHANPON_ARCHIVE_ERROR_EMPTY_NAME,
        CHANPON_ARCHIVE_ERROR_WRONG_PASSWORD,
        CHANPON_ARCHIVE_ERROR_USE_BUTTON,
        CHANPON_ARCHIVE_ERROR_ALREADY_IN_PROGRESS,
        CHANPON_ARCHIVE_MESSAGE_EXECUTING,
        CHANPON_ARCHIVE_MESSAGE_SUCCESS,
        CHANPON_ARCHIVE_MESSAGE_FAILED,
        CHANPON_ARCHIVE_MESSAGE_CANCELLED,
    )
}
