package com.awabi2048.ccsystem.features.misc.listener

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import org.bukkit.SoundCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

/**
 * 音楽再生リスナー
 */
class MusicListener : Listener {

    internal companion object {
        fun replayDelayAfterPlay(durationTicks: Long): Long = (durationTicks - 20L).coerceAtLeast(0L)
    }

    private val musicTasks = HashMap<UUID, org.bukkit.scheduler.BukkitTask>()
    private val currentSounds = HashMap<UUID, String>()

    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        playMusic(event.player, event.player.world.key.toString())
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        // ログイン直後は再生されないことがあるため、20tick遅延させる
        CCSystem.instance.server.scheduler.runTaskLater(CCSystem.instance, Runnable {
            if (event.player.isOnline) {
                playMusic(event.player, event.player.world.key.toString())
            }
        }, 20L)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        stopMusic(event.player)
    }

    /**
     * 全プレイヤーの音楽を停止
     */
    fun stopAllPlayersMusic() {
        CCSystem.instance.server.onlinePlayers.forEach { stopMusic(it) }
    }

    /**
     * 全プレイヤーの音楽を開始
     */
    fun startAllPlayersMusic() {
        CCSystem.instance.server.onlinePlayers.forEach { playMusic(it, it.world.key.toString()) }
    }

    /**
     * プレイヤーの音楽を停止
     */
    fun stopMusic(player: org.bukkit.entity.Player) {
        musicTasks.remove(player.uniqueId)?.cancel()
        val lastSound = currentSounds.remove(player.uniqueId)
        
        // 独自BGMはRECORDSカテゴリで再生しているため、停止時だけ同カテゴリを明示する。
        player.stopSound(SoundCategory.RECORDS)
        if (lastSound != null) {
            player.stopSound(lastSound, SoundCategory.RECORDS)
        }
    }

    /**
     * 音楽を再生
     */
    fun playMusic(player: org.bukkit.entity.Player, worldName: String) {
        stopMusic(player) // 以前のBGMを停止
        
        // 個人の再生設定をチェック (デフォルト true)
        if (!PlayerDataManager.getBoolean(player.uniqueId, "play_music", true)) {
            stopMusic(player)
            return
        }

        // 音楽再生が無効かチェック
        if (!ConfigManager.isMusicEnabled()) {
            return
        }

        // ワールドの音楽設定を取得
        val musicSetting = ConfigManager.getMusicSetting(worldName) ?: return
        
        val soundId = musicSetting.sound
        val volume = musicSetting.volume
        val pitch = musicSetting.pitch
        val durationTicks = musicSetting.duration.toLong() * 20L
        var ticksUntilReplay = 0L

        val task = CCSystem.instance.server.scheduler.runTaskTimer(CCSystem.instance, Runnable {
            if (player.isOnline) {
                // 再生中も設定をチェック
                if (!PlayerDataManager.getBoolean(player.uniqueId, "play_music", true)) {
                    stopMusic(player)
                    return@Runnable
                }

                currentSounds[player.uniqueId] = soundId

                // 曲の長さとは独立して、毎秒バニラの音楽カテゴリを抑止する。
                player.stopSound(SoundCategory.MUSIC)

                if (ticksUntilReplay <= 0L) {
                    // BGMとして再生するためRECORDSを使用し、位置はプレイヤーの現在地とする。
                    player.playSound(player.location, soundId, SoundCategory.RECORDS, volume, pitch)
                    ticksUntilReplay = replayDelayAfterPlay(durationTicks)
                } else {
                    ticksUntilReplay -= 20L
                }
            } else {
                stopMusic(player)
            }
        }, 0L, 20L)

        musicTasks[player.uniqueId] = task
    }
}
