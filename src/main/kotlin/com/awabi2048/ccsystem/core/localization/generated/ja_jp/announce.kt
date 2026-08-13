package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpAnnounceCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "announce"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "announce_usage", value = EmbeddedLocalizedValue.Text("§c使用法: /announcement [-menu]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.menu_title", value = EmbeddedLocalizedValue.Text("§0お知らせ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.item_default_title", value = EmbeddedLocalizedValue.Text("お知らせ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.datetime_format", value = EmbeddedLocalizedValue.Text("yyyy/MM/dd HH:mm"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.issued_at", value = EmbeddedLocalizedValue.Text("発行"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.updated_at", value = EmbeddedLocalizedValue.Text("最終更新"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.expires_at", value = EmbeddedLocalizedValue.Text("期限"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.indefinite", value = EmbeddedLocalizedValue.Text("無期限"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.edit", value = EmbeddedLocalizedValue.Text("お知らせ内容を編集"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.lore.delete", value = EmbeddedLocalizedValue.Text("お知らせを削除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.notify.issued_title", value = EmbeddedLocalizedValue.Text("§a新しいお知らせ: §f%title%"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.notify.toast_title", value = EmbeddedLocalizedValue.Text("§a新しいお知らせが%count%件あります！"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.notify.toast_description", value = EmbeddedLocalizedValue.Text("§7/anで表示"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.menu_command_item_name", value = EmbeddedLocalizedValue.Text("§cメニューを開く"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.menu_command_item_lore", value = EmbeddedLocalizedValue.Text("メニューコマンドを実行"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.add_item_name", value = EmbeddedLocalizedValue.Text("§fお知らせを追加"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.add_item_lore", value = EmbeddedLocalizedValue.Text("新しいお知らせを作成"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.add_limit_reached", value = EmbeddedLocalizedValue.Text("§cお知らせは最大%max%件までです。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.icon_select_instruction", value = EmbeddedLocalizedValue.Text("§e追加したいお知らせのアイコンとして、あなたのインベントリ内のアイテムをクリックしてください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.menu_command_not_configured", value = EmbeddedLocalizedValue.Text("§cannounce_menu_command が設定されていません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.menu_command_failed", value = EmbeddedLocalizedValue.Text("§cメニューコマンドの実行に失敗しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.add_success", value = EmbeddedLocalizedValue.Text("§aお知らせを追加しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.edit_success", value = EmbeddedLocalizedValue.Text("§aお知らせを更新しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_success", value = EmbeddedLocalizedValue.Text("§aお知らせを削除しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.edit_target_not_found", value = EmbeddedLocalizedValue.Text("§c対象のお知らせが見つかりません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.invalid_title", value = EmbeddedLocalizedValue.Text("§cタイトルは必須です。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.invalid_duration", value = EmbeddedLocalizedValue.Text("§c終了時刻の形式が不正です。(例: 2026/03/10-10:30 / 2026/03/10 / 10:30)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.end_at_not_future", value = EmbeddedLocalizedValue.Text("§c終了時刻は現在より後を指定してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.title_warning.required", value = EmbeddedLocalizedValue.Text("§c§nタイトルは必須です"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.end_at_warning.invalid_format", value = EmbeddedLocalizedValue.Text("§c§n日付・時刻の形式が不正です"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.end_at_warning.not_future", value = EmbeddedLocalizedValue.Text("§c§n終了時刻は現在より後を指定してください"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.dialog_title", value = EmbeddedLocalizedValue.Text("お知らせ追加"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.dialog_body", value = EmbeddedLocalizedValue.TextList(listOf("タイトル(必須)・内容3行・終了時刻・無期限設定を入力してください。", "終了時刻の例: 2026/03/10-10:30 / 2026/03/10 / 10:30", "無期限=true の場合、終了時刻は無視されます。")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.dialog_confirm", value = EmbeddedLocalizedValue.Text("追加"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.dialog_cancel", value = EmbeddedLocalizedValue.Text("キャンセル"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.edit_dialog_title", value = EmbeddedLocalizedValue.Text("お知らせ編集"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.edit_dialog_body", value = EmbeddedLocalizedValue.TextList(listOf("タイトル・内容・終了時刻・無期限設定を更新できます。")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.edit_dialog_confirm", value = EmbeddedLocalizedValue.Text("更新"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_title", value = EmbeddedLocalizedValue.Text("お知らせ削除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_body", value = EmbeddedLocalizedValue.TextList(listOf("次のお知らせを削除します。", "%title%")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_content_label", value = EmbeddedLocalizedValue.Text("内容:"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_content_empty", value = EmbeddedLocalizedValue.Text("(内容なし)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_confirm", value = EmbeddedLocalizedValue.Text("削除"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "announce.delete_dialog_cancel", value = EmbeddedLocalizedValue.Text("キャンセル"), domain = DOMAIN),
    )

}
