package com.awabi2048.ccsystem.core.time

import com.awabi2048.ccsystem.api.time.SharedClockService
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

class SharedClockServiceImpl(
    private val clock: Clock = Clock.system(TOKYO_ZONE)
) : SharedClockService {
    override val zoneId: ZoneId = TOKYO_ZONE

    override fun now(): ZonedDateTime = ZonedDateTime.now(clock.withZone(zoneId))

    companion object {
        @JvmField
        val TOKYO_ZONE: ZoneId = ZoneId.of("Asia/Tokyo")
    }
}
