package com.awabi2048.ccsystem.features.lwcx.service

import com.awabi2048.ccsystem.features.lwcx.gateway.LwcProtectionRecord
import java.time.Instant

data class LwcWorldSummary(
    val world: String,
    val count: Int,
    val typeCounts: Map<String, Int>,
    val latestCreation: Instant?,
    val latestChangedAt: Long
)

data class LwcSnapshot(
    val total: Int,
    val existing: Int,
    val missing: Int
)

data class LwcStatusReport(
    val existing: LwcWorldSummary,
    val missing: LwcWorldSummary,
    val allWorlds: List<LwcWorldSummary>,
    val remainingMissingWorlds: List<LwcWorldSummary>,
    val topMyWorlds: List<LwcWorldSummary>,
    val snapshot: LwcSnapshot,
    val previousSnapshot: LwcSnapshot?
) {
    val totalDelta: Int?
        get() = previousSnapshot?.let { snapshot.total - it.total }

    val existingDelta: Int?
        get() = previousSnapshot?.let { snapshot.existing - it.existing }

    val missingDelta: Int?
        get() = previousSnapshot?.let { snapshot.missing - it.missing }
}

object LwcStatusAggregator {
    fun aggregate(
        records: List<LwcProtectionRecord>,
        existingWorlds: Set<String>,
        archivedWorlds: Set<String>,
        myWorldPrefix: String = "my_world."
    ): LwcStatusReport {
        val allWorlds = records.groupBy { it.world }.values
            .map(::summarize)
            .sortedWith(compareByDescending<LwcWorldSummary> { it.count }.thenBy { it.world })

        val existingRecords = records.filter { it.world in existingWorlds }
        val missingRecords = records.filter { it.world !in existingWorlds }
        val remainingRecords = missingRecords.filter { it.world !in archivedWorlds }
        val existing = summarize("__existing__", existingRecords)
        val missing = summarize("__missing__", missingRecords)
        val remainingMissingWorlds = remainingRecords.groupBy { it.world }.values
            .map(::summarize)
            .sortedByDescending { it.count }
        val topMyWorlds = allWorlds
            .filter { it.world.startsWith(myWorldPrefix) }
            .take(10)

        return LwcStatusReport(
            existing = existing,
            missing = missing,
            allWorlds = allWorlds,
            remainingMissingWorlds = remainingMissingWorlds,
            topMyWorlds = topMyWorlds,
            snapshot = LwcSnapshot(records.size, existing.count, missing.count),
            previousSnapshot = null
        )
    }

    fun withPreviousSnapshot(report: LwcStatusReport, previous: LwcSnapshot?): LwcStatusReport {
        return report.copy(previousSnapshot = previous)
    }

    private fun summarize(records: List<LwcProtectionRecord>): LwcWorldSummary {
        val world = records.firstOrNull()?.world ?: ""
        return summarize(world, records)
    }

    private fun summarize(world: String, records: List<LwcProtectionRecord>): LwcWorldSummary {
        return LwcWorldSummary(
            world = world,
            count = records.size,
            typeCounts = records.groupingBy { it.type }.eachCount().toSortedMap(),
            latestCreation = records.mapNotNull { it.creation }.maxOrNull(),
            latestChangedAt = records.maxOfOrNull { it.lastAccessed } ?: 0L
        )
    }
}
