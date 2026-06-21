package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.GuiLoreFrame;
import com.awabi2048.ccsystem.api.gui.GuiLoreBlock;
import com.awabi2048.ccsystem.api.gui.GuiLoreLine;
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec;
import com.awabi2048.ccsystem.core.gui.LoreServiceImpl;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoreServiceTest {
    private static List<String> plain(List<Component> lines) {
        return lines.stream()
            .map(PlainTextComponentSerializer.plainText()::serialize)
            .collect(Collectors.toList());
    }

    @Test
    void richLorePreservesModuleSpecifiedLayoutExactly() {
        List<Component> rendered = new LoreServiceImpl().render(
            new GuiLoreSpec.Rich(
                List.of(
                    new GuiLoreLine.Text("説明"),
                    GuiLoreLine.Spacer.INSTANCE,
                    GuiLoreLine.Spacer.INSTANCE,
                    new GuiLoreLine.Data("現在値", 3, "§e"),
                    GuiLoreLine.Separator.INSTANCE,
                    new GuiLoreLine.Action("左クリック", "変更")
                ),
                GuiLoreFrame.NONE
            )
        );

        List<String> lines = plain(rendered);
        assertEquals(6, lines.size());
        assertEquals("説明", lines.get(0));
        assertEquals("", lines.get(1));
        assertEquals("", lines.get(2));
        assertTrue(lines.get(3).endsWith("現在値 3"));
        assertEquals(30, lines.get(4).codePointCount(0, lines.get(4).length()));
        assertTrue(lines.get(5).endsWith("左クリック 変更"));
    }

    @Test
    void frameOnlyAddsRequestedOuterSeparators() {
        List<String> lines = plain(new LoreServiceImpl().render(
            new GuiLoreSpec.Rich(
                List.of(new GuiLoreLine.Text("本文")),
                GuiLoreFrame.BOTH
            )
        ));

        assertEquals(3, lines.size());
        assertEquals(30, lines.get(0).codePointCount(0, lines.get(0).length()));
        assertEquals("本文", lines.get(1));
        assertEquals(30, lines.get(2).codePointCount(0, lines.get(2).length()));
    }

    @Test
    void autoLoreWithBothFrameWrapsLinesAndPreservesBlanksAsSpacers() {
        List<Component> rendered = new LoreServiceImpl().render(
            new GuiLoreSpec.Auto(
                List.of("データ1", "", "データ2"),
                GuiLoreFrame.BOTH
            )
        );

        List<String> lines = plain(rendered);
        // separator + データ1 + spacer + データ2 + separator
        assertEquals(5, lines.size());
        assertEquals(30, lines.get(0).codePointCount(0, lines.get(0).length()));
        assertEquals("データ1", lines.get(1));
        assertEquals("", lines.get(2));
        assertEquals("データ2", lines.get(3));
        assertEquals(30, lines.get(4).codePointCount(0, lines.get(4).length()));
    }

    @Test
    void blocksInsertExactlyOneSeparatorBetweenBlocks() {
        List<String> lines = plain(new LoreServiceImpl().render(
            new GuiLoreSpec.Blocks(
                List.of(
                    new GuiLoreBlock(List.of(new GuiLoreLine.Text("説明"))),
                    new GuiLoreBlock(List.of(new GuiLoreLine.Data("現在値", 3, "§e"))),
                    new GuiLoreBlock(List.of(new GuiLoreLine.Action("左クリック", "変更")))
                )
            )
        ));

        assertEquals(7, lines.size());
        assertEquals(30, lines.get(0).codePointCount(0, lines.get(0).length()));
        assertEquals("説明", lines.get(1));
        assertEquals(30, lines.get(2).codePointCount(0, lines.get(2).length()));
        assertTrue(lines.get(3).endsWith("現在値 3"));
        assertEquals(30, lines.get(4).codePointCount(0, lines.get(4).length()));
        assertTrue(lines.get(5).endsWith("左クリック 変更"));
        assertEquals(30, lines.get(6).codePointCount(0, lines.get(6).length()));
    }

    @Test
    void blockRejectsEmptySpacerOnlyAndExplicitSeparatorContent() {
        assertThrows(IllegalArgumentException.class, () -> new GuiLoreBlock(List.of()));
        assertThrows(
            IllegalArgumentException.class,
            () -> new GuiLoreBlock(List.of(GuiLoreLine.Spacer.INSTANCE))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new GuiLoreBlock(List.of(GuiLoreLine.Separator.INSTANCE))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new GuiLoreBlock(List.of(new GuiLoreLine.Raw("§8§m----------------")))
        );
    }
}
