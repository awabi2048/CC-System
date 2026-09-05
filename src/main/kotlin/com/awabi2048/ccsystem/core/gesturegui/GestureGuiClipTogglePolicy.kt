package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiSessionState

/**
 * クリップ(固定)と追従の往復トグルを一箇所へ集約します。
 *
 * クリップは [GestureGuiService.pinToCurrentPosition] で追従を止め、
 * [GestureGuiService.unpinToFollow] で追従へ戻す2状態のトグルです。
 * 固定済みへの再pin、未固定への解除は冪等に失敗させることで、
 * ボタンガードと実行側の条件が同じ判定を共有できます。
 */
internal object GestureGuiClipTogglePolicy {
    /** 追従中のACTIVEセッションに対するpin(固定開始)を許可します。 */
    fun canPin(state: GestureGuiSessionState, fixedAnchor: Any?): Boolean =
        state == GestureGuiSessionState.ACTIVE && fixedAnchor == null

    /** 固定中のACTIVEセッションに対するunpin(解除)を許可します。 */
    fun canUnpin(state: GestureGuiSessionState, fixedAnchor: Any?): Boolean =
        state == GestureGuiSessionState.ACTIVE && fixedAnchor != null

    /**
     * 解除時は固定poseを捨てて現在の目位置へposeを再計算します。
     * 固定中はyawも不変のため、yawを変えずに位置だけ追従へ復帰させます。
     */
    const val UNPIN_RECOMPUTES_POSE: Boolean = true
}
