package com.awabi2048.ccsystem.core.gesturegui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * LOD 境界（15 ブロック）とヒステリシスの回帰試験です。
 *
 * 境界直上での FULL/BACKGROUND_ONLY 往復を防ぐことが目的であり、
 * 負荷試験の代わりに遷移条件をここで固定します。
 */
class GestureViewerLodPolicyTest {
    @Test
    fun `15ブロック未満はFULL`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.HIDDEN, 14.9 * 14.9, operable = true, visible = true,
        )
        assertEquals(GestureViewerLod.FULL, resolved)
    }

    @Test
    fun `15ブロック以上はBACKGROUND_ONLY`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.HIDDEN, 15.1 * 15.1, operable = true, visible = true,
        )
        assertEquals(GestureViewerLod.BACKGROUND_ONLY, resolved)
    }

    @Test
    fun `FULLはヒステリシス内では維持される`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.FULL, 15.5 * 15.5, operable = true, visible = true,
        )
        assertEquals(GestureViewerLod.FULL, resolved)
    }

    @Test
    fun `FULLはヒステリシス超過でBACKGROUND_ONLYへ遷移する`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.FULL, 16.5 * 16.5, operable = true, visible = true,
        )
        assertEquals(GestureViewerLod.BACKGROUND_ONLY, resolved)
    }

    @Test
    fun `非表示はHIDDEN`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.FULL, 1.0, operable = true, visible = false,
        )
        assertEquals(GestureViewerLod.HIDDEN, resolved)
    }

    @Test
    fun `操作不可はBACKGROUND_ONLY`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.FULL, 1.0, operable = false, visible = true,
        )
        assertEquals(GestureViewerLod.BACKGROUND_ONLY, resolved)
    }
}
