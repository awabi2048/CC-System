package com.awabi2048.ccsystem.core.gesturegui

/** 親画面・子画面で共有する開閉アニメーションのtick境界です。 */
internal object GestureGuiAnimationTimeline {
    const val TRANSITION_TICKS = 3
    const val POINT_HOLD_TICKS = 3

    const val OPEN_TO_POINT_DELAY = 1L
    const val OPEN_TO_LINE_DELAY = OPEN_TO_POINT_DELAY + TRANSITION_TICKS + POINT_HOLD_TICKS
    const val OPEN_TO_FULL_DELAY = OPEN_TO_LINE_DELAY + TRANSITION_TICKS
    const val OPEN_COMPLETE_DELAY = OPEN_TO_FULL_DELAY + TRANSITION_TICKS

    const val CLOSE_TO_POINT_DELAY = TRANSITION_TICKS.toLong()
    const val CLOSE_TO_ZERO_DELAY = CLOSE_TO_POINT_DELAY + TRANSITION_TICKS + POINT_HOLD_TICKS
    const val CLOSE_COMPLETE_DELAY = CLOSE_TO_ZERO_DELAY + TRANSITION_TICKS
}
