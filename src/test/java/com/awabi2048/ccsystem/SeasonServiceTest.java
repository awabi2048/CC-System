package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.time.Season;
import com.awabi2048.ccsystem.core.time.SeasonServiceImpl;
import com.awabi2048.ccsystem.core.time.SharedClockServiceImpl;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonServiceTest {
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    @Test
    void resolvesAllSeasonBoundaries() throws Exception {
        var service = serviceAt("2026-01-01T00:00:00Z", Files.createTempDirectory("season-test").resolve("override.yml").toFile());

        assertEquals(Season.WINTER, service.seasonAt(LocalDate.of(2026, 2, 28)));
        assertEquals(Season.SPRING, service.seasonAt(LocalDate.of(2026, 3, 1)));
        assertEquals(Season.SUMMER, service.seasonAt(LocalDate.of(2026, 6, 1)));
        assertEquals(Season.AUTUMN, service.seasonAt(LocalDate.of(2026, 9, 1)));
        assertEquals(Season.WINTER, service.seasonAt(LocalDate.of(2026, 12, 1)));
    }

    @Test
    void usesTokyoDateAtUtcBoundary() throws Exception {
        var service = serviceAt("2026-02-28T15:30:00Z", Files.createTempDirectory("season-test").resolve("override.yml").toFile());

        assertEquals(Season.SPRING, service.currentSeason());
    }

    @Test
    void persistsAndClearsOverride() throws Exception {
        var file = Files.createTempDirectory("season-test").resolve("override.yml").toFile();
        var service = serviceAt("2026-07-19T00:00:00Z", file);
        service.setOverride(Season.WINTER, "test-admin");

        var reloaded = serviceAt("2026-07-19T00:00:00Z", file);
        assertEquals(Season.WINTER, reloaded.currentSeason());
        assertTrue(reloaded.overrideState() != null);
        assertEquals("test-admin", reloaded.overrideState().getActor());

        assertTrue(reloaded.clearOverride("test-admin"));
        assertFalse(reloaded.clearOverride("test-admin"));
        assertNull(serviceAt("2026-07-19T00:00:00Z", file).overrideState());
        assertEquals(Season.SUMMER, serviceAt("2026-07-19T00:00:00Z", file).currentSeason());
    }

    @Test
    void loadsConfigurableSeasonBoundaries() throws Exception {
        Path directory = Files.createTempDirectory("season-settings-test");
        var settings = directory.resolve("season.yml");
        Files.writeString(settings, """
            config_version: 1
            enabled: true
            timezone: Asia/Tokyo
            mode: AUTO
            boundaries:
              spring: "02-15"
              summer: "05-15"
              autumn: "08-15"
              winter: "11-15"
            """);
        var clock = new SharedClockServiceImpl(
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), TOKYO),
            settings.toFile()
        );
        var service = new SeasonServiceImpl(
            clock,
            directory.resolve("override.yml").toFile(),
            Logger.getLogger("SeasonServiceTest"),
            settings.toFile()
        );

        assertEquals(Season.WINTER, service.seasonAt(LocalDate.of(2026, 2, 14)));
        assertEquals(Season.SPRING, service.seasonAt(LocalDate.of(2026, 2, 15)));
        assertEquals(Season.SUMMER, service.seasonAt(LocalDate.of(2026, 5, 15)));
        assertEquals(Season.AUTUMN, service.seasonAt(LocalDate.of(2026, 8, 15)));
        assertEquals(Season.WINTER, service.seasonAt(LocalDate.of(2026, 11, 15)));
    }

    private SeasonServiceImpl serviceAt(String instant, java.io.File file) {
        var clock = new SharedClockServiceImpl(Clock.fixed(Instant.parse(instant), TOKYO));
        return new SeasonServiceImpl(clock, file, Logger.getLogger("SeasonServiceTest"));
    }
}
