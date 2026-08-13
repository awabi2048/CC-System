package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpChanponAdminWizardCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "chanpon/admin_wizard"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.title", value = EmbeddedLocalizedValue.Text("管理者ワールド作成"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.select_template", value = EmbeddedLocalizedValue.Text("テンプレートを選択してください"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.detail_content_available", value = EmbeddedLocalizedValue.Text("{description}\n\n作成時スポーン: {spawn}\n状態: {status}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.detail_content_unavailable", value = EmbeddedLocalizedValue.Text("{description}\n\n作成時スポーン: {spawn}\n状態: {status}\n理由: {reason}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.no_description", value = EmbeddedLocalizedValue.Text("説明はありません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.unset", value = EmbeddedLocalizedValue.Text("未設定"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.status_available", value = EmbeddedLocalizedValue.Text("作成可能"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.status_unavailable", value = EmbeddedLocalizedValue.Text("設定不備のため作成不可"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.validation.missing_directory", value = EmbeddedLocalizedValue.Text("テンプレートのワールドデータが見つかりません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.validation.missing_origin", value = EmbeddedLocalizedValue.Text("作成時スポーンが登録されていません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.use_template", value = EmbeddedLocalizedValue.Text("このテンプレートを使用"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.preview", value = EmbeddedLocalizedValue.Text("テンプレートをプレビュー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.back", value = EmbeddedLocalizedValue.Text("テンプレート一覧へ戻る"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.input_name", value = EmbeddedLocalizedValue.Text("ワールド名を入力してください"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.name_placeholder", value = EmbeddedLocalizedValue.Text("ワールド名"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.invalid_name", value = EmbeddedLocalizedValue.Text("ワールド名は1〜32文字で入力してください"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.confirm_title", value = EmbeddedLocalizedValue.Text("作成確認"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.confirm_content", value = EmbeddedLocalizedValue.Text("作成先: {owner}\nテンプレート: {template}\nワールド名: {world}\n作成時スポーン: {spawn}\n消費ポイント: {cost}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.create", value = EmbeddedLocalizedValue.Text("作成する"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.cancel", value = EmbeddedLocalizedValue.Text("キャンセル"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.created", value = EmbeddedLocalizedValue.Text("ワールドを作成しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.create_failed", value = EmbeddedLocalizedValue.Text("ワールドの作成に失敗しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.cancelled", value = EmbeddedLocalizedValue.Text("操作をキャンセルしました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.unavailable", value = EmbeddedLocalizedValue.Text("統合版フォームまたはMyWorldManager APIを利用できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.admin_wizard.usage", value = EmbeddedLocalizedValue.Text("/chanpon-mwm create_world <所有者>"), domain = DOMAIN),
    )

}
