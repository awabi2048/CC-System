package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.core.time.SharedClockServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SharedClockServiceTest {
    @Test
    void usesTokyoDateAndStableDayKey() {
        var clock = Clock.fixed(Instant.parse("2026-07-18T15:30:00Z"), ZoneOffset.UTC);
        var service = new SharedClockServiceImpl(clock);

        assertEquals("Asia/Tokyo", service.getZoneId().getId());
        assertEquals("2026-07-19", service.currentDayKey());
        assertEquals(0, service.now().getHour());
    }
}
