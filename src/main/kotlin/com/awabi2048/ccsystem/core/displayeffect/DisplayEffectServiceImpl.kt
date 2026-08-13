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
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleEmissionRequest
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticlePresetInfo
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleVisibilityMode
import com.awabi2048.ccsystem.core.displayeffect.paper.PaperDisplayEffectBackend
import com.awabi2048.ccsystem.core.displayeffect.paper.PaperMaterialAssetResolver
import com.awabi2048.ccsystem.core.displayeffect.paper.PaperDisplayParticleBackend
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import java.util.UUID
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.DisplayParticleLimitType

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

    private data class ActiveDisplayParticle(
        val instance: DisplayEffectInstanceImpl,
        val owner: Plugin,
        val runtime: DisplayParticleRuntime,
        val backend: PaperDisplayParticleBackend
    )

    private val materialAssetResolver = PaperMaterialAssetResolver()
    private val defaultAssetResolver: DisplayEffectAssetResolver = materialAssetResolver
    private val activeEffects = linkedMapOf<UUID, ActiveEffect>()
    private val activeDisplayParticles = linkedMapOf<UUID, ActiveDisplayParticle>()
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
        val ownerCount = activeEffects.values.count { it.owner === owner } +
            activeDisplayParticles.values.count { it.owner === owner }
        val activeEntityCount = activeEffects.size
        val ownerEntityCount = activeEffects.values.count { it.owner === owner } +
            activeDisplayParticles.values.filter { it.owner === owner }.sumOf { it.runtime.liveEntityCount }
        val tickCount = startsThisTick[owner] ?: 0
        if (activeEffects.size >= MAX_ACTIVE_EFFECTS ||
            ownerCount >= MAX_ACTIVE_EFFECTS_PER_OWNER ||
            tickCount >= MAX_STARTS_PER_TICK_PER_OWNER ||
            activeEntityCount >= MAX_ACTIVE_EFFECTS ||
            ownerEntityCount >= MAX_ACTIVE_DISPLAY_ENTITIES_PER_OWNER
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

    override fun listDisplayParticlePresets(): List<DisplayParticlePresetInfo> =
        DisplayParticleCatalog.list().map { it.info() }

    override fun emitDisplayParticles(
        owner: Plugin,
        anchor: Location,
        request: DisplayParticleEmissionRequest
    ): DisplayEffectStartResult {
        commonRejection(owner, anchor)?.let { return it }
        val preset = DisplayParticleCatalog.find(request.presetId)
            ?: return DisplayEffectStartResult.Rejected(
                DisplayEffectStartRejection.UNKNOWN_PATTERN,
                "未登録のDisplayパーティクル・プリセットです: ${request.presetId.value}"
            )
        if (request.visibilityMode == DisplayParticleVisibilityMode.NORMAL &&
            anchor.world?.players.orEmpty().none { it.location.distanceSquared(anchor) <= NORMAL_VIEW_DISTANCE_SQUARED }
        ) {
            return DisplayEffectStartResult.Rejected(
                DisplayEffectStartRejection.NO_VIEWERS,
                "表示距離内にプレイヤーがいないため生成しませんでした"
            )
        }

        val entityCost = request.count
        if (entityCost > ConfigManager.getDisplayParticleLimit(DisplayParticleLimitType.EMISSION)) {
            return DisplayEffectStartResult.Rejected(
                DisplayEffectStartRejection.CAPACITY_EXCEEDED,
                "1回のDisplayパーティクル放出上限を超えます: required=$entityCost " +
                    "limit=${ConfigManager.getDisplayParticleLimit(DisplayParticleLimitType.EMISSION)}"
            )
        }
        val ownerEntityCount = activeDisplayParticles.values.filter { it.owner === owner }.sumOf { it.runtime.liveEntityCount }
        val totalEntityCount = currentDisplayParticleCount()
        val tickCount = startsThisTick[owner] ?: 0
        if (totalEntityCount + entityCost > ConfigManager.getDisplayParticleLimit(DisplayParticleLimitType.GLOBAL) ||
            ownerEntityCount + entityCost > ConfigManager.getDisplayParticleLimit(DisplayParticleLimitType.OWNER) ||
            tickCount + entityCost > ConfigManager.getDisplayParticleLimit(DisplayParticleLimitType.PER_TICK)
        ) {
            return DisplayEffectStartResult.Rejected(
                DisplayEffectStartRejection.CAPACITY_EXCEEDED,
                "Displayパーティクル上限を超えます: required=$entityCost active=$totalEntityCount " +
                    "global=${ConfigManager.getDisplayParticleLimit(DisplayParticleLimitType.GLOBAL)} " +
                    "owner=${ConfigManager.getDisplayParticleLimit(DisplayParticleLimitType.OWNER)} " +
                    "perTick=${ConfigManager.getDisplayParticleLimit(DisplayParticleLimitType.PER_TICK)}"
            )
        }

        val instanceId = UUID.randomUUID()
        val backend = PaperDisplayParticleBackend(plugin, anchor, materialAssetResolver, owner.name, instanceId)
        val runtime = DisplayParticleRuntime(preset, request, backend)
        return when (val result = runtime.start()) {
            DisplayEffectRuntimeResult.Started -> {
                val instance = DisplayEffectInstanceImpl(instanceId, this)
                activeDisplayParticles[instanceId] = ActiveDisplayParticle(instance, owner, runtime, backend)
                startsThisTick[owner] = tickCount + entityCost
                DisplayEffectStartResult.Started(instance)
            }
            is DisplayEffectRuntimeResult.Failed -> rejected(result.error)
            else -> DisplayEffectStartResult.Rejected(
                DisplayEffectStartRejection.BACKEND_FAILURE,
                "Displayパーティクルの開始状態が不正です: $result"
            )
        }
    }

    internal fun stop(instanceId: UUID): DisplayEffectStopResult {
        if (!Bukkit.isPrimaryThread()) return DisplayEffectStopResult.NotMainThread
        val particle = activeDisplayParticles.remove(instanceId)
        if (particle != null) {
            val result = particle.runtime.stop(DisplayEffectDisposalReason.CANCELLED)
            particle.instance.markStopped(result)
            return if (result is DisplayEffectRuntimeResult.Stopped) DisplayEffectStopResult.Stopped
            else DisplayEffectStopResult.ServiceUnavailable
        }
        val active = activeEffects.remove(instanceId) ?: return DisplayEffectStopResult.AlreadyStopped
        val result = active.runtime.stop(DisplayEffectDisposalReason.CANCELLED)
        active.instance.markStopped(result)
        return if (result is DisplayEffectRuntimeResult.Stopped) {
            DisplayEffectStopResult.Stopped
        } else {
            DisplayEffectStopResult.ServiceUnavailable
        }
    }

    internal fun currentDisplayParticleCount(): Int =
        activeDisplayParticles.values.sumOf { it.runtime.liveEntityCount }

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
        activeDisplayParticles.entries.toList()
            .filter { (_, active) -> active.owner === event.plugin }
            .forEach { (instanceId, active) ->
                activeDisplayParticles.remove(instanceId)
                active.runtime.stop(DisplayEffectDisposalReason.OWNER_DISABLED)
                active.instance.markTerminated(DisplayEffectInstanceStatus.STOPPED, DisplayEffectTerminationReason.OWNER_DISABLED)
            }
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
        activeDisplayParticles.values.toList().forEach { active ->
            active.runtime.stop(DisplayEffectDisposalReason.SHUTDOWN)
            active.instance.markTerminated(DisplayEffectInstanceStatus.STOPPED, DisplayEffectTerminationReason.SHUTDOWN)
        }
        activeDisplayParticles.clear()
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
        activeDisplayParticles.entries.toList().forEach { (instanceId, active) ->
            when (val result = active.runtime.tick()) {
                DisplayEffectRuntimeResult.Advanced,
                DisplayEffectRuntimeResult.Ignored,
                DisplayEffectRuntimeResult.Started -> Unit
                is DisplayEffectRuntimeResult.Stopped -> {
                    activeDisplayParticles.remove(instanceId)
                    active.instance.markStopped(result)
                }
                is DisplayEffectRuntimeResult.Failed -> {
                    activeDisplayParticles.remove(instanceId)
                    active.instance.markFailed(result.error)
                    active.backend.disposeAll(DisplayEffectDisposalReason.FAILED)
                    plugin.logger.warning("[DisplayParticle] runtime failed: instance=$instanceId error=${result.error.message}")
                }
            }
        }
    }

    private fun commonRejection(owner: Plugin, anchor: Location): DisplayEffectStartResult.Rejected? {
        if (!Bukkit.isPrimaryThread()) return DisplayEffectStartResult.Rejected(
            DisplayEffectStartRejection.NOT_MAIN_THREAD, "Display Effectはメインスレッドから開始してください"
        )
        if (shutdown || !plugin.isEnabled) return DisplayEffectStartResult.Rejected(
            DisplayEffectStartRejection.SHUTDOWN, "Display Effect serviceは停止済みです"
        )
        if (!owner.isEnabled) return DisplayEffectStartResult.Rejected(
            DisplayEffectStartRejection.OWNER_DISABLED, "owner Pluginが無効です: ${owner.name}"
        )
        val world = anchor.world ?: return DisplayEffectStartResult.Rejected(
            DisplayEffectStartRejection.WORLD_UNAVAILABLE, "anchorにWorldがありません"
        )
        if (Bukkit.getWorld(world.uid) == null) return DisplayEffectStartResult.Rejected(
            DisplayEffectStartRejection.WORLD_UNAVAILABLE, "Worldが利用できません: ${world.uid}"
        )
        val tick = Bukkit.getCurrentTick()
        if (tick != currentTick) {
            currentTick = tick
            startsThisTick.clear()
        }
        return null
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
        // 通常Display Effect向けの防御上限です。パーティクルのowner制限とは独立に維持します。
        private const val MAX_ACTIVE_DISPLAY_ENTITIES_PER_OWNER = 128
        private const val NORMAL_VIEW_DISTANCE_SQUARED = 32.0 * 32.0
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
