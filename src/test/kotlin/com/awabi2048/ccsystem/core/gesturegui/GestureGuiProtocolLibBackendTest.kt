package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.core.gesturegui.GestureGuiProtocolLibBackend.Companion.ID_BACKGROUND_ALIAS_CHECK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 仮想描画の packing・flags 変換の回帰試験です。
 *
 * 定数は実サーバー（Chiyogami 26.1.2）の NMS 実体で確定しており、
 * ここではその対応が崩れていないことを固定します。
 */
class GestureGuiProtocolLibBackendTest {
    @Test
    fun `brightness packはblockとskyを合成する`() {
        assertEquals(255, GestureGuiProtocolLibBackend.packBrightness(15, 15))
        assertEquals(0, GestureGuiProtocolLibBackend.packBrightness(0, 0))
        assertEquals(0x97, GestureGuiProtocolLibBackend.packBrightness(7, 9))
    }

    @Test
    fun `text flagsはseeThroughとalignmentを合成する`() {
        assertEquals(0.toByte(), GestureGuiProtocolLibBackend.textFlags(false, 0))
        assertEquals(2.toByte(), GestureGuiProtocolLibBackend.textFlags(true, 0))
        assertEquals(8.toByte(), GestureGuiProtocolLibBackend.textFlags(false, 8))
        assertEquals(18.toByte(), GestureGuiProtocolLibBackend.textFlags(true, 16))
    }

    @Test
    fun `yawはpacket用byteへ変換される`() {
        assertEquals(0.toByte(), GestureGuiProtocolLibBackend.toPackedByte(0f))
        assertEquals(64.toByte(), GestureGuiProtocolLibBackend.toPackedByte(90f))
        assertEquals((-128).toByte(), GestureGuiProtocolLibBackend.toPackedByte(180f))
    }

    @Test
    fun `metadata indexはNMS確定値と一致する`() {        // Display 8-22・種別固有 23-27 の先頭がずれていないことだけ固定します。
        assertEquals(11, GestureGuiProtocolLibBackend.ID_TRANSLATION)
        assertEquals(12, GestureGuiProtocolLibBackend.ID_SCALE)
        assertEquals(15, GestureGuiProtocolLibBackend.ID_BILLBOARD)
        assertEquals(16, GestureGuiProtocolLibBackend.ID_BRIGHTNESS)
        assertEquals(22, GestureGuiProtocolLibBackend.ID_GLOW_COLOR)
        assertEquals(23, GestureGuiProtocolLibBackend.ID_BLOCK_STATE)
        assertEquals(23, GestureGuiProtocolLibBackend.ID_ITEM_STACK)
        assertEquals(24, GestureGuiProtocolLibBackend.ID_ITEM_DISPLAY_TYPE)
        assertEquals(23, GestureGuiProtocolLibBackend.ID_TEXT)
        assertEquals(24, GestureGuiProtocolLibBackend.ID_LINE_WIDTH)
        assertEquals(27, GestureGuiProtocolLibBackend.ID_TEXT_FLAGS)
        // 背景色 index も TextDisplay 固有域（25）であることを固定します。
        assertEquals(25, ID_BACKGROUND_ALIAS_CHECK)
        // 補間 index は Display 域（8-10）であることを固定します。
        assertEquals(8, GestureGuiProtocolLibBackend.ID_TRANSFORM_START)
        assertEquals(9, GestureGuiProtocolLibBackend.ID_TRANSFORM_DURATION)
        assertEquals(10, GestureGuiProtocolLibBackend.ID_POSROT_DURATION)
    }

    @Test
    fun `補間期間は旧Bukkit経路と同値である`() {
        assertEquals(3, GestureGuiProtocolLibBackend.TRANSFORM_INTERP_TICKS)
        assertEquals(1, GestureGuiProtocolLibBackend.POSROT_INTERP_TICKS)
    }

    @Test
    fun `相対移動は微差分を量子化する`() {
        assertEquals(4096, GestureGuiProtocolLibBackend.relativeShort(1.0))
        assertEquals(-2048, GestureGuiProtocolLibBackend.relativeShort(-0.5))
        assertEquals(0, GestureGuiProtocolLibBackend.relativeShort(0.0))
    }

    @Test
    fun `相対移動の範囲外は再同期のためnullである`() {
        assertEquals(null, GestureGuiProtocolLibBackend.relativeShort(8.0))
        assertEquals(null, GestureGuiProtocolLibBackend.relativeShort(-9.0))
    }

    @Test
    fun `drift超過だけ再同期する`() {
        assertFalse(GestureGuiProtocolLibBackend.exceedsDrift(1.0, 1.003))
        assertTrue(GestureGuiProtocolLibBackend.exceedsDrift(1.0, 1.005))
    }
}
