package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsChanponArchiveCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "chanpon/archive"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.archive.prompt.name_input", value = EmbeddedLocalizedValue.Text("Enter the annual archive name in chat."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.prompt.password_input", value = EmbeddedLocalizedValue.Text("Enter the archive password in chat."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.prompt.cancel_hint", value = EmbeddedLocalizedValue.Text("[Click to cancel]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.prompt.cancel_hover", value = EmbeddedLocalizedValue.Text("Cancel"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.name", value = EmbeddedLocalizedValue.Text("Archive name: {name} - Proceed?"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.proceed", value = EmbeddedLocalizedValue.Text("[Proceed]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.proceed_hover", value = EmbeddedLocalizedValue.Text("Click to enter password"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.final", value = EmbeddedLocalizedValue.Text("Execute annual archive. Archive name: {name}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.execute", value = EmbeddedLocalizedValue.Text("[EXECUTE]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.execute_hover", value = EmbeddedLocalizedValue.Text("Click to begin annual update. This cannot be undone."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.error.empty_name", value = EmbeddedLocalizedValue.Text("Archive name cannot be empty."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.error.wrong_password", value = EmbeddedLocalizedValue.Text("Incorrect password. Cancelled."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.error.use_button", value = EmbeddedLocalizedValue.Text("Use the buttons, not chat."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.error.already_in_progress", value = EmbeddedLocalizedValue.Text("Another archive operation is already in progress."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.message.executing", value = EmbeddedLocalizedValue.Text("Annual archive process starting..."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.message.success", value = EmbeddedLocalizedValue.Text("Annual archive completed: {name}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.message.failed", value = EmbeddedLocalizedValue.Text("Annual archive failed: {reason}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.message.cancelled", value = EmbeddedLocalizedValue.Text("Annual archive cancelled."), domain = DOMAIN),
    )

}
