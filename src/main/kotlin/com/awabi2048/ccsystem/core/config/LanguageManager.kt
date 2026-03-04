package com.awabi2048.ccsystem.core.config

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

/**
 * 言語管理マネージャー
 */
object LanguageManager {
    private val langFiles = mutableMapOf<String, YamlConfiguration>()

    /**
     * 言語ファイルをロードする
     */
    fun load() {
        langFiles.clear()
        val langDir = File(CCSystem.instance.dataFolder, "lang")
        if (!langDir.exists()) {
            langDir.mkdirs()
        }

        // デフォルト言語ファイルを確認
        val defaultLangs = listOf("ja_jp", "en_us")
        defaultLangs.forEach { lang ->
            val file = File(langDir, "$lang.yml")
            if (!file.exists()) {
                CCSystem.instance.saveResource("lang/$lang.yml", false)
            } else {
                val config = YamlConfiguration.loadConfiguration(file)
                val resourceStream = CCSystem.instance.getResource("lang/$lang.yml")
                if (resourceStream != null) {
                    val reader =
                        java.io.InputStreamReader(
                            resourceStream,
                            java.nio.charset.StandardCharsets.UTF_8
                        )
                    val defaultConfig = YamlConfiguration.loadConfiguration(reader)
                    var changed = false
                    for (key in defaultConfig.getKeys(true)) {
                        if (!config.contains(key)) {
                            config.set(key, defaultConfig.get(key))
                            changed = true
                        }
                    }
                    if (changed) {
                        try {
                            config.save(file)
                        } catch (e: Exception) {
                            CCSystem.instance.logger.warning(
                                "Failed to save updated language file: ${file.name}"
                            )
                        }
                    }
                }
            }
        }

        // すべての言語ファイルを読み込み
        langDir.listFiles()?.forEach { file ->
            if (file.extension == "yml") {
                val config = YamlConfiguration.loadConfiguration(file)
                langFiles[file.nameWithoutExtension.lowercase()] = config
            }
        }
    }

    /**
     * プレイヤーの言語を設定
     */
    fun setPlayerLang(player: Player, lang: String) {
        val normalizedLang = lang.lowercase()
        val defaultLang = getDefaultLanguageCode()
        if (normalizedLang == defaultLang) {
            PlayerDataManager.setIfDataFileExists(player.uniqueId, "lang", null)
            return
        }
        PlayerDataManager.set(player.uniqueId, "lang", normalizedLang)
    }

    private fun getDefaultLanguageCode(): String {
        val configured = ConfigManager.getDefaultLanguage().lowercase()
        return when {
            langFiles.containsKey(configured) -> configured
            langFiles.containsKey("ja_jp") -> "ja_jp"
            langFiles.isNotEmpty() -> langFiles.keys.first()
            else -> "ja_jp"
        }
    }

    private fun getPlayerLanguageCode(player: Player?): String {
        val defaultLang = getDefaultLanguageCode()
        if (player == null) {
            return defaultLang
        }
        return PlayerDataManager.getString(player.uniqueId, "lang", defaultLang)
            ?.lowercase()
            ?: defaultLang
    }

    /**
     * メッセージを取得（Component形式）
     */
    fun getMessage(
        player: Player?,
        key: String,
        vararg placeholders: Pair<String, String>
    ): Component {
        val defaultLang = getDefaultLanguageCode()
        val lang = getPlayerLanguageCode(player)
        val config = langFiles[lang] ?: langFiles[defaultLang]

        var message = config?.getString(key) ?: key
        val prefix = config?.getString("prefix") ?: ""

        if (key != "prefix") {
            message = prefix + message
        }

        placeholders.forEach { (placeholder, value) ->
            message = message.replace("%$placeholder%", value)
        }

        return LegacyComponentSerializer.legacyAmpersand().deserialize(message)
    }

    /**
     * メッセージを取得（Component形式、prefixなし）
     */
    fun getMessageWithoutPrefix(
        player: Player?,
        key: String,
        vararg placeholders: Pair<String, String>
    ): Component {
        val defaultLang = getDefaultLanguageCode()
        val lang = getPlayerLanguageCode(player)
        val config = langFiles[lang] ?: langFiles[defaultLang]

        var message = config?.getString(key) ?: key
        placeholders.forEach { (placeholder, value) ->
            message = message.replace("%$placeholder%", value)
        }

        return LegacyComponentSerializer.legacyAmpersand().deserialize(message)
    }

    /**
     * 生の文字列メッセージを取得
     */
    fun getRawString(player: Player?, key: String): String {
        return getRawString(player, key, *emptyArray())
    }

    fun getRawString(
        player: Player?,
        key: String,
        vararg placeholders: Pair<String, String>
    ): String {
        val defaultLang = getDefaultLanguageCode()
        val lang = getPlayerLanguageCode(player)
        val config = langFiles[lang] ?: langFiles[defaultLang]

        var message = config?.getString(key) ?: key
        placeholders.forEach { (placeholder, value) ->
            message = message.replace("%$placeholder%", value)
        }
        return message
    }

    /**
     * 文字列リストを取得
     */
    fun getStringList(player: Player?, key: String): List<String> {
        val defaultLang = getDefaultLanguageCode()
        val lang = getPlayerLanguageCode(player)
        val config = langFiles[lang] ?: langFiles[defaultLang]
        return config?.getStringList(key) ?: emptyList()
    }

    fun getStringListWithPlaceholders(
        player: Player?,
        key: String,
        vararg placeholders: Pair<String, String>
    ): List<String> {
        val list = getStringList(player, key)
        return list.map { line ->
            var result = line
            placeholders.forEach { (placeholder, value) ->
                result = result.replace("%$placeholder%", value)
            }
            result
        }
    }

    fun deserializeLegacy(message: String): Component {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message)
    }

    /**
     * カスタムメッセージのスタイルを取得
     */
    fun getCustomMessageStyle(player: Player?, id: String): String {
        val key = "custom_messages.$id.style"
        val style = getRawString(player, key) ?: "batch"
        return style.lowercase()
    }

    /**
     * カスタムメッセージのテキストリストを取得
     */
    fun getCustomMessageTexts(player: Player?, id: String): List<String> {
        val key = "custom_messages.$id.texts"
        return getStringList(player, key)
    }

    /**
     * カスタムメッセージIDの一覧を取得
     */
    fun getCustomMessageIds(player: Player?): Set<String> {
        val defaultLang = getDefaultLanguageCode()
        val lang = getPlayerLanguageCode(player)
        val config = langFiles[lang] ?: langFiles[defaultLang]
        val customMessagesSection = config?.getConfigurationSection("custom_messages")
        return customMessagesSection?.getKeys(false) ?: emptySet()
    }
}
