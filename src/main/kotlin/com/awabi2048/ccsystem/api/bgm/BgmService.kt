package com.awabi2048.ccsystem.api.bgm

import org.bukkit.entity.Player

/**
 * ループBGMの一元管理サービス。
 * プレイヤー単位に単一再生スロットを保証し、[BgmSource] の優先順位で
 * 複数予約の競合を解決する。上位予約の解放時は下位へ自動復帰する。
 */
interface BgmService {
    /**
     * 指定sourceの予約としてループ再生を要求する。
     * 上位予約が有効な間は本要求を保持したまま抑止される。
     */
    fun acquire(player: Player, source: BgmSource, request: BgmRequest)

    /**
     * 指定sourceの予約を解放する。解放後に下位予約があれば自動復帰する。
     */
    fun release(player: Player, source: BgmSource)

    /**
     * プレイヤーの全予約を破棄し、再生中のループを停止する。
     */
    fun stop(player: Player)

    /**
     * 指定キーに一致する予約を破棄する。一致する再生が有効なら停止し、下位へ自動復帰する。
     * 不一致の場合は何もしない。
     */
    fun stop(player: Player, soundKey: String)

    /**
     * 全プレイヤーの全予約を破棄し、全ループ再生を停止する。
     */
    fun stopAll()

    /**
     * 有効な（実際に再生中の）ループがあるか判定する。
     * soundKey指定時はキー一致も要求する。
     */
    fun isPlaying(player: Player, soundKey: String? = null): Boolean

    /**
     * 有効なループ再生の開始時刻（ナノ秒）を返す。なければnull。
     * soundKey指定時はキー一致も要求する。
     */
    fun getPlaybackStartNanos(player: Player, soundKey: String? = null): Long?
}
