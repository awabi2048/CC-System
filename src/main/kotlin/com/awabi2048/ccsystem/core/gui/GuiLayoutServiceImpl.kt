package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiConfirmationLayout
import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiFrameSection
import com.awabi2048.ccsystem.api.gui.GuiFrameSpec
import com.awabi2048.ccsystem.api.gui.GuiFreeLayout
import com.awabi2048.ccsystem.api.gui.GuiItemSpec
import com.awabi2048.ccsystem.api.gui.GuiLayoutStyle
import com.awabi2048.ccsystem.api.gui.GuiLayoutService
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiNameSpec
import com.awabi2048.ccsystem.api.gui.GuiNameStyle
import com.awabi2048.ccsystem.api.gui.GuiPagedListLayout
import com.awabi2048.ccsystem.api.gui.GuiSettingsLayout
import com.awabi2048.ccsystem.api.gui.GuiThreeChoiceLayout
import org.bukkit.Material
import org.bukkit.inventory.Inventory

class GuiLayoutServiceImpl(
    private val guiElementService: GuiElementServiceImpl = GuiElementServiceImpl()
) : GuiLayoutService {
    private val frameElement = GuiItemSpec(
        material = Material.BLACK_STAINED_GLASS_PANE,
        name = GuiNameSpec.Text(" ", GuiNameStyle.DEFAULT),
        lore = GuiLoreSpec.None,
        role = GuiElementRole.DECORATION,
        amount = 1
    )
    private val emptyElement = GuiItemSpec(
        material = Material.GRAY_STAINED_GLASS_PANE,
        name = GuiNameSpec.Text(" ", GuiNameStyle.DEFAULT),
        lore = GuiLoreSpec.None,
        role = GuiElementRole.DECORATION,
        amount = 1
    )

    override fun size45(): Int = 45

    override fun size54(): Int = 54

    override fun backSlot45(): Int = 40

    override fun footerLeftSlot54(): Int = 45

    override fun footerCenterSlot54(): Int = 49

    override fun backSlot54(): Int = footerCenterSlot54()

    override fun confirmation45(): GuiConfirmationLayout {
        return GuiConfirmationLayout(
            size = size45(),
            previewSlot = 22,
            confirmSlot = 20,
            cancelSlot = 24
        )
    }

    override fun pagedList54(): GuiPagedListLayout {
        return GuiPagedListLayout(
            size = size54(),
            previousPageSlot = 0,
            nextPageSlot = 8,
            backSlot = footerLeftSlot54(),
            infoSlot = footerCenterSlot54(),
            itemSlots = (9..44).toList()
        )
    }

    override fun settings54(): GuiSettingsLayout {
        return GuiSettingsLayout(
            size = size54(),
            backSlot = footerLeftSlot54(),
            infoSlot = footerCenterSlot54()
        )
    }

    override fun threeChoice45(): GuiThreeChoiceLayout {
        return GuiThreeChoiceLayout(
            size = size45(),
            leftSlot = 20,
            centerSlot = 22,
            rightSlot = 24,
            backSlot = backSlot45()
        )
    }

    override fun free45(): GuiFreeLayout {
        return GuiFreeLayout(
            size = size45(),
            backSlot = backSlot45(),
            style = GuiLayoutStyle.FREE_45
        )
    }

    override fun free54(): GuiFreeLayout {
        return GuiFreeLayout(
            size = size54(),
            backSlot = backSlot54(),
            style = GuiLayoutStyle.FREE_54
        )
    }

    override fun applyStandardFrame(inventory: Inventory) {
        guiElementService.applyFrame(
            inventory,
            GuiFrameSpec(
                header = GuiFrameSection.Row(frameElement),
                footer = GuiFrameSection.Row(frameElement),
                emptySlot = emptyElement
            )
        )
    }
}
