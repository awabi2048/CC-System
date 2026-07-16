package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.GuiElementRole;
import com.awabi2048.ccsystem.api.gui.GuiLoreLine;
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec;
import com.awabi2048.ccsystem.api.gui.GuiMenuIconAction;
import com.awabi2048.ccsystem.api.gui.GuiMenuIconData;
import com.awabi2048.ccsystem.api.gui.GuiMenuIconOption;
import com.awabi2048.ccsystem.api.gui.GuiMenuIconSpec;
import com.awabi2048.ccsystem.api.gui.GuiNameSpec;
import com.awabi2048.ccsystem.api.gui.GuiNameStyle;
import com.awabi2048.ccsystem.core.gui.GuiMenuIconLoreFactory;
import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiMenuIconLoreFactoryTest {
    @Test
    void allBlocksUseCanonicalOrderAndTypes() {
        GuiLoreSpec.Blocks lore = assertInstanceOf(
            GuiLoreSpec.Blocks.class,
            GuiMenuIconLoreFactory.INSTANCE.build(specWith(
                List.of("説明"),
                List.of(new GuiMenuIconData("状態", "オン", "§a")),
                List.of(new GuiMenuIconOption("設定", true, "§a", "§7")),
                List.of("注意"),
                List.of("危険"),
                List.of(
                    new GuiMenuIconAction("無効操作", "隠す", null, false),
                    new GuiMenuIconAction("左クリック", "開く", null, true),
                    new GuiMenuIconAction("右クリック", "編集", null, true)
                )
            ))
        );

        assertEquals(6, lore.getBlocks().size());
        assertInstanceOf(GuiLoreLine.Text.class, lore.getBlocks().get(0).getLines().get(0));
        assertInstanceOf(GuiLoreLine.Data.class, lore.getBlocks().get(1).getLines().get(0));
        assertInstanceOf(GuiLoreLine.Option.class, lore.getBlocks().get(2).getLines().get(0));
        assertInstanceOf(GuiLoreLine.Warning.class, lore.getBlocks().get(3).getLines().get(0));
        assertInstanceOf(GuiLoreLine.Danger.class, lore.getBlocks().get(4).getLines().get(0));
        assertEquals(2, lore.getBlocks().get(5).getLines().size());
        GuiLoreLine.Action action = assertInstanceOf(
            GuiLoreLine.Action.class,
            lore.getBlocks().get(5).getLines().get(0)
        );
        assertEquals("左クリック", action.getOperation());
        assertEquals("開く", action.getAction());
        GuiLoreLine.Action secondAction = assertInstanceOf(
            GuiLoreLine.Action.class,
            lore.getBlocks().get(5).getLines().get(1)
        );
        assertEquals("右クリック", secondAction.getOperation());
        assertEquals("編集", secondAction.getAction());
    }

    @Test
    void emptyBlocksAreOmitted() {
        GuiLoreSpec.Blocks lore = assertInstanceOf(
            GuiLoreSpec.Blocks.class,
            GuiMenuIconLoreFactory.INSTANCE.build(specWith(
                List.of(),
                List.of(),
                List.of(new GuiMenuIconOption("設定", true, "§a", "§7")),
                List.of(),
                List.of(),
                List.of()
            ))
        );

        assertEquals(1, lore.getBlocks().size());
        assertInstanceOf(GuiLoreLine.Option.class, lore.getBlocks().get(0).getLines().get(0));
    }

    @Test
    void singleActionUsesResolvedText() {
        GuiLoreSpec.Blocks lore = assertInstanceOf(
            GuiLoreSpec.Blocks.class,
            GuiMenuIconLoreFactory.INSTANCE.build(spec(List.of(
                new GuiMenuIconAction("クリック", "開く", "クリックで開く", true)
            )))
        );

        GuiLoreLine.SingleAction action = assertInstanceOf(
            GuiLoreLine.SingleAction.class,
            lore.getBlocks().getLast().getLines().getFirst()
        );
        assertEquals("クリック", action.getOperation());
        assertEquals("開く", action.getAction());
        assertEquals("クリックで開く", action.getResolvedText());
    }

    @Test
    void multipleActionsRetainOperations() {
        GuiLoreSpec.Blocks lore = assertInstanceOf(
            GuiLoreSpec.Blocks.class,
            GuiMenuIconLoreFactory.INSTANCE.build(spec(List.of(
                new GuiMenuIconAction("左クリック", "取得", null, true),
                new GuiMenuIconAction("右クリック", "編集", null, true)
            )))
        );

        assertTrue(lore.getBlocks().getLast().getLines().stream().allMatch(GuiLoreLine.Action.class::isInstance));
    }

    @Test
    void singleActionRequiresResolvedText() {
        assertThrows(IllegalArgumentException.class, () ->
            GuiMenuIconLoreFactory.INSTANCE.build(spec(List.of(
                new GuiMenuIconAction("クリック", "開く", null, true)
            )))
        );
    }

    private GuiMenuIconSpec spec(List<GuiMenuIconAction> actions) {
        return specWith(
            List.of("通知を切り替えます"),
            List.of(new GuiMenuIconData("現在", "オン", "§a")),
            List.of(new GuiMenuIconOption("オン", true, "§a", "§7")),
            List.of(),
            List.of(),
            actions
        );
    }

    private GuiMenuIconSpec specWith(
        List<String> description,
        List<GuiMenuIconData> data,
        List<GuiMenuIconOption> options,
        List<String> warnings,
        List<String> dangers,
        List<GuiMenuIconAction> actions
    ) {
        return new GuiMenuIconSpec(
            Material.BELL,
            new GuiNameSpec.Text("通知", GuiNameStyle.SUCCESS),
            GuiElementRole.CONTENT,
            1,
            description,
            data,
            options,
            warnings,
            dangers,
            actions,
            true
        );
    }
}
