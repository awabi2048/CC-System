package com.awabi2048.ccsystem;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSchemaSourceContractTest {
    @Test
    void migrationUsesBackupValidationAndAtomicReplacement() throws Exception {
        String source = Files.readString(
            Path.of("src/main/kotlin/com/awabi2048/ccsystem/core/config/ConfigSchemaServiceImpl.kt"),
            StandardCharsets.UTF_8
        );
        assertTrue(source.contains("backup(spec.targetPath)"));
        assertTrue(source.contains("spec.validator.validate(original)"));
        assertTrue(source.contains("StandardCopyOption.ATOMIC_MOVE"));
        assertTrue(source.contains("ConfigPreparationState.FUTURE_VERSION"));
    }
}
