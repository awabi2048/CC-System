package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.GuiCycle;
import com.awabi2048.ccsystem.api.gui.GuiCycleDirection;
import java.util.List;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuiCycleTest {

    @Test
    void mapsSupportedClicksToSemanticDirections() {
        assertEquals(GuiCycleDirection.NEXT, GuiCycle.INSTANCE.direction(ClickType.LEFT));
        assertEquals(GuiCycleDirection.PREVIOUS, GuiCycle.INSTANCE.direction(ClickType.RIGHT));
        assertNull(GuiCycle.INSTANCE.direction(ClickType.SHIFT_LEFT));
        assertNull(GuiCycle.INSTANCE.direction(ClickType.SHIFT_RIGHT));
    }

    @Test
    void cyclesInBothDirectionsIncludingBoundaries() {
        List<String> values = List.of("a", "b", "c");

        assertEquals("b", GuiCycle.INSTANCE.select("a", values, GuiCycleDirection.NEXT));
        assertEquals("a", GuiCycle.INSTANCE.select("b", values, GuiCycleDirection.PREVIOUS));
        assertEquals("a", GuiCycle.INSTANCE.select("c", values, GuiCycleDirection.NEXT));
        assertEquals("c", GuiCycle.INSTANCE.select("a", values, GuiCycleDirection.PREVIOUS));
    }
}
