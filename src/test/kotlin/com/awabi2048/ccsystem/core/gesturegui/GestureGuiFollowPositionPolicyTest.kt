package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVector3
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GestureGuiFollowPositionPolicyTest {
    @Test
    fun `vertical movement also requests a follow pose update`() {
        assertTrue(
            GestureGuiFollowPositionPolicy.hasMoved(
                current = GestureGuiVector3(10.0, 65.5, 20.0),
                anchorX = 10.0,
                anchorY = 64.0,
                anchorZ = 20.0,
            ),
        )
    }

    @Test
    fun `following is unconditional - movement on every single axis requests an update`() {
        // 追従モードでは軸による追従の除外を行いません。X/Y/Zのどれか1軸だけの
        // 移動でも必ず追従pose更新を要求します(仕様削除の固定テスト)。
        listOf(
            GestureGuiVector3(11.0, 64.0, 20.0) to "X only",
            GestureGuiVector3(10.0, 64.5, 20.0) to "Y only",
            GestureGuiVector3(10.0, 64.0, 21.0) to "Z only",
        ).forEach { (current, label) ->
            assertTrue(
                GestureGuiFollowPositionPolicy.hasMoved(
                    current = current,
                    anchorX = 10.0,
                    anchorY = 64.0,
                    anchorZ = 20.0,
                ),
                "movement on $label must request a follow pose update",
            )
        }
    }

    @Test
    fun `unchanged eye position does not request a follow pose update`() {
        assertFalse(
            GestureGuiFollowPositionPolicy.hasMoved(
                current = GestureGuiVector3(10.0, 64.0, 20.0),
                anchorX = 10.0,
                anchorY = 64.0,
                anchorZ = 20.0,
            ),
        )
    }
}
