package com.awabi2048.ccsystem.features.rentalarea.storage

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.PlayerInventoryInteraction
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
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
    private const val MENU_OWNER = "cc-system"
    private const val MENU_ID = "rental-remained-items"

    init {
        CCSystem.getAPI().getMenuRuntimeService().register(
            InventoryMenuDefinition(
                owner = MENU_OWNER,
                id = MENU_ID,
                renderer = { context ->
                    val areaId = context.route.payload.getValue("area")
                    val items = loadAreaItems(context.player.uniqueId, areaId)
                    InventoryMenuView(
                        size = 54,
                        title = LegacyComponentSerializer.legacySection()
                            .deserialize("§8回収アイテム: $areaId"),
                        elements = emptyList(),
                        standardFrame = false,
                        inputSlots = (0 until 54).toSet(),
                        inputItems = items.mapIndexed { index, item -> index to item }.toMap(),
                        playerInventoryInteraction = PlayerInventoryInteraction.INTERACTIVE,
                    )
                },
                actions = emptyMap(),
                onClose = { context -> onInventoryClose(context.player, context.inventory) },
            ),
        )
    }
    private val storageDir: File by lazy {
        val dir = File(CCSystem.instance.dataFolder, "data/rental_area/remained_item")
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
            if (item.type == Material.AIR) {
                continue
            }
            config.set("$basePath.$index", item)
            index++
        }

        if (index > 0) {
            config.save(file)
        }
    }

    fun openStorage(player: Player, areaId: String): Boolean {
        val playerId = player.uniqueId
        val items = loadAreaItems(playerId, areaId)
        if (items.isEmpty()) {
            val file = getFile(playerId)
            if (file.exists()) {
                removeAreaSection(file, YamlConfiguration.loadConfiguration(file), areaId)
            }
            return false
        }

        playerInventories.getOrPut(playerId) { mutableMapOf() }[areaId] = items.toMutableList()
        openInventories[playerId] = areaId
        return CCSystem.getAPI().getMenuRuntimeService().open(
            player,
            MenuRoute(MENU_OWNER, MENU_ID, mapOf("area" to areaId)),
        )
    }

    private fun loadAreaItems(playerId: UUID, areaId: String): List<ItemStack> {
        val file = getFile(playerId)
        if (!file.exists()) return emptyList()
        val section = YamlConfiguration.loadConfiguration(file)
            .getConfigurationSection("$areaId.items") ?: return emptyList()
        return section.getKeys(false).mapNotNull(section::getItemStack)
            .filter { it.type != Material.AIR }
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
