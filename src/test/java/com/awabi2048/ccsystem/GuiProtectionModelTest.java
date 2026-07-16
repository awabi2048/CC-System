package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicy;
import com.awabi2048.ccsystem.core.gui.GuiItemMarker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiProtectionModelTest {
    @Test
    void markerUsesTheCCSystemNamespace() {
        assertEquals("cc-system", GuiItemMarker.INSTANCE.getMarkerKey().getNamespace());
        assertEquals("gui_marker", GuiItemMarker.INSTANCE.getMarkerKey().getKey());
        assertEquals("gui_role", GuiItemMarker.INSTANCE.getRoleKey().getKey());
    }

    @Test
    void policyValidatesInputSlots() {
        var policy = new GuiInventoryPolicy(java.util.Set.of(10, 11), true);

        assertTrue(policy.acceptsTopSlot(10));
        assertFalse(policy.acceptsTopSlot(12));
        assertThrows(IllegalArgumentException.class, () -> new GuiInventoryPolicy(java.util.Set.of(-1), false));
    }
}
