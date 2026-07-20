package com.awabi2048.ccsystem.api.gui

import org.bukkit.event.inventory.ClickType

enum class GuiCycleDirection {
    NEXT,
    PREVIOUS
}

/**
 * GUI上の候補リストを循環させるための共通規則。
 *
 * 左クリックは次、右クリックは前としてこのクラス内で解釈する。
 * 呼び出し側はクリック種別または意味方向を渡し、Booleanで方向を表現しない。
 * 現在値が候補に存在しない場合はデータ不整合として例外にし、
 * 暗黙のフォールバックを行わない。
 */
object GuiCycle {
    fun direction(click: ClickType): GuiCycleDirection? = when (click) {
        ClickType.LEFT -> GuiCycleDirection.NEXT
        ClickType.RIGHT -> GuiCycleDirection.PREVIOUS
        else -> null
    }

    fun <T> select(current: T, values: Array<T>, direction: GuiCycleDirection): T {
        return select(current, values.asList(), direction)
    }

    fun <T> select(current: T, values: List<T>, direction: GuiCycleDirection): T {
        require(values.isNotEmpty()) { "GUI cycle values must not be empty" }
        val index = values.indexOf(current)
        require(index >= 0) { "Current GUI cycle value is not included in values: $current" }
        return values[cycleIndex(index, values.size, direction)]
    }

    fun <T> selectNullable(current: T?, values: List<T?>, direction: GuiCycleDirection): T? {
        require(values.isNotEmpty()) { "GUI cycle values must not be empty" }
        val index = values.indexOf(current)
        require(index >= 0) { "Current GUI cycle value is not included in values: $current" }
        return values[cycleIndex(index, values.size, direction)]
    }

    private fun cycleIndex(index: Int, size: Int, direction: GuiCycleDirection): Int {
        val delta = when (direction) {
            GuiCycleDirection.NEXT -> 1
            GuiCycleDirection.PREVIOUS -> -1
        }
        return Math.floorMod(index + delta, size)
    }
}
