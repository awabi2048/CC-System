package com.awabi2048.ccsystem

import java.nio.file.Path
import kotlin.io.path.readText
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class DialogSoundContractTest {
    @Test
    fun `native dialog actions do not add plugin click sounds`() {
        val source = Path.of(
            "src/main/kotlin/com/awabi2048/ccsystem/core/gui/MenuDialogServiceImpl.kt"
        ).readText()

        assertFalse("sounds.play(" in source)
        assertFalse("MenuSoundPolicyResolver.resolve(" in source)
    }
}
