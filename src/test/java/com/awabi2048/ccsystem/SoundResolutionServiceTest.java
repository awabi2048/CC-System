package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.core.sound.SoundResolutionServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SoundResolutionServiceTest {
    @Test
    void normalizesEnumAndResourceIdForms() {
        assertNull(SoundResolutionServiceImpl.normalizedResourceId("BLOCK_NOTE_BLOCK_BELL"));
        assertEquals(
            "minecraft:block.note_block.bell",
            SoundResolutionServiceImpl.normalizedResourceId("minecraft:block.note_block.bell")
        );
        assertEquals(
            "minecraft:block.note_block.bell",
            SoundResolutionServiceImpl.normalizedResourceId("block.note_block.bell")
        );
    }
}
