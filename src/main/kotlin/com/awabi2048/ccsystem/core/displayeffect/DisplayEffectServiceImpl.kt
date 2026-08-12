package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetResolver
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetResolutionException
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectInstance
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectInstanceStatus
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectNodeDefinition
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectService
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectStartRejection
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectStartResult
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectStopResult
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectTerminationReason
import com.awabi2048.ccsystem.core.displayeffect.paper.PaperDisplayEffectBackend
import com.awabi2048.ccsystem.core.displayeffect.paper.PaperMaterialAssetResolver
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import java.util.UUID

/** CC-SystemのメインスレッドでDisplay Effect Runtime群を進行させるサービスです。 */
internal class DisplayEffectServiceImpl(
    private val plugin: Plugin
) : DisplayEffectService, Listener {
    private data class ActiveEffect(
        val instance: DisplayEffectInstanceImpl,
        val owner: Plugin,
        val runtime: DisplayEffectRuntime,
        val backend: PaperDisplayEffectBackend
    )

    private val defaultAssetResolver: DisplayEffectAssetResolver = PaperMaterialAssetResolver()
    private val activeEffects = linkedMapOf<UUID, ActiveEffect>()
    private val startsThisTick = linkedMapOf<Plugin, Int>()
    private var shutdown = false
    private var currentTick = -1
    private val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { tick() }, 1L, 1L)

    override fun start(
        owner: Plugin,
        anchor: Location,
        definition: DisplayEffectNodeDefinition
    ): DisplayEffectStartResult = start(owner, anchor, definition, defaultAssetResolver)

    override fun start(
        owner: Plugin,
        anchor: Location,
        definition: DisplayEffectNodeDefinition,
        assetResolver: DisplayEffectAssetResolver
    ): DisplayEffectStartResult {
        if (!Bukkit.isPrimaryThread()) {
            return DisplayEffectStartResult.Rejected(
                DisplayEffectStartRejection.NOT_MAIN_THREAD,
                "Display EffectはPaperのメインスレッドから開始してください"
            )
        }
        if (shutdown || !plugin.isEnabled) {
            return DisplayEffectStartResult.Rejected(
                DisplayEffectStartRejection.SHUTDOWN,
                "Display Effect serviceは停止済みです"
            )
        }
        if (!owner.isEnabled) {
            return DisplayEffectStartResult.Rejected(
                DisplayEffectStartRejection.OWNER_DISABLED,
                "Display Effectのowner Pluginが有効ではありません: ${owner.name}"
            )
        }
        val tick = Bukkit.getCurrentTick()
        if (tick != currentTick) {
            currentTick = tick
            startsThisTick.clear()
        }
        val ownerCount = activeEffects.values.count { it.owner === owner }
        val tickCount = startsThisTick[owner] ?: 0
        if (activeEffects.size >= MAX_ACTIVE_EFFECTS ||
            ownerCount >= MAX_ACTIVE_EFFECTS_PER_OWNER ||
            tickCount >= MAX_STARTS_PER_TICK_PER_OWNER
        ) {
            return DisplayEffectStartResult.Rejected(
                DisplayEffectStartRejection.CAPACITY_EXCEEDED,
                "Display Effectの生成上限に達しました: owner=${owner.name} active=${activeEffects.size}"
            )
        }
        val world = anchor.world
            ?: return DisplayEffectStartResult.Rejected(
                DisplayEffectStartRejection.WORLD_UNAVAILABLE,
                "Display EffectのanchorにWorldがありません"
            )
        if (Bukkit.getWorld(world.uid) == null) {
            return DisplayEffectStartResult.Rejected(
                DisplayEffectStartRejection.WORLD_UNAVAILABLE,
                "Display EffectのWorldが利用できません: ${world.uid}"
            )
        }

        val instanceId = UUID.randomUUID()
        val backend = runCatching {
            PaperDisplayEffectBackend(plugin, anchor, assetResolver, owner.name)
        }.getOrElse { failure ->
            return rejected(failure)
        }
        val runtime = DisplayEffectRuntime(definition, backend, instanceId)
        return when (val result = runtime.start()) {
            DisplayEffectRuntimeResult.Started -> {
                val instance = DisplayEffectInstanceImpl(instanceId, this)
                activeEffects[instanceId] = ActiveEffect(instance, owner, runtime, backend)
                startsThisTick[owner] = tickCount + 1
                DisplayEffectStartResult.Started(instance)
            }

            is DisplayEffectRuntimeResult.Failed -> {
                backend.disposeAll(DisplayEffectDisposalReason.FAILED)
                rejected(result.error)
            }

            else -> {
                backend.disposeAll(DisplayEffectDisposalReason.FAILED)
                DisplayEffectStartResult.Rejected(
                    DisplayEffectStartRejection.BACKEND_FAILURE,
                    "Display Effectの開始状態が不正です: $result"
                )
            }
        }
    }

    internal fun stop(instanceId: UUID): DisplayEffectStopResult {
        if (!Bukkit.isPrimaryThread()) return DisplayEffectStopResult.NotMainThread
        val active = activeEffects.remove(instanceId) ?: return DisplayEffectStopResult.AlreadyStopped
        val result = active.runtime.stop(DisplayEffectDisposalReason.CANCELLED)
        active.instance.markStopped(result)
        return if (result is DisplayEffectRuntimeResult.Stopped) {
            DisplayEffectStopResult.Stopped
        } else {
            DisplayEffectStopResult.ServiceUnavailable
        }
    }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        if (event.plugin === plugin) return
        activeEffects.entries.toList()
            .filter { (_, active) -> active.owner === event.plugin }
            .forEach { (instanceId, active) ->
                activeEffects.remove(instanceId)
                active.runtime.stop(DisplayEffectDisposalReason.OWNER_DISABLED)
                active.backend.disposeAll(DisplayEffectDisposalReason.OWNER_DISABLED)
                active.instance.markTerminated(
                    DisplayEffectInstanceStatus.STOPPED,
                    DisplayEffectTerminationReason.OWNER_DISABLED
                )
            }
        startsThisTick.remove(event.plugin)
    }

    internal fun shutdown() {
        if (!Bukkit.isPrimaryThread()) {
            plugin.logger.warning("[DisplayEffect] shutdownはメインスレッドから呼び出してください")
            return
        }
        if (shutdown) return
        shutdown = true
        task.cancel()
        activeEffects.values.toList().forEach { active ->
            active.runtime.stop(DisplayEffectDisposalReason.SHUTDOWN)
            active.backend.disposeAll(DisplayEffectDisposalReason.SHUTDOWN)
            active.instance.markTerminated(
                DisplayEffectInstanceStatus.STOPPED,
                DisplayEffectTerminationReason.SHUTDOWN
            )
        }
        activeEffects.clear()
    }

    private fun tick() {
        if (shutdown) return
        activeEffects.entries.toList().forEach { (instanceId, active) ->
            val result = try {
                active.runtime.tick()
            } catch (failure: Throwable) {
                // 1件のbackend異常で共有schedulerを停止させず、そのinstanceだけを
                // FAILEDとして破棄します。Runtime自身の既知の失敗は通常ここへ到達しません。
                activeEffects.remove(instanceId)
                active.instance.markFailed(failure)
                active.backend.disposeAll(DisplayEffectDisposalReason.FAILED)
                plugin.logger.warning(
                    "[DisplayEffect] unexpected runtime failure: instance=$instanceId " +
                        "error=${failure.message}"
                )
                return@forEach
            }
            when (result) {
                DisplayEffectRuntimeResult.Advanced,
                DisplayEffectRuntimeResult.Ignored -> Unit

                is DisplayEffectRuntimeResult.Stopped -> {
                    activeEffects.remove(instanceId)
                    active.instance.markStopped(result)
                }

                is DisplayEffectRuntimeResult.Failed -> {
                    activeEffects.remove(instanceId)
                    active.instance.markFailed(result.error)
                    plugin.logger.warning(
                        "[DisplayEffect] runtime failed: instance=$instanceId error=${result.error.message}"
                    )
                }

                DisplayEffectRuntimeResult.Started -> Unit
            }
        }
    }

    private fun rejected(failure: Throwable): DisplayEffectStartResult.Rejected {
        val reason = when (failure) {
            is DisplayEffectWorldUnavailableException -> DisplayEffectStartRejection.WORLD_UNAVAILABLE
            is DisplayEffectAssetResolutionException -> DisplayEffectStartRejection.ASSET_UNAVAILABLE
            is IllegalArgumentException -> DisplayEffectStartRejection.INVALID_DEFINITION
            else -> DisplayEffectStartRejection.BACKEND_FAILURE
        }
        return DisplayEffectStartResult.Rejected(reason, failure.message ?: failure::class.simpleName.orEmpty())
    }

    private class DisplayEffectInstanceImpl(
        override val id: UUID,
        private val service: DisplayEffectServiceImpl
    ) : DisplayEffectInstance {
        @Volatile
        override var status: DisplayEffectInstanceStatus = DisplayEffectInstanceStatus.ACTIVE
            private set
        @Volatile
        override var terminalReason: DisplayEffectTerminationReason? = null
            private set

        override fun stop(): DisplayEffectStopResult = service.stop(id)

        fun markStopped(result: DisplayEffectRuntimeResult) {
            val reason = when (result) {
                is DisplayEffectRuntimeResult.Stopped -> result.reason.toPublicTerminationReason()
                is DisplayEffectRuntimeResult.Failed -> DisplayEffectTerminationReason.FAILED
                else -> return
            }
            markTerminated(
                if (reason == DisplayEffectTerminationReason.FAILED) {
                    DisplayEffectInstanceStatus.FAILED
                } else {
                    DisplayEffectInstanceStatus.STOPPED
                },
                reason
            )
        }

        fun markFailed(@Suppress("UNUSED_PARAMETER") error: Throwable) {
            markTerminated(DisplayEffectInstanceStatus.FAILED, DisplayEffectTerminationReason.FAILED)
        }

        fun markTerminated(nextStatus: DisplayEffectInstanceStatus, reason: DisplayEffectTerminationReason) {
            status = nextStatus
            terminalReason = reason
        }
    }

    private companion object {
        private const val MAX_ACTIVE_EFFECTS = 512
        private const val MAX_ACTIVE_EFFECTS_PER_OWNER = 128
        private const val MAX_STARTS_PER_TICK_PER_OWNER = 32
    }
}

private fun DisplayEffectDisposalReason.toPublicTerminationReason(): DisplayEffectTerminationReason = when (this) {
    DisplayEffectDisposalReason.EXPIRED -> DisplayEffectTerminationReason.EXPIRED
    DisplayEffectDisposalReason.CANCELLED -> DisplayEffectTerminationReason.CANCELLED
    DisplayEffectDisposalReason.OWNER_DISABLED -> DisplayEffectTerminationReason.OWNER_DISABLED
    DisplayEffectDisposalReason.BACKEND_INVALIDATED -> DisplayEffectTerminationReason.BACKEND_INVALIDATED
    DisplayEffectDisposalReason.WORLD_UNAVAILABLE -> DisplayEffectTerminationReason.WORLD_UNAVAILABLE
    DisplayEffectDisposalReason.SHUTDOWN -> DisplayEffectTerminationReason.SHUTDOWN
    DisplayEffectDisposalReason.FAILED -> DisplayEffectTerminationReason.FAILED
}
