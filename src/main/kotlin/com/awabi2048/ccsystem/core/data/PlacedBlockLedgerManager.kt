package com.awabi2048.ccsystem.core.data

import com.awabi2048.ccsystem.CCSystem
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

object PlacedBlockLedgerManager {
    data class PackedBlockRecord(val worldId: UUID, val packedPos: Long)

    private val worldRecords = mutableMapOf<UUID, MutableSet<Long>>()
    private lateinit var dataFile: File
    private lateinit var config: YamlConfiguration

    fun load() {
        dataFile = File(CCSystem.instance.dataFolder, "placed_block_ledger.yml")
        if (!dataFile.exists()) {
            dataFile.parentFile?.mkdirs()
            dataFile.createNewFile()
        }

        config = YamlConfiguration.loadConfiguration(dataFile)
        worldRecords.clear()

        val worldsSection = config.getConfigurationSection("worlds") ?: return
        for (worldKey in worldsSection.getKeys(false)) {
            val worldId = parseUuid(worldKey) ?: continue
            val csv = worldsSection.getString(worldKey).orEmpty()
            if (csv.isBlank()) {
                worldRecords[worldId] = mutableSetOf()
                continue
            }

            val set = mutableSetOf<Long>()
            csv.split(',')
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { token ->
                    parseBase36UnsignedLong(token)?.let { set.add(it) }
                }

            worldRecords[worldId] = set
        }
    }

    fun save() {
        val worldsPath = "worlds"
        config.set(worldsPath, null)

        for ((worldId, records) in worldRecords) {
            val csv = records
                .asSequence()
                .map { java.lang.Long.toUnsignedString(it, 36) }
                .joinToString(",")
            config.set("$worldsPath.$worldId", csv)
        }

        config.save(dataFile)
    }

    fun registerPlacement(worldId: UUID, x: Int, y: Int, z: Int) {
        val set = worldRecords.getOrPut(worldId) { mutableSetOf() }
        if (set.add(pack(x, y, z))) {
            save()
        }
    }

    fun unregisterPlacement(worldId: UUID, x: Int, y: Int, z: Int) {
        val set = worldRecords[worldId] ?: return
        if (!set.remove(pack(x, y, z))) {
            return
        }
        if (set.isEmpty()) {
            worldRecords.remove(worldId)
        }
        save()
    }

    fun contains(worldId: UUID, x: Int, y: Int, z: Int): Boolean {
        return worldRecords[worldId]?.contains(pack(x, y, z)) == true
    }

    fun removeInCuboid(
        worldId: UUID,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        minZ: Int,
        maxZ: Int
    ): List<PackedBlockRecord> {
        val set = worldRecords[worldId] ?: return emptyList()
        val removed = mutableListOf<PackedBlockRecord>()

        val iterator = set.iterator()
        while (iterator.hasNext()) {
            val packed = iterator.next()
            val (x, y, z) = unpack(packed)
            if (x in minX..maxX && y in minY..maxY && z in minZ..maxZ) {
                iterator.remove()
                removed += PackedBlockRecord(worldId, packed)
            }
        }

        if (set.isEmpty()) {
            worldRecords.remove(worldId)
        }

        if (removed.isNotEmpty()) {
            save()
        }

        return removed
    }

    fun pack(x: Int, y: Int, z: Int): Long {
        val xPart = (x.toLong() and MASK_26_BITS) shl 38
        val zPart = (z.toLong() and MASK_26_BITS) shl 12
        val yPart = y.toLong() and MASK_12_BITS
        return xPart or zPart or yPart
    }

    fun unpack(packed: Long): Triple<Int, Int, Int> {
        val xRaw = ((packed ushr 38) and MASK_26_BITS).toInt()
        val zRaw = ((packed ushr 12) and MASK_26_BITS).toInt()
        val yRaw = (packed and MASK_12_BITS).toInt()

        val x = signExtend26(xRaw)
        val z = signExtend26(zRaw)
        val y = signExtend12(yRaw)
        return Triple(x, y, z)
    }

    private fun signExtend26(value: Int): Int {
        return if ((value and SIGN_26_BIT) != 0) value or INV_MASK_26_BITS_INT else value
    }

    private fun signExtend12(value: Int): Int {
        return if ((value and SIGN_12_BIT) != 0) value or INV_MASK_12_BITS_INT else value
    }

    private fun parseBase36UnsignedLong(token: String): Long? {
        return try {
            java.lang.Long.parseUnsignedLong(token, 36)
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun parseUuid(raw: String): UUID? {
        return try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private const val MASK_26_BITS: Long = 0x3FFFFFFL
    private const val MASK_12_BITS: Long = 0xFFFL
    private const val SIGN_26_BIT: Int = 0x2000000
    private const val SIGN_12_BIT: Int = 0x800
    private const val INV_MASK_26_BITS_INT: Int = -0x4000000
    private const val INV_MASK_12_BITS_INT: Int = -0x1000
}
