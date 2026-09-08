package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiBounds

/**
 * 切り抜き状態です。空の交差を無制限と区別し、入れ子で表示・操作が復活するのを防ぎます。
 * 矩形は画面中央原点の座標で保持し、Layout Engine と compiler が同じ演算を使います。
 */
sealed interface GestureGuiClip {
    data object Unbounded : GestureGuiClip
    data object Empty : GestureGuiClip
    data class Region(val bounds: GestureGuiBounds) : GestureGuiClip

    fun intersect(bounds: GestureGuiBounds): GestureGuiClip = when (this) {
        Unbounded -> Region(bounds)
        Empty -> Empty
        is Region -> {
            val left = maxOf(this.bounds.minX, bounds.minX)
            val bottom = maxOf(this.bounds.minY, bounds.minY)
            val right = minOf(this.bounds.maxX, bounds.maxX)
            val top = minOf(this.bounds.maxY, bounds.maxY)
            if (left >= right || bottom >= top) Empty else Region(GestureGuiBounds(left, bottom, right, top))
        }
    }

    fun clip(bounds: GestureGuiBounds): GestureGuiBounds? = (intersect(bounds) as? Region)?.bounds

    fun contains(bounds: GestureGuiBounds): Boolean = when (this) {
        Unbounded -> true
        Empty -> false
        is Region -> bounds.minX >= this.bounds.minX && bounds.maxX <= this.bounds.maxX &&
            bounds.minY >= this.bounds.minY && bounds.maxY <= this.bounds.maxY
    }
}
