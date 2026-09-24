package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose

/** 子画面同士の法線方向間隔を、配置とモーダル背景で共有します。 */
internal object GestureGuiChildDepth {
    /** 親の表示面から最初の子画面までのZ方向の間隔です。 */
    const val PARENT_CHILD_GAP: Double = 0.125

    /** 子画面を一段積むたびに追加するZ方向の間隔です。 */
    const val STACK_GAP: Double = 0.125

    /** 親から子までの距離です。親が子画面なら、その既存のスタック位置を差し引きます。 */
    fun childOffset(childIndex: Int, parentChildIndex: Int): Double {
        require(childIndex >= 0 && parentChildIndex in -1 until childIndex) {
            "親画面は子画面より背面のスタック位置にある必要があります。"
        }
        return (childIndex - parentChildIndex) * STACK_GAP
    }

    /** 子画面に属する遮蔽面を、子の背景よりこの距離だけ背面へ置きます。 */
    const val OVERLAY_RECESS: Double = 0.005

    /** 親画面のposeを参照せず、子画面だけから遮蔽面を配置します。 */
    fun overlayPose(childPose: GestureGuiScreenPose, width: Double, height: Double): GestureGuiScreenPose =
        childPose.copy(
            center = childPose.center + childPose.normal * OVERLAY_RECESS,
            width = width,
            height = height,
        )
}
