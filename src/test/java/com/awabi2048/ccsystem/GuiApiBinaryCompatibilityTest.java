package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.GuiItemSpec;
import com.awabi2048.ccsystem.api.gui.GuiFrameSpec;
import com.awabi2048.ccsystem.api.gui.GuiLoreLine;
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec;
import com.awabi2048.ccsystem.api.gui.GuiNameSpec;
import com.awabi2048.ccsystem.api.gui.GuiConfirmationLayout;
import com.awabi2048.ccsystem.api.gui.GuiPagedListLayout;
import com.awabi2048.ccsystem.api.gui.GuiSevenColumnListLayout;
import com.awabi2048.ccsystem.api.gui.GuiSevenColumnPage;
import com.awabi2048.ccsystem.api.gui.GuiSettingsLayout;
import com.awabi2048.ccsystem.api.gui.GuiThreeChoiceLayout;
import com.awabi2048.ccsystem.api.gui.GuiMenuIconAction;
import com.awabi2048.ccsystem.api.gui.GuiMenuIconData;
import com.awabi2048.ccsystem.api.gui.GuiMenuIconOption;
import com.awabi2048.ccsystem.api.gui.GuiMenuIconSpec;
import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition;
import com.awabi2048.ccsystem.api.gui.InventoryMenuRenderer;
import com.awabi2048.ccsystem.api.gui.InventoryMenuView;
import com.awabi2048.ccsystem.api.gui.MenuActionHandler;
import com.awabi2048.ccsystem.api.gui.MenuActionSoundPolicy;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;
import java.util.List;
import kotlin.jvm.internal.DefaultConstructorMarker;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GuiApiBinaryCompatibilityTest {
    @Test
    void publicGuiModelsDoNotExposeDefaultConstructorMarker() {
        List<Class<?>> models = List.of(
            GuiItemSpec.class,
            GuiFrameSpec.class,
            GuiNameSpec.Text.class,
            GuiLoreSpec.Rich.class,
            GuiLoreLine.Data.class,
            GuiLoreLine.ComponentData.class,
            GuiLoreLine.Metadata.class,
            GuiLoreLine.StyledText.class,
            GuiLoreLine.SingleAction.class,
            GuiLoreLine.Option.class,
            GuiMenuIconAction.class,
            GuiMenuIconData.class,
            GuiMenuIconOption.class,
            GuiMenuIconSpec.class,
            GuiConfirmationLayout.class,
            GuiPagedListLayout.class,
            GuiSevenColumnListLayout.class,
            GuiSevenColumnPage.class,
            GuiSettingsLayout.class,
            GuiThreeChoiceLayout.class
        );

        for (Class<?> model : models) {
            for (Constructor<?> constructor : model.getDeclaredConstructors()) {
                boolean hasMarker = List.of(constructor.getParameterTypes()).stream()
                    .anyMatch(type -> type.getName().endsWith("DefaultConstructorMarker"));
                assertFalse(hasMarker, model.getName() + " must not expose a Kotlin default constructor");
            }
        }
    }

    @Test
    void runtimeModelsRetainPre218DefaultConstructorSignatures() {
        assertDoesNotThrow(() -> InventoryMenuDefinition.class.getDeclaredConstructor(
            String.class,
            String.class,
            InventoryMenuRenderer.class,
            Map.class,
            MenuActionSoundPolicy.class,
            int.class,
            DefaultConstructorMarker.class
        ).newInstance(
            "legacy",
            "definition",
            (InventoryMenuRenderer) context -> new InventoryMenuView(
                9,
                Component.empty(),
                List.of(),
                true,
                Set.of(),
                Map.of(),
                false
            ),
            Map.of(),
            null,
            0x10,
            null
        ));
        assertDoesNotThrow(() -> InventoryMenuView.class.getDeclaredConstructor(
            int.class,
            Component.class,
            List.class,
            boolean.class,
            Set.class,
            boolean.class,
            int.class,
            DefaultConstructorMarker.class
        ).newInstance(
            9,
            Component.empty(),
            List.of(),
            false,
            null,
            false,
            0x38,
            null
        ));
    }
}
