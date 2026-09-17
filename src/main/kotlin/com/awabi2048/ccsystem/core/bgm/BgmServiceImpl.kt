package com.awabi2048.ccsystem.core.bgm

import com.awabi2048.ccsystem.api.bgm.BgmRequest
import com.awabi2048.ccsystem.api.bgm.BgmService
import com.awabi2048.ccsystem.api.bgm.BgmSource
import org.bukkit.Bukkit
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * [BgmService] の実装。
 * 旧cc-content側BGMManagerの1tick精密ループ再生を基盤とし、
 * プレイヤー単位の単一スロットと [BgmSource] 優先順位による予約解決を一元化する。
 * 主スレッド前提（旧実装と同一）。
 */
class BgmServiceImpl(private val plugin: JavaPlugin) : BgmService {
    private class ActivePlayback(
        val soundKey: String,
        val pitch: Float,
        val volume: Float,
        val category: SoundCategory?,
        val suppressVanillaMusic: Boolean,
        val source: BgmSource,
        val startTick: Long,
        val loopDurationTicks: Long,
        var nextPlayAtTick: Long,
    ) {
        lateinit var task: BukkitTask
        @Volatile var loopCount: Long = 0L
        @Volatile var elapsedTicks: Long = 0L
    }

    /** プレイヤーごとのsource別予約。下位予約は上位解放まで保持される。 */
    private val reservations = mutableMapOf<UUID, MutableMap<BgmSource, BgmRequest>>()

    /** プレイヤーごとの有効な再生。単一スロット。 */
    private val activePlaybacks = mutableMapOf<UUID, ActivePlayback>()

    override fun acquire(player: Player, source: BgmSource, request: BgmRequest) {
        val playerReservations = reservations.getOrPut(player.uniqueId) { mutableMapOf() }
        playerReservations[source] = request
        refresh(player)
    }

    override fun release(player: Player, source: BgmSource) {
        reservations[player.uniqueId]?.remove(source)
        if (reservations[player.uniqueId]?.isEmpty() == true) {
            reservations.remove(player.uniqueId)
        }
        refresh(player)
    }

    override fun stop(player: Player) {
        reservations.remove(player.uniqueId)
        stopActivePlayback(player, activePlaybacks.remove(player.uniqueId))
    }

    override fun stop(player: Player, soundKey: String) {
        reservations[player.uniqueId]?.entries?.removeIf { it.value.soundKey == soundKey }
        if (reservations[player.uniqueId]?.isEmpty() == true) {
            reservations.remove(player.uniqueId)
        }
        val active = activePlaybacks[player.uniqueId]
        if (active != null && active.soundKey == soundKey) {
            activePlaybacks.remove(player.uniqueId)
            stopActivePlayback(player, active)
        }
        refresh(player)
    }

    override fun stopAll() {
        val entries = activePlaybacks.toMap()
        entries.values.forEach { it.task.cancel() }
        activePlaybacks.clear()
        reservations.clear()
        for (player in Bukkit.getOnlinePlayers()) {
            entries[player.uniqueId]?.let { stopPlayerSound(player, it) }
        }
    }

    override fun isPlaying(player: Player, soundKey: String?): Boolean {
        val active = activePlaybacks[player.uniqueId] ?: return false
        return soundKey == null || active.soundKey == soundKey
    }

    override fun getPlaybackStartNanos(player: Player, soundKey: String?): Long? {
        val active = activePlaybacks[player.uniqueId] ?: return null
        if (soundKey != null && active.soundKey != soundKey) return null
        return active.startTick * 50_000_000L
    }

    /**
     * 予約集合から有効化すべきsourceを解決する。純粋関数。
     */
    companion object {
        @JvmStatic
        fun resolveActiveSource(sources: Collection<BgmSource>): BgmSource? {
            return sources.maxByOrNull { it.priority }
        }
    }

    // 有効な予約を再生へ反映する。同一source・同一要求なら継続し、再頭出ししない。
    private fun refresh(player: Player) {
        if (!player.isOnline) {
            reservations.remove(player.uniqueId)
            stopActivePlayback(player, activePlaybacks.remove(player.uniqueId))
            return
        }
        val playerReservations = reservations[player.uniqueId]
        if (playerReservations == null) {
            stopActivePlayback(player, activePlaybacks.remove(player.uniqueId))
            return
        }
        val winner = resolveActiveSource(playerReservations.keys)
        if (winner == null) {
            stopActivePlayback(player, activePlaybacks.remove(player.uniqueId))
            return
        }
        val request = playerReservations[winner] ?: return
        val active = activePlaybacks[player.uniqueId]
        if (active != null && active.source == winner && sameRequest(active, request)) {
            return
        }
        stopActivePlayback(player, activePlaybacks.remove(player.uniqueId))
        startPlayback(player, winner, request)
    }

    private fun sameRequest(active: ActivePlayback, request: BgmRequest): Boolean {
        return active.soundKey == request.soundKey &&
            active.pitch == request.pitch &&
            active.volume == request.volume &&
            active.category == request.category &&
            active.suppressVanillaMusic == request.suppressVanillaMusic &&
            active.loopDurationTicks == request.loopTicks
    }

    private fun startPlayback(player: Player, source: BgmSource, request: BgmRequest) {
        val startTick = Bukkit.getCurrentTick().toLong()
        val playback = ActivePlayback(
            soundKey = request.soundKey,
            pitch = request.pitch,
            volume = request.volume,
            category = request.category,
            suppressVanillaMusic = request.suppressVanillaMusic,
            source = source,
            startTick = startTick,
            loopDurationTicks = request.loopTicks,
            nextPlayAtTick = startTick + request.loopTicks,
        )
        // 初回即時再生後に監視タスクを常駐させる（旧BGMManagerと同一の順序）。
        val task = object : org.bukkit.scheduler.BukkitRunnable() {
            override fun run() {
                if (!player.isOnline) {
                    this@BgmServiceImpl.stop(player)
                    return
                }
                val current = activePlaybacks[player.uniqueId] ?: return
                val currentTick = Bukkit.getCurrentTick().toLong()
                current.elapsedTicks++
                if (current.suppressVanillaMusic && current.elapsedTicks % 20L == 0L) {
                    player.stopSound(SoundCategory.MUSIC)
                }
                if (currentTick >= current.nextPlayAtTick) {
                    playPlayerSound(player, current)
                    var next = current.nextPlayAtTick + current.loopDurationTicks
                    while (next <= currentTick) {
                        next += current.loopDurationTicks
                    }
                    current.nextPlayAtTick = next
                    current.loopCount++
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
        playback.task = task
        activePlaybacks[player.uniqueId] = playback
        playPlayerSound(player, playback)
    }

    private fun playPlayerSound(player: Player, playback: ActivePlayback) {
        val category = playback.category
        if (category == null) {
            player.playSound(player.location, playback.soundKey, playback.volume, playback.pitch)
        } else {
            player.playSound(player.location, playback.soundKey, category, playback.volume, playback.pitch)
        }
    }

    private fun stopActivePlayback(player: Player, playback: ActivePlayback?) {
        if (playback == null) return
        playback.task.cancel()
        stopPlayerSound(player, playback)
    }

    private fun stopPlayerSound(player: Player, playback: ActivePlayback) {
        player.stopSound(playback.soundKey)
        playback.category?.let { player.stopSound(playback.soundKey, it) }
    }
}
