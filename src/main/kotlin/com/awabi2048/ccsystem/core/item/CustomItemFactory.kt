package com.awabi2048.ccsystem.core.item

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.api.gui.GuiLoreBlock
import com.awabi2048.ccsystem.api.gui.GuiLoreLine
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.MenuAcceptedClicks
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.displayeffect.DisplayParticleBookSample
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.persistence.PersistentDataType

object CustomItemFactory {
    private const val RENTAL_TICKET_ITEM_ID = "rental_ticket"
    private const val DISPLAY_PARTICLE_SAMPLE_ITEM_ID = "cc-system.display_particle_book_sample"

    private val itemIdKey: NamespacedKey
        get() = NamespacedKey(CCSystem.instance, "custom_item_id")

    private val rentalDaysKey: NamespacedKey
        get() = NamespacedKey(CCSystem.instance, "rental_days")

    fun createRentalTicket(player: Player?, days: Int, amount: Int = 1): ItemStack {
        val item = ItemStack(Material.POISONOUS_POTATO, amount.coerceIn(1, 64))
        val meta = item.itemMeta ?: return item

        meta.displayName(LanguageManager.getMessage(player, "rental_ticket_name"))

        val daysLabel = LanguageManager.getRawString(player, "rental_ticket_days")
        val daysUnit = LanguageManager.getRawString(player, "rental_ticket_days_unit")
        val action = LanguageManager.getRawString(player, "rental_ticket_action")
        meta.lore(CCSystem.getAPI().getLoreService().render(GuiLoreSpec.Blocks(listOf(
            GuiLoreBlock(listOf(GuiLoreLine.Data(daysLabel, "$days$daysUnit", "§e"))),
            GuiLoreBlock(listOf(GuiLoreLine.Interaction(player, MenuAcceptedClicks.RIGHT, action)))
        ))))

        meta.persistentDataContainer.set(itemIdKey, PersistentDataType.STRING, RENTAL_TICKET_ITEM_ID)
        meta.persistentDataContainer.set(rentalDaysKey, PersistentDataType.INTEGER, days)

        meta.setItemModel(NamespacedKey.minecraft("filled_map"))
        item.itemMeta = meta
        item.unsetData(DataComponentTypes.CONSUMABLE)

        return item
    }

    fun isRentalTicket(item: ItemStack?): Boolean {
        if (item == null || item.type == Material.AIR) {
            return false
        }
        val meta = item.itemMeta ?: return false
        val itemId = meta.persistentDataContainer.get(itemIdKey, PersistentDataType.STRING)
        return itemId == RENTAL_TICKET_ITEM_ID
    }

    fun getRentalDays(item: ItemStack?): Int? {
        if (!isRentalTicket(item)) {
            return null
        }
        return item?.itemMeta?.persistentDataContainer?.get(rentalDaysKey, PersistentDataType.INTEGER)
    }

    /** 7ページの初期設定を編集できる、ボクセルパーティクル検証用の本と羽ペンを生成します。 */
    fun createDisplayParticleSampleBook(player: Player?): ItemStack {
        val item = ItemStack(Material.WRITABLE_BOOK)
        val meta = item.itemMeta as? BookMeta ?: return item
        meta.displayName(LanguageManager.getMessageWithoutPrefix(player, "management.debug.particle_sample_item.name"))
        meta.lore(CCSystem.getAPI().getLoreService().render(GuiLoreSpec.Blocks(listOf(
            GuiLoreBlock(listOf(GuiLoreLine.Text(
                LanguageManager.getRawString(player, "management.debug.particle_sample_item.description")
            ))),
            GuiLoreBlock(listOf(GuiLoreLine.Interaction(
                player,
                MenuAcceptedClicks.RIGHT,
                LanguageManager.getRawString(player, "management.debug.particle_sample_item.operation")
            )))
        ))))
        meta.pages(DisplayParticleBookSample.pages.map(Component::text))
        meta.persistentDataContainer.set(itemIdKey, PersistentDataType.STRING, DISPLAY_PARTICLE_SAMPLE_ITEM_ID)
        item.itemMeta = meta
        return item
    }
}
