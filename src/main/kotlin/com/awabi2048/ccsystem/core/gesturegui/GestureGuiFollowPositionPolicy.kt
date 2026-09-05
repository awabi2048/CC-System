package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3

/**
 * プレイヤー追従画面の再配置条件です。
 *
 * XZだけを比較すると、ジャンプや段差移動のようなY方向だけの移動で画面poseが
 * 古い高さに残ります。その状態では描画とray判定の基準が分岐し、クリック用catcher
 * だけがプレイヤーへ移動して入力を失うため、3軸すべてを同じ条件で比較します。
 */
internal object GestureGuiFollowPositionPolicy {
    fun hasMoved(
        current: GestureGuiVector3,
        anchorX: Double,
        anchorY: Double,
        anchorZ: Double,
    ): Boolean =
        current.x != anchorX || current.y != anchorY || current.z != anchorZ
}
