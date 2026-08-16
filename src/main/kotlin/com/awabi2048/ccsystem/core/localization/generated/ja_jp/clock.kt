package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpClockCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "clock"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "clock.usage.main", value = EmbeddedLocalizedValue.Text("§c使用法: /clock <timer|alarm|list|cancel>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.usage.timer", value = EmbeddedLocalizedValue.Text("§c使用法: /clock timer <時間(s|m|h)> <title> [forceBar]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.usage.alarm", value = EmbeddedLocalizedValue.Text("§c使用法: /clock alarm <時刻(HH:mm)> <title> [forceBar]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.usage.cancel", value = EmbeddedLocalizedValue.Text("§c使用法: /clock cancel <title>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.invalid_duration_format", value = EmbeddedLocalizedValue.Text("§c時間の形式が不正です。(例: 30s, 90m, 2h)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.duration_out_of_range", value = EmbeddedLocalizedValue.Text("§c時間は1秒以上24時間未満で指定してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.invalid_time_format", value = EmbeddedLocalizedValue.Text("§c時刻の形式が不正です。(例: 07:30)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.alarm_not_future", value = EmbeddedLocalizedValue.Text("§c設定時刻は現在時刻より後を指定してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.title_required", value = EmbeddedLocalizedValue.Text("§cタイトルを指定してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.set_success", value = EmbeddedLocalizedValue.Text("§aクロックを設定しました。(ID: %id%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.cancel_success", value = EmbeddedLocalizedValue.Text("§aクロックをキャンセルしました。(title: %title%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.cancel_not_found", value = EmbeddedLocalizedValue.Text("§c指定タイトルのクロックが見つかりません。(title: %title%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.title_conflict", value = EmbeddedLocalizedValue.Text("§c同名タイトルのクロックが既に存在します: %title%"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.list_header", value = EmbeddedLocalizedValue.Text("§7[§bClock一覧§7]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.list_empty", value = EmbeddedLocalizedValue.Text("§e現在実行中のクロックはありません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.list_line", value = EmbeddedLocalizedValue.Text("§7- [%id%] %type% (%arg%) title=§f%title%§7, 残り=%remaining%, force=%force%"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.type.timer", value = EmbeddedLocalizedValue.Text("タイマー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.type.alarm", value = EmbeddedLocalizedValue.Text("アラーム"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.timer.started", value = EmbeddedLocalizedValue.Text("§b%duration%のタイマー§7「§6%title%§7」が開始されました！"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.timer.completed", value = EmbeddedLocalizedValue.Text("§aタイマー§7「§6%title%§7」が終了しました！"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.alarm.started", value = EmbeddedLocalizedValue.Text("§7アラーム「§e%title%§7」が§b%time%§7に設定されました！"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.alarm.completed", value = EmbeddedLocalizedValue.Text("§dアラーム§7「§e%title%§7」の時刻です！"), domain = DOMAIN),
    )

}
