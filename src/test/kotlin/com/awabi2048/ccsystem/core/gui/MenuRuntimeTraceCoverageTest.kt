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
            "MenuRuntimeInteractionKind.CLICK_BRANCHES",
            "is MenuInteraction.ClickBranches ->",
            "recordClickTrace(",
        ).forEach { expected -> assertTrue(expected in source, "missing trace coverage: $expected") }
    }

    @Test
    fun `runtime listener applies every update variant and retains its application result`() {
        val source = Path.of(
            "src/main/kotlin/com/awabi2048/ccsystem/core/gui/MenuRuntimeServiceImpl.kt",
        ).readText()
        listOf(
            "): MenuRuntimeUpdateApplication {",
            "MenuUpdate.None",
            "MenuUpdate.Refresh",
            "MenuUpdate.Resume",
            "MenuUpdate.Close",
            "MenuUpdate.Cancel",
            "MenuUpdate.Back",
            "is MenuUpdate.Replace",
            "is MenuUpdate.Navigate",
            "MenuRuntimeUpdateFailureReason.CONTRACT_INVALID",
            "MenuRuntimeUpdateFailureReason.NO_HISTORY",
            "MenuRuntimeUpdateFailureReason.STALE_REVISION",
            "MenuRuntimeUpdateFailureReason.EXCEPTION",
            "operationResult = outcome.operationResult",
            "application = application",
            "observedRoute = after?.route",
            "afterRevision = after?.revision",
        ).forEach { expected -> assertTrue(expected in source, "missing update application coverage: $expected") }
    }
}
