package com.awabi2048.ccsystem.core.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CheckboxLoreFormatterTest {
    @Test
    fun `チェック状態ごとの可変色で共通記号を描画する`() {
        assertEquals("§6◆ WorldEdit", LoreFormatter.checkboxLine("WorldEdit", true, "§6", "§8", "◆"))
        assertEquals("§8◆ WorldEdit", LoreFormatter.checkboxLine("WorldEdit", false, "§6", "§8", "◆"))
    }
}
