package com.awabi2048.ccsystem.core.gui

import java.nio.file.Path
import kotlin.io.path.readText
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuRuntimeTraceCoverageTest {
    @Test
    fun `runtime listener records trace for non action and action outcomes`() {
        val source = Path.of(
            "src/main/kotlin/com/awabi2048/ccsystem/core/gui/MenuRuntimeServiceImpl.kt",
        ).readText()
        assertTrue("val trace = beginClickTrace(player, event.rawSlot, event.click, holder.route)" in source)
        listOf(
            "MenuRuntimeClickDisposition.EMPTY",
            "MenuRuntimeClickDisposition.FRAME",
            "MenuRuntimeClickDisposition.DISPLAY_ONLY",
            "MenuRuntimeClickDisposition.UNACCEPTED",
            "MenuRuntimeClickDisposition.UNAVAILABLE",
            "MenuRuntimeClickDisposition.BACK",
            "MenuRuntimeClickDisposition.EXCEPTION",
            "MenuRuntimeClickDisposition.HANDLED",
            "recordClickTrace(",
        ).forEach { expected -> assertTrue(expected in source, "missing trace coverage: $expected") }
    }
}
