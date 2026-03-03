package com.awabi2048.ccsystem.features.announce.manager

import com.awabi2048.ccsystem.CCSystem
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.UUID

object AnnouncementManager {
    const val MAX_VISIBLE_ANNOUNCEMENTS = 28
    const val PLAYER_DATA_LAST_LOGOUT_AT = "announce.last_logout_at"
    const val PLAYER_DATA_LAST_CHECKED_AT = "announce.last_checked_at"

    data class Announcement(
        val id: String,
        val title: String,
        val icon: Material,
        val contentLines: List<String>,
        val endAtRaw: String?,
        val indefinite: Boolean,
        val issuedAt: Instant,
        val expiresAt: Instant?,
        val updatedAt: Instant
    )

    enum class AddResult {
        SUCCESS,
        NOT_FOUND,
        LIMIT_REACHED,
        INVALID_TITLE,
        INVALID_END_AT_FORMAT,
        END_AT_NOT_FUTURE
    }

    private enum class EndAtParseResultType {
        SUCCESS,
        INVALID_FORMAT,
        NOT_FUTURE
    }

    private data class EndAtParseResult(
        val type: EndAtParseResultType,
        val instant: Instant? = null
    )

    private val announcements = linkedMapOf<String, Announcement>()
    private val expireTasks = mutableMapOf<String, BukkitTask>()
    private lateinit var dataFile: File
    private lateinit var config: YamlConfiguration
    private val tokyoZone: ZoneId = ZoneId.of("Asia/Tokyo")

    fun load() {
        unload()

        dataFile = File(CCSystem.instance.dataFolder, "announce_data.yml")
        if (!dataFile.exists()) {
            dataFile.parentFile?.mkdirs()
            dataFile.createNewFile()
        }

        config = YamlConfiguration.loadConfiguration(dataFile)
        announcements.clear()

        val section = config.getConfigurationSection("announcements") ?: return
        val now = Instant.now()
        var changed = false

        for (id in section.getKeys(false)) {
            val entry = section.getConfigurationSection(id) ?: continue

            val title = entry.getString("title")?.trim()
            if (title.isNullOrEmpty()) {
                continue
            }

            val iconRaw = entry.getString("icon") ?: continue
            val icon = Material.matchMaterial(iconRaw.uppercase()) ?: continue

            val contentLines = normalizeContent(entry.getStringList("content_lines"))
            val endAtRaw = entry.getString("end_at_raw")
            val indefinite = entry.getBoolean("indefinite", false)

            val issuedAt = parseInstant(entry.getString("issued_at")) ?: now
            val expiresAt = parseInstant(entry.getString("expires_at"))
            val updatedAt = parseInstant(entry.getString("updated_at")) ?: issuedAt

            val announcement = Announcement(
                id = id,
                title = normalizeTitle(title),
                icon = icon,
                contentLines = contentLines,
                endAtRaw = endAtRaw,
                indefinite = indefinite,
                issuedAt = issuedAt,
                expiresAt = expiresAt,
                updatedAt = updatedAt
            )

            if (!announcement.indefinite && announcement.expiresAt != null && !announcement.expiresAt.isAfter(now)) {
                changed = true
                continue
            }

            announcements[id] = announcement
            scheduleExpiration(announcement)
        }

        if (changed) {
            saveAll()
        }
    }

    fun unload() {
        expireTasks.values.forEach { it.cancel() }
        expireTasks.clear()
    }

    fun getAnnouncementsForMenu(): List<Announcement> {
        return announcements.values
            .sortedByDescending { it.issuedAt }
            .take(MAX_VISIBLE_ANNOUNCEMENTS)
    }

    fun getAnnouncementCount(): Int {
        return announcements.size
    }

    fun getAnnouncementById(id: String): Announcement? {
        return announcements[id]
    }

    fun getAnnouncementsUpdatedAfter(since: Instant): List<Announcement> {
        return announcements.values
            .filter { it.updatedAt.isAfter(since) }
            .sortedByDescending { it.issuedAt }
    }

    fun getUncheckedAnnouncements(lastCheckedAt: Instant?): List<Announcement> {
        if (lastCheckedAt == null) {
            return announcements.values
                .sortedByDescending { it.issuedAt }
                .take(MAX_VISIBLE_ANNOUNCEMENTS)
        }

        return announcements.values
            .filter { it.updatedAt.isAfter(lastCheckedAt) }
            .sortedByDescending { it.issuedAt }
            .take(MAX_VISIBLE_ANNOUNCEMENTS)
    }

    fun getNotificationTargetAnnouncements(lastLogoutAt: Instant?, lastCheckedAt: Instant?): List<Announcement> {
        val byId = linkedMapOf<String, Announcement>()

        if (lastCheckedAt == null) {
            announcements.values.forEach { byId[it.id] = it }
        } else {
            announcements.values
                .filter { it.updatedAt.isAfter(lastCheckedAt) }
                .forEach { byId[it.id] = it }
        }

        if (lastLogoutAt != null) {
            announcements.values
                .filter { it.updatedAt.isAfter(lastLogoutAt) }
                .forEach { byId[it.id] = it }
        }

        return byId.values
            .sortedByDescending { it.issuedAt }
            .take(MAX_VISIBLE_ANNOUNCEMENTS)
    }

    fun addAnnouncement(
        title: String,
        icon: Material,
        contentLines: List<String>,
        endAtInput: String?,
        indefinite: Boolean
    ): AddResult {
        if (announcements.size >= MAX_VISIBLE_ANNOUNCEMENTS) {
            return AddResult.LIMIT_REACHED
        }

        val normalizedTitle = normalizeTitle(title)
        if (normalizedTitle.isBlank()) {
            return AddResult.INVALID_TITLE
        }

        val normalizedEndAtInput = endAtInput?.trim()?.ifBlank { null }
        val expiresAt = if (indefinite) {
            null
        } else {
            val parseResult = parseEndAt(normalizedEndAtInput)
            when (parseResult.type) {
                EndAtParseResultType.SUCCESS -> parseResult.instant
                EndAtParseResultType.INVALID_FORMAT -> return AddResult.INVALID_END_AT_FORMAT
                EndAtParseResultType.NOT_FUTURE -> return AddResult.END_AT_NOT_FUTURE
            }
        }

        val now = Instant.now()
        val id = generateId()
        val announcement = Announcement(
            id = id,
            title = normalizedTitle,
            icon = icon,
            contentLines = normalizeContent(contentLines),
            endAtRaw = normalizedEndAtInput,
            indefinite = indefinite,
            issuedAt = now,
            expiresAt = expiresAt,
            updatedAt = now
        )

        announcements[id] = announcement
        saveAll()
        scheduleExpiration(announcement)
        return AddResult.SUCCESS
    }

    fun updateAnnouncement(
        id: String,
        title: String,
        contentLines: List<String>,
        endAtInput: String?,
        indefinite: Boolean
    ): AddResult {
        val existing = announcements[id] ?: return AddResult.NOT_FOUND

        val normalizedTitle = normalizeTitle(title)
        if (normalizedTitle.isBlank()) {
            return AddResult.INVALID_TITLE
        }

        val normalizedEndAtInput = endAtInput?.trim()?.ifBlank { null }
        val expiresAt = if (indefinite) {
            null
        } else {
            val parseResult = parseEndAt(normalizedEndAtInput)
            when (parseResult.type) {
                EndAtParseResultType.SUCCESS -> parseResult.instant
                EndAtParseResultType.INVALID_FORMAT -> return AddResult.INVALID_END_AT_FORMAT
                EndAtParseResultType.NOT_FUTURE -> return AddResult.END_AT_NOT_FUTURE
            }
        }

        val now = Instant.now()
        val updated = existing.copy(
            title = normalizedTitle,
            contentLines = normalizeContent(contentLines),
            endAtRaw = normalizedEndAtInput,
            indefinite = indefinite,
            expiresAt = expiresAt,
            updatedAt = now
        )

        announcements[id] = updated
        saveAll()
        scheduleExpiration(updated)
        return AddResult.SUCCESS
    }

    fun deleteAnnouncement(id: String): Boolean {
        val removed = announcements.remove(id) ?: return false
        expireTasks.remove(id)?.cancel()
        saveAll()
        CCSystem.instance.logger.info("[Announce] お知らせを削除しました: ${removed.id}")
        return true
    }

    private fun scheduleExpiration(announcement: Announcement) {
        expireTasks.remove(announcement.id)?.cancel()

        if (announcement.indefinite) {
            return
        }

        val expiresAt = announcement.expiresAt ?: return
        val now = Instant.now()
        if (!expiresAt.isAfter(now)) {
            expireAnnouncement(announcement.id)
            return
        }

        val delayMillis = Duration.between(now, expiresAt).toMillis().coerceAtLeast(1L)
        val delayTicks = ((delayMillis + 49L) / 50L).coerceAtLeast(1L)

        val task = CCSystem.instance.server.scheduler.runTaskLater(
            CCSystem.instance,
            Runnable {
                expireAnnouncement(announcement.id)
            },
            delayTicks
        )
        expireTasks[announcement.id] = task
    }

    private fun expireAnnouncement(id: String) {
        val removed = announcements.remove(id) ?: return
        expireTasks.remove(id)?.cancel()
        saveAll()
        CCSystem.instance.logger.info("[Announce] 期限切れによりお知らせを削除しました: ${removed.id}")
    }

    private fun saveAll() {
        config.set("announcements", null)

        for ((id, announcement) in announcements) {
            val basePath = "announcements.$id"
            config.set("$basePath.title", announcement.title)
            config.set("$basePath.icon", announcement.icon.name)
            config.set("$basePath.content_lines", normalizeContent(announcement.contentLines))
            config.set("$basePath.end_at_raw", announcement.endAtRaw)
            config.set("$basePath.indefinite", announcement.indefinite)
            config.set("$basePath.issued_at", announcement.issuedAt.toString())
            config.set("$basePath.expires_at", announcement.expiresAt?.toString())
            config.set("$basePath.updated_at", announcement.updatedAt.toString())
        }

        config.save(dataFile)
    }

    private fun parseEndAt(raw: String?): EndAtParseResult {
        if (raw.isNullOrBlank()) {
            return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
        }

        val now = Instant.now()
        val nowTokyo = now.atZone(tokyoZone)

        val dateTimeMatch = Regex("^\\s*(\\d{4})/(\\d{1,2})/(\\d{1,2})-(\\d{1,2}):(\\d{2})\\s*$").matchEntire(raw)
        if (dateTimeMatch != null) {
            val year = dateTimeMatch.groupValues[1].toIntOrNull()
                ?: return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
            val month = dateTimeMatch.groupValues[2].toIntOrNull()
                ?: return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
            val day = dateTimeMatch.groupValues[3].toIntOrNull()
                ?: return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
            val hour = dateTimeMatch.groupValues[4].toIntOrNull()
                ?: return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
            val minute = dateTimeMatch.groupValues[5].toIntOrNull()
                ?: return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)

            val localDateTime = try {
                LocalDateTime.of(year, month, day, hour, minute)
            } catch (_: Exception) {
                return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
            }

            val instant = localDateTime.atZone(tokyoZone).toInstant()
            return if (instant.isAfter(now)) {
                EndAtParseResult(EndAtParseResultType.SUCCESS, instant)
            } else {
                EndAtParseResult(EndAtParseResultType.NOT_FUTURE)
            }
        }

        val dateOnlyMatch = Regex("^\\s*(\\d{4})/(\\d{1,2})/(\\d{1,2})\\s*$").matchEntire(raw)
        if (dateOnlyMatch != null) {
            val year = dateOnlyMatch.groupValues[1].toIntOrNull()
                ?: return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
            val month = dateOnlyMatch.groupValues[2].toIntOrNull()
                ?: return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
            val day = dateOnlyMatch.groupValues[3].toIntOrNull()
                ?: return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)

            val localDate = try {
                LocalDate.of(year, month, day)
            } catch (_: Exception) {
                return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
            }

            val instant = localDate.atStartOfDay(tokyoZone).toInstant()
            return if (instant.isAfter(now)) {
                EndAtParseResult(EndAtParseResultType.SUCCESS, instant)
            } else {
                EndAtParseResult(EndAtParseResultType.NOT_FUTURE)
            }
        }

        val timeOnlyMatch = Regex("^\\s*(\\d{1,2}):(\\d{2})\\s*$").matchEntire(raw)
        if (timeOnlyMatch != null) {
            val hour = timeOnlyMatch.groupValues[1].toIntOrNull()
                ?: return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
            val minute = timeOnlyMatch.groupValues[2].toIntOrNull()
                ?: return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)

            val localTime = try {
                LocalTime.of(hour, minute)
            } catch (_: Exception) {
                return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
            }

            val instant = nowTokyo.toLocalDate().atTime(localTime).atZone(tokyoZone).toInstant()
            return if (instant.isAfter(now)) {
                EndAtParseResult(EndAtParseResultType.SUCCESS, instant)
            } else {
                EndAtParseResult(EndAtParseResultType.NOT_FUTURE)
            }
        }

        return EndAtParseResult(EndAtParseResultType.INVALID_FORMAT)
    }

    fun parseInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) {
            return null
        }
        return try {
            Instant.parse(raw)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun normalizeContent(contentLines: List<String>): List<String> {
        return (0 until 3).map { index ->
            contentLines.getOrNull(index)?.take(128) ?: ""
        }
    }

    private fun normalizeTitle(title: String): String {
        return title.trim().take(64)
    }

    private fun generateId(): String {
        while (true) {
            val id = UUID.randomUUID().toString().replace("-", "").take(12)
            if (!announcements.containsKey(id)) {
                return id
            }
        }
    }
}
