package com.awabi2048.ccsystem.core.config

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.jar.JarFile

class UnifiedLanguageManager(private val plugin: JavaPlugin) {
    data class ValidationResult(
        val errors: List<String>
    ) {
        val isValid: Boolean = errors.isEmpty()
    }

    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val plainSerializer = PlainTextComponentSerializer.plainText()
    private var languageData: Map<String, YamlConfiguration> = emptyMap()

    fun load() {
        languageData = loadAllLanguages().also { loaded ->
            if (loaded.isEmpty()) {
                throw IllegalStateException("言語ファイルが見つかりません")
            }
        }
    }

    fun reload() {
        load()
    }

    fun validate(): ValidationResult {
        return runCatching {
            loadAllLanguages()
            ValidationResult(emptyList())
        }.getOrElse { error ->
            ValidationResult(listOf(error.message ?: "不明な言語エラー"))
        }
    }

    fun resolveLocale(player: Player?): String {
        if (player == null) {
            return resolveLocale(ConfigManager.getDefaultLanguage())
        }
        return resolveLocale(player.locale)
    }

    fun resolveLocale(sender: CommandSender?): String {
        return resolveLocale(sender as? Player)
    }

    fun getSupportedLanguages(): Set<String> {
        return languageData.keys
    }

    fun getDefaultLanguage(): String {
        return resolveLocale(ConfigManager.getDefaultLanguage())
    }

    fun getString(player: Player?, key: String): String {
        return getString(player, key, emptyMap())
    }

    fun getString(player: Player?, key: String, placeholders: Map<String, Any>): String {
        val locale = resolveLocale(player)
        val config = requireLanguage(locale)
        val raw = when {
            config.isString(key) -> config.getString(key)
            else -> null
        } ?: throw IllegalStateException("言語キーが見つからないか型が不正です: locale=$locale key=$key expected=String")
        return applyPlaceholders(raw, placeholders)
    }

    fun getStringList(player: Player?, key: String): List<String> {
        return getStringList(player, key, emptyMap())
    }

    fun getStringList(player: Player?, key: String, placeholders: Map<String, Any>): List<String> {
        val locale = resolveLocale(player)
        val config = requireLanguage(locale)
        if (!config.isList(key)) {
            throw IllegalStateException("言語キーが見つからないか型が不正です: locale=$locale key=$key expected=List")
        }
        return config.getStringList(key).map { applyPlaceholders(it, placeholders) }
    }

    fun getComponent(player: Player?, key: String): Component {
        return getComponent(player, key, emptyMap())
    }

    fun getComponent(player: Player?, key: String, placeholders: Map<String, Any>): Component {
        return serializer.deserialize(getString(player, key, placeholders)).decoration(TextDecoration.ITALIC, false)
    }

    fun getComponentList(player: Player?, key: String): List<Component> {
        return getComponentList(player, key, emptyMap())
    }

    fun getComponentList(player: Player?, key: String, placeholders: Map<String, Any>): List<Component> {
        return getStringList(player, key, placeholders).map {
            serializer.deserialize(it).decoration(TextDecoration.ITALIC, false)
        }
    }

    fun getRawString(locale: String, key: String): String? {
        val resolvedLocale = resolveLocale(locale)
        val config = languageData[resolvedLocale] ?: return null
        return config.getString(key)
    }

    fun getRawStringList(locale: String, key: String): List<String>? {
        val resolvedLocale = resolveLocale(locale)
        val config = languageData[resolvedLocale] ?: return null
        if (!config.isList(key)) {
            return null
        }
        return config.getStringList(key)
    }

    fun hasKey(player: Player?, key: String): Boolean {
        return hasKey(resolveLocale(player), key)
    }

    fun hasKey(locale: String, key: String): Boolean {
        return languageData[resolveLocale(locale)]?.contains(key) == true
    }

    fun getSectionKeys(player: Player?, key: String): Set<String> {
        val locale = resolveLocale(player)
        return languageData[locale]?.getConfigurationSection(key)?.getKeys(false) ?: emptySet()
    }

    fun isKeyMatch(title: String, key: String): Boolean {
        return buildTemplateRegex(key)?.matches(title) == true
    }

    fun isKeyStartWith(title: String, key: String): Boolean {
        val regex = buildTemplateRegex(key, anchoredEnd = false) ?: return false
        return regex.containsMatchIn(title)
    }

    fun deserialize(message: String): Component {
        return serializer.deserialize(message).decoration(TextDecoration.ITALIC, false)
    }

    private fun resolveLocale(raw: String?): String {
        val normalized = normalizeLocale(raw)
        return when {
            languageData.containsKey(normalized) -> normalized
            languageData.containsKey(ConfigManager.getDefaultLanguage()) -> ConfigManager.getDefaultLanguage()
            languageData.containsKey("ja_jp") -> "ja_jp"
            languageData.isNotEmpty() -> languageData.keys.first()
            else -> "ja_jp"
        }
    }

    private fun requireLanguage(locale: String): YamlConfiguration {
        return languageData[locale]
            ?: throw IllegalStateException("言語がロードされていません: $locale")
    }

    private fun normalizeLocale(raw: String?): String {
        val normalized = raw?.trim()?.lowercase()?.replace('-', '_').orEmpty()
        return when {
            normalized.isBlank() -> ConfigManager.getDefaultLanguage()
            normalized == "ja" -> "ja_jp"
            normalized == "en" -> "en_us"
            else -> normalized
        }
    }

    private fun applyPlaceholders(template: String, placeholders: Map<String, Any>): String {
        var resolved = template
        for ((key, value) in placeholders) {
            val replacement = value.toString()
            resolved = resolved.replace("{$key}", replacement)
            resolved = resolved.replace("%$key%", replacement)
        }
        return resolved
    }

    private fun buildTemplateRegex(key: String, anchoredEnd: Boolean = true): Regex? {
        val templates = languageData.values.mapNotNull { it.getString(key) }.distinct()
        if (templates.isEmpty()) {
            return null
        }
        val union = templates.joinToString("|") { raw ->
            val plain = plainSerializer.serialize(serializer.deserialize(raw))
            val escaped = Regex.escape(plain)
                .replace(Regex("\\\\\\{[^}]+\\\\\\}"), ".*")
                .replace(Regex("%[^%]+%"), ".*")
            if (anchoredEnd) {
                "^$escaped$"
            } else {
                "^$escaped"
            }
        }
        return Regex(union)
    }

    private fun loadAllLanguages(): Map<String, YamlConfiguration> {
        val locales = discoverLocales()
        return locales.associateWith { locale -> loadLocale(locale) }
    }

    private fun discoverLocales(): Set<String> {
        val locales = linkedSetOf<String>()
        locales += normalizeLocale(ConfigManager.getDefaultLanguage())
        locales += "ja_jp"
        locales += "en_us"

        val dataLangDir = File(plugin.dataFolder, "lang")
        if (dataLangDir.exists()) {
            dataLangDir.listFiles()?.forEach { file ->
                when {
                    file.isDirectory -> locales += normalizeLocale(file.name)
                    file.isFile && file.extension.equals("yml", ignoreCase = true) -> locales += normalizeLocale(file.nameWithoutExtension)
                }
            }
        }

        val jarFile = runCatching {
            File(plugin.javaClass.protectionDomain.codeSource.location.toURI())
        }.getOrNull()
        if (jarFile != null && jarFile.isFile) {
            JarFile(jarFile).use { jar ->
                jar.entries().asSequence()
                    .map { it.name }
                    .filter { it.startsWith("lang/") && it != "lang/" }
                    .forEach { entry ->
                        val remainder = entry.removePrefix("lang/")
                        val locale = remainder.substringBefore('/').substringBefore('.')
                        if (locale.isNotBlank()) {
                            locales += normalizeLocale(locale)
                        }
                    }
            }
        }

        return locales
    }

    private fun loadLocale(locale: String): YamlConfiguration {
        val merged = YamlConfiguration()
        val mergedKeys = mutableMapOf<String, String>()

        mergeSplitFilesFromResource(locale, merged, mergedKeys)
        mergeSplitFilesFromDataFolder(locale, merged, mergedKeys)

        if (merged.getKeys(true).isNotEmpty()) {
            return merged
        }

        val single = loadSingleFile(locale)
        if (single != null) {
            return single
        }

        throw IllegalStateException("言語ファイルが見つかりません: $locale")
    }

    private fun mergeSplitFilesFromResource(locale: String, target: YamlConfiguration, mergedKeys: MutableMap<String, String>) {
        val jarFile = runCatching {
            File(plugin.javaClass.protectionDomain.codeSource.location.toURI())
        }.getOrNull() ?: return
        if (!jarFile.isFile) {
            return
        }
        JarFile(jarFile).use { jar ->
            jar.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("lang/$locale/") && it.name.endsWith(".yml") }
                .sortedBy { it.name }
                .forEach { entry ->
                    jar.getInputStream(entry).use { input ->
                        InputStreamReader(input, StandardCharsets.UTF_8).use { reader ->
                            val config = YamlConfiguration.loadConfiguration(reader)
                            mergeConfig(target, config, mergedKeys, "resource:${entry.name}")
                        }
                    }
                }
        }
    }

    private fun mergeSplitFilesFromDataFolder(locale: String, target: YamlConfiguration, mergedKeys: MutableMap<String, String>) {
        val localeDir = File(plugin.dataFolder, "lang/$locale")
        if (!localeDir.isDirectory) {
            return
        }
        localeDir.listFiles { file -> file.isFile && file.extension.equals("yml", ignoreCase = true) }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                val config = YamlConfiguration.loadConfiguration(file)
                mergeConfig(target, config, mergedKeys, "data:${file.absolutePath}")
            }
    }

    private fun loadSingleFile(locale: String): YamlConfiguration? {
        val fromDataFolder = File(plugin.dataFolder, "lang/$locale.yml")
        if (fromDataFolder.isFile) {
            return YamlConfiguration.loadConfiguration(fromDataFolder)
        }

        val resource = plugin.getResource("lang/$locale.yml") ?: return null
        return resource.use { input ->
            InputStreamReader(input, StandardCharsets.UTF_8).use { reader ->
                YamlConfiguration.loadConfiguration(reader)
            }
        }
    }

    private fun mergeConfig(
        target: YamlConfiguration,
        source: YamlConfiguration,
        mergedKeys: MutableMap<String, String>,
        sourceName: String
    ) {
        for (key in source.getKeys(true)) {
            if (source.isConfigurationSection(key)) {
                continue
            }
            val value = source.get(key)
            val previous = mergedKeys.putIfAbsent(key, sourceName)
            if (previous != null) {
                throw IllegalStateException("言語キーが重複しています: key=$key sources=$previous,$sourceName")
            }
            target.set(key, cloneValue(value))
        }
    }

    private fun cloneValue(value: Any?): Any? {
        return if (value is List<*>) value.toList() else value
    }
}
