package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.features.lwcx.gateway.LwcProtectionRecord;
import com.awabi2048.ccsystem.features.lwcx.service.LwcStatusAggregator;
import com.awabi2048.ccsystem.features.lwcx.service.LwcStatusReport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LwcStatusAggregatorTest {
    @Test
    void separatesExistingMissingArchivedAndMyWorldTopTen() {
        var records = List.of(
            new LwcProtectionRecord(1, "world", "private", Instant.parse("2026-07-01T00:00:00Z"), 10L),
            new LwcProtectionRecord(2, "world", "public", Instant.parse("2026-07-02T00:00:00Z"), 20L),
            new LwcProtectionRecord(3, "deleted", "private", Instant.parse("2026-07-03T00:00:00Z"), 30L),
            new LwcProtectionRecord(4, "archived", "private", Instant.parse("2026-07-04T00:00:00Z"), 40L),
            new LwcProtectionRecord(5, "my_world.one", "private", Instant.parse("2026-07-05T00:00:00Z"), 50L)
        );

        LwcStatusReport report = LwcStatusAggregator.INSTANCE.aggregate(
            records,
            Set.of("world", "my_world.one"),
            Set.of("archived"),
            "my_world."
        );

        assertEquals(3, report.getExisting().getCount());
        assertEquals(2, report.getMissing().getCount());
        assertEquals(1, report.getRemainingMissingWorlds().size());
        assertEquals("deleted", report.getRemainingMissingWorlds().get(0).getWorld());
        assertEquals(1, report.getTopMyWorlds().size());
        assertEquals(2, report.getExisting().getTypeCounts().get("private"));
        assertEquals(Instant.parse("2026-07-05T00:00:00Z"), report.getExisting().getLatestCreation());
        assertEquals(50L, report.getExisting().getLatestChangedAt());
        assertEquals(1, report.getRemainingMissingWorlds().get(0).getCount());
        assertEquals(1, report.getRemainingMissingWorlds().get(0).getTypeCounts().get("private"));
        assertNull(report.getRemainingMissingWorlds().stream()
            .filter(summary -> summary.getWorld().equals("archived"))
            .findFirst()
            .orElse(null));
    }

    @Test
    void computesSnapshotDeltaFromPreviousSnapshot() {
        var report = LwcStatusAggregator.INSTANCE.aggregate(
            List.of(new LwcProtectionRecord(1, "world", "private", null, 0L)),
            Set.of("world"),
            Set.of(),
            "my_world."
        );

        var withPrevious = LwcStatusAggregator.INSTANCE.withPreviousSnapshot(
            report,
            new com.awabi2048.ccsystem.features.lwcx.service.LwcSnapshot(3, 2, 1)
        );

        assertEquals(-2, withPrevious.getTotalDelta());
        assertEquals(-1, withPrevious.getExistingDelta());
        assertEquals(-1, withPrevious.getMissingDelta());
        assertTrue(withPrevious.getPreviousSnapshot() != null);
    }

    @Test
    void preservesNullCreationButStillReportsLastChangedTimestamp() {
        var report = LwcStatusAggregator.INSTANCE.aggregate(
            List.of(new LwcProtectionRecord(1, "missing", "private", null, 123L)),
            Set.of(),
            Set.of(),
            "my_world."
        );

        assertNull(report.getMissing().getLatestCreation());
        assertEquals(123L, report.getMissing().getLatestChangedAt());
    }
}
