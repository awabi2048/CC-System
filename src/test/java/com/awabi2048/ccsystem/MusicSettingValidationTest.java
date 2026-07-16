package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.core.config.ConfigManager;
import com.awabi2048.ccsystem.features.misc.listener.MusicListener;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MusicSettingValidationTest {
    @Test
    void preservesValidMusicSettings() {
        YamlConfiguration section = section();
        section.set("sound", "custom:forest_theme");
        section.set("volume", 1.25);
        section.set("pitch", 0.75);
        section.set("duration", 30);

        ConfigManager.MusicSetting setting = ConfigManager.INSTANCE.validateMusicSetting$CC_System("world", section);

        assertEquals("custom:forest_theme", setting.getSound());
        assertEquals(1.25F, setting.getVolume());
        assertEquals(0.75F, setting.getPitch());
        assertEquals(30, setting.getDuration());
    }

    @Test
    void rejectsInvalidDurationVolumePitchAndSound() {
        List<String> invalidKeys = List.of("duration", "volume", "pitch", "sound");
        List<Object> invalidValues = List.of(0, 0.0, 2.1, "invalid sound id");

        for (int i = 0; i < invalidKeys.size(); i++) {
            YamlConfiguration section = section();
            section.set(invalidKeys.get(i), invalidValues.get(i));
            assertThrows(IllegalArgumentException.class,
                    () -> ConfigManager.INSTANCE.validateMusicSetting$CC_System("world", section));
        }
    }

    @Test
    void rejectsFiniteDoubleThatOverflowsToFloatInfinity() {
        YamlConfiguration section = section();
        section.set("volume", Double.MAX_VALUE);

        assertThrows(IllegalArgumentException.class,
                () -> ConfigManager.INSTANCE.validateMusicSetting$CC_System("world", section));
    }

    @Test
    void schedulesTheNextTrackAtTheConfiguredDuration() {
        assertEquals(0L, MusicListener.Companion.replayDelayAfterPlay(20L));
        assertEquals(3980L, MusicListener.Companion.replayDelayAfterPlay(4000L));
    }

    private static YamlConfiguration section() {
        YamlConfiguration section = new YamlConfiguration();
        section.set("sound", "minecraft:music.game");
        section.set("volume", 1.0);
        section.set("pitch", 1.0);
        section.set("duration", 20);
        return section;
    }
}
