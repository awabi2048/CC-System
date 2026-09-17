package com.awabi2048.ccsystem.api.bgm

import org.bukkit.SoundCategory

/**
 * ループBGMの再生要求を表す。
 * 旧cc-content側BGMManagerのplayPrecise相当（soundKey・loopTicks・pitch）に
 * 音量・カテゴリ・バニラ音楽抑止を加えたものである。
 */
data class BgmRequest(
    /** 再生するサウンドキー（例: "kota_server:ost_3.sukima_dungeon"）。 */
    val soundKey: String,
    /** 1ループのtick数。1以上。 */
    val loopTicks: Long,
    /** 再生ピッチ。0.1〜2.0。 */
    val pitch: Float = 1.0f,
    /** 再生音量。正の有限値。 */
    val volume: Float = 1.0f,
    /** 再生カテゴリ。nullの場合は既定カテゴリ再生（旧BGMManagerと同一の呼出し）。 */
    val category: SoundCategory? = null,
    /** 有効時は再生中20tickごとにバニラの音楽カテゴリを抑止する。 */
    val suppressVanillaMusic: Boolean = false,
) {
    init {
        validateRequest(this)
    }

    companion object {
        const val MIN_PITCH: Float = 0.1f
        const val MAX_PITCH: Float = 2.0f

        private val SOUND_KEY_PATTERN = Regex("^[a-z0-9._-]+:[a-z0-9._/-]+$")

        /**
         * 要求値を検証する。不正は補正せず例外で起動・実行を止める。
         * テスト容易化のため純粋関数として分離している。
         */
        @JvmStatic
        fun validateRequest(request: BgmRequest) {
            val key = request.soundKey.trim()
            require(key.isNotEmpty()) { "BGMサウンドキーが空です" }
            require(SOUND_KEY_PATTERN.matches(key)) { "BGMサウンドキー形式が不正です: ${request.soundKey}" }
            require(request.loopTicks >= 1L) { "BGM再生tickが不正です: key=${request.soundKey} loopTicks=${request.loopTicks}" }
            require(request.pitch.isFinite() && request.pitch in MIN_PITCH..MAX_PITCH) {
                "BGMピッチが不正です: key=${request.soundKey} pitch=${request.pitch}"
            }
            require(request.volume.isFinite() && request.volume > 0.0f) {
                "BGM音量が不正です: key=${request.soundKey} volume=${request.volume}"
            }
        }
    }
}
