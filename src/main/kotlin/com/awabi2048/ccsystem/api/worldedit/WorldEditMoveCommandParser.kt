package com.awabi2048.ccsystem.api.worldedit

/**
 * WorldEditの `//move` に必要な位置引数だけを取り出すパーサーです。
 *
 * WorldEditのコマンド引数全体を再実装するのではなく、座標同期に必要な
 * 「移動回数」と「移動方向／ベクトル」だけを扱います。マスク等のスイッチは
 * 移動先判定に影響しないため無視します。WorldEdit APIへ触れないため、
 * WorldEdit不在時も安全に呼び出せます。
 */
object WorldEditMoveCommandParser {
    private val integerPattern = Regex("[+-]?\\d+")
    private val vectorPattern = Regex("[+-]?\\d+,[+-]?\\d+,[+-]?\\d+")

    fun parse(message: String): WorldEditMoveCommand? {
        val tokens = message.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        if (tokens.isEmpty() || WorldEditCommands.normalizedCommandName(tokens.first()) != "move") {
            return null
        }

        val positionalArguments = collectPositionalArguments(tokens.drop(1))
        val first = positionalArguments.firstOrNull()
        val multiplier: Int
        val offsetToken: String

        if (first == null) {
            multiplier = 1
            offsetToken = "forward"
        } else if (integerPattern.matches(first)) {
            multiplier = first.toIntOrNull() ?: return null
            offsetToken = positionalArguments.getOrNull(1) ?: "forward"
        } else {
            multiplier = 1
            offsetToken = first
        }

        if (multiplier < 1 || offsetToken.isBlank()) {
            return null
        }

        return WorldEditMoveCommand(multiplier, offsetToken)
    }

    private fun collectPositionalArguments(arguments: List<String>): List<String> {
        val positional = mutableListOf<String>()
        var skipNext = false

        for (argument in arguments) {
            if (skipNext) {
                skipNext = false
                continue
            }

            // -m/--mask is the only supported move switch that consumes a value.
            // Other switches are boolean and can simply be ignored here.
            if (argument == "-m" || argument == "--mask") {
                skipNext = true
                continue
            }
            if (
                argument.startsWith("-") &&
                !integerPattern.matches(argument) &&
                !vectorPattern.matches(argument)
            ) {
                continue
            }

            positional += argument
        }
        return positional
    }
}
