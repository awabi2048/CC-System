package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.GuiLoreFrame;
import com.awabi2048.ccsystem.api.gui.GuiLoreBlock;
import com.awabi2048.ccsystem.api.gui.GuiLoreLine;
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec;
import com.awabi2048.ccsystem.api.gui.GuiStatusTone;
import com.awabi2048.ccsystem.core.gui.LoreServiceImpl;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.kyori.adventure.text.format.NamedTextColor;

class LoreServiceTest {
    private static List<String> plain(List<Component> lines) {
        return lines.stream()
            .map(PlainTextComponentSerializer.plainText()::serialize)
            .collect(Collectors.toList());
    }

    @Test
    void richLoreCompressesRedundantSpacersWithoutChangingContentOrder() {
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
        assertEquals(5, lines.size());
        assertEquals("説明", lines.get(0));
        assertEquals("", lines.get(1));
        assertTrue(lines.get(2).endsWith("現在値 3"));
        assertEquals(30, lines.get(3).codePointCount(0, lines.get(3).length()));
        assertTrue(lines.get(4).endsWith("左クリック 変更"));
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
    void richLoreWithBothFrameWrapsLinesAndPreservesBlanksAsSpacers() {
        List<Component> rendered = new LoreServiceImpl().render(
            new GuiLoreSpec.Rich(
                List.of(new GuiLoreLine.Text("データ1"), GuiLoreLine.Spacer.INSTANCE, new GuiLoreLine.Text("データ2")),
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
    void richLoreKeepsExplicitSeparatorAsTheOnlyIntermediateRule() {
        List<String> lines = plain(new LoreServiceImpl().render(
            new GuiLoreSpec.Rich(
                List.of(
                    new GuiLoreLine.Text("上段"),
                    GuiLoreLine.Separator.INSTANCE,
                    GuiLoreLine.Separator.INSTANCE,
                    GuiLoreLine.Spacer.INSTANCE,
                    GuiLoreLine.Spacer.INSTANCE,
                    new GuiLoreLine.Text("下段")
                ),
                GuiLoreFrame.BOTH
            )
        ));

        assertEquals(5, lines.size());
        assertEquals(30, lines.get(0).codePointCount(0, lines.get(0).length()));
        assertEquals("上段", lines.get(1));
        assertEquals(30, lines.get(2).codePointCount(0, lines.get(2).length()));
        assertEquals("下段", lines.get(3));
        assertEquals(30, lines.get(4).codePointCount(0, lines.get(4).length()));
    }

    @Test
    void explicitBoundarySeparatorsAreNotDuplicatedByStandardFrame() {
        List<String> lines = plain(new LoreServiceImpl().render(
            new GuiLoreSpec.Rich(
                List.of(GuiLoreLine.Separator.INSTANCE, new GuiLoreLine.Text("説明"), GuiLoreLine.Separator.INSTANCE),
                GuiLoreFrame.BOTH
            )
        ));

        assertEquals(3, lines.size());
        assertEquals("説明", lines.get(1));
    }

    @Test
    void blocksUseOuterSeparatorsAndSingleSpacerBetweenBlocks() {
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
        assertEquals("", lines.get(2));
        assertTrue(lines.get(3).endsWith("現在値 3"));
        assertEquals("", lines.get(4));
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
    }

    @Test
    void componentLorePreservesAdventureEvents() {
        Component source = Component.text("対象").hoverEvent(Component.text("詳細"));
        List<Component> rendered = new LoreServiceImpl().render(
            new GuiLoreSpec.Rich(List.of(new GuiLoreLine.Component(source)), GuiLoreFrame.NONE)
        );

        assertEquals(1, rendered.size());
        assertNotNull(rendered.getFirst().hoverEvent());
    }

    @Test
    void progressPathRendersAlignedMarkersAndLabelsFromSemanticState() {
        List<Component> rendered = new LoreServiceImpl().render(
            new GuiLoreSpec.Rich(
                List.of(new GuiLoreLine.ProgressPath(
                    List.of("新規", "訪問者", "開拓者", "冒険者", "達成者"),
                    2
                )),
                GuiLoreFrame.NONE
            )
        );

        assertEquals(List.of(
            "  ●━━━●━━━◆───○───○",
            " 新規   訪問者  開拓者  冒険者  達成者"
        ), plain(rendered));
        assertEquals("minecraft:uniform", rendered.get(0).font().asString());
        assertEquals("minecraft:uniform", rendered.get(1).font().asString());
    }

    @Test
    void statusDataUsesOnlyTheTaskMarkerForCompletionState() {
        List<Component> rendered = new LoreServiceImpl().render(
            new GuiLoreSpec.Rich(
                List.of(
                    new GuiLoreLine.StatusData("採掘", "12 / 20", "§c", GuiStatusTone.INCOMPLETE),
                    new GuiLoreLine.StatusComponentData(
                        Component.text("ゾンビ"),
                        Component.text("3 / 3", NamedTextColor.GREEN),
                        GuiStatusTone.COMPLETE
                    )
                ),
                GuiLoreFrame.NONE
            )
        );

        assertEquals(List.of(
            "❙ 採掘 12 / 20",
            "❙ ゾンビ 3 / 3"
        ), plain(rendered));
        assertEquals(NamedTextColor.RED, rendered.get(0).color());
        assertEquals(NamedTextColor.GREEN, rendered.get(1).color());
        assertEquals(NamedTextColor.GRAY, rendered.get(0).children().getFirst().color());
        assertEquals(NamedTextColor.GRAY, rendered.get(1).children().getFirst().color());
    }
}
