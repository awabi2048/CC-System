package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickTrace
import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickTraceAwaitException
import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickTraceAwaitFailureReason
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

/** player単位でclick traceを有界に保持するRuntime内部の診断用バッファです。 */
internal class MenuRuntimeClickTraceStore(
    private val capacityPerPlayer: Int = DEFAULT_CAPACITY_PER_PLAYER,
) {
    init {
        require(capacityPerPlayer > 0) { "trace capacity must be positive" }
    }

    /** run切替、待機登録、容量超過を同じ世代境界で直列化します。 */
    private val lock = Any()
    private val runs = HashMap<UUID, Run>()
    private val terminalWaiters = HashMap<TraceKey, CompletableFuture<MenuRuntimeClickTrace>>()
    private var nextGeneration = 0L

    fun start(playerId: UUID, requestedRunId: String? = null): String {
        val runId = requestedRunId?.trim()?.takeIf(String::isNotEmpty) ?: UUID.randomUUID().toString()
        val cancellations = synchronized(lock) {
            val removed = removeWaitersForPlayerLocked(playerId, "click trace run was replaced")
            runs[playerId] = newRun(runId)
            removed
        }
        cancelAll(cancellations)
        return runId
    }

    fun next(playerId: UUID): Identity = synchronized(lock) {
        val run = runs.getOrPut(playerId) { newRun(UUID.randomUUID().toString()) }
        run.sequence += 1
        Identity(run.runId, run.sequence, run.generation)
    }

    fun append(playerId: UUID, identity: Identity, trace: MenuRuntimeClickTrace) {
        val completions = mutableListOf<PendingCompletion>()
        val cancellations = mutableListOf<PendingCancellation>()
        synchronized(lock) {
            val run = runs[playerId] ?: return
            if (
                run.generation != identity.generation ||
                run.runId != identity.runId ||
                trace.playerId != playerId ||
                trace.runId != identity.runId ||
                trace.sequence != identity.sequence
            ) {
                return
            }
            if (run.traces.any { it.sequence == trace.sequence }) return

            run.traces.addLast(trace)
            while (run.traces.size > capacityPerPlayer) {
                val evicted = run.traces.removeFirst()
                terminalWaiters.remove(TraceKey(playerId, evicted.runId, evicted.sequence))?.let { future ->
                    cancellations += PendingCancellation(future, "click trace was evicted")
                }
            }
            if (trace.application.terminal) {
                terminalWaiters.remove(TraceKey(playerId, trace.runId, trace.sequence))?.let { future ->
                    completions += PendingCompletion(future, trace)
                }
            }
        }
        completeAll(completions)
        cancelAll(cancellations)
    }

    fun terminal(playerId: UUID, runId: String, sequence: Long): MenuRuntimeClickTrace? = synchronized(lock) {
        val run = runs[playerId]?.takeIf { it.runId == runId } ?: return@synchronized null
        run.traces.firstOrNull { it.sequence == sequence }?.takeIf { it.application.terminal }
    }

    fun awaitTerminal(playerId: UUID, runId: String, sequence: Long): CompletableFuture<MenuRuntimeClickTrace> =
        synchronized(lock) {
            val run = runs[playerId]?.takeIf { it.runId == runId }
                ?: return@synchronized failedFuture(MenuRuntimeClickTraceAwaitFailureReason.UNKNOWN_RUN)
            val trace = run.traces.firstOrNull { it.sequence == sequence }
                ?: return@synchronized failedFuture(MenuRuntimeClickTraceAwaitFailureReason.UNKNOWN_SEQUENCE)
            if (trace.application.terminal) return@synchronized CompletableFuture.completedFuture(trace)

            val key = TraceKey(playerId, runId, sequence)
            terminalWaiters.getOrPut(key) {
                CompletableFuture<MenuRuntimeClickTrace>().also { future ->
                    future.whenComplete { _, _ ->
                        synchronized(lock) {
                            terminalWaiters.remove(key, future)
                        }
                    }
                }
            }
        }

    fun update(
        playerId: UUID,
        identity: Identity,
        transform: (MenuRuntimeClickTrace) -> MenuRuntimeClickTrace,
    ): MenuRuntimeClickTrace? {
        var completion: PendingCompletion? = null
        val updated = synchronized(lock) {
            val run = runs[playerId] ?: return null
            if (run.generation != identity.generation || run.runId != identity.runId) return null
            val traces = run.traces.toList()
            val index = traces.indexOfFirst { it.sequence == identity.sequence }
            if (index < 0) return null
            val transformed = transform(traces[index])
            if (transformed.runId != identity.runId || transformed.sequence != identity.sequence) return null

            run.traces.clear()
            traces.forEachIndexed { currentIndex, trace ->
                run.traces.addLast(if (currentIndex == index) transformed else trace)
            }
            if (transformed.application.terminal) {
                terminalWaiters.remove(TraceKey(playerId, identity.runId, identity.sequence))?.let { future ->
                    completion = PendingCompletion(future, transformed)
                }
            }
            transformed
        }
        completion?.let(::complete)
        return updated
    }

    fun all(playerId: UUID): List<MenuRuntimeClickTrace> = synchronized(lock) {
        runs[playerId]?.traces?.toList().orEmpty()
    }

    fun latest(playerId: UUID): MenuRuntimeClickTrace? = synchronized(lock) {
        runs[playerId]?.traces?.lastOrNull()
    }

    fun clear(playerId: UUID) {
        val cancellations = synchronized(lock) {
            runs.remove(playerId)
            removeWaitersForPlayerLocked(playerId, "click traces were cleared")
        }
        cancelAll(cancellations)
    }

    fun clearOwner(owner: String) {
        val cancellations = mutableListOf<PendingCancellation>()
        synchronized(lock) {
            runs.forEach { (playerId, run) ->
                val iterator = run.traces.iterator()
                while (iterator.hasNext()) {
                    val trace = iterator.next()
                    val matches = trace.beforeRoute?.owner == owner || trace.afterRoute?.owner == owner
                    if (!matches) continue

                    iterator.remove()
                    terminalWaiters.remove(TraceKey(playerId, trace.runId, trace.sequence))?.let { future ->
                        cancellations += PendingCancellation(future, "click trace owner was cleared: $owner")
                    }
                }
            }
        }
        cancelAll(cancellations)
    }

    internal data class Identity(
        val runId: String,
        val sequence: Long,
        val generation: Long,
    )

    private fun newRun(runId: String): Run = Run(runId, ++nextGeneration)

    private fun removeWaitersForPlayerLocked(playerId: UUID, message: String): List<PendingCancellation> {
        val cancellations = mutableListOf<PendingCancellation>()
        val iterator = terminalWaiters.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key.playerId != playerId) continue
            iterator.remove()
            cancellations += PendingCancellation(entry.value, message)
        }
        return cancellations
    }

    private fun failedFuture(
        reason: MenuRuntimeClickTraceAwaitFailureReason,
    ): CompletableFuture<MenuRuntimeClickTrace> = CompletableFuture.failedFuture(
        MenuRuntimeClickTraceAwaitException(reason),
    )

    private fun completeAll(completions: List<PendingCompletion>) {
        completions.forEach(::complete)
    }

    private fun complete(completion: PendingCompletion) {
        completion.future.complete(completion.trace)
    }

    private fun cancelAll(cancellations: List<PendingCancellation>) {
        cancellations.forEach { cancellation ->
            cancellation.future.completeExceptionally(CancellationException(cancellation.message))
        }
    }

    private data class TraceKey(
        val playerId: UUID,
        val runId: String,
        val sequence: Long,
    )

    private data class PendingCompletion(
        val future: CompletableFuture<MenuRuntimeClickTrace>,
        val trace: MenuRuntimeClickTrace,
    )

    private data class PendingCancellation(
        val future: CompletableFuture<MenuRuntimeClickTrace>,
        val message: String,
    )

    private class Run(
        val runId: String,
        val generation: Long,
        var sequence: Long = 0,
        val traces: ArrayDeque<MenuRuntimeClickTrace> = ArrayDeque(),
    )

    private companion object {
        const val DEFAULT_CAPACITY_PER_PLAYER = 128
    }
}
