package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementRole
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

/** 共通生成GUIアイテムを通常アイテムと区別するPDC定義。 */
object GuiItemMarker {
    val markerKey = NamespacedKey("cc-system", "gui_marker")
    val roleKey = NamespacedKey("cc-system", "gui_role")

    fun mark(meta: ItemMeta, role: GuiElementRole) {
        meta.persistentDataContainer.set(markerKey, PersistentDataType.BYTE, 1)
        meta.persistentDataContainer.set(roleKey, PersistentDataType.STRING, role.name)
    }

    fun isMarked(item: ItemStack?): Boolean {
        return item?.itemMeta?.persistentDataContainer?.has(markerKey, PersistentDataType.BYTE) == true
    }

    fun role(item: ItemStack?): GuiElementRole? {
        val raw = item?.itemMeta?.persistentDataContainer?.get(roleKey, PersistentDataType.STRING) ?: return null
        return runCatching { GuiElementRole.valueOf(raw) }.getOrNull()
    }
}
