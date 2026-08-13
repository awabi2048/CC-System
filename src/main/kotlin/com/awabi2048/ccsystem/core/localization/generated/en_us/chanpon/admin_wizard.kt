package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsChanponAdminWizardCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "chanpon/admin_wizard"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.title", value = EmbeddedLocalizedValue.Text("Administrator World Creation"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.select_template", value = EmbeddedLocalizedValue.Text("Select a template"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.detail_content_available", value = EmbeddedLocalizedValue.Text("{description}\n\nCreation spawn: {spawn}\nStatus: {status}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.detail_content_unavailable", value = EmbeddedLocalizedValue.Text("{description}\n\nCreation spawn: {spawn}\nStatus: {status}\nReason: {reason}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.no_description", value = EmbeddedLocalizedValue.Text("No description"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.unset", value = EmbeddedLocalizedValue.Text("Not set"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.status_available", value = EmbeddedLocalizedValue.Text("Available"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.status_unavailable", value = EmbeddedLocalizedValue.Text("Unavailable due to incomplete configuration"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.validation.missing_directory", value = EmbeddedLocalizedValue.Text("The template world data could not be found"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.validation.missing_origin", value = EmbeddedLocalizedValue.Text("No creation spawn is registered"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.use_template", value = EmbeddedLocalizedValue.Text("Use this template"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.preview", value = EmbeddedLocalizedValue.Text("Preview template"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.back", value = EmbeddedLocalizedValue.Text("Back to template list"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.input_name", value = EmbeddedLocalizedValue.Text("Enter the world name"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.name_placeholder", value = EmbeddedLocalizedValue.Text("World name"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.invalid_name", value = EmbeddedLocalizedValue.Text("Enter a world name between 1 and 32 characters"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.confirm_title", value = EmbeddedLocalizedValue.Text("Confirm Creation"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.confirm_content", value = EmbeddedLocalizedValue.Text("Owner: {owner}\nTemplate: {template}\nWorld name: {world}\nCreation spawn: {spawn}\nCost: {cost}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.create", value = EmbeddedLocalizedValue.Text("Create"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.cancel", value = EmbeddedLocalizedValue.Text("Cancel"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.created", value = EmbeddedLocalizedValue.Text("World created"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.create_failed", value = EmbeddedLocalizedValue.Text("Failed to create the world"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.cancelled", value = EmbeddedLocalizedValue.Text("Operation cancelled"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.unavailable", value = EmbeddedLocalizedValue.Text("The Bedrock form or MyWorldManager API is unavailable"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.usage", value = EmbeddedLocalizedValue.Text("/chanpon-mwm create_world <owner>"), domain = DOMAIN),
    )

}
