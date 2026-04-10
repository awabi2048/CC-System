package com.awabi2048.ccsystem.core.config

import com.awabi2048.ccsystem.CCSystem
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object LanguageManager {
    private lateinit var unified: UnifiedLanguageManager

    fun load() {
        if (!::unified.isInitialized) {
            unified = UnifiedLanguageManager(CCSystem.instance)
        }
        unified.load()
    }

    fun reload() {
        load()
    }

    fun validate(): UnifiedLanguageManager.ValidationResult {
        if (!::unified.isInitialized) {
            unified = UnifiedLanguageManager(CCSystem.instance)
        }
        return unified.validate()
    }

    fun getSupportedLanguages(): Set<String> {
        return unified.getSupportedLanguages()
    }

    fun getPlayerLanguageCode(player: Player?): String {
        return unified.resolveLocale(player)
    }

    fun getMessage(player: Player?, key: String, vararg placeholders: Pair<String, String>): Component {
        val resolved = getRawString(player, key, *placeholders)
        val prefixed = if (key == "prefix") resolved else getRawString(player, "prefix") + resolved
        return unified.deserialize(prefixed)
    }

    fun getMessageWithoutPrefix(player: Player?, key: String, vararg placeholders: Pair<String, String>): Component {
        return unified.deserialize(getRawString(player, key, *placeholders))
    }

    fun getRawString(player: Player?, key: String): String {
        return getRawString(player, key, *emptyArray())
    }

    fun getRawString(player: Player?, key: String, vararg placeholders: Pair<String, String>): String {
        return unified.getString(player, key, placeholders.toMap())
    }

    fun getStringList(player: Player?, key: String): List<String> {
        return unified.getStringList(player, key)
    }

    fun getStringListWithPlaceholders(player: Player?, key: String, vararg placeholders: Pair<String, String>): List<String> {
        return unified.getStringList(player, key, placeholders.toMap())
    }

    fun deserializeLegacy(message: String): Component {
        return unified.deserialize(message)
    }

    fun getCustomMessageStyle(player: Player?, id: String): String {
        return unified.getString(player, "custom_messages.$id.style").lowercase()
    }

    fun getCustomMessageTexts(player: Player?, id: String): List<String> {
        return unified.getStringList(player, "custom_messages.$id.texts")
    }

    fun getCustomMessageIds(player: Player?): Set<String> {
        return unified.getSectionKeys(player, "custom_messages")
    }

    fun getUnified(): UnifiedLanguageManager {
        return unified
    }
}
