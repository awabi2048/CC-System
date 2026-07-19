package com.awabi2048.ccsystem;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageRuntimeSourcePolicyTest {
    @Test
    void languageRuntimeUsesOnlyJarResources() throws IOException {
        String pluginSource = Files.readString(
            Path.of("src/main/kotlin/com/awabi2048/ccsystem/CCSystem.kt")
        );
        String managerSource = Files.readString(
            Path.of("src/main/kotlin/com/awabi2048/ccsystem/core/config/UnifiedLanguageManager.kt")
        );

        assertFalse(pluginSource.contains("saveSplitLanguageResources"));
        assertFalse(managerSource.contains("discoverDataLanguageFiles"));
        assertFalse(managerSource.contains("sourcePlugin.dataFolder"));
        assertFalse(managerSource.contains("sourceName = \"data:"));
        assertTrue(managerSource.contains("JarFile(jarFile)"));
        assertTrue(managerSource.contains("sourceName = \"resource:"));
    }
}
