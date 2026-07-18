package com.awabi2048.ccsystem.features.rentalarea.manager

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.data.PlacedBlockLedgerManager
import com.awabi2048.ccsystem.core.time.DatePolicy
import com.awabi2048.ccsystem.features.rentalarea.storage.RemainedItemManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

object RentalAreaManager {
    data class IntVector3(val x: Int, val y: Int, val z: Int)

    data class RentalArea(
        val id: String,
        val worldKey: String,
        val pos1: IntVector3,
        val pos2: IntVector3,
        val owner: UUID?,
        val expireDate: LocalDate?
    ) {
        fun isInside(location: Location): Boolean {
            if (location.world?.key?.toString() != worldKey) {
                return false
            }

            val minX = minOf(pos1.x, pos2.x)
            val maxX = maxOf(pos1.x, pos2.x)
            val minY = minOf(pos1.y, pos2.y)
            val maxY = maxOf(pos1.y, pos2.y)
            val minZ = minOf(pos1.z, pos2.z)
            val maxZ = maxOf(pos1.z, pos2.z)

            return location.blockX in minX..maxX &&
                location.blockY in minY..maxY &&
                location.blockZ in minZ..maxZ
        }
    }

    enum class ContractResult {
        SUCCESS,
        AREA_ALREADY_OWNED,
        PLAYER_ALREADY_HAS_AREA,
        AREA_NOT_FOUND,
        INVALID_DAYS
    }

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val areas = linkedMapOf<String, RentalArea>()
    private lateinit var dataFile: File
    private lateinit var config: YamlConfiguration

    fun load() {
        dataFile = File(CCSystem.instance.dataFolder, "data/rental_area/rental_area_data.yml")
        if (!dataFile.exists()) {
            dataFile.parentFile?.mkdirs()
            dataFile.createNewFile()
        }

        config = YamlConfiguration.loadConfiguration(dataFile)
        areas.clear()

        for (areaId in config.getKeys(false)) {
            val section = config.getConfigurationSection(areaId)
            if (section == null) {
                CCSystem.instance.logger.info("[RentalArea] area '$areaId' を読み飛ばしました: セクション形式ではありません")
                continue
            }

            val worldKey = section.getString("world_key")
            val pos1Raw = section.getString("pos_1")
            val pos2Raw = section.getString("pos_2")

            if (worldKey.isNullOrBlank() || pos1Raw.isNullOrBlank() || pos2Raw.isNullOrBlank()) {
                CCSystem.instance.logger.info("[RentalArea] area '$areaId' を読み飛ばしました: world_key/pos_1/pos_2 が不足")
                continue
            }

            val pos1 = parseVector(pos1Raw)
            val pos2 = parseVector(pos2Raw)
            if (pos1 == null || pos2 == null) {
                CCSystem.instance.logger.info("[RentalArea] area '$areaId' を読み飛ばしました: 座標形式が不正")
                continue
            }

            val owner = parseUuid(section.getString("owner"))
            val expireDate = parseDate(section.getString("expire_date"))

            areas[areaId] = RentalArea(
                id = areaId,
                worldKey = worldKey,
                pos1 = pos1,
                pos2 = pos2,
                owner = owner,
                expireDate = expireDate
            )
        }
    }

    fun getArea(areaId: String): RentalArea? = areas[areaId]

    fun getAreaAt(location: Location): RentalArea? {
        return areas.values.firstOrNull { it.isInside(location) }
    }

    fun getOwnedArea(owner: UUID, today: LocalDate = LocalDate.now()): RentalArea? {
        return areas.values.firstOrNull {
            it.owner == owner && it.expireDate != null && !DatePolicy.isExpired(today, it.expireDate)
        }
    }

    fun remainingDays(area: RentalArea, today: LocalDate = LocalDate.now()): Int {
        val expireDate = area.expireDate ?: return 0
        return DatePolicy.remainingDays(today, expireDate)
    }

    fun isActiveOwnerAt(playerId: UUID, location: Location, today: LocalDate = LocalDate.now()): Boolean {
        val area = getAreaAt(location) ?: return false
        val owner = area.owner ?: return false
        val expireDate = area.expireDate ?: return false
        if (DatePolicy.isExpired(today, expireDate)) {
            return false
        }
        return owner == playerId
    }

    fun isOwnerAt(playerId: UUID, location: Location): Boolean {
        val area = getAreaAt(location) ?: return false
        val owner = area.owner ?: return false
        return owner == playerId
    }

    fun contractArea(
        areaId: String,
        owner: UUID,
        days: Int,
        today: LocalDate = LocalDate.now()
    ): ContractResult {
        if (days <= 0) {
            return ContractResult.INVALID_DAYS
        }

        val area = areas[areaId] ?: return ContractResult.AREA_NOT_FOUND
        if (area.owner != null && area.expireDate != null && !DatePolicy.isExpired(today, area.expireDate)) {
            return ContractResult.AREA_ALREADY_OWNED
        }

        if (getOwnedArea(owner, today) != null) {
            return ContractResult.PLAYER_ALREADY_HAS_AREA
        }

        val expireDate = today.plusDays(days.toLong())
        val updated = area.copy(owner = owner, expireDate = expireDate)
        areas[areaId] = updated
        saveAreaState(areaId)

        return ContractResult.SUCCESS
    }

    fun updateDay(today: LocalDate = LocalDate.now()): Int {
        var expiredCount = 0

        val updatedMap = areas.mapValues { (_, area) ->
            val expireDate = area.expireDate
            if (area.owner != null && expireDate != null && DatePolicy.isExpired(today, expireDate)) {
                mockCleanupArea(area)
                expiredCount += 1
                area.copy(owner = null, expireDate = null)
            } else {
                area
            }
        }

        areas.clear()
        areas.putAll(updatedMap)

        if (expiredCount > 0) {
            saveAllAreaStates()
        }

        return expiredCount
    }

    fun isProtectedFor(playerId: UUID, location: Location, today: LocalDate = LocalDate.now()): Boolean {
        val area = getAreaAt(location) ?: return false
        val owner = area.owner ?: return false
        val expireDate = area.expireDate ?: return false

        if (DatePolicy.isExpired(today, expireDate)) {
            return false
        }

        return owner != playerId
    }

    private fun mockCleanupArea(area: RentalArea) {
        val key = NamespacedKey.fromString(area.worldKey)
        val world = key?.let(Bukkit::getWorld)
        if (world == null) {
            CCSystem.instance.logger.info("[RentalArea] area '${area.id}' の撤去をスキップ: ワールド未ロード")
            return
        }

        val owner = area.owner

        val minX = minOf(area.pos1.x, area.pos2.x)
        val maxX = maxOf(area.pos1.x, area.pos2.x)
        val minY = minOf(area.pos1.y, area.pos2.y)
        val maxY = maxOf(area.pos1.y, area.pos2.y)
        val minZ = minOf(area.pos1.z, area.pos2.z)
        val maxZ = maxOf(area.pos1.z, area.pos2.z)

        val targets = PlacedBlockLedgerManager.removeInCuboid(
            worldId = world.uid,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            minZ = minZ,
            maxZ = maxZ
        )

        val collectedItems = mutableListOf<org.bukkit.inventory.ItemStack>()

        for (target in targets) {
            val (x, y, z) = PlacedBlockLedgerManager.unpack(target.packedPos)
            val block = world.getBlockAt(x, y, z)
            val state = block.state

            if (owner != null) {
                val items = RemainedItemManager.collectBlockItems(state)
                collectedItems.addAll(items)
            }

            block.setType(Material.AIR, false)
        }

        if (owner != null && collectedItems.isNotEmpty()) {
            RemainedItemManager.saveItems(owner, area.id, collectedItems)
        }

        CCSystem.instance.logger.info("[RentalArea] area '${area.id}' を期限切れで撤去しました (対象: ${targets.size} ブロック, 回収アイテム: ${collectedItems.size} 個)")
    }

    private fun saveAreaState(areaId: String) {
        val area = areas[areaId] ?: return
        val basePath = area.id
        config.set("$basePath.owner", area.owner?.toString())
        config.set("$basePath.expire_date", area.expireDate?.format(dateFormatter))
        config.save(dataFile)
    }

    private fun saveAllAreaStates() {
        for (area in areas.values) {
            val basePath = area.id
            config.set("$basePath.owner", area.owner?.toString())
            config.set("$basePath.expire_date", area.expireDate?.format(dateFormatter))
        }
        config.save(dataFile)
    }

    private fun parseVector(raw: String): IntVector3? {
        val match = Regex("^\\s*\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)\\s*$").matchEntire(raw)
            ?: return null

        val x = match.groupValues[1].toIntOrNull() ?: return null
        val y = match.groupValues[2].toIntOrNull() ?: return null
        val z = match.groupValues[3].toIntOrNull() ?: return null
        return IntVector3(x, y, z)
    }

    private fun parseUuid(raw: String?): UUID? {
        if (raw.isNullOrBlank()) {
            return null
        }
        return try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) {
            return null
        }
        return try {
            LocalDate.parse(raw, dateFormatter)
        } catch (_: Exception) {
            null
        }
    }
}
