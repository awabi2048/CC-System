package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.core.gui.GuiLayoutServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiCenteredSevenColumnSlotsTest {
    private final GuiLayoutServiceImpl service = new GuiLayoutServiceImpl();

    @Test
    void centersOneThroughSevenItemsInTheSharedSevenColumnRegion() {
        assertEquals(List.of(22), service.centeredSevenColumnSlots(2, 1));
        assertEquals(List.of(21, 23), service.centeredSevenColumnSlots(2, 2));
        assertEquals(List.of(20, 22, 24), service.centeredSevenColumnSlots(2, 3));
        assertEquals(List.of(19, 21, 23, 25), service.centeredSevenColumnSlots(2, 4));
        assertEquals(List.of(20, 21, 22, 23, 24), service.centeredSevenColumnSlots(2, 5));
        assertEquals(List.of(19, 20, 21, 23, 24, 25), service.centeredSevenColumnSlots(2, 6));
        assertEquals(List.of(19, 20, 21, 22, 23, 24, 25), service.centeredSevenColumnSlots(2, 7));
    }

    @Test
    void handlesEmptyRowsAndRejectsUnsupportedInput() {
        assertEquals(List.of(), service.centeredSevenColumnSlots(2, 0));
        assertThrows(IllegalArgumentException.class, () -> service.centeredSevenColumnSlots(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> service.centeredSevenColumnSlots(2, 8));
    }
}
