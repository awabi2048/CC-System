package com.awabi2048.ccsystem.features.misc.inputmacro

/**
 * input_macro コマンドテンプレートの解析処理。
 *
 * Bukkit API へ依存しない純粋な処理として実装し、単体テスト可能にする。
 * 仕様: `.docs/planning/active-review/2026-08-02-cc-system-input-macro-dialog-spec.md` 第3章
 */
sealed interface InputMacroTemplateParseResult {
    /**
     * 解析成功。command は Bukkit へ渡す直前に先頭の `/` を1文字だけ除去済みのテンプレート。
     */
    data class Success(val command: String) : InputMacroTemplateParseResult

    /**
     * 解析失敗。理由は言語メッセージへ対応づけるために保持する。
     */
    data class Failure(val reason: Reason) : InputMacroTemplateParseResult {
        enum class Reason {
            /** 引用符が片側だけ残っている */
            UNPAIRED_QUOTE,

            /** 空、または `/` 除去後に空になる */
            EMPTY,

            /** CR / LF / NUL を含む */
            FORBIDDEN_CONTROL,
        }
    }
}

object InputMacroTemplateParser {
    private const val SINGLE_QUOTE = '\''
    private const val DOUBLE_QUOTE = '"'

    /**
     * 先頭引数（input_macro）と末尾の実行モードを除いた引数列を、半角空白1文字で再結合して解析する。
     *
     * - 再結合後の先頭と末尾が同じ引用符で囲まれている場合、外側の引用符1組だけを除去する。
     * - 引用符が片側だけの場合と、制御文字を含む場合は不正として拒否する。
     * - テンプレート先頭の `/` は1文字だけ除去し、除去後に空になる場合は拒否する。
     */
    fun parse(parts: List<String>): InputMacroTemplateParseResult {
        val joined = parts.joinToString(" ")
        if (InputMacroControl.containsForbidden(joined)) {
            return InputMacroTemplateParseResult.Failure(InputMacroTemplateParseResult.Failure.Reason.FORBIDDEN_CONTROL)
        }
        val unquoted = stripOuterQuotes(joined)
            ?: return InputMacroTemplateParseResult.Failure(InputMacroTemplateParseResult.Failure.Reason.UNPAIRED_QUOTE)
        if (unquoted.isEmpty()) {
            return InputMacroTemplateParseResult.Failure(InputMacroTemplateParseResult.Failure.Reason.EMPTY)
        }
        // 先頭の `/` を1文字だけ除去する。
        val command = unquoted.removePrefix("/")
        if (command.isEmpty()) {
            return InputMacroTemplateParseResult.Failure(InputMacroTemplateParseResult.Failure.Reason.EMPTY)
        }
        return InputMacroTemplateParseResult.Success(command)
    }

    /**
     * 先頭と末尾が同じ引用符（`'` または `"`）で囲まれている場合だけ、外側の引用符1組を除去する。
     * 引用符が片側だけ（先頭だけ・末尾だけ、または対応しない組）の場合は null（不正）を返す。
     */
    private fun stripOuterQuotes(value: String): String? {
        if (value.isEmpty()) {
            return value
        }
        val first = value.first()
        val last = value.last()
        val startsWithQuote = first == SINGLE_QUOTE || first == DOUBLE_QUOTE
        val endsWithQuote = last == SINGLE_QUOTE || last == DOUBLE_QUOTE
        if (!startsWithQuote && !endsWithQuote) {
            return value
        }
        if (startsWithQuote && endsWithQuote && first == last) {
            return value.substring(1, value.length - 1)
        }
        return null
    }
}
