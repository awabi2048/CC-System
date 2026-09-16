package com.awabi2048.ccsystem.api.worldedit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldEditCommandsTest {
    @Test
    fun `copyとcutを検出する`() {
        assertEquals(WorldEditClipboardOperation.COPY, WorldEditCommands.parseClipboardOperation("//copy"))
        assertEquals(WorldEditClipboardOperation.CUT, WorldEditCommands.parseClipboardOperation("//cut"))
        assertNull(WorldEditCommands.parseClipboardOperation("//paste"))
        assertNull(WorldEditCommands.parseClipboardOperation("//move"))
    }

    @Test
    fun `名前空間と大小文字とスラッシュ数を正規化する`() {
        assertEquals(WorldEditClipboardOperation.COPY, WorldEditCommands.parseClipboardOperation("/worldedit:copy"))
        assertEquals(WorldEditClipboardOperation.CUT, WorldEditCommands.parseClipboardOperation("//CUT"))
        assertTrue(WorldEditCommands.isPasteCommand("//PASTE -e"))
        assertTrue(WorldEditCommands.isPasteCommand("/fawe:paste"))
    }

    @Test
    fun `pasteとmoveを判定する`() {
        assertTrue(WorldEditCommands.isPasteCommand("//paste"))
        assertFalse(WorldEditCommands.isPasteCommand("//copy"))
        assertTrue(WorldEditCommands.isMoveCommand("//move 2 north"))
        assertFalse(WorldEditCommands.isMoveCommand("//copy"))
    }

    @Test
    fun `undoとredoを判定する`() {
        assertTrue(WorldEditCommands.isUndoCommand("//undo"))
        assertTrue(WorldEditCommands.isUndoCommand("//UNDO 3"))
        assertTrue(WorldEditCommands.isUndoCommand("/fawe:undo"))
        assertFalse(WorldEditCommands.isUndoCommand("//redo"))
        assertFalse(WorldEditCommands.isUndoCommand("//copy"))
        assertTrue(WorldEditCommands.isRedoCommand("//redo"))
        assertTrue(WorldEditCommands.isRedoCommand("//REDO 2"))
        assertTrue(WorldEditCommands.isRedoCommand("/worldedit:redo"))
        assertFalse(WorldEditCommands.isRedoCommand("//undo"))
        assertFalse(WorldEditCommands.isRedoCommand("//paste"))
    }

    @Test
    fun `対象外の一括操作を判定する`() {
        assertTrue(WorldEditCommands.isUnsupportedBulkCommand("//set stone"))
        assertTrue(WorldEditCommands.isUnsupportedBulkCommand("//replace stone dirt"))
        assertTrue(WorldEditCommands.isUnsupportedBulkCommand("//stack"))
        assertTrue(WorldEditCommands.isUnsupportedBulkCommand("//rotate 90"))
        assertTrue(WorldEditCommands.isUnsupportedBulkCommand("//flip"))
        assertTrue(WorldEditCommands.isUnsupportedBulkCommand("//schem load foo"))
        assertTrue(WorldEditCommands.isUnsupportedBulkCommand("//schematic save foo"))
        assertTrue(WorldEditCommands.isUnsupportedBulkCommand("/clone 0 0 0 1 1 1 2 2 2"))
        assertTrue(WorldEditCommands.isUnsupportedBulkCommand("/minecraft:fill 0 0 0 1 1 1 stone"))
        assertTrue(WorldEditCommands.isUnsupportedBulkCommand("/setblock 0 0 0 stone"))
    }

    @Test
    fun `同期対象と無関係なコマンドは対象外にしない`() {
        assertFalse(WorldEditCommands.isUnsupportedBulkCommand("//copy"))
        assertFalse(WorldEditCommands.isUnsupportedBulkCommand("//cut"))
        assertFalse(WorldEditCommands.isUnsupportedBulkCommand("//paste"))
        assertFalse(WorldEditCommands.isUnsupportedBulkCommand("//move 1 north"))
        assertFalse(WorldEditCommands.isUnsupportedBulkCommand("/tp @p 0 64 0"))
        assertFalse(WorldEditCommands.isUnsupportedBulkCommand("/data get block 0 0 0"))
        assertFalse(WorldEditCommands.isUnsupportedBulkCommand("//undo"))
        assertFalse(WorldEditCommands.isUnsupportedBulkCommand("//redo"))
    }
}
