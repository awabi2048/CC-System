package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3

/**
 * プレイヤー追従画面の再配置条件です。
 *
 * 追従モードでは軸による追従の除外を行いません。XZだけを比較すると、ジャンプや
 * 段差移動のようなY方向だけの移動で画面poseが古い高さに残ります。その状態では
 * 描画とray判定の基準が分岐し、クリック用catcherだけがプレイヤーへ移動して入力を
 * 失うため、3軸すべてを同じ条件で比較し、どの軸の移動でも必ず追従します。
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
