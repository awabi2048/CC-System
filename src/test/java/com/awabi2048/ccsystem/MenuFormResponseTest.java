package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.MenuFormResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuFormResponseTest {
    @Test
    void missingValuesUseStableDefaults() {
        var response = new MenuFormResponse(Map.of("name", "world"), Map.of("public", true));

        assertEquals("world", response.textValue("name"));
        assertEquals("", response.textValue("missing"));
        assertTrue(response.toggleValue("public"));
        assertFalse(response.toggleValue("missing"));
    }
}
