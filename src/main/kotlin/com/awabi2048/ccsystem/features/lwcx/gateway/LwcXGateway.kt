package com.awabi2048.ccsystem.features.lwcx.gateway

import com.griefcraft.lwc.LWC
import com.griefcraft.model.Protection
import java.util.Locale
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * LWCX公式APIへの唯一の接続点。LWC未導入時にこのクラスを生成しないことで任意依存を維持する。
 */
class LwcXGateway : LwcGateway {
    private fun lwc(): LWC {
        check(LWC.ENABLED) { "LWC is not enabled" }
        return LWC.getInstance() ?: error("LWC instance is unavailable")
    }

    override fun loadProtectionRecords(): List<LwcProtectionRecord> {
        return lwc().physicalDatabase.loadProtections().map(::toRecord)
    }

    override fun loadProtectionHandles(worldName: String): List<LwcProtectionHandle> {
        return lwc().physicalDatabase.loadProtections()
            .asSequence()
            .filter { it.world == worldName }
            .map(::ProtectionHandle)
            .toList()
    }

    private fun toRecord(protection: Protection): LwcProtectionRecord {
        return LwcProtectionRecord(
            id = protection.id,
            world = protection.world,
            type = protection.type.name.lowercase(Locale.ROOT),
            creation = parseCreation(protection.creation),
            lastAccessed = protection.lastAccessed
        )
    }

    private class ProtectionHandle(private val protection: Protection) : LwcProtectionHandle {
        override val record: LwcProtectionRecord = LwcProtectionRecord(
            id = protection.id,
            world = protection.world,
            type = protection.type.name.lowercase(Locale.ROOT),
            creation = parseCreation(protection.creation),
            lastAccessed = protection.lastAccessed
        )

        override fun remove() {
            protection.remove()
        }
    }

}

private fun parseCreation(raw: String?): Instant? {
    if (raw == null) return null
    require(raw.isNotBlank()) { "LWC creation timestamp is blank" }
    return try {
        Instant.parse(raw)
    } catch (_: java.time.format.DateTimeParseException) {
        try {
            LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC)
        } catch (_: java.time.format.DateTimeParseException) {
            try {
                LocalDate.parse(raw).atStartOfDay().toInstant(ZoneOffset.UTC)
            } catch (error: java.time.format.DateTimeParseException) {
                throw IllegalArgumentException("Invalid LWC creation timestamp: $raw", error)
            }
        }
    }
}
