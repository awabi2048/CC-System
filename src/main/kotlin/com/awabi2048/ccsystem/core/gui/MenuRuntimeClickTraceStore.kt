package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickTrace
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** player単位でclick traceを有界に保持するRuntime内部の診断用バッファです。 */
internal class MenuRuntimeClickTraceStore(
    private val capacityPerPlayer: Int = DEFAULT_CAPACITY_PER_PLAYER,
) {
    init {
        require(capacityPerPlayer > 0) { "trace capacity must be positive" }
    }

    private val runs = ConcurrentHashMap<UUID, Run>()

    fun start(playerId: UUID, requestedRunId: String? = null): String {
        val runId = requestedRunId?.trim()?.takeIf(String::isNotEmpty) ?: UUID.randomUUID().toString()
        runs[playerId] = Run(runId)
        return runId
    }

    fun next(playerId: UUID): Identity {
        val run = runs.computeIfAbsent(playerId) { Run(UUID.randomUUID().toString()) }
        synchronized(run) {
            run.sequence += 1
            return Identity(run.runId, run.sequence)
        }
    }

    fun append(playerId: UUID, trace: MenuRuntimeClickTrace) {
        val run = runs.computeIfAbsent(playerId) { Run(trace.runId) }
        synchronized(run) {
            if (run.runId != trace.runId) return
            run.traces.addLast(trace)
            while (run.traces.size > capacityPerPlayer) run.traces.removeFirst()
        }
    }

    fun all(playerId: UUID): List<MenuRuntimeClickTrace> {
        val run = runs[playerId] ?: return emptyList()
        synchronized(run) {
            return run.traces.toList()
        }
    }

    fun latest(playerId: UUID): MenuRuntimeClickTrace? {
        val run = runs[playerId] ?: return null
        synchronized(run) {
            return run.traces.lastOrNull()
        }
    }

    fun clear(playerId: UUID) {
        runs.remove(playerId)
    }

    fun clearOwner(owner: String) {
        runs.values.forEach { run ->
            synchronized(run) {
                run.traces.removeIf { trace ->
                    trace.beforeRoute?.owner == owner || trace.afterRoute?.owner == owner
                }
            }
        }
    }

    internal data class Identity(
        val runId: String,
        val sequence: Long,
    )

    private class Run(
        val runId: String,
        var sequence: Long = 0,
        val traces: ArrayDeque<MenuRuntimeClickTrace> = ArrayDeque(),
    )

    private companion object {
        const val DEFAULT_CAPACITY_PER_PLAYER = 128
    }
}
