package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.gui.MenuClickType;
import com.awabi2048.ccsystem.api.gui.MenuSound;
import com.awabi2048.ccsystem.api.gui.MenuSoundPolicy;
import com.awabi2048.ccsystem.core.gui.MenuSoundPolicyResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MenuSoundPolicyResolverTest {
    @Test
    void defaultUsesSemanticClickSound() {
        var resolved = MenuSoundPolicyResolver.INSTANCE.resolve(
            MenuSoundPolicy.Default.INSTANCE,
            MenuSoundPolicy.Default.INSTANCE,
            MenuClickType.CONFIRM
        );

        assertEquals("UI_BUTTON_CLICK", resolved.getSound());
        assertEquals(1.6f, resolved.getPitch());
    }

    @Test
    void fallbackMayExplicitlySilenceDefaultActionSound() {
        var resolved = MenuSoundPolicyResolver.INSTANCE.resolve(
            MenuSoundPolicy.Default.INSTANCE,
            MenuSoundPolicy.Silent.INSTANCE,
            MenuClickType.DEFAULT
        );

        assertNull(resolved);
    }

    @Test
    void actionCustomSoundOverridesMenuPolicy() {
        var custom = new MenuSound("BLOCK_NOTE_BLOCK_BELL", 0.8f, 0.5f);
        var resolved = MenuSoundPolicyResolver.INSTANCE.resolve(
            new MenuSoundPolicy.Custom(custom),
            MenuSoundPolicy.Silent.INSTANCE,
            MenuClickType.CANCEL
        );

        assertEquals(custom, resolved);
    }

    @Test
    void actionMayExplicitlySilenceCustomMenuSound() {
        var resolved = MenuSoundPolicyResolver.INSTANCE.resolve(
            MenuSoundPolicy.Silent.INSTANCE,
            new MenuSoundPolicy.Custom(new MenuSound("UI_BUTTON_CLICK", 1.0f, 1.0f)),
            MenuClickType.DEFAULT
        );

        assertNull(resolved);
    }
}
