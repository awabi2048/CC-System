package com.awabi2048.ccsystem.features.misc.inputmacro

/**
 * input_macro のマクロ展開処理。
 *
 * Bukkit API へ依存しない純粋な処理として実装し、単体テスト可能にする。
 * 仕様: `.docs/planning/active-review/2026-08-02-cc-system-input-macro-dialog-spec.md` 第4章
 */
object InputMacroExpander {
    // 一回の走査で置換するため、挿入値に含まれるマクロ文字列は再展開されない。
    private val macroPattern = Regex("%(player_input|player_name|player_uuid)%")

    /**
     * テンプレート内の3種類のマクロを、対応する値へ一回だけ展開する。
     *
     * - マクロ名は大文字・小文字を区別する。
     * - 未知の `%...%` 形式は変更せずそのまま残す。
     * - 同じマクロはすべて同じ値へ展開する。
     */
    fun expand(
        command: String,
        playerInput: String,
        playerName: String,
        playerUuid: String,
    ): String {
        return macroPattern.replace(command) { match ->
            when (match.groupValues[1]) {
                "player_input" -> playerInput
                "player_name" -> playerName
                else -> playerUuid
            }
        }
    }
}
