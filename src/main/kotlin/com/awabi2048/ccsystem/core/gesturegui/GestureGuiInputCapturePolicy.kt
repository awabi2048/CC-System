package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiSessionState

/**
 * Gesture GUIがワールド入力を所有できる状態を、セッション状態と視線から決めます。
 *
 * OPENINGだけは画面の生成途中であり、Interactionがまだ揃わないtickにワールド操作が
 * 漏れると危険なため、従来どおり参加者の入力を保持します。ACTIVEでは現在のrayが
 * 操作可能な画面へ当たっている場合に限り、クリックや補助入力を画面へ捕捉します。
 */
internal object GestureGuiInputCapturePolicy {
    fun isActive(
        state: GestureGuiSessionState,
        participating: Boolean,
        lookingAtScreen: Boolean,
    ): Boolean {
        if (!participating) return false
        return when (state) {
            GestureGuiSessionState.OPENING -> true
            GestureGuiSessionState.ACTIVE -> lookingAtScreen
            GestureGuiSessionState.CLOSING -> false
        }
    }

    /**
     * Shift+Jumpの終了操作を、視線の一時的なずれから独立して判定します。
     *
     * 追従画面はプレイヤーの移動中に一瞬だけrayの交差を失うことがあるため、
     * 通常クリックと同じ視線条件を使うと終了操作まで失われます。
     */
    fun isCloseGestureActive(
        state: GestureGuiSessionState,
        participating: Boolean,
    ): Boolean = participating && state != GestureGuiSessionState.CLOSING

    /** スニーク右クリックを、画面外を含めて完全に消費する条件です。 */
    fun isSneakSecondarySuppressed(
        state: GestureGuiSessionState,
        participating: Boolean,
        sneaking: Boolean,
        secondaryInputEnabled: Boolean,
    ): Boolean =
        isCloseGestureActive(state, participating) && sneaking && !secondaryInputEnabled
}
