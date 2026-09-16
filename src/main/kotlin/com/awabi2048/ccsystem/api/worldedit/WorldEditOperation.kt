package com.awabi2048.ccsystem.api.worldedit

/**
 * WorldEditのクリップボード操作種別です。
 *
 * Bukkitの通常ブロックイベントを通らない外部編集のため、コマンド文字列の
 * 先読みで操作種別だけを保持し、実ブロックの確定は遅延タスクへ委ねます。
 */
enum class WorldEditClipboardOperation {
    COPY,
    CUT,
}

/**
 * WorldEditの `//move` に必要な位置引数だけを保持します。
 *
 * WorldEditのコマンド引数全体を再実装するのではなく、座標同期に必要な
 * 「移動回数」と「移動方向／ベクトル」だけを扱います。マスク等のスイッチは
 * 移動先判定に影響しないため無視します。
 */
data class WorldEditMoveCommand(
    val multiplier: Int,
    val offsetToken: String,
)
