package com.awabi2048.ccsystem.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// config/npc_message.yml の検証・解決をBukkitサーバーなしで確認します。
class NpcMessageConfigTest {

    @Test
    fun `正常な言語分岐定義を保持する`() {
        val yaml = YamlConfiguration()
        yaml.set("messages.example.style", "random")
        yaml.set("messages.example.texts.ja_jp", listOf("§aこんにちは", "§bさようなら"))
        yaml.set("messages.example.texts.en_us", listOf("hello"))

        val parsed = ConfigManager.parseNpcMessageEntries(yaml, "ja_jp")

        assertEquals(setOf("example"), parsed.keys)
        assertEquals("random", parsed.getValue("example").style)
        assertEquals(listOf("§aこんにちは", "§bさようなら"), parsed.getValue("example").textsByLocale.getValue("ja_jp"))
        assertEquals(listOf("hello"), parsed.getValue("example").textsByLocale.getValue("en_us"))
    }

    @Test
    fun `任意ロケールの追加を許可する`() {
        val yaml = YamlConfiguration()
        yaml.set("messages.example.style", "batch")
        yaml.set("messages.example.texts.ja_jp", listOf("日本語"))
        yaml.set("messages.example.texts.fr_fr", listOf("bonjour"))

        val parsed = ConfigManager.parseNpcMessageEntries(yaml, "ja_jp")

        assertTrue(parsed.getValue("example").textsByLocale.containsKey("fr_fr"))
    }

    @Test
    fun `不足ロケールは既定言語へフォールバックする`() {
        val entry = ConfigManager.NpcMessageEntry(
            style = "batch",
            textsByLocale = mapOf("ja_jp" to listOf("日本語"))
        )

        assertEquals(listOf("日本語"), ConfigManager.resolveNpcMessageTexts(entry, "en_us", "ja_jp"))
        assertEquals(listOf("日本語"), ConfigManager.resolveNpcMessageTexts(entry, null, "ja_jp"))
        assertEquals(listOf("日本語"), ConfigManager.resolveNpcMessageTexts(entry, "JA", "ja_jp"))
    }

    @Test
    fun `不正な形式と空本文と既定言語不足を拒否する`() {
        // 不正style
        val invalidStyle = YamlConfiguration()
        invalidStyle.set("messages.bad.style", "shuffle")
        invalidStyle.set("messages.bad.texts.ja_jp", listOf("本文"))
        assertThrows<IllegalArgumentException> {
            ConfigManager.parseNpcMessageEntries(invalidStyle, "ja_jp")
        }

        // 空texts
        val emptyTexts = YamlConfiguration()
        emptyTexts.set("messages.bad.style", "random")
        emptyTexts.set("messages.bad.texts.ja_jp", emptyList<String>())
        assertThrows<IllegalArgumentException> {
            ConfigManager.parseNpcMessageEntries(emptyTexts, "ja_jp")
        }

        // 既定言語なし
        val missingDefault = YamlConfiguration()
        missingDefault.set("messages.bad.style", "random")
        missingDefault.set("messages.bad.texts.en_us", listOf("hello"))
        assertThrows<IllegalArgumentException> {
            ConfigManager.parseNpcMessageEntries(missingDefault, "ja_jp")
        }
    }

    @Test
    fun `messages節なしは空として許容する`() {
        val parsed = ConfigManager.parseNpcMessageEntries(YamlConfiguration(), "ja_jp")

        assertTrue(parsed.isEmpty())
    }
}
