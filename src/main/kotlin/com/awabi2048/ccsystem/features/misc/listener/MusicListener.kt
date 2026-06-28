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

    private val musicTasks = HashMap<UUID, org.bukkit.scheduler.BukkitTask>()
    private val currentSounds = HashMap<UUID, String>()

    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        playMusic(event.player, event.player.world.name)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        // ログイン直後は再生されないことがあるため、20tick遅延させる
        CCSystem.instance.server.scheduler.runTaskLater(CCSystem.instance, Runnable {
            if (event.player.isOnline) {
                playMusic(event.player, event.player.world.name)
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
        CCSystem.instance.server.onlinePlayers.forEach { playMusic(it, it.world.name) }
    }

    /**
     * プレイヤーの音楽を停止
     */
    fun stopMusic(player: org.bukkit.entity.Player) {
        musicTasks.remove(player.uniqueId)?.cancel()
        val lastSound = currentSounds.remove(player.uniqueId)
        
        // 念のため停止パケットも送る (RECORDSカテゴリを指定)
        try {
            player.stopSound(SoundCategory.RECORDS)
            if (lastSound != null) {
                player.stopSound(lastSound, SoundCategory.RECORDS)
            }
        } catch (e: NoSuchMethodError) {
            player.stopSound("")
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
        val duration = musicSetting.duration.toLong() // 秒

        val task = CCSystem.instance.server.scheduler.runTaskTimer(CCSystem.instance, Runnable {
            if (player.isOnline) {
                // 再生中も設定をチェック
                if (!PlayerDataManager.getBoolean(player.uniqueId, "play_music", true)) {
                    stopMusic(player)
                    return@Runnable
                }

                currentSounds[player.uniqueId] = soundId

                // 独自BGMを再生している間は、バニラの music カテゴリ（レコード・ディスク再生等）を停止し続ける。
                // これにより独自BGMとバニラBGMが重ならない。
                try {
                    player.stopSound(SoundCategory.MUSIC)
                } catch (e: NoSuchMethodError) {
                    // フォールバック不可
                }

                // BGMとして再生するため、SoundCategory.RECORDSを使用し、位置はプレイヤーの現在地
                try {
                    player.playSound(player.location, soundId, SoundCategory.RECORDS, volume, pitch)
                } catch (e: NoSuchMethodError) {
                    // 古いバージョンなどのフォールバック (引数の型を明示して曖昧さを回避)
                    player.playSound(player.location, soundId, volume, pitch)
                }
            } else {
                // プレイヤーがオフラインならタスクキャンセル (念のため)
                musicTasks.remove(player.uniqueId)?.cancel()
            }
        }, 0L, duration * 20L)

        musicTasks[player.uniqueId] = task
    }
}