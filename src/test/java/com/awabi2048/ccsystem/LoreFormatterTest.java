package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.core.gui.LoreFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void warningLineUsesReferenceMarkerWithUnderline() {
        String line = LoreFormatter.INSTANCE.warningLine("テスト警告");
        assertTrue(line.startsWith("§c§n"), "warningLine should start with red underline");
        assertEquals("§c§n※ テスト警告", line);
    }

    @Test
    void dangerLineUsesDarkRedBoldWithAsterisk() {
        String line = LoreFormatter.INSTANCE.dangerLine("テスト危険");
        assertEquals("§4§l※ テスト危険", line);
    }
}
