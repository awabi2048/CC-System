package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.core.gui.LoreFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoreFormatterTest {

    @Test
    void separatorAlwaysUsesThirtyUnits() {
        String shortSeparator = LoreFormatter.INSTANCE.separator(List.of("short"));
        String longSeparator = LoreFormatter.INSTANCE.separator(List.of("a very long lore line"));
        String visibleSeparator = shortSeparator.substring(4);

        assertEquals(shortSeparator, longSeparator);
        assertEquals(30, visibleSeparator.codePointCount(0, visibleSeparator.length()));
    }

    @Test
    void warningLineUsesReferenceMarkerWithoutUnderline() {
        String line = LoreFormatter.INSTANCE.warningLine("テスト警告");
        assertTrue(line.startsWith("§c※"), "warningLine should start with a red reference marker");
        assertFalse(line.contains("§n"), "warningLine should not underline only part of the warning");
        assertEquals("§c※ テスト警告", line);
    }

    @Test
    void dangerLineUsesDarkRedBoldWithAsterisk() {
        String line = LoreFormatter.INSTANCE.dangerLine("テスト危険");
        assertEquals("§4§l※ テスト危険", line);
    }
}
