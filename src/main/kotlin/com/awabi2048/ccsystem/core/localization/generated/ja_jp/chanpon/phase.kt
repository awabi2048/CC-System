package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpChanponPhaseCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "chanpon/phase"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.display", value = EmbeddedLocalizedValue.Text("建築の許可"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.current", value = EmbeddedLocalizedValue.Text("現在"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.allowed", value = EmbeddedLocalizedValue.Text("許可"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.denied", value = EmbeddedLocalizedValue.Text("禁止"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.allowed_description", value = EmbeddedLocalizedValue.Text("管理者以外のプレイヤーも建築できます。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.denied_description", value = EmbeddedLocalizedValue.Text("管理者以外のプレイヤーは建築できません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.allow_action", value = EmbeddedLocalizedValue.Text("建築を許可"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.build.deny_action", value = EmbeddedLocalizedValue.Text("建築を禁止"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.display", value = EmbeddedLocalizedValue.Text("ワールド作成の可否"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.current", value = EmbeddedLocalizedValue.Text("現在"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.next_action", value = EmbeddedLocalizedValue.Text("次の設定"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.previous_action", value = EmbeddedLocalizedValue.Text("前の設定"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.cycle_action", value = EmbeddedLocalizedValue.Text("設定を切り替え"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.denied.display", value = EmbeddedLocalizedValue.Text("完全拒否"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.denied.description", value = EmbeddedLocalizedValue.Text("通常・本番用ともにワールドを作成できません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.non_production.display", value = EmbeddedLocalizedValue.Text("本番用以外はOK"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.non_production.description", value = EmbeddedLocalizedValue.Text("通常のワールドのみ作成できます。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.production.display", value = EmbeddedLocalizedValue.Text("本番用もOK"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation.production.description", value = EmbeddedLocalizedValue.Text("通常・本番用ともにワールドを作成できます。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.no_permission", value = EmbeddedLocalizedValue.Text("§cちゃんぽん管理者権限が必要です。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.player_only", value = EmbeddedLocalizedValue.Text("§cこのコマンドはプレイヤーのみ実行できます。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.unknown_subcommand", value = EmbeddedLocalizedValue.Text("§c不明なサブコマンドです。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.usage_get_linked_portal", value = EmbeddedLocalizedValue.Text("§e使用法: /chanpon-mwm get_linked_portal <プレイヤー>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.player_not_found", value = EmbeddedLocalizedValue.Text("§c対象プレイヤーがオンラインではありません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.target_not_in_world", value = EmbeddedLocalizedValue.Text("§c対象プレイヤーはマイワールドにいません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.portal_failed", value = EmbeddedLocalizedValue.Text("§cワールドポータルの取得に失敗しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.portal_received", value = EmbeddedLocalizedValue.Text("§aワールドポータルを取得しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.usage_unzip_archive", value = EmbeddedLocalizedValue.Text("§e使用法: /chanpon-mwm unzip_archive <アーカイブ名>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.archive_unzipped", value = EmbeddedLocalizedValue.Text("§aアーカイブを展開しました: §f{archive}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.command.archive_unzip_failed", value = EmbeddedLocalizedValue.Text("§cアーカイブの展開に失敗しました: §f{reason}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.creation_denied", value = EmbeddedLocalizedValue.Text("§c現在はワールドを作成できません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.controls.production_denied", value = EmbeddedLocalizedValue.Text("§c現在は本番用ワールドを作成・切り替えできません。"), domain = DOMAIN),
    )

}
