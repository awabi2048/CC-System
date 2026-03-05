package com.awabi2048.ccsystem.features.clock.manager

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.LinkedHashMap
import java.util.Locale
import java.util.UUID

object ClockManager {
    enum class ClockType {
        TIMER,
        ALARM
    }

    data class ActiveClock(
        val id: String,
        val type: ClockType,
        val title: String,
        val argLabel: String,
        val startEpochMillis: Long,
        val triggerEpochMillis: Long,
        val forceBar: Boolean,
        val setterName: String
    )

    const val PLAYER_DATA_CLOCK_BAR = "clock_bar"

    private val activeClocks = LinkedHashMap<String, ActiveClock>()
    private val bossBarsByClock = mutableMapOf<String, MutableMap<UUID, BossBar>>()
    private val alarmMinuteMarkerByClock = mutableMapOf<String, Long>()
    private val serverZoneId: ZoneId = ZoneId.systemDefault()
    private val alarmDisplayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val alarmDisplayFormatterEn: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    private var monitorTask: BukkitTask? = null
    private lateinit var dataFile: File
    private lateinit var config: YamlConfiguration

    fun load() {
        unload()

        dataFile = File(CCSystem.instance.dataFolder, "data/clock/clock_data.yml")
        if (!dataFile.exists()) {
            dataFile.parentFile?.mkdirs()
            dataFile.createNewFile()
        }

        config = YamlConfiguration.loadConfiguration(dataFile)
        activeClocks.clear()

        val now = System.currentTimeMillis()
        val expiredOnStartup = mutableListOf<ActiveClock>()
        val section = config.getConfigurationSection("clocks")

        if (section != null) {
            for (id in section.getKeys(false)) {
                val path = "clocks.$id"
                val type = runCatching {
                    ClockType.valueOf(config.getString("$path.type") ?: "")
                }.getOrNull() ?: continue

                val title = config.getString("$path.title")?.trim().orEmpty()
                if (title.isEmpty()) {
                    continue
                }

                val argLabel = config.getString("$path.arg_label")?.trim().orEmpty()
                val startEpochMillis = config.getLong("$path.start_epoch_millis", -1L)
                val triggerEpochMillis = config.getLong("$path.trigger_epoch_millis", -1L)
                val forceBar = config.getBoolean("$path.force_bar", false)
                val setterName = config.getString("$path.setter_name") ?: "unknown"

                if (startEpochMillis <= 0L || triggerEpochMillis <= 0L || triggerEpochMillis <= startEpochMillis) {
                    continue
                }

                val clock = ActiveClock(
                    id = id,
                    type = type,
                    title = title,
                    argLabel = argLabel,
                    startEpochMillis = startEpochMillis,
                    triggerEpochMillis = triggerEpochMillis,
                    forceBar = forceBar,
                    setterName = setterName
                )

                if (clock.triggerEpochMillis <= now) {
                    expiredOnStartup.add(clock)
                } else if (hasTitleConflict(clock.title)) {
                    CCSystem.instance.logger.warning("[Clock] タイトル重複のため復元をスキップしました: ${clock.title}")
                } else {
                    activeClocks[clock.id] = clock
                }
            }
        }

        saveAll()
        startMonitorTask()

        if (expiredOnStartup.isNotEmpty()) {
            expiredOnStartup.forEach { notifyClockCompleted(it) }
        }
    }

    fun unload() {
        monitorTask?.cancel()
        monitorTask = null

        bossBarsByClock.values.forEach { bars ->
            bars.values.forEach { it.removeAll() }
            bars.clear()
        }
        bossBarsByClock.clear()
        alarmMinuteMarkerByClock.clear()

        if (::config.isInitialized) {
            saveAll()
        }

        activeClocks.clear()
    }

    fun createTimer(
        title: String,
        durationMillis: Long,
        durationLabel: String,
        forceBar: Boolean,
        setterName: String
    ): ActiveClock? {
        if (hasTitleConflict(title)) {
            return null
        }
        val now = System.currentTimeMillis()
        val clock = ActiveClock(
            id = generateId(),
            type = ClockType.TIMER,
            title = title,
            argLabel = durationLabel,
            startEpochMillis = now,
            triggerEpochMillis = now + durationMillis,
            forceBar = forceBar,
            setterName = setterName
        )
        activeClocks[clock.id] = clock
        saveAll()
        notifyClockStarted(clock)
        return clock
    }

    fun createAlarm(
        title: String,
        triggerEpochMillis: Long,
        alarmLabel: String,
        forceBar: Boolean,
        setterName: String
    ): ActiveClock? {
        if (hasTitleConflict(title)) {
            return null
        }
        val now = System.currentTimeMillis()
        val clock = ActiveClock(
            id = generateId(),
            type = ClockType.ALARM,
            title = title,
            argLabel = alarmLabel,
            startEpochMillis = now,
            triggerEpochMillis = triggerEpochMillis,
            forceBar = forceBar,
            setterName = setterName
        )
        activeClocks[clock.id] = clock
        saveAll()
        notifyClockStarted(clock)
        return clock
    }

    fun hasTitleConflict(title: String): Boolean {
        return findByTitle(title) != null
    }

    fun cancel(id: String): ActiveClock? {
        val removed = activeClocks.remove(id) ?: return null
        removeBossBars(id)
        saveAll()
        return removed
    }

    fun cancelByTitle(title: String): ActiveClock? {
        val target = findByTitle(title) ?: return null
        activeClocks.remove(target.id)
        removeBossBars(target.id)
        saveAll()
        return target
    }

    private fun findByTitle(title: String): ActiveClock? {
        val normalized = title.trim()
        if (normalized.isEmpty()) {
            return null
        }
        return activeClocks.values.firstOrNull { it.title.equals(normalized, ignoreCase = true) }
    }

    fun getActiveClocks(): List<ActiveClock> {
        return activeClocks.values.sortedBy { it.triggerEpochMillis }
    }

    fun getRemainingMillis(clock: ActiveClock): Long {
        return (clock.triggerEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun formatAlarmLabelFromEpochMillis(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(serverZoneId)
            .toLocalTime()
            .format(alarmDisplayFormatter)
    }

    private fun startMonitorTask() {
        monitorTask?.cancel()
        monitorTask = Bukkit.getScheduler().runTaskTimer(
            CCSystem.instance,
            Runnable { tick() },
            20L,
            20L
        )
    }

    private fun tick() {
        if (activeClocks.isEmpty()) {
            if (bossBarsByClock.isNotEmpty()) {
                bossBarsByClock.keys.toList().forEach { removeBossBars(it) }
            }
            return
        }

        val now = System.currentTimeMillis()
        val completed = mutableListOf<ActiveClock>()

        for (clock in activeClocks.values.toList()) {
            if (now >= clock.triggerEpochMillis) {
                completed.add(clock)
            } else {
                updateBossBars(clock, now)
            }
        }

        if (completed.isNotEmpty()) {
            completed.forEach {
                activeClocks.remove(it.id)
                removeBossBars(it.id)
            }
            saveAll()
            completed.forEach { notifyClockCompleted(it) }
        }

        val aliveIds = activeClocks.keys
        bossBarsByClock.keys.toList().filter { it !in aliveIds }.forEach { removeBossBars(it) }
    }

    private fun updateBossBars(clock: ActiveClock, now: Long) {
        val totalMillis = (clock.triggerEpochMillis - clock.startEpochMillis).coerceAtLeast(1L)
        val remainingMillis = (clock.triggerEpochMillis - now).coerceAtLeast(0L)
        val progress = (remainingMillis.toDouble() / totalMillis.toDouble()).coerceIn(0.0, 1.0)

        val viewers = Bukkit.getOnlinePlayers().filter { shouldShowBarToPlayer(it, clock.forceBar) }
        val viewerIds = viewers.mapTo(mutableSetOf()) { it.uniqueId }
        val bars = bossBarsByClock.getOrPut(clock.id) { mutableMapOf() }

        val shouldRefreshAlarm = if (clock.type == ClockType.ALARM) {
            val minuteMarker = (remainingMillis / 60000L).coerceAtLeast(0L)
            val previous = alarmMinuteMarkerByClock[clock.id]
            if (previous == null || previous != minuteMarker) {
                alarmMinuteMarkerByClock[clock.id] = minuteMarker
                true
            } else {
                false
            }
        } else {
            true
        }

        for (player in viewers) {
            val existing = bars[player.uniqueId]
            val bar = existing ?: run {
                val color = if (clock.type == ClockType.TIMER) BarColor.BLUE else BarColor.GREEN
                Bukkit.createBossBar(buildBossBarTitle(clock, remainingMillis, player), color, BarStyle.SOLID).also {
                    it.addPlayer(player)
                    bars[player.uniqueId] = it
                }
            }

            if (existing != null && !bar.players.contains(player)) {
                bar.addPlayer(player)
            }

            if (clock.type == ClockType.TIMER || shouldRefreshAlarm || existing == null) {
                bar.progress = progress
                bar.setTitle(buildBossBarTitle(clock, remainingMillis, player))
            }
        }

        for ((uuid, bar) in bars.toMap()) {
            if (uuid !in viewerIds) {
                bar.removeAll()
                bars.remove(uuid)
            }
        }

        if (bars.isEmpty()) {
            bossBarsByClock.remove(clock.id)
            if (clock.type == ClockType.ALARM) {
                alarmMinuteMarkerByClock.remove(clock.id)
            }
        }
    }

    private fun shouldShowBarToPlayer(player: Player, forceBar: Boolean): Boolean {
        if (forceBar) {
            return true
        }
        return PlayerDataManager.getBoolean(player.uniqueId, PLAYER_DATA_CLOCK_BAR, true)
    }

    private fun buildBossBarTitle(clock: ActiveClock, remainingMillis: Long, player: Player): String {
        return if (clock.type == ClockType.TIMER) {
            val remainingLabel = formatDurationForPlayer(player, remainingMillis, includeSeconds = true)
            val remainingWord = if (isEnglish(player)) "remaining" else "残り"
            "${clock.title} §7$remainingWord §a$remainingLabel".take(64)
        } else {
            val alarmTime = formatAlarmTimeForPlayer(clock.triggerEpochMillis, player)
            val afterLabel = formatDurationForPlayer(player, remainingMillis, includeSeconds = false)
            val afterPhrase = if (isEnglish(player)) {
                "in $afterLabel"
            } else {
                "${afterLabel}後"
            }
            "${clock.title} §7$alarmTime ($afterPhrase)".take(64)
        }
    }

    private fun removeBossBars(id: String) {
        bossBarsByClock.remove(id)?.values?.forEach { it.removeAll() }
        alarmMinuteMarkerByClock.remove(id)
    }

    private fun saveAll() {
        if (!::config.isInitialized) {
            return
        }

        config.set("clocks", null)
        for ((id, clock) in activeClocks) {
            val path = "clocks.$id"
            config.set("$path.type", clock.type.name)
            config.set("$path.title", clock.title)
            config.set("$path.arg_label", clock.argLabel)
            config.set("$path.start_epoch_millis", clock.startEpochMillis)
            config.set("$path.trigger_epoch_millis", clock.triggerEpochMillis)
            config.set("$path.force_bar", clock.forceBar)
            config.set("$path.setter_name", clock.setterName)
        }
        config.save(dataFile)
    }

    private fun notifyClockStarted(clock: ActiveClock) {
        for (player in Bukkit.getOnlinePlayers()) {
            val message = when (clock.type) {
                ClockType.TIMER -> LanguageManager.getMessage(
                    player,
                    "clock.timer.started",
                    "duration" to clock.argLabel,
                    "title" to clock.title,
                    "id" to clock.id
                )

                ClockType.ALARM -> LanguageManager.getMessage(
                    player,
                    "clock.alarm.started",
                    "time" to clock.argLabel,
                    "title" to clock.title,
                    "id" to clock.id
                )
            }
            player.sendMessage(message)
        }
    }

    private fun notifyClockCompleted(clock: ActiveClock) {
        for (player in Bukkit.getOnlinePlayers()) {
            val message = when (clock.type) {
                ClockType.TIMER -> LanguageManager.getMessage(
                    player,
                    "clock.timer.completed",
                    "duration" to clock.argLabel,
                    "title" to clock.title,
                    "id" to clock.id
                )

                ClockType.ALARM -> LanguageManager.getMessage(
                    player,
                    "clock.alarm.completed",
                    "time" to clock.argLabel,
                    "title" to clock.title,
                    "id" to clock.id
                )
            }
            player.sendMessage(message)
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f)
        }
    }

    private fun formatAlarmTimeForPlayer(epochMillis: Long, player: Player): String {
        val zoned = Instant.ofEpochMilli(epochMillis).atZone(serverZoneId)
        return if (isEnglish(player)) {
            zoned.toLocalTime().format(alarmDisplayFormatterEn)
        } else {
            zoned.toLocalTime().format(alarmDisplayFormatter)
        }
    }

    private fun formatDurationForPlayer(player: Player, remainingMillis: Long, includeSeconds: Boolean): String {
        return if (includeSeconds) {
            val totalSeconds = ((remainingMillis + 999L) / 1000L).coerceAtLeast(0L)
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            if (isEnglish(player)) {
                "${hours}h ${minutes}m ${seconds}s"
            } else {
                "${hours}時間${minutes}分${seconds}秒"
            }
        } else {
            val totalMinutes = ((remainingMillis + 59999L) / 60000L).coerceAtLeast(0L)
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            if (isEnglish(player)) {
                "${hours}h ${minutes}m"
            } else {
                "${hours}時間${minutes}分"
            }
        }
    }

    private fun isEnglish(player: Player): Boolean {
        val defaultLang = ConfigManager.getDefaultLanguage().lowercase()
        val lang = PlayerDataManager.getString(player.uniqueId, "lang", defaultLang)
            ?.lowercase()
            ?: defaultLang
        return lang.startsWith("en")
    }

    private fun generateId(): String {
        while (true) {
            val id = UUID.randomUUID().toString().replace("-", "").take(8)
            if (!activeClocks.containsKey(id)) {
                return id
            }
        }
    }
}
