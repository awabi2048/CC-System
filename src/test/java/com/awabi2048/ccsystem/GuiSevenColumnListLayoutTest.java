package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.GuiSevenColumnListLayout;
import com.awabi2048.ccsystem.core.gui.GuiLayoutServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiSevenColumnListLayoutTest {
    private final GuiLayoutServiceImpl service = new GuiLayoutServiceImpl();

    @Test
    void layoutBandsMatchPageItemCount() {
        assertLayout(0, 45, 7);
        assertLayout(1, 45, 7);
        assertLayout(7, 45, 7);
        assertLayout(8, 54, 14);
        assertLayout(14, 54, 14);
        assertLayout(15, 45, 21);
        assertLayout(21, 45, 21);
        assertLayout(22, 54, 28);
        assertLayout(28, 54, 28);
        assertLayout(29, 54, 28);
    }

    @Test
    void finalPageUsesItsActualItemCount() {
        var page = service.sevenColumnPage(29, 1);

        assertEquals(1, page.getPage());
        assertEquals(2, page.getTotalPages());
        assertEquals(28, page.getStartIndex());
        assertEquals(1, page.getItemCount());
        assertEquals(45, page.getLayout().getSize());
        assertEquals(7, page.getLayout().getItemsPerPage());
    }

    private void assertLayout(int itemCount, int expectedSize, int expectedCapacity) {
        GuiSevenColumnListLayout layout = service.sevenColumnList(itemCount);
        assertEquals(expectedSize, layout.getSize(), "itemCount=" + itemCount);
        assertEquals(expectedCapacity, layout.getItemsPerPage(), "itemCount=" + itemCount);
        assertEquals(expectedCapacity, layout.getItemSlots().size(), "itemCount=" + itemCount);
    }
}
