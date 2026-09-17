package com.awabi2048.ccsystem.features.misc.listener

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.api.bgm.BgmRequest
import com.awabi2048.ccsystem.api.bgm.BgmService
import com.awabi2048.ccsystem.api.bgm.BgmSource
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import org.bukkit.SoundCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * 音楽再生リスナー。
 * 実再生は [BgmService] が一元管理し、本リスナーはワールド連動の予約（[BgmSource.WORLD]）のみを担当する。
 */
class MusicListener(private val bgmService: BgmService) : Listener {

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
        bgmService.release(player, BgmSource.WORLD)
        // 移行期の残存音（旧方式のRECORDS再生）も確実に止める。
        player.stopSound(SoundCategory.RECORDS)
    }

    /**
     * 音楽を再生
     */
    fun playMusic(player: org.bukkit.entity.Player, worldName: String) {
        // 既存のワールド予約を破棄してから評価する（旧来のstopMusic先行と同義）。
        bgmService.release(player, BgmSource.WORLD)

        // 個人の再生設定をチェック (デフォルト true)
        if (!PlayerDataManager.getBoolean(player.uniqueId, "play_music", true)) {
            return
        }

        // 音楽再生が無効かチェック
        if (!ConfigManager.isMusicEnabled()) {
            return
        }

        // ワールドの音楽設定を取得
        val musicSetting = ConfigManager.getMusicSetting(worldName) ?: return

        bgmService.acquire(
            player,
            BgmSource.WORLD,
            BgmRequest(
                soundKey = musicSetting.sound,
                loopTicks = musicSetting.duration.toLong() * 20L,
                pitch = musicSetting.pitch,
                volume = musicSetting.volume,
                category = SoundCategory.RECORDS,
                suppressVanillaMusic = true,
            )
        )
    }
}
