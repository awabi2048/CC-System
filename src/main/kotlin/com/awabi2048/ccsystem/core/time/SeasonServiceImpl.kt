package com.awabi2048.ccsystem.core.time

import com.awabi2048.ccsystem.api.time.Season
import com.awabi2048.ccsystem.api.time.SeasonContext
import com.awabi2048.ccsystem.api.time.SeasonOverride
import com.awabi2048.ccsystem.api.time.SeasonService
import com.awabi2048.ccsystem.api.time.SharedClockService
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.logging.Logger
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration

class SeasonServiceImpl(
    private val clock: SharedClockService,
    private val storageFile: File,
    private val logger: Logger
) : SeasonService {
    @Volatile
    private var override: SeasonOverride? = loadOverride()

    override fun currentSeason(): Season = override?.season ?: seasonAt(clock.currentDate())

    override fun seasonAt(date: LocalDate): Season = when (date.monthValue) {
        in 3..5 -> Season.SPRING
        in 6..8 -> Season.SUMMER
        in 9..11 -> Season.AUTUMN
        else -> Season.WINTER
    }

    override fun currentContext(world: World): SeasonContext {
        val currentOverride = override
        val date = clock.currentDate()
        return SeasonContext(
            season = currentOverride?.season ?: seasonAt(date),
            date = date,
            dayKey = clock.currentDayKey(),
            zoneId = clock.zoneId,
            overridden = currentOverride != null
        )
    }

    override fun overrideState(): SeasonOverride? = override

    @Synchronized
    override fun setOverride(season: Season, actor: String) {
        val next = SeasonOverride(season, actor, clock.now().toInstant().toString())
        persist(next)
        override = next
        logger.info("[CC-System][Season] 管理者上書きを開始しました: season=$season actor=$actor")
    }

    @Synchronized
    override fun clearOverride(actor: String): Boolean {
        if (override == null) return false
        persist(null)
        override = null
        logger.info("[CC-System][Season] 管理者上書きを解除しました: actor=$actor")
        return true
    }

    private fun loadOverride(): SeasonOverride? {
        if (!storageFile.isFile) return null
        val yaml = YamlConfiguration.loadConfiguration(storageFile)
        if (!yaml.getBoolean("active", false)) return null
        val rawSeason = yaml.getString("season") ?: return invalidStoredOverride("seasonがありません")
        val season = runCatching { Season.valueOf(rawSeason.uppercase()) }.getOrNull()
            ?: return invalidStoredOverride("seasonが不正です: $rawSeason")
        val actor = yaml.getString("actor")?.takeIf { it.isNotBlank() }
            ?: return invalidStoredOverride("actorがありません")
        val changedAt = yaml.getString("changed_at")?.takeIf { it.isNotBlank() }
            ?: return invalidStoredOverride("changed_atがありません")
        return SeasonOverride(season, actor, changedAt)
    }

    private fun invalidStoredOverride(reason: String): SeasonOverride? {
        logger.warning("[CC-System][Season] 保存済み上書きを無効化しました: $reason")
        return null
    }

    private fun persist(value: SeasonOverride?) {
        storageFile.parentFile.mkdirs()
        val yaml = YamlConfiguration()
        yaml.set("schema_version", 1)
        yaml.set("active", value != null)
        yaml.set("season", value?.season?.name)
        yaml.set("actor", value?.actor)
        yaml.set("changed_at", value?.changedAt)

        val temporary = File(storageFile.parentFile, "${storageFile.name}.tmp")
        yaml.save(temporary)
        try {
            Files.move(
                temporary.toPath(),
                storageFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
