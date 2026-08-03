package com.awabi2048.ccsystem.core.gui

/** Loreの操作案内へ流用されたName文字列から、旧来のLegacy装飾コードだけを除去します。 */
internal object LegacyFormatting {
    private val legacyCode = Regex("§[0-9A-FK-ORa-fk-or]")

    fun strip(value: String): String = legacyCode.replace(value, "")
}
