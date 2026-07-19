package com.awabi2048.ccsystem.api.time

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** 日次処理と期間判定で共有するサーバー時計。 */
interface SharedClockService {
    val zoneId: ZoneId

    fun now(): ZonedDateTime

    fun currentDate(): LocalDate = now().toLocalDate()

    fun currentDayKey(): String = currentDate().toString()
}
