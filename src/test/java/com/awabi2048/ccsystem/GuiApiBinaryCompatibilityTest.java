package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.GuiItemSpec;
import com.awabi2048.ccsystem.api.gui.GuiFrameSpec;
import com.awabi2048.ccsystem.api.gui.GuiLoreLine;
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec;
import com.awabi2048.ccsystem.api.gui.GuiNameSpec;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class GuiApiBinaryCompatibilityTest {
    @Test
    void publicGuiModelsDoNotExposeDefaultConstructorMarker() {
        List<Class<?>> models = List.of(
            GuiItemSpec.class,
            GuiFrameSpec.class,
            GuiNameSpec.Text.class,
            GuiLoreSpec.Auto.class,
            GuiLoreSpec.Rich.class,
            GuiLoreLine.Data.class
        );

        for (Class<?> model : models) {
            for (Constructor<?> constructor : model.getDeclaredConstructors()) {
                boolean hasMarker = List.of(constructor.getParameterTypes()).stream()
                    .anyMatch(type -> type.getName().endsWith("DefaultConstructorMarker"));
                assertFalse(hasMarker, model.getName() + " must not expose a Kotlin default constructor");
            }
        }
    }
}
