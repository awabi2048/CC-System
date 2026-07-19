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
import java.time.MonthDay
import java.util.logging.Logger
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration

class SeasonServiceImpl @JvmOverloads constructor(
    private val clock: SharedClockService,
    private val storageFile: File,
    private val logger: Logger,
    private val settingsFile: File? = null
) : SeasonService {
    @Volatile
    private var boundaries: SeasonBoundaries = loadBoundaries()

    @Volatile
    private var override: SeasonOverride? = loadOverride()

    override fun currentSeason(): Season = override?.season ?: seasonAt(clock.currentDate())

    override fun seasonAt(date: LocalDate): Season = boundaries.resolve(MonthDay.from(date))

    fun reload() {
        boundaries = loadBoundaries()
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

    private fun loadBoundaries(): SeasonBoundaries {
        val file = settingsFile?.takeIf(File::isFile) ?: return SeasonBoundaries.DEFAULT
        val config = YamlConfiguration.loadConfiguration(file)
        require(config.getInt("config_version", -1) == 1) {
            "config/season.yml.config_version must be the integer 1"
        }
        return SeasonBoundaries(
            spring = parseBoundary(config, "spring"),
            summer = parseBoundary(config, "summer"),
            autumn = parseBoundary(config, "autumn"),
            winter = parseBoundary(config, "winter")
        )
    }

    private fun parseBoundary(config: YamlConfiguration, id: String): MonthDay {
        val raw = config.getString("boundaries.$id")?.takeIf(String::isNotBlank)
            ?: error("config/season.yml.boundaries.$id must not be blank")
        require(raw.matches(Regex("""\d{2}-\d{2}"""))) {
            "config/season.yml.boundaries.$id must use MM-dd"
        }
        return runCatching { MonthDay.parse("--$raw") }
            .getOrElse {
                throw IllegalArgumentException("config/season.yml.boundaries.$id is invalid: $raw", it)
            }
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

private data class SeasonBoundaries(
    val spring: MonthDay,
    val summer: MonthDay,
    val autumn: MonthDay,
    val winter: MonthDay
) {
    init {
        require(spring < summer && summer < autumn && autumn < winter) {
            "config/season.yml boundaries must be ordered spring < summer < autumn < winter"
        }
    }

    fun resolve(day: MonthDay): Season = when {
        day >= spring && day < summer -> Season.SPRING
        day >= summer && day < autumn -> Season.SUMMER
        day >= autumn && day < winter -> Season.AUTUMN
        else -> Season.WINTER
    }

    companion object {
        val DEFAULT = SeasonBoundaries(
            MonthDay.of(3, 1),
            MonthDay.of(6, 1),
            MonthDay.of(9, 1),
            MonthDay.of(12, 1)
        )
    }
}
