package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object ClockKeys {
    @JvmField val CLOCK_USAGE_MAIN: LocalizationKey<String> = LocalizationKey.text("clock.usage.main", setOf())
    @JvmField val CLOCK_USAGE_TIMER: LocalizationKey<String> = LocalizationKey.text("clock.usage.timer", setOf())
    @JvmField val CLOCK_USAGE_ALARM: LocalizationKey<String> = LocalizationKey.text("clock.usage.alarm", setOf())
    @JvmField val CLOCK_USAGE_CANCEL: LocalizationKey<String> = LocalizationKey.text("clock.usage.cancel", setOf())
    @JvmField val CLOCK_INVALID_DURATION_FORMAT: LocalizationKey<String> = LocalizationKey.text("clock.invalid_duration_format", setOf())
    @JvmField val CLOCK_DURATION_OUT_OF_RANGE: LocalizationKey<String> = LocalizationKey.text("clock.duration_out_of_range", setOf())
    @JvmField val CLOCK_INVALID_TIME_FORMAT: LocalizationKey<String> = LocalizationKey.text("clock.invalid_time_format", setOf())
    @JvmField val CLOCK_ALARM_NOT_FUTURE: LocalizationKey<String> = LocalizationKey.text("clock.alarm_not_future", setOf())
    @JvmField val CLOCK_TITLE_REQUIRED: LocalizationKey<String> = LocalizationKey.text("clock.title_required", setOf())
    @JvmField val CLOCK_SET_SUCCESS: LocalizationKey<String> = LocalizationKey.text("clock.set_success", setOf("id"))
    @JvmField val CLOCK_CANCEL_SUCCESS: LocalizationKey<String> = LocalizationKey.text("clock.cancel_success", setOf("title"))
    @JvmField val CLOCK_CANCEL_NOT_FOUND: LocalizationKey<String> = LocalizationKey.text("clock.cancel_not_found", setOf("title"))
    @JvmField val CLOCK_TITLE_CONFLICT: LocalizationKey<String> = LocalizationKey.text("clock.title_conflict", setOf("title"))
    @JvmField val CLOCK_LIST_HEADER: LocalizationKey<String> = LocalizationKey.text("clock.list_header", setOf())
    @JvmField val CLOCK_LIST_EMPTY: LocalizationKey<String> = LocalizationKey.text("clock.list_empty", setOf())
    @JvmField val CLOCK_LIST_LINE: LocalizationKey<String> = LocalizationKey.text("clock.list_line", setOf("arg", "force", "id", "remaining", "title", "type"))
    @JvmField val CLOCK_TYPE_TIMER: LocalizationKey<String> = LocalizationKey.text("clock.type.timer", setOf())
    @JvmField val CLOCK_TYPE_ALARM: LocalizationKey<String> = LocalizationKey.text("clock.type.alarm", setOf())
    @JvmField val CLOCK_TIMER_STARTED: LocalizationKey<String> = LocalizationKey.text("clock.timer.started", setOf("duration", "title"))
    @JvmField val CLOCK_TIMER_COMPLETED: LocalizationKey<String> = LocalizationKey.text("clock.timer.completed", setOf("title"))
    @JvmField val CLOCK_ALARM_STARTED: LocalizationKey<String> = LocalizationKey.text("clock.alarm.started", setOf("time", "title"))
    @JvmField val CLOCK_ALARM_COMPLETED: LocalizationKey<String> = LocalizationKey.text("clock.alarm.completed", setOf("title"))

    internal fun all(): List<LocalizationKey<*>> = listOf(
        CLOCK_USAGE_MAIN,
        CLOCK_USAGE_TIMER,
        CLOCK_USAGE_ALARM,
        CLOCK_USAGE_CANCEL,
        CLOCK_INVALID_DURATION_FORMAT,
        CLOCK_DURATION_OUT_OF_RANGE,
        CLOCK_INVALID_TIME_FORMAT,
        CLOCK_ALARM_NOT_FUTURE,
        CLOCK_TITLE_REQUIRED,
        CLOCK_SET_SUCCESS,
        CLOCK_CANCEL_SUCCESS,
        CLOCK_CANCEL_NOT_FOUND,
        CLOCK_TITLE_CONFLICT,
        CLOCK_LIST_HEADER,
        CLOCK_LIST_EMPTY,
        CLOCK_LIST_LINE,
        CLOCK_TYPE_TIMER,
        CLOCK_TYPE_ALARM,
        CLOCK_TIMER_STARTED,
        CLOCK_TIMER_COMPLETED,
        CLOCK_ALARM_STARTED,
        CLOCK_ALARM_COMPLETED,
    )
}
