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
    private val dataDir = File(CCSystem.instance.dataFolder, "userdata")

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

    /**
     * データを保存
     */
    fun save(uuid: UUID) {
        val data = playerData[uuid] ?: return
        try {
            data.save(getFile(uuid))
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