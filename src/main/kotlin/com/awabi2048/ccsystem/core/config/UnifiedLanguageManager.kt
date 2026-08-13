package com.awabi2048.ccsystem.core.config

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationCatalog
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * コンパイル済みカタログに対するlocale解決と表示変換を担当します。
 * 言語データは不変なので、ファイル探索・解析・reloadという概念を持ちません。
 */
class UnifiedLanguageManager(@Suppress("UNUSED_PARAMETER") plugin: JavaPlugin) {
    data class ValidationResult(
        val errors: List<String>,
        val errorsByFeature: Map<String, List<String>> = emptyMap(),
    ) {
        val isValid: Boolean = errors.isEmpty()
        fun errorsFor(feature: String): List<String> = errorsByFeature[feature].orEmpty()
    }

    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val plainSerializer = PlainTextComponentSerializer.plainText()

    fun load() {
        val validation = validate()
        check(validation.isValid) {
            "埋込ローカライズカタログの検証に失敗しました:\n" + validation.errors.joinToString("\n") { "- $it" }
        }
    }

    fun reload() = load()

    fun validate(): ValidationResult {
        val result = EmbeddedLocalizationCatalog.validate()
        return ValidationResult(result.errors, result.errorsByDomain)
    }

    fun resolveLocale(player: Player?): String =
        if (player == null) resolveLocale(ConfigManager.getDefaultLanguage()) else resolveLocale(player.locale().toString())

    fun resolveLocale(sender: CommandSender?): String = resolveLocale(sender as? Player)

    fun getSupportedLanguages(): Set<String> = EmbeddedLocalizationCatalog.locales

    fun getDefaultLanguage(): String = resolveLocale(ConfigManager.getDefaultLanguage())

    fun getString(player: Player?, key: String): String = getString(player, key, emptyMap())

    fun getString(player: Player?, key: String, placeholders: Map<String, Any>): String {
        val locale = resolveLocale(player)
        val value = EmbeddedLocalizationCatalog.value(locale, key)
        require(value is EmbeddedLocalizedValue.Text) {
            "言語キーが見つからないか型が不正です: locale=$locale key=$key expected=String"
        }
        return applyPlaceholders(value.value, placeholders)
    }

    fun getStringList(player: Player?, key: String): List<String> = getStringList(player, key, emptyMap())

    fun getStringList(player: Player?, key: String, placeholders: Map<String, Any>): List<String> {
        val locale = resolveLocale(player)
        val value = EmbeddedLocalizationCatalog.value(locale, key)
        require(value is EmbeddedLocalizedValue.TextList) {
            "言語キーが見つからないか型が不正です: locale=$locale key=$key expected=List"
        }
        return value.values.map { applyPlaceholders(it, placeholders) }
    }

    fun getComponent(player: Player?, key: String): Component = getComponent(player, key, emptyMap())

    fun getComponent(player: Player?, key: String, placeholders: Map<String, Any>): Component =
        normalizeComponent(serializer.deserialize(getString(player, key, placeholders)))

    fun getComponentList(player: Player?, key: String): List<Component> = getComponentList(player, key, emptyMap())

    fun getComponentList(player: Player?, key: String, placeholders: Map<String, Any>): List<Component> =
        getStringList(player, key, placeholders).map { normalizeComponent(serializer.deserialize(it)) }

    fun getRawString(locale: String, key: String): String? =
        (EmbeddedLocalizationCatalog.value(resolveLocale(locale), key) as? EmbeddedLocalizedValue.Text)?.value

    fun getRawStringList(locale: String, key: String): List<String>? =
        (EmbeddedLocalizationCatalog.value(resolveLocale(locale), key) as? EmbeddedLocalizedValue.TextList)?.values

    fun hasKey(player: Player?, key: String): Boolean = hasKey(resolveLocale(player), key)

    fun hasKey(locale: String, key: String): Boolean = EmbeddedLocalizationCatalog.contains(resolveLocale(locale), key)

    fun getSectionKeys(player: Player?, key: String): Set<String> =
        EmbeddedLocalizationCatalog.childKeys(resolveLocale(player), key)

    fun isKeyMatch(title: String, key: String): Boolean = buildTemplateRegex(key)?.matches(title) == true

    fun isKeyStartWith(title: String, key: String): Boolean =
        buildTemplateRegex(key, anchoredEnd = false)?.containsMatchIn(title) == true

    fun deserialize(message: String): Component = normalizeComponent(serializer.deserialize(message))

    private fun normalizeComponent(component: Component): Component = component
        .colorIfAbsent(NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false)

    private fun resolveLocale(raw: String?): String {
        val normalized = normalizeLocale(raw)
        val locales = EmbeddedLocalizationCatalog.locales
        return when {
            normalized in locales -> normalized
            normalizeLocale(ConfigManager.getDefaultLanguage()) in locales -> normalizeLocale(ConfigManager.getDefaultLanguage())
            "ja_jp" in locales -> "ja_jp"
            else -> locales.first()
        }
    }

    private fun normalizeLocale(raw: String?): String {
        val normalized = raw?.trim()?.lowercase()?.replace('-', '_').orEmpty()
        return when (normalized) {
            "", "ja" -> "ja_jp"
            "en" -> "en_us"
            else -> normalized
        }
    }

    private fun applyPlaceholders(template: String, placeholders: Map<String, Any>): String =
        placeholders.entries.fold(template) { result, (key, value) ->
            result.replace("{$key}", value.toString()).replace("%$key%", value.toString())
        }

    private fun buildTemplateRegex(key: String, anchoredEnd: Boolean = true): Regex? {
        val templates = EmbeddedLocalizationCatalog.locales.mapNotNull { getRawString(it, key) }.distinct()
        if (templates.isEmpty()) return null
        val union = templates.joinToString("|") { raw ->
            val plain = plainSerializer.serialize(serializer.deserialize(raw))
            val escaped = Regex.escape(plain)
                .replace(Regex("\\\\\\{[^}]+\\\\\\}"), ".*")
                .replace(Regex("%[^%]+%"), ".*")
            if (anchoredEnd) "^$escaped$" else "^$escaped"
        }
        return Regex(union)
    }
}
