package com.awabi2048.ccsystem.core.gesturegui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * LOD 境界（10 ブロック）とヒステリシスの回帰試験です。
 *
 * 「10 ブロック以内にいるプレイヤーには内容を視認できる」仕様を固定します。
 * 範囲内は表示・操作権限を問わず FULL、範囲外は canView が
 * BACKGROUND_ONLY と HIDDEN だけを分けます。
 */
class GestureViewerLodPolicyTest {
    @Test
    fun `10ブロック以内はFULL`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.HIDDEN, 9.9 * 9.9, visible = true,
        )
        assertEquals(GestureViewerLod.FULL, resolved)
    }

    @Test
    fun `10ブロック以内はcanView不可でもFULL`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.HIDDEN, 9.9 * 9.9, visible = false,
        )
        assertEquals(GestureViewerLod.FULL, resolved)
    }

    @Test
    fun `10ブロック超のcanViewはBACKGROUND_ONLY`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.HIDDEN, 10.1 * 10.1, visible = true,
        )
        assertEquals(GestureViewerLod.BACKGROUND_ONLY, resolved)
    }

    @Test
    fun `10ブロック超のcanView不可はHIDDEN`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.BACKGROUND_ONLY, 12.0 * 12.0, visible = false,
        )
        assertEquals(GestureViewerLod.HIDDEN, resolved)
    }

    @Test
    fun `FULLはヒステリシス内では維持される`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.FULL, 10.5 * 10.5, visible = true,
        )
        assertEquals(GestureViewerLod.FULL, resolved)
    }

    @Test
    fun `FULLはヒステリシス超過でBACKGROUND_ONLYへ遷移する`() {
        val resolved = GestureViewerLodPolicy.resolve(
            GestureViewerLod.FULL, 11.5 * 11.5, visible = true,
        )
        assertEquals(GestureViewerLod.BACKGROUND_ONLY, resolved)
    }
}
