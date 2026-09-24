package com.awabi2048.ccsystem.core.gesturegui

/** 子画面同士の法線方向間隔を、配置とモーダル背景で共有します。 */
internal object GestureGuiChildDepth {
    /** 親の表示面から最初の子画面までのZ方向の間隔です。 */
    const val PARENT_CHILD_GAP: Double = 0.125

    /** 子画面を一段積むたびに追加するZ方向の間隔です。 */
    const val STACK_GAP: Double = 0.125

    fun childOffset(childIndex: Int): Double {
        require(childIndex >= 0) { "子画面のスタック位置は0以上である必要があります。" }
        return PARENT_CHILD_GAP + childIndex * STACK_GAP
    }

    fun modalOverlayOffset(childIndex: Int): Double {
        require(childIndex >= 0) { "モーダル背景のスタック位置は0以上である必要があります。" }
        return childIndex * STACK_GAP
    }
}
