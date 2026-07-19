package com.awabi2048.ccsystem.core.time

import com.awabi2048.ccsystem.api.time.SharedClockService
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import org.bukkit.configuration.file.YamlConfiguration

class SharedClockServiceImpl @JvmOverloads constructor(
    private val clock: Clock = Clock.systemUTC(),
    private val settingsFile: File? = null
) : SharedClockService {
    @Volatile
    override var zoneId: ZoneId = loadZoneId()
        private set

    override fun now(): ZonedDateTime = ZonedDateTime.now(clock.withZone(zoneId))

    fun reload() {
        zoneId = loadZoneId()
    }

    private fun loadZoneId(): ZoneId {
        val file = settingsFile?.takeIf(File::isFile) ?: return TOKYO_ZONE
        val config = YamlConfiguration.loadConfiguration(file)
        require(config.getInt("config_version", -1) == 1) {
            "config/season.yml.config_version must be the integer 1"
        }
        require(config.get("enabled") is Boolean) {
            "config/season.yml.enabled must be a boolean"
        }
        require(config.getString("mode") == "AUTO") {
            "config/season.yml.mode must be AUTO; forced state is stored separately"
        }
        val timezone = config.getString("timezone")?.takeIf(String::isNotBlank)
            ?: error("config/season.yml.timezone must not be blank")
        return runCatching { ZoneId.of(timezone) }
            .getOrElse { throw IllegalArgumentException("config/season.yml.timezone is invalid: $timezone", it) }
    }

    companion object {
        @JvmField
        val TOKYO_ZONE: ZoneId = ZoneId.of("Asia/Tokyo")
    }
}
