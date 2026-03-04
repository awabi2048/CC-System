package com.awabi2048.ccsystem.core.data

import com.awabi2048.ccsystem.CCSystem
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

/**
 * プレイヤーデータ管理マネージャー
 */
object PlayerDataManager {
    private val playerData = mutableMapOf<UUID, YamlConfiguration>()
    private val dataDir = File(CCSystem.instance.dataFolder, "playerdata")

    /**
     * プレイヤーデータをロードする
     */
    fun load() {
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
    }

    private fun getFile(uuid: UUID): File {
        return File(dataDir, "$uuid.yml")
    }

    private fun getData(uuid: UUID): YamlConfiguration {
        return playerData.getOrPut(uuid) {
            val file = getFile(uuid)
            if (file.exists()) {
                YamlConfiguration.loadConfiguration(file)
            } else {
                YamlConfiguration()
            }
        }
    }

    private fun getDataIfFileExists(uuid: UUID): YamlConfiguration? {
        val file = getFile(uuid)
        if (!file.exists()) {
            return null
        }
        return playerData.getOrPut(uuid) {
            YamlConfiguration.loadConfiguration(file)
        }
    }

    fun hasPlayerDataFile(uuid: UUID): Boolean {
        return getFile(uuid).exists()
    }

    /**
     * 文字列データを取得
     */
    fun getString(uuid: UUID, key: String, default: String? = null): String? {
        return getData(uuid).getString(key, default)
    }

    /**
     * 整数データを取得
     */
    fun getInt(uuid: UUID, key: String, default: Int = 0): Int {
        return getData(uuid).getInt(key, default)
    }

    /**
     * 真偽値データを取得
     */
    fun getBoolean(uuid: UUID, key: String, default: Boolean = false): Boolean {
        return getData(uuid).getBoolean(key, default)
    }

    /**
     * データを設定
     */
    fun set(uuid: UUID, key: String, value: Any?) {
        val data = getData(uuid)
        data.set(key, value)
        save(uuid)
    }

    fun setIfDataFileExists(uuid: UUID, key: String, value: Any?): Boolean {
        val data = getDataIfFileExists(uuid) ?: return false
        data.set(key, value)
        save(uuid)
        return true
    }

    /**
     * データを保存
     */
    fun save(uuid: UUID) {
        val data = playerData[uuid] ?: return
        val file = getFile(uuid)
        if (data.getKeys(true).isEmpty()) {
            if (file.exists()) {
                file.delete()
            }
            return
        }
        try {
            file.parentFile?.mkdirs()
            data.save(file)
        } catch (e: Exception) {
            CCSystem.instance.logger.severe("Failed to save player data for $uuid: ${e.message}")
        }
    }

    /**
     * プレイヤーデータをアンロード
     */
    fun unload(uuid: UUID) {
        save(uuid)
        playerData.remove(uuid)
    }
}
