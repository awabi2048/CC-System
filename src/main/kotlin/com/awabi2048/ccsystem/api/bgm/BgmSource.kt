package com.awabi2048.ccsystem.api.bgm

/**
 * BGM取得主体を表す。
 * 数値が大きいほど優先され、上位の予約がある間は下位の予約を再生しない。
 * 下位予約は保持されたままとなり、上位の解放時に自動復帰する。
 */
enum class BgmSource(val priority: Int) {
    /** ワールド連動BGM（MusicListener）。最下位。 */
    WORLD(0),

    /** スキマダンジョンBGM。 */
    SUKIMA(10),

    /** アリーナロビーBGM。 */
    ARENA_LOBBY(20),

    /** アリーナ通常BGM。 */
    ARENA_NORMAL(30),

    /** アリーナ戦闘BGM。最上位。 */
    ARENA_COMBAT(40);
}
