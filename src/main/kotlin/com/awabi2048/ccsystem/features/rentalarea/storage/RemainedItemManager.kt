package com.awabi2048.ccsystem.features.rentalarea.storage

import com.awabi2048.ccsystem.CCSystem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockState
import org.bukkit.block.Container
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.UUID

object RemainedItemManager {
    private val storageDir: File by lazy {
        val dir = File(CCSystem.instance.dataFolder, "rental_area/remained_item")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val playerInventories = mutableMapOf<UUID, MutableMap<String, MutableList<ItemStack>>>()
    private val openInventories = mutableMapOf<UUID, String>()

    fun load() {
        playerInventories.clear()
    }

    fun hasRemainedItems(playerId: UUID): Boolean {
        val file = getFile(playerId)
        if (!file.exists()) {
            return false
        }
        val config = YamlConfiguration.loadConfiguration(file)
        return config.getKeys(false).isNotEmpty()
    }

    fun getRemainedAreaIds(playerId: UUID): List<String> {
        val file = getFile(playerId)
        if (!file.exists()) {
            return emptyList()
        }
        val config = YamlConfiguration.loadConfiguration(file)
        return config.getKeys(false).toList()
    }

    fun getTotalItemCount(playerId: UUID): Int {
        val file = getFile(playerId)
        if (!file.exists()) {
            return 0
        }
        val config = YamlConfiguration.loadConfiguration(file)
        var count = 0
        for (areaId in config.getKeys(false)) {
            val itemsSection = config.getConfigurationSection("$areaId.items") ?: continue
            count += itemsSection.getKeys(false).size
        }
        return count
    }

    fun saveItems(playerId: UUID, areaId: String, items: List<ItemStack>) {
        if (items.isEmpty()) {
            return
        }

        val file = getFile(playerId)
        val config = if (file.exists()) {
            YamlConfiguration.loadConfiguration(file)
        } else {
            YamlConfiguration()
        }

        val basePath = "$areaId.items"
        var index = 0
        for (item in items) {
            if (item == null || item.type == Material.AIR) {
                continue
            }
            config.set("$basePath.$index", item)
            index++
        }

        if (index > 0) {
            config.save(file)
        }
    }

    fun openStorage(player: Player, areaId: String): Inventory? {
        val playerId = player.uniqueId
        val file = getFile(playerId)
        if (!file.exists()) {
            return null
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val itemsSection = config.getConfigurationSection("$areaId.items")
        if (itemsSection == null || itemsSection.getKeys(false).isEmpty()) {
            return null
        }

        val items = mutableListOf<ItemStack>()
        for (key in itemsSection.getKeys(false)) {
            val item = itemsSection.getItemStack(key)
            if (item != null && item.type != Material.AIR) {
                items.add(item)
            }
        }

        if (items.isEmpty()) {
            removeAreaSection(file, config, areaId)
            return null
        }

        val title = "§8回収アイテム: $areaId"
        val inventory = Bukkit.createInventory(null, 54, title)

        for (item in items) {
            inventory.addItem(item)
        }

        playerInventories.getOrPut(playerId) { mutableMapOf() }[areaId] = items.toMutableList()
        openInventories[playerId] = areaId

        player.openInventory(inventory)
        return inventory
    }

    fun onInventoryClose(player: Player, inventory: Inventory) {
        val playerId = player.uniqueId
        val areaId = openInventories.remove(playerId) ?: return

        val remainingItems = mutableListOf<ItemStack>()
        for (item in inventory.contents) {
            if (item != null && item.type != Material.AIR) {
                remainingItems.add(item)
            }
        }

        val file = getFile(playerId)
        if (!file.exists()) {
            return
        }

        val config = YamlConfiguration.loadConfiguration(file)

        if (remainingItems.isEmpty()) {
            config.set(areaId, null)
            if (config.getKeys(false).isEmpty()) {
                file.delete()
            } else {
                config.save(file)
            }
        } else {
            config.set("$areaId.items", null)
            var index = 0
            for (item in remainingItems) {
                config.set("$areaId.items.$index", item)
                index++
            }
            config.save(file)
        }

        playerInventories.getOrDefault(playerId, mutableMapOf()).remove(areaId)
    }

    fun isOpenedStorage(playerId: UUID): Boolean {
        return openInventories.containsKey(playerId)
    }

    fun getOpenedAreaId(playerId: UUID): String? {
        return openInventories[playerId]
    }

    fun collectBlockItems(blockState: BlockState): List<ItemStack> {
        val items = mutableListOf<ItemStack>()

        if (blockState is Container) {
            val inventory = blockState.inventory
            for (item in inventory.contents) {
                if (item != null && item.type != Material.AIR) {
                    items.add(item.clone())
                }
            }
            inventory.clear()
        }

        val blockItem = blockState.block.type.key.toString()
        if (blockItem != "minecraft:air") {
            val stack = ItemStack(blockState.block.type, 1)
            items.add(stack)
        }

        return items
    }

    private fun getFile(playerId: UUID): File {
        return File(storageDir, "$playerId.yml")
    }

    private fun removeAreaSection(file: File, config: YamlConfiguration, areaId: String) {
        config.set(areaId, null)
        if (config.getKeys(false).isEmpty()) {
            file.delete()
        } else {
            config.save(file)
        }
    }
}
