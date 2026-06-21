package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementService
import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiFrameSection
import com.awabi2048.ccsystem.api.gui.GuiFrameSpec
import com.awabi2048.ccsystem.api.gui.GuiItemSpec
import com.awabi2048.ccsystem.api.gui.GuiLoreFrame
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiNameStyle
import com.awabi2048.ccsystem.api.gui.GuiNameSpec
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Inventory

class GuiElementServiceImpl : GuiElementService {
    private val loreService = LoreServiceImpl()
    private val legacy = LegacyComponentSerializer.legacySection()
    private val colorCodePattern = Regex("(?i)[\u00A7&][0-9A-FK-ORX]")
    private val dataLinePattern = Regex("^\u00A77([^:\uFF1A]+)[:\uFF1A]\\s*(.*)$")
    private val richDataPrefixPattern = Regex("^\u00A7f\u00A7l\\|\\s*")
    private val richActionPrefixPattern = Regex("^\u00A7e\u00A7l\\|\\s*")
    private val actionPrefixPattern = Regex("^\u00A7e\u2759\\s*")

    override fun component(raw: String): Component {
        return normalizeComponent(legacy.deserialize(raw))
    }

    override fun name(raw: String, style: GuiNameStyle): Component {
        val normalized = raw.trim()
        val styled = if (colorCodePattern.containsMatchIn(normalized)) normalized else style.colorCode + normalized
        return component(styled)
    }

    override fun lore(spec: GuiLoreSpec): List<Component> {
        return loreService.render(spec)
    }

    override fun autoLore(lines: List<String>, closingSeparator: Boolean): List<Component> {
        return loreService.render(
            GuiLoreSpec.Auto(
                lines,
                if (closingSeparator) GuiLoreFrame.BOTH else GuiLoreFrame.TOP
            )
        )
    }

    override fun item(spec: GuiItemSpec): ItemStack {
        val item = ItemStack(spec.material, spec.amount.coerceIn(1, spec.material.maxStackSize.coerceAtLeast(1)))
        val meta = item.itemMeta ?: return item
        meta.displayName(
            when (val name = spec.name) {
                GuiNameSpec.Empty -> Component.empty()
                is GuiNameSpec.Text -> this.name(name.text, name.style)
            }
        )
        val nameOnlyRole = spec.role in setOf(
            GuiElementRole.BACK,
            GuiElementRole.CONFIRM,
            GuiElementRole.CANCEL,
            GuiElementRole.NAVIGATION,
        )
        val lore = if (nameOnlyRole) emptyList() else lore(spec.lore)
        if (lore.isNotEmpty()) {
            meta.lore(lore)
        }
        // GUIアイコンでは素材固有の説明を表示せず、NameとLoreだけを情報源にする。
        meta.addItemFlags(
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        )
        meta.isHideTooltip = spec.role == GuiElementRole.DECORATION
        item.itemMeta = meta
        return item
    }

    override fun applyFrame(inventory: Inventory, spec: GuiFrameSpec) {
        applySection(inventory, spec.header, 0 until minOf(9, inventory.size))
        val footerStart = (inventory.size - 9).coerceAtLeast(0)
        applySection(inventory, spec.footer, footerStart until inventory.size)
        spec.emptySlot?.let { fillEmpty(inventory, it) }
    }

    override fun fillEmpty(inventory: Inventory, element: GuiItemSpec) {
        for (slot in 0 until inventory.size) {
            if (inventory.getItem(slot) == null) inventory.setItem(slot, item(element))
        }
    }

    private fun applySection(inventory: Inventory, section: GuiFrameSection, rowSlots: IntRange) {
        val slots = when (section) {
            GuiFrameSection.None -> return
            is GuiFrameSection.Row -> rowSlots
            is GuiFrameSection.Slots -> section.slots
        }
        val element = when (section) {
            is GuiFrameSection.Row -> section.element
            is GuiFrameSection.Slots -> section.element
            GuiFrameSection.None -> return
        }
        slots.filter { it in 0 until inventory.size }.forEach { inventory.setItem(it, item(element)) }
    }

    override fun decoration(material: Material): ItemStack {
        return item(
            GuiItemSpec(
                material = material,
                name = GuiNameSpec.Empty,
                lore = GuiLoreSpec.None,
                role = GuiElementRole.DECORATION,
                amount = 1
            )
        )
    }

    override fun backItem(name: String, material: Material): ItemStack {
        return item(
            GuiItemSpec(
                material = material,
                name = GuiNameSpec.Text(name, GuiNameStyle.MUTED),
                lore = GuiLoreSpec.None,
                role = GuiElementRole.BACK,
                amount = 1
            )
        )
    }

    override fun confirmItem(name: String, confirm: Boolean): ItemStack {
        return item(
            GuiItemSpec(
                material = if (confirm) Material.LIME_CONCRETE else Material.RED_CONCRETE,
                name = GuiNameSpec.Text(name, if (confirm) GuiNameStyle.SUCCESS else GuiNameStyle.DANGER),
                lore = GuiLoreSpec.None,
                role = if (confirm) GuiElementRole.CONFIRM else GuiElementRole.CANCEL,
                amount = 1
            )
        )
    }

    private fun normalizeComponents(lines: List<Component>): List<Component> {
        return lines
            .map(LoreFormatter::normalizeSeparator)
            .map(::normalizeComponent)
    }

    private fun normalizeComponent(component: Component): Component {
        return component
            .colorIfAbsent(NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false)
    }
}
