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
        return new GuiMenuIconSpec(
            Material.BELL,
            new GuiNameSpec.Text("通知", GuiNameStyle.SUCCESS),
            GuiElementRole.CONTENT,
            1,
            List.of("通知を切り替えます"),
            List.of(new GuiMenuIconData("現在", "オン", "§a")),
            List.of(new GuiMenuIconOption("オン", true, "§a", "§7")),
            List.of(),
            List.of(),
            actions,
            true
        );
    }
}
