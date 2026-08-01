package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickTrace
import java.util.ArrayDeque
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
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
    private val terminalWaiters = ConcurrentHashMap<TraceKey, CompletableFuture<MenuRuntimeClickTrace>>()

    fun start(playerId: UUID, requestedRunId: String? = null): String {
        val runId = requestedRunId?.trim()?.takeIf(String::isNotEmpty) ?: UUID.randomUUID().toString()
        cancelWaiters(playerId, "click trace run was replaced")
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
            completeWaiterIfTerminal(playerId, trace)
        }
    }

    fun terminal(playerId: UUID, runId: String, sequence: Long): MenuRuntimeClickTrace? {
        val run = runs[playerId] ?: return null
        synchronized(run) {
            return run.traces.firstOrNull { it.runId == runId && it.sequence == sequence }
                ?.takeIf { it.application.terminal }
        }
    }

    fun awaitTerminal(playerId: UUID, runId: String, sequence: Long): CompletableFuture<MenuRuntimeClickTrace> {
        terminal(playerId, runId, sequence)?.let { return CompletableFuture.completedFuture(it) }
        return terminalWaiters.computeIfAbsent(TraceKey(playerId, runId, sequence)) {
            CompletableFuture<MenuRuntimeClickTrace>()
        }.also { future ->
            terminal(playerId, runId, sequence)?.let { completed ->
                future.complete(completed)
                terminalWaiters.remove(TraceKey(playerId, runId, sequence), future)
            }
        }
    }

    fun update(
        playerId: UUID,
        identity: Identity,
        transform: (MenuRuntimeClickTrace) -> MenuRuntimeClickTrace,
    ): MenuRuntimeClickTrace? {
        val run = runs[playerId] ?: return null
        synchronized(run) {
            if (run.runId != identity.runId) return null
            val traces = run.traces.toList()
            val index = traces.indexOfFirst { it.sequence == identity.sequence }
            if (index < 0) return null
            val updated = transform(traces[index])
            run.traces.clear()
            traces.forEachIndexed { currentIndex, trace ->
                run.traces.addLast(if (currentIndex == index) updated else trace)
            }
            completeWaiterIfTerminal(playerId, updated)
            return updated
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
        cancelWaiters(playerId, "click traces were cleared")
    }

    fun clearOwner(owner: String) {
        val removed = mutableListOf<TraceKey>()
        runs.forEach { (playerId, run) ->
            synchronized(run) {
                run.traces.removeIf { trace ->
                    val matches = trace.beforeRoute?.owner == owner || trace.afterRoute?.owner == owner
                    if (matches) {
                        removed += TraceKey(playerId, trace.runId, trace.sequence)
                    }
                    matches
                }
            }
        }
        removed.forEach { key ->
            terminalWaiters.remove(key)?.completeExceptionally(
                CancellationException("click trace owner was cleared: $owner"),
            )
        }
    }

    internal data class Identity(
        val runId: String,
        val sequence: Long,
    )

    private fun completeWaiterIfTerminal(playerId: UUID, trace: MenuRuntimeClickTrace) {
        if (!trace.application.terminal) return
        val key = TraceKey(playerId, trace.runId, trace.sequence)
        terminalWaiters.remove(key)?.complete(trace)
    }

    private fun cancelWaiters(playerId: UUID, message: String) {
        terminalWaiters.entries.removeIf { (key, future) ->
            if (key.playerId != playerId) return@removeIf false
            future.completeExceptionally(CancellationException(message))
            true
        }
    }

    private data class TraceKey(
        val playerId: UUID,
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
