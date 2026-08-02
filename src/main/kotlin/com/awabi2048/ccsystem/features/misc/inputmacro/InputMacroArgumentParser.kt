package com.awabi2048.ccsystem.features.misc.inputmacro

/**
 * input_macro の引数列（input_macro サブコマンドを除いたもの）の解析処理。
 *
 * Bukkit API へ依存しない純粋な処理として実装し、単体テスト可能にする。
 * 仕様: `.docs/planning/active-review/2026-08-02-cc-system-input-macro-dialog-spec.md` 第3章
 */
sealed interface InputMacroArgumentResult {
    data class Success(
        val playerName: String,
        val templateParts: List<String>,
        val mode: InputMacroMode,
    ) : InputMacroArgumentResult

    data class Failure(val reason: Reason) : InputMacroArgumentResult {
        enum class Reason {
            /** 対象プレイヤー名が指定されていない */
            MISSING_PLAYER,

            /** テンプレートが空 */
            MISSING_TEMPLATE,
        }
    }
}

object InputMacroArgumentParser {

    /**
     * `[プレイヤー名, テンプレート..., モード?]` を解析する。
     *
     * - 最後の引数が `console` / `player` に完全一致（大文字・小文字を区別しない）する場合だけ、
     *   実行モードとして解釈してテンプレートから除外する。
     * - それ以外の場合はモード省略として `console` を既定とし、最後の引数もテンプレートへ含める。
     * - プレイヤー名、またはテンプレートが存在しない場合は失敗を返す。
     */
    fun parse(args: List<String>): InputMacroArgumentResult {
        val playerName = args.firstOrNull()
            ?: return InputMacroArgumentResult.Failure(InputMacroArgumentResult.Failure.Reason.MISSING_PLAYER)
        val rest = args.drop(1)
        val mode = InputMacroMode.from(rest.lastOrNull())
        val templateParts = if (mode != null) rest.dropLast(1) else rest
        if (templateParts.isEmpty()) {
            return InputMacroArgumentResult.Failure(InputMacroArgumentResult.Failure.Reason.MISSING_TEMPLATE)
        }
        return InputMacroArgumentResult.Success(
            playerName = playerName,
            templateParts = templateParts,
            mode = mode ?: InputMacroMode.CONSOLE,
        )
    }
}
