package com.awabi2048.ccsystem.features.misc.listener

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class DynamicDistanceListener : Listener {

    companion object {
        private const val MIN_DISTANCE = 2
        private const val SPEED_EMA_ALPHA = 0.35
        private const val TELEPORT_DISTANCE_THRESHOLD = 32.0
    }

    private data class AppliedDistance(val view: Int, val simulation: Int, val send: Int)

    private data class PlayerState(
        var lastLocation: Location? = null,
        var smoothedSpeedBps: Double = 0.0,
        var lastAppliedTick: Long = Long.MIN_VALUE,
        var lastAppliedDistance: AppliedDistance? = null,
        var adjusted: Boolean = false
    )

    private val states = mutableMapOf<UUID, PlayerState>()
    private var task: BukkitTask? = null
    private var logicalTick: Long = 0L

    init {
        startMonitor()
    }

    fun reload() {
        stopMonitor()
        resetAllPlayersToWorldDefaults()
        states.clear()
        logicalTick = 0L
        startMonitor()
    }

    fun shutdown() {
        stopMonitor()
        resetAllPlayersToWorldDefaults()
        states.clear()
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        states[event.player.uniqueId] = PlayerState(lastLocation = event.player.location.clone())
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        states.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onTeleport(event: PlayerTeleportEvent) {
        val state = states.getOrPut(event.player.uniqueId) { PlayerState() }
        state.lastLocation = event.to.clone()
        state.smoothedSpeedBps = 0.0
    }

    private fun startMonitor() {
        val settings = ConfigManager.getDynamicDistanceSettings()
        if (!settings.enabled) {
            return
        }

        task = Bukkit.getScheduler().runTaskTimer(
            CCSystem.instance,
            Runnable { runTick() },
            settings.intervalTicks,
            settings.intervalTicks
        )
    }

    private fun stopMonitor() {
        task?.cancel()
        task = null
    }

    private fun runTick() {
        val settings = ConfigManager.getDynamicDistanceSettings()
        if (!settings.enabled) {
            resetAllPlayersToWorldDefaults()
            states.clear()
            return
        }

        val onlinePlayers = Bukkit.getOnlinePlayers()
        val onlineCount = onlinePlayers.size
        val seconds = settings.intervalTicks.toDouble() / 20.0
        val debugEnabled = ConfigManager.isDebug()
        logicalTick += settings.intervalTicks

        for (player in onlinePlayers) {
            val state = states.getOrPut(player.uniqueId) { PlayerState() }
            updateSmoothedSpeed(player, state, seconds)

            if (isWorldBlacklisted(player.world.name, settings.worldBlacklist)) {
                if (state.adjusted) {
                    resetPlayerToWorldDefault(player)
                    state.adjusted = false
                    state.lastAppliedDistance = null
                }
                if (debugEnabled) {
                    player.sendActionBar(legacy("§8[DD] §7blacklisted: ${player.world.name}"))
                }
                continue
            }

            val speedDelta = selectSpeedDelta(settings.speedRules, state.smoothedSpeedBps)
            val onlineDelta = selectOnlineDelta(settings.onlineRules, onlineCount)

            val rawView = settings.baseView + speedDelta.view + onlineDelta.view
            val rawSimulation = settings.baseSimulation + speedDelta.simulation + onlineDelta.simulation
            val rawSend = settings.baseSend + speedDelta.send + onlineDelta.send

            var view = rawView
            var simulation = rawSimulation
            var send = rawSend

            view = max(MIN_DISTANCE, view)
            simulation = max(MIN_DISTANCE, simulation)
            send = max(MIN_DISTANCE, send)

            view = view.coerceIn(settings.viewLimit.first, settings.viewLimit.last)
            simulation = simulation.coerceIn(settings.simulationLimit.first, settings.simulationLimit.last)
            send = send.coerceIn(settings.sendLimit.first, settings.sendLimit.last)

            view = max(MIN_DISTANCE, view)
            simulation = max(MIN_DISTANCE, simulation)
            send = max(MIN_DISTANCE, send)

            simulation = min(simulation, view)
            send = min(send, view)

            val clientViewDistance = player.clientViewDistance
            if (clientViewDistance >= MIN_DISTANCE) {
                send = min(send, clientViewDistance)
            }
            send = max(MIN_DISTANCE, send)

            val target = AppliedDistance(view, simulation, send)
            val currentApplied = state.lastAppliedDistance
            val changed = currentApplied != target
            val canApply = logicalTick - state.lastAppliedTick >= settings.applyCooldownTicks
            val shouldApply = changed && (canApply || currentApplied == null)

            if (shouldApply) {
                applyDistance(player, target)
                state.lastAppliedTick = logicalTick
                state.lastAppliedDistance = target
                state.adjusted = true
            }

            if (debugEnabled) {
                sendDebugActionBar(
                    player = player,
                    state = state,
                    onlineCount = onlineCount,
                    speedDelta = speedDelta,
                    onlineDelta = onlineDelta,
                    target = target,
                    changed = changed,
                    shouldApply = shouldApply
                )
            }
        }
    }

    private fun sendDebugActionBar(
        player: Player,
        state: PlayerState,
        onlineCount: Int,
        speedDelta: ConfigManager.DistanceDelta,
        onlineDelta: ConfigManager.DistanceDelta,
        target: AppliedDistance,
        changed: Boolean,
        shouldApply: Boolean
    ) {
        val speedText = String.format(Locale.US, "%.2f", state.smoothedSpeedBps)
        val applyState = when {
            shouldApply -> "apply"
            changed -> "cooldown"
            else -> "hold"
        }
        player.sendActionBar(legacy(
            "§8[DD] §fV:${target.view} S:${target.simulation} Send:${target.send} " +
                "§7spd:$speedText p:$onlineCount " +
                "dS(${speedDelta.view}/${speedDelta.simulation}/${speedDelta.send}) " +
                "dP(${onlineDelta.view}/${onlineDelta.simulation}/${onlineDelta.send}) §e$applyState"
        ))
    }

    private fun legacy(text: String): Component = LegacyComponentSerializer.legacySection().deserialize(text)

    private fun applyDistance(player: Player, distance: AppliedDistance) {
        player.setViewDistance(distance.view)
        player.setSimulationDistance(distance.simulation)
        player.setSendViewDistance(distance.send)
    }

    private fun updateSmoothedSpeed(player: Player, state: PlayerState, seconds: Double) {
        val current = player.location
        val previous = state.lastLocation
        state.lastLocation = current.clone()

        if (previous == null || previous.world?.uid != current.world.uid || seconds <= 0.0) {
            state.smoothedSpeedBps = 0.0
            return
        }

        val dx = current.x - previous.x
        val dz = current.z - previous.z
        val horizontalDistance = sqrt(dx * dx + dz * dz)

        val rawSpeed = if (horizontalDistance >= TELEPORT_DISTANCE_THRESHOLD) {
            0.0
        } else {
            horizontalDistance / seconds
        }

        state.smoothedSpeedBps += (rawSpeed - state.smoothedSpeedBps) * SPEED_EMA_ALPHA
    }

    private fun selectSpeedDelta(rules: List<ConfigManager.SpeedFactorRule>, speedBps: Double): ConfigManager.DistanceDelta {
        if (rules.isEmpty()) {
            return ConfigManager.DistanceDelta(0, 0, 0)
        }
        return rules.firstOrNull { speedBps <= it.maxBps }?.delta ?: rules.last().delta
    }

    private fun selectOnlineDelta(rules: List<ConfigManager.OnlineFactorRule>, onlineCount: Int): ConfigManager.DistanceDelta {
        if (rules.isEmpty()) {
            return ConfigManager.DistanceDelta(0, 0, 0)
        }
        return rules.firstOrNull { onlineCount <= it.maxPlayers }?.delta ?: rules.last().delta
    }

    private fun isWorldBlacklisted(worldName: String, blacklist: List<String>): Boolean {
        if (blacklist.isEmpty()) {
            return false
        }

        val lowerName = worldName.lowercase()
        return blacklist.any { rawPattern ->
            val pattern = rawPattern.trim().lowercase()
            if (pattern.isEmpty()) {
                false
            } else if (!pattern.contains("*")) {
                lowerName == pattern
            } else {
                wildcardToRegex(pattern).matches(lowerName)
            }
        }
    }

    private fun wildcardToRegex(pattern: String): Regex {
        val escaped = Regex.escape(pattern).replace("\\*", ".*")
        return Regex("^$escaped$")
    }

    private fun resetAllPlayersToWorldDefaults() {
        for (player in Bukkit.getOnlinePlayers()) {
            resetPlayerToWorldDefault(player)
        }
    }

    private fun resetPlayerToWorldDefault(player: Player) {
        val world = player.world
        player.setViewDistance(max(MIN_DISTANCE, world.viewDistance))
        player.setSimulationDistance(max(MIN_DISTANCE, world.simulationDistance))

        val worldSend = world.sendViewDistance
        val safeWorldSend = if (worldSend >= MIN_DISTANCE) worldSend else world.viewDistance
        val clientViewDistance = player.clientViewDistance
        val finalSend = if (clientViewDistance >= MIN_DISTANCE) {
            min(safeWorldSend, clientViewDistance)
        } else {
            safeWorldSend
        }

        player.setSendViewDistance(max(MIN_DISTANCE, finalSend))
    }
}
