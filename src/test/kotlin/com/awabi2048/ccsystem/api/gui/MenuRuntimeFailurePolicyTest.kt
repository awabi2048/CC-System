package com.awabi2048.ccsystem.api.gui

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@Suppress("DEPRECATION")
class MenuRuntimeFailurePolicyTest {
    @Test
    fun `virtual machine errors and thread death are never converted to diagnostics`() {
        assertThrows(OutOfMemoryError::class.java) {
            OutOfMemoryError("test").rethrowIfUnrecoverableMenuRuntimeFailure()
        }
        assertThrows(ThreadDeath::class.java) {
            ThreadDeath().rethrowIfUnrecoverableMenuRuntimeFailure()
        }
    }

    @Test
    fun `linkage errors remain diagnosable integration failures`() {
        assertDoesNotThrow {
            LinkageError("missing optional integration").rethrowIfUnrecoverableMenuRuntimeFailure()
        }
    }
}
