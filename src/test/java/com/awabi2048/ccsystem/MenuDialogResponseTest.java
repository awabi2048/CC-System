package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.MenuDialogResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuDialogResponseTest {
    @Test
    void missingValuesUseSafeTypedDefaults() {
        var response = new MenuDialogResponse(
            Map.of("name", "sample"),
            Map.of("enabled", true)
        );

        assertEquals("sample", response.textValue("name"));
        assertEquals("", response.textValue("missing"));
        assertTrue(response.booleanValue("enabled"));
        assertFalse(response.booleanValue("missing"));
    }
}
