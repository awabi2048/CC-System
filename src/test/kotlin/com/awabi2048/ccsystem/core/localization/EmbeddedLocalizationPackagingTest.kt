package com.awabi2048.ccsystem.core.localization

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class EmbeddedLocalizationPackagingTest {
    @Test
    fun `言語YAMLをリソースへ再導入しない`() {
        val resourceRoot = Path.of("src/main/resources")
        val languageYaml = Files.walk(resourceRoot).use { paths ->
            paths.filter(Files::isRegularFile)
                .map(resourceRoot::relativize)
                .filter { path ->
                    path.toString().replace('\\', '/').startsWith("lang/") &&
                        path.fileName.toString().lowercase().let { it.endsWith(".yml") || it.endsWith(".yaml") }
                }
                .toList()
        }
        assertTrue(languageYaml.isEmpty(), "言語データはKotlinカタログへ埋め込んでください: $languageYaml")
    }

    @Test
    fun `ランタイム実装はJAR走査やYAML設定へ依存しない`() {
        val sources = listOf(
            Path.of("src/main/kotlin/com/awabi2048/ccsystem/core/config/UnifiedLanguageManager.kt"),
            Path.of("src/main/kotlin/com/awabi2048/ccsystem/core/localization/EmbeddedLocalizationCatalog.kt"),
        ).joinToString("\n") { Files.readString(it) }
        assertFalse(sources.contains("JarFile"))
        assertFalse(sources.contains("YamlConfiguration"))
        assertFalse(sources.contains("SnakeYAML"))
        assertFalse(sources.contains("getResource("))
    }
}
