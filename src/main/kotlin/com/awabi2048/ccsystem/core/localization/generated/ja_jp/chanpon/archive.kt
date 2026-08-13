package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpChanponArchiveCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "chanpon/archive"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.archive.prompt.name_input", value = EmbeddedLocalizedValue.Text("§e年次更新アーカイブ名をチャットで入力してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.prompt.password_input", value = EmbeddedLocalizedValue.Text("§eアーカイブのパスワードをチャットで入力してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.prompt.cancel_hint", value = EmbeddedLocalizedValue.Text("§7§o[クリックでキャンセル]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.prompt.cancel_hover", value = EmbeddedLocalizedValue.Text("§cキャンセルする"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.name", value = EmbeddedLocalizedValue.Text("§eアーカイブ名: §f{name}§e でよろしいですか？"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.proceed", value = EmbeddedLocalizedValue.Text("§a§l[続行する]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.proceed_hover", value = EmbeddedLocalizedValue.Text("§aクリックでパスワード入力へ進みます。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.final", value = EmbeddedLocalizedValue.Text("§c年次更新を実行します。§eアーカイブ名: §f{name}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.execute", value = EmbeddedLocalizedValue.Text("§4§l[実行する]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.confirm.execute_hover", value = EmbeddedLocalizedValue.Text("§cクリックで年次更新を開始します。この操作は取り消せません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.error.empty_name", value = EmbeddedLocalizedValue.Text("§c空のアーカイブ名は使用できません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.error.wrong_password", value = EmbeddedLocalizedValue.Text("§cパスワードが間違っています。キャンセルしました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.error.use_button", value = EmbeddedLocalizedValue.Text("§cチャットではなくボタンで操作してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.error.already_in_progress", value = EmbeddedLocalizedValue.Text("§c別のアーカイブ操作が進行中です。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.message.executing", value = EmbeddedLocalizedValue.Text("§a年次更新処理を開始します..."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.message.success", value = EmbeddedLocalizedValue.Text("§a年次更新が完了しました: §f{name}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.message.failed", value = EmbeddedLocalizedValue.Text("§c年次更新に失敗しました: §f{reason}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.archive.message.cancelled", value = EmbeddedLocalizedValue.Text("§7年次更新をキャンセルしました。"), domain = DOMAIN),
    )

}
