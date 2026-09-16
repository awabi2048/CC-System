package com.awabi2048.ccsystem.api.worldedit

import java.util.Locale

/**
 * WorldEdit／バニラ一括操作コマンドの純粋な分類器です。
 *
 * WorldEdit APIへ触れないため、WorldEdit不在時も安全に呼び出せます。
 * 先頭トークンの正規化（`/`・名前空間・大小文字）だけを行い、実際の引数
 * 解釈は各操作のパーサーへ委ねます。
 */
object WorldEditCommands {
    /**
     * `//copy`・`worldedit:copy` 等から操作種別を取り出します。
     *
     * 該当しない場合は null を返します。
     */
    fun parseClipboardOperation(message: String): WorldEditClipboardOperation? =
        when (normalizedCommandName(message)) {
            "copy" -> WorldEditClipboardOperation.COPY
            "cut" -> WorldEditClipboardOperation.CUT
            else -> null
        }

    /** `//paste` 系かを判定します。 */
    fun isPasteCommand(message: String): Boolean =
        normalizedCommandName(message) == "paste"

    /** `//move` 系かを判定します。詳細な引数解釈は [WorldEditMoveCommandParser] へ委ねます。 */
    fun isMoveCommand(message: String): Boolean =
        normalizedCommandName(message) == "move"

    /** `//undo` 系かを判定します。引数（回数・世界）は無視し履歴巻き戻しとして扱います。 */
    fun isUndoCommand(message: String): Boolean =
        normalizedCommandName(message) == "undo"

    /** `//redo` 系かを判定します。引数は無視し履歴繰返しとして扱います。 */
    fun isRedoCommand(message: String): Boolean =
        normalizedCommandName(message) == "redo"

    /**
     * 自動同期の対象外であり、座標台帳を壊し得る一括操作かを判定します。
     *
     * WorldEditの `//set`・`//replace`・`//stack`・`//rotate`・`//flip`・
     * `//schematic`(`//schem`) と、Bukkitイベントを通さず実体だけを書き換える
     * バニラの `/clone`・`/fill`・`/setblock` が対象です。検出時は警告と
     * 周期的掃除へ委ね、自動追従しません。undo／redo は専用経路で扱うため
     * 含めません。
     */
    fun isUnsupportedBulkCommand(message: String): Boolean =
        when (normalizedCommandName(message)) {
            "set",
            "replace",
            "stack",
            "rotate",
            "flip",
            "schematic",
            "schem",
            "clone",
            "fill",
            "setblock",
            -> true
            else -> false
        }

    /**
     * 先頭トークンをコマンド名へ正規化します。
     *
     * `//copy`・`/worldedit:copy`・`/minecraft:clone` 等を同一名へ畳みます。
     */
    fun normalizedCommandName(message: String): String {
        val firstToken = message.trim().substringBefore(' ')
        val withoutSlash = firstToken.dropWhile { it == '/' }
        val withoutNamespace = withoutSlash.substringAfterLast(':')
        return withoutNamespace.dropWhile { it == '/' }.lowercase(Locale.ROOT)
    }
}
