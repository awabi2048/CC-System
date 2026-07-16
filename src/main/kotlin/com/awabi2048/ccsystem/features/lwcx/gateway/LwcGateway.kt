package com.awabi2048.ccsystem.features.lwcx.gateway

import java.time.Instant

data class LwcProtectionRecord(
    val id: Int,
    val world: String,
    val type: String,
    val creation: Instant?,
    val lastAccessed: Long
)

interface LwcProtectionHandle {
    val record: LwcProtectionRecord

    fun remove()
}

interface LwcGateway {
    fun loadProtectionRecords(): List<LwcProtectionRecord>

    fun loadProtectionHandles(worldName: String): List<LwcProtectionHandle>
}
