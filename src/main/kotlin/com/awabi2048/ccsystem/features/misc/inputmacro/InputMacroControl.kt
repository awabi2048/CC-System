package com.awabi2048.ccsystem.features.misc.inputmacro

/**
 * input_macro が禁止する制御文字の検証。
 *
 * コマンドテンプレートとプレイヤー入力の両方で、改行やNULによる複数コマンド化を防ぐために使用する。
 */
object InputMacroControl {
    private val forbidden = setOf('\r', '\n', '\u0000')

    /** 引数にCR / LF / NUL が含まれる場合に true を返す。 */
    fun containsForbidden(value: String): Boolean = forbidden.any { value.contains(it) }
}
