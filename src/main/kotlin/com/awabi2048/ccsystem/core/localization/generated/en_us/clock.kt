package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsClockCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "clock"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "clock.usage.main", value = EmbeddedLocalizedValue.Text("§cUsage: /clock <timer|alarm|list|cancel>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.usage.timer", value = EmbeddedLocalizedValue.Text("§cUsage: /clock timer <time(s|m|h)> <title> [forceBar]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.usage.alarm", value = EmbeddedLocalizedValue.Text("§cUsage: /clock alarm <time(HH:mm)> <title> [forceBar]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.usage.cancel", value = EmbeddedLocalizedValue.Text("§cUsage: /clock cancel <title>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.invalid_duration_format", value = EmbeddedLocalizedValue.Text("§cInvalid duration format. (e.g. 30s, 90m, 2h)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.duration_out_of_range", value = EmbeddedLocalizedValue.Text("§cDuration must be 1 second or more and less than 24 hours."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.invalid_time_format", value = EmbeddedLocalizedValue.Text("§cInvalid time format. (e.g. 07:30)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.alarm_not_future", value = EmbeddedLocalizedValue.Text("§cPlease specify a time later than now."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.title_required", value = EmbeddedLocalizedValue.Text("§cPlease provide a title."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.set_success", value = EmbeddedLocalizedValue.Text("§aClock has been set. (ID: %id%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.cancel_success", value = EmbeddedLocalizedValue.Text("§aClock has been canceled. (title: %title%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.cancel_not_found", value = EmbeddedLocalizedValue.Text("§cClock not found for the specified title. (title: %title%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.title_conflict", value = EmbeddedLocalizedValue.Text("§cA clock with the same title already exists: %title%"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.list_header", value = EmbeddedLocalizedValue.Text("§7[§bClock List§7]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.list_empty", value = EmbeddedLocalizedValue.Text("§eNo active clocks right now."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.list_line", value = EmbeddedLocalizedValue.Text("§7- [%id%] %type% (%arg%) title=§f%title%§7, remaining=%remaining%, force=%force%"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.type.timer", value = EmbeddedLocalizedValue.Text("Timer"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.type.alarm", value = EmbeddedLocalizedValue.Text("Alarm"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.timer.started", value = EmbeddedLocalizedValue.Text("§b%duration% timer§7 \"§6%title%§7\" has started!"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.timer.completed", value = EmbeddedLocalizedValue.Text("§aTimer \"§6%title%§7\" is complete!"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.alarm.started", value = EmbeddedLocalizedValue.Text("§7Alarm \"§e%title%§7\" has been set for §b%time%§7!"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "clock.alarm.completed", value = EmbeddedLocalizedValue.Text("§dAlarm \"§e%title%§7\" is ringing now!"), domain = DOMAIN),
    )

}
