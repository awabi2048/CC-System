package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component

interface LoreService {
    fun render(spec: GuiLoreSpec): List<Component>
}
