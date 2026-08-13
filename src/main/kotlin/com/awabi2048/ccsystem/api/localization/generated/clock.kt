package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object ClockKeys {
    @JvmField val CLOCK_USAGE_MAIN: LocalizationKey<String> = LocalizationKey.text("clock.usage.main")
    @JvmField val CLOCK_USAGE_TIMER: LocalizationKey<String> = LocalizationKey.text("clock.usage.timer")
    @JvmField val CLOCK_USAGE_ALARM: LocalizationKey<String> = LocalizationKey.text("clock.usage.alarm")
    @JvmField val CLOCK_USAGE_CANCEL: LocalizationKey<String> = LocalizationKey.text("clock.usage.cancel")
    @JvmField val CLOCK_INVALID_DURATION_FORMAT: LocalizationKey<String> = LocalizationKey.text("clock.invalid_duration_format")
    @JvmField val CLOCK_DURATION_OUT_OF_RANGE: LocalizationKey<String> = LocalizationKey.text("clock.duration_out_of_range")
    @JvmField val CLOCK_INVALID_TIME_FORMAT: LocalizationKey<String> = LocalizationKey.text("clock.invalid_time_format")
    @JvmField val CLOCK_ALARM_NOT_FUTURE: LocalizationKey<String> = LocalizationKey.text("clock.alarm_not_future")
    @JvmField val CLOCK_TITLE_REQUIRED: LocalizationKey<String> = LocalizationKey.text("clock.title_required")
    @JvmField val CLOCK_SET_SUCCESS: LocalizationKey<String> = LocalizationKey.text("clock.set_success")
    @JvmField val CLOCK_CANCEL_SUCCESS: LocalizationKey<String> = LocalizationKey.text("clock.cancel_success")
    @JvmField val CLOCK_CANCEL_NOT_FOUND: LocalizationKey<String> = LocalizationKey.text("clock.cancel_not_found")
    @JvmField val CLOCK_TITLE_CONFLICT: LocalizationKey<String> = LocalizationKey.text("clock.title_conflict")
    @JvmField val CLOCK_LIST_HEADER: LocalizationKey<String> = LocalizationKey.text("clock.list_header")
    @JvmField val CLOCK_LIST_EMPTY: LocalizationKey<String> = LocalizationKey.text("clock.list_empty")
    @JvmField val CLOCK_LIST_LINE: LocalizationKey<String> = LocalizationKey.text("clock.list_line")
    @JvmField val CLOCK_TYPE_TIMER: LocalizationKey<String> = LocalizationKey.text("clock.type.timer")
    @JvmField val CLOCK_TYPE_ALARM: LocalizationKey<String> = LocalizationKey.text("clock.type.alarm")
    @JvmField val CLOCK_TIMER_STARTED: LocalizationKey<String> = LocalizationKey.text("clock.timer.started")
    @JvmField val CLOCK_TIMER_COMPLETED: LocalizationKey<String> = LocalizationKey.text("clock.timer.completed")
    @JvmField val CLOCK_ALARM_STARTED: LocalizationKey<String> = LocalizationKey.text("clock.alarm.started")
    @JvmField val CLOCK_ALARM_COMPLETED: LocalizationKey<String> = LocalizationKey.text("clock.alarm.completed")

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
