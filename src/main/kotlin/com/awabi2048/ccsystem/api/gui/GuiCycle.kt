package com.awabi2048.ccsystem.api.gui

/**
 * GUI上の候補リストを循環させるための共通規則。
 *
 * 左クリックは次、右クリックは前を選ぶ画面規則に合わせ、呼び出し側は
 * `reverse = event.isRightClick` を渡す。現在値が候補に存在しない場合は、
 * データ不整合として例外にし、暗黙のフォールバックを行わない。
 */
object GuiCycle {
    fun <T> select(current: T, values: Array<T>, reverse: Boolean = false): T {
        return select(current, values.asList(), reverse)
    }

    fun <T> select(current: T, values: List<T>, reverse: Boolean = false): T {
        require(values.isNotEmpty()) { "GUI cycle values must not be empty" }
        val index = values.indexOf(current)
        require(index >= 0) { "Current GUI cycle value is not included in values: $current" }
        return values[cycleIndex(index, values.size, reverse)]
    }

    fun <T> selectNullable(current: T?, values: List<T?>, reverse: Boolean = false): T? {
        require(values.isNotEmpty()) { "GUI cycle values must not be empty" }
        val index = values.indexOf(current)
        require(index >= 0) { "Current GUI cycle value is not included in values: $current" }
        return values[cycleIndex(index, values.size, reverse)]
    }

    private fun cycleIndex(index: Int, size: Int, reverse: Boolean): Int {
        val delta = if (reverse) -1 else 1
        return Math.floorMod(index + delta, size)
    }
}
