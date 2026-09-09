package com.awabi2048.ccsystem.core.gesturegui

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 通常同期と hover 経路のキー所有権の回帰試験です。
 *
 * hover キーが通常画面の過剰分掃除に巻き込まれると、生成直後に削除され
 * 説明表示が欠落するため、所有権の境界をここで固定します。
 */
class GestureGuiVirtualKeyOwnershipTest {
    @Test
    fun `hoverキーはhover経路の専有である`() {
        assertTrue(GestureGuiVirtualScreens.isHoverManagedKey("s:upper/hover"))
        assertTrue(GestureGuiVirtualScreens.isHoverManagedKey("c:child/hoverBlock"))
    }

    @Test
    fun `通常キーと遮蔽面は通常同期の管理である`() {
        assertFalse(GestureGuiVirtualScreens.isHoverManagedKey("s:upper/bg"))
        assertFalse(GestureGuiVirtualScreens.isHoverManagedKey("s:upper/v/node1"))
        assertFalse(GestureGuiVirtualScreens.isHoverManagedKey("s:upper/v/node1/outline/0"))
        assertFalse(GestureGuiVirtualScreens.isHoverManagedKey("c:child/overlay"))
        assertFalse(GestureGuiVirtualScreens.isHoverManagedKey("s:upper/dummy"))
    }
}
