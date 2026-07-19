package com.awabi2048.ccsystem.core.config

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.jar.JarFile

class UnifiedLanguageManager(private val plugin: JavaPlugin) {
    data class ValidationResult(
        val errors: List<String>,
        val errorsByFeature: Map<String, List<String>> = emptyMap()
    ) {
        val isValid: Boolean = errors.isEmpty()

        fun errorsFor(feature: String): List<String> {
            return errorsByFeature[feature].orEmpty()
        }
    }

    private data class SourceRegistration(
        val sourceId: String,
        val plugin: JavaPlugin,
        val fileNames: Set<String>
    )

    private data class DiscoveredLanguageFile(
        val locale: String,
        val fileName: String,
        val sourceName: String,
        val content: String
    )

    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val plainSerializer = PlainTextComponentSerializer.plainText()
    private val registrations = linkedMapOf<String, SourceRegistration>()
    private var languageDataBySource: Map<String, Map<String, YamlConfiguration>> = emptyMap()

    init {
        registrations[plugin.name] = SourceRegistration(plugin.name, plugin, emptySet())
    }

    fun load() {
        val validation = validateSource(plugin, defaultFeatureByFile())
        if (!validation.isValid) {
            val detail = validation.errors.joinToString("\n") { "- $it" }
            throw IllegalStateException("言語ファイル検証に失敗しました:\n$detail")
        }

        languageDataBySource = loadAllLanguages().also { loaded ->
            if (loaded.isEmpty() || loaded.values.all { it.isEmpty() }) {
                throw IllegalStateException("言語ファイルが見つかりません")
            }
        }
    }

    fun reload() {
        load()
    }

    fun validate(): ValidationResult {
        return validateSource(plugin, defaultFeatureByFile())
    }

    fun validateSource(sourcePlugin: JavaPlugin, featureByFile: Map<String, String>): ValidationResult {
        val filesByLocale = discoverResourceLanguageFiles(sourcePlugin)
        val errorsByFeature = linkedMapOf<String, MutableList<String>>()

        if (filesByLocale.isEmpty()) {
            val feature = featureByFile.values.firstOrNull() ?: sourcePlugin.name
            errorsByFeature.getOrPut(feature) { mutableListOf() } += "lang/<locale>/*.yml が見つかりません"
            return ValidationResult(flattenErrors(errorsByFeature), errorsByFeature.mapValues { it.value.toList() })
        }

        val requiredLocales = linkedSetOf(normalizeLocale(ConfigManager.getDefaultLanguage()), "ja_jp", "en_us")
        for (locale in requiredLocales) {
            if (!filesByLocale.containsKey(locale)) {
                val feature = featureByFile.values.firstOrNull() ?: sourcePlugin.name
                errorsByFeature.getOrPut(feature) { mutableListOf() } += "locale=$locale の言語ディレクトリが見つかりません"
            }
        }

        val parsedByLocale = linkedMapOf<String, Map<String, Pair<DiscoveredLanguageFile, Map<*, *>>>>()
        for ((locale, files) in filesByLocale) {
            val mergedKeys = mutableMapOf<String, String>()
            val parsedFiles = linkedMapOf<String, Pair<DiscoveredLanguageFile, Map<*, *>>>()
            for ((fileName, file) in files.toSortedMap()) {
                val feature = featureFor(fileName, featureByFile, sourcePlugin.name)
                val parsed = runCatching { validateYaml(file.content, file.sourceName) }
                    .onFailure { error ->
                        errorsByFeature.getOrPut(feature) { mutableListOf() } += "${file.sourceName} -> ${error.message}"
                    }
                    .getOrNull() ?: continue

                parsedFiles[fileName] = file to parsed
                collectLeafKeys(parsed).forEach { key ->
                    val previous = mergedKeys.putIfAbsent(key, file.sourceName)
                    if (previous != null) {
                        errorsByFeature.getOrPut(feature) { mutableListOf() } +=
                            "locale=$locale key=$key が重複しています: $previous, ${file.sourceName}"
                    }
                }
            }
            parsedByLocale[locale] = parsedFiles
        }

        val allFileNames = parsedByLocale.values.flatMap { it.keys }.toSortedSet()
        for (fileName in allFileNames) {
            val feature = featureFor(fileName, featureByFile, sourcePlugin.name)
            val ja = parsedByLocale["ja_jp"]?.get(fileName)?.second
            val en = parsedByLocale["en_us"]?.get(fileName)?.second

            if (ja == null) {
                errorsByFeature.getOrPut(feature) { mutableListOf() } += "ja_jp/$fileName が見つかりません"
            }
            if (en == null) {
                errorsByFeature.getOrPut(feature) { mutableListOf() } += "en_us/$fileName が見つかりません"
            }
            if (ja != null && en != null) {
                validateStructure(ja, en, "", errorsByFeature.getOrPut(feature) { mutableListOf() })
                validateStructure(en, ja, "", errorsByFeature.getOrPut(feature) { mutableListOf() })
            }
        }

        return ValidationResult(flattenErrors(errorsByFeature), errorsByFeature.mapValues { it.value.toList() })
    }

    fun registerSource(sourceId: String, sourcePlugin: JavaPlugin, fileNames: Set<String> = emptySet()) {
        registrations[sourceId] = SourceRegistration(sourceId, sourcePlugin, fileNames)
        languageDataBySource = loadAllLanguages()
    }

    fun unregisterSource(sourceId: String) {
        if (sourceId == plugin.name) {
            return
        }
        registrations.remove(sourceId)
        languageDataBySource = loadAllLanguages()
    }

    fun resolveLocale(player: Player?): String {
        if (player == null) {
            return resolveLocale(ConfigManager.getDefaultLanguage())
        }
        return resolveLocale(player.locale().toString())
    }

    fun resolveLocale(sender: CommandSender?): String {
        return resolveLocale(sender as? Player)
    }

    fun getSupportedLanguages(): Set<String> {
        return availableLocales()
    }

    fun getDefaultLanguage(): String {
        return resolveLocale(ConfigManager.getDefaultLanguage())
    }

    fun getString(player: Player?, key: String): String {
        return getString(player, key, emptyMap())
    }

    fun getString(player: Player?, key: String, placeholders: Map<String, Any>): String {
        return getString(plugin.name, player, key, placeholders)
    }

    fun getString(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): String {
        val locale = resolveLocale(player)
        val config = requireLanguage(sourceId, locale)
        val raw = when {
            config.isString(key) -> config.getString(key)
            else -> null
        } ?: throw IllegalStateException("言語キーが見つからないか型が不正です: source=$sourceId locale=$locale key=$key expected=String")
        return applyPlaceholders(raw, placeholders)
    }

    fun getStringList(player: Player?, key: String): List<String> {
        return getStringList(player, key, emptyMap())
    }

    fun getStringList(player: Player?, key: String, placeholders: Map<String, Any>): List<String> {
        return getStringList(plugin.name, player, key, placeholders)
    }

    fun getStringList(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): List<String> {
        val locale = resolveLocale(player)
        val config = requireLanguage(sourceId, locale)
        if (!config.isList(key)) {
            throw IllegalStateException("言語キーが見つからないか型が不正です: source=$sourceId locale=$locale key=$key expected=List")
        }
        return config.getStringList(key).map { applyPlaceholders(it, placeholders) }
    }

    fun getComponent(player: Player?, key: String): Component {
        return getComponent(player, key, emptyMap())
    }

    fun getComponent(player: Player?, key: String, placeholders: Map<String, Any>): Component {
        return getComponent(plugin.name, player, key, placeholders)
    }

    fun getComponent(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): Component {
        return normalizeComponent(serializer.deserialize(getString(sourceId, player, key, placeholders)))
    }

    fun getComponentList(player: Player?, key: String): List<Component> {
        return getComponentList(player, key, emptyMap())
    }

    fun getComponentList(player: Player?, key: String, placeholders: Map<String, Any>): List<Component> {
        return getComponentList(plugin.name, player, key, placeholders)
    }

    fun getComponentList(sourceId: String, player: Player?, key: String, placeholders: Map<String, Any> = emptyMap()): List<Component> {
        return getStringList(sourceId, player, key, placeholders).map {
            normalizeComponent(serializer.deserialize(it))
        }
    }

    fun getRawString(locale: String, key: String): String? {
        return getRawString(plugin.name, locale, key)
    }

    fun getRawString(sourceId: String, locale: String, key: String): String? {
        val resolvedLocale = resolveLocale(locale)
        val config = languageDataBySource[sourceId]?.get(resolvedLocale) ?: return null
        return config.getString(key)
    }

    fun getRawStringList(locale: String, key: String): List<String>? {
        return getRawStringList(plugin.name, locale, key)
    }

    fun getRawStringList(sourceId: String, locale: String, key: String): List<String>? {
        val resolvedLocale = resolveLocale(locale)
        val config = languageDataBySource[sourceId]?.get(resolvedLocale) ?: return null
        if (!config.isList(key)) {
            return null
        }
        return config.getStringList(key)
    }

    fun hasKey(player: Player?, key: String): Boolean {
        return hasKey(plugin.name, resolveLocale(player), key)
    }

    fun hasKey(locale: String, key: String): Boolean {
        return hasKey(plugin.name, locale, key)
    }

    fun hasKey(sourceId: String, player: Player?, key: String): Boolean {
        return hasKey(sourceId, resolveLocale(player), key)
    }

    fun hasKey(sourceId: String, locale: String, key: String): Boolean {
        return languageDataBySource[sourceId]?.get(resolveLocale(locale))?.contains(key) == true
    }

    fun getSectionKeys(player: Player?, key: String): Set<String> {
        val locale = resolveLocale(player)
        return languageDataBySource[plugin.name]?.get(locale)?.getConfigurationSection(key)?.getKeys(false) ?: emptySet()
    }

    fun isKeyMatch(title: String, key: String): Boolean {
        return isKeyMatch(plugin.name, title, key)
    }

    fun isKeyMatch(sourceId: String, title: String, key: String): Boolean {
        return buildTemplateRegex(sourceId, key)?.matches(title) == true
    }

    fun isKeyStartWith(title: String, key: String): Boolean {
        return isKeyStartWith(plugin.name, title, key)
    }

    fun isKeyStartWith(sourceId: String, title: String, key: String): Boolean {
        val regex = buildTemplateRegex(sourceId, key, anchoredEnd = false) ?: return false
        return regex.containsMatchIn(title)
    }

    fun deserialize(message: String): Component {
        return normalizeComponent(serializer.deserialize(message))
    }

    private fun normalizeComponent(component: Component): Component {
        return component
            .colorIfAbsent(NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false)
    }

    private fun resolveLocale(raw: String?): String {
        val normalized = normalizeLocale(raw)
        return when {
            availableLocales().contains(normalized) -> normalized
            availableLocales().contains(ConfigManager.getDefaultLanguage()) -> ConfigManager.getDefaultLanguage()
            availableLocales().contains("ja_jp") -> "ja_jp"
            availableLocales().isNotEmpty() -> availableLocales().first()
            else -> "ja_jp"
        }
    }

    private fun requireLanguage(sourceId: String, locale: String): YamlConfiguration {
        return languageDataBySource[sourceId]?.get(locale)
            ?: throw IllegalStateException("言語がロードされていません: source=$sourceId locale=$locale")
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

    private fun buildTemplateRegex(sourceId: String, key: String, anchoredEnd: Boolean = true): Regex? {
        val templates = languageDataBySource[sourceId].orEmpty().values.mapNotNull { it.getString(key) }.distinct()
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

    private fun loadAllLanguages(): Map<String, Map<String, YamlConfiguration>> {
        return registrations.values.associate { source ->
            source.sourceId to loadSourceLanguages(source)
        }
    }

    private fun discoverLocales(source: SourceRegistration): Set<String> {
        val locales = linkedSetOf<String>()
        locales += normalizeLocale(ConfigManager.getDefaultLanguage())
        locales += "ja_jp"
        locales += "en_us"

        locales += discoverResourceLanguageFiles(source.plugin, source.fileNames).keys

        return locales
    }

    private fun loadSourceLanguages(source: SourceRegistration): Map<String, YamlConfiguration> {
        val locales = discoverLocales(source)
        return locales.associateWith { locale -> loadLocale(source, locale) }
    }

    private fun loadLocale(source: SourceRegistration, locale: String): YamlConfiguration {
        val merged = YamlConfiguration()
        val mergedKeys = mutableMapOf<String, String>()

        discoverResourceLanguageFiles(source.plugin, source.fileNames)[locale]
            ?.toSortedMap()
            ?.values
            ?.forEach { file ->
                val config = YamlConfiguration.loadConfiguration(file.content.reader())
                mergeConfig(target = merged, source = config, mergedKeys = mergedKeys, sourceName = file.sourceName)
            }

        if (merged.getKeys(true).isEmpty()) {
            throw IllegalStateException("言語ファイルが見つかりません: $locale")
        }

        return merged
    }

    private fun availableLocales(): Set<String> {
        return languageDataBySource.values.flatMap { it.keys }.toCollection(linkedSetOf())
    }

    private fun discoverResourceLanguageFiles(sourcePlugin: JavaPlugin, allowedFileNames: Set<String> = emptySet()): Map<String, Map<String, DiscoveredLanguageFile>> {
        val jarFile = runCatching {
            File(sourcePlugin.javaClass.protectionDomain.codeSource.location.toURI())
        }.getOrNull() ?: return emptyMap()
        if (!jarFile.isFile) {
            return emptyMap()
        }

        val files = linkedMapOf<String, MutableMap<String, DiscoveredLanguageFile>>()
        JarFile(jarFile).use { jar ->
            jar.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("lang/") && it.name.endsWith(".yml") }
                .forEach { entry ->
                    val remainder = entry.name.removePrefix("lang/")
                    val locale = normalizeLocale(remainder.substringBefore('/'))
                    val fileName = remainder.substringAfter('/', missingDelimiterValue = "")
                    if (locale.isBlank() || fileName.isBlank()) {
                        return@forEach
                    }
                    if (allowedFileNames.isNotEmpty() && fileName !in allowedFileNames) {
                        return@forEach
                    }
                    jar.getInputStream(entry).use { input ->
                        val content = InputStreamReader(input, StandardCharsets.UTF_8).use { it.readText() }
                        files.getOrPut(locale) { linkedMapOf() }[fileName] = DiscoveredLanguageFile(
                            locale = locale,
                            fileName = fileName,
                            sourceName = "resource:${entry.name}",
                            content = content
                        )
                    }
                }
        }
        return files
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

    private fun validateYaml(content: String, source: String): Map<*, *> {
        val options = LoaderOptions().apply {
            isAllowDuplicateKeys = false
            maxAliasesForCollections = 50
        }
        val yaml = Yaml(SafeConstructor(options))
        val loaded = yaml.load<Any?>(content)
        if (loaded !is Map<*, *>) {
            throw IllegalStateException("YAMLのルートがMapではありません: $source")
        }
        return loaded
    }

    private fun validateStructure(expected: Any?, actual: Any?, path: String, errors: MutableList<String>) {
        val expectedType = nodeType(expected)
        val actualType = nodeType(actual)
        val displayPath = if (path.isBlank()) "<root>" else path

        if (expectedType != actualType) {
            errors += "$displayPath: 型不一致 expected=$expectedType actual=$actualType"
            return
        }

        if (expected is Map<*, *> && actual is Map<*, *>) {
            for ((key, expectedValue) in expected) {
                if (key !is String) {
                    continue
                }
                if (!actual.containsKey(key)) {
                    val missingPath = if (path.isBlank()) key else "$path.$key"
                    errors += "$missingPath: キー不足"
                    continue
                }
                val childPath = if (path.isBlank()) key else "$path.$key"
                validateStructure(expectedValue, actual[key], childPath, errors)
            }
        }
    }

    private fun collectLeafKeys(map: Map<*, *>, prefix: String = ""): List<String> {
        val keys = mutableListOf<String>()
        for ((rawKey, value) in map) {
            val key = rawKey as? String ?: continue
            val fullKey = if (prefix.isBlank()) key else "$prefix.$key"
            if (value is Map<*, *>) {
                keys += collectLeafKeys(value, fullKey)
            } else {
                keys += fullKey
            }
        }
        return keys
    }

    private fun nodeType(value: Any?): String {
        return when (value) {
            is Map<*, *> -> "Map"
            is List<*> -> "List"
            null -> "Null"
            else -> "Scalar"
        }
    }

    private fun featureFor(fileName: String, featureByFile: Map<String, String>, defaultFeature: String): String {
        return featureByFile[fileName]
            ?: featureByFile[fileName.substringBeforeLast('.', fileName)]
            ?: defaultFeature
    }

    private fun flattenErrors(errorsByFeature: Map<String, List<String>>): List<String> {
        return errorsByFeature.flatMap { (feature, messages) ->
            messages.map { "[$feature] $it" }
        }
    }

    private fun cloneValue(value: Any?): Any? {
        return if (value is List<*>) value.toList() else value
    }

    private fun defaultFeatureByFile(): Map<String, String> {
        return mapOf(
            "_common.yml" to "cc-system",
            "clock.yml" to "clock",
            "announce.yml" to "announce",
            "resource.yml" to "resource_world",
            "rental_area.yml" to "rental_area",
            "custom_messages.yml" to "custom_messages",
            "npc_messages.yml" to "npc_message"
        )
    }
}
