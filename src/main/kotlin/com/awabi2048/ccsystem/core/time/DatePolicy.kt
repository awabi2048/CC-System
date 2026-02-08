package com.awabi2048.ccsystem.core.time

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object DatePolicy {
    fun isExpired(today: LocalDate, expireDate: LocalDate): Boolean {
        return !today.isBefore(expireDate)
    }

    fun remainingDays(today: LocalDate, expireDate: LocalDate): Int {
        if (isExpired(today, expireDate)) {
            return 0
        }
        return ChronoUnit.DAYS.between(today, expireDate).toInt()
    }
}
