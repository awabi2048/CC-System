package com.awabi2048.ccsystem.api.worldedit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WorldEditMoveCommandParserTest {
    @Test
    fun `引数なしは1回forwardとして扱う`() {
        assertEquals(WorldEditMoveCommand(1, "forward"), WorldEditMoveCommandParser.parse("//move"))
    }

    @Test
    fun `回数と方向を取り出す`() {
        assertEquals(WorldEditMoveCommand(2, "north"), WorldEditMoveCommandParser.parse("//move 2 north"))
        assertEquals(WorldEditMoveCommand(1, "up"), WorldEditMoveCommandParser.parse("//move up"))
    }

    @Test
    fun `明示ベクトルを取り出す`() {
        assertEquals(WorldEditMoveCommand(1, "1,0,-2"), WorldEditMoveCommandParser.parse("//move 1,0,-2"))
        assertEquals(WorldEditMoveCommand(3, "1,0,-2"), WorldEditMoveCommandParser.parse("//move 3 1,0,-2"))
    }

    @Test
    fun `マスクの値付きスイッチを消費する`() {
        assertEquals(
            WorldEditMoveCommand(2, "north"),
            WorldEditMoveCommandParser.parse("//move -m stone 2 north"),
        )
    }

    @Test
    fun `真偽値スイッチを無視する`() {
        assertEquals(
            WorldEditMoveCommand(1, "north"),
            WorldEditMoveCommandParser.parse("//move -e north"),
        )
    }

    @Test
    fun `move以外と0回以下は棄却する`() {
        assertNull(WorldEditMoveCommandParser.parse("//copy"))
        assertNull(WorldEditMoveCommandParser.parse("/tp @p 0 64 0"))
        assertNull(WorldEditMoveCommandParser.parse("//move 0 north"))
    }
}
