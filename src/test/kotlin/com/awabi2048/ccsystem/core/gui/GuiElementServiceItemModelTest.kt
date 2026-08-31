package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiItemSpec
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiNameSpec
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class GuiElementServiceItemModelTest {
    @Test
    fun `GUI item model is part of the structured spec and common renderer contract`() {
        val model = NamespacedKey("kota_server", "custom_item/cooking/liquid_display")
        val spec = GuiItemSpec(
            Material.STONE,
            GuiNameSpec.Empty,
            GuiLoreSpec.None,
            GuiElementRole.CONTENT,
            1,
            model,
        )
        val source = Files.readString(
            Path.of("src/main/kotlin/com/awabi2048/ccsystem/core/gui/GuiElementServiceImpl.kt")
        )

        assertEquals(model, spec.itemModel)
        assertTrue(source.contains("normalizedSpec.itemModel?.let(meta::setItemModel)"))
        assertTrue(source.contains("override fun isGuiItem(item: ItemStack?): Boolean"))
    }
}
