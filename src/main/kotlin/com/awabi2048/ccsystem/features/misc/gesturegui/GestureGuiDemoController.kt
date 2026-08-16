package com.awabi2048.ccsystem.features.misc.gesturegui

import com.awabi2048.ccsystem.api.CCSystemAPI
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiAccess
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiChildOptions
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiBounds
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiElement
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiHoverText
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenDefinition
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiView
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVisual
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.localization.generated.GestureGuiKeys
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** 汎用APIの全描画・入力経路を実サーバーで確認するための管理者向け画面です。 */
class GestureGuiDemoController(private val api: CCSystemAPI) {
    fun open(player: Player, screenCount: Int) {
        api.getGestureGuiService().open(player, (0 until screenCount).map { screenIndex -> view(player, screenIndex) })
    }

    fun close(player: Player): Boolean = api.getGestureGuiService().close(player.uniqueId)

    /** 複数画面指定時に同じ見本を複製せず、用途の異なるレイアウトを並べます。 */
    private fun view(owner: Player, index: Int): GestureGuiView = when (index) {
        0 -> controlsView(owner)
        1 -> statusView(owner)
        else -> choiceView(owner)
    }

    private fun controlsView(owner: Player): GestureGuiView {
        val gestures = GestureGuiGesture.entries
        val elementWidth = 0.24
        val startX = -0.60
        val elements = gestures.mapIndexed { column, gesture ->
            val centerX = startX + column * 0.30
            GestureGuiElement(
                elementId = gesture.name.lowercase(),
                bounds = GestureGuiBounds(centerX - elementWidth / 2, -0.27, centerX + elementWidth / 2, 0.02),
                acceptedGestures = setOf(gesture),
                hoverText = GestureGuiHoverText(
                    text = label(owner, gesture),
                    x = centerX,
                    y = 0.06,
                    size = 0.006,
                    lineWidth = 100,
                ),
                targetVisualId = "icon-${gesture.name}",
            )
        } + GestureGuiElement(
            elementId = "title-text",
            bounds = GestureGuiBounds(-0.35, 0.21, 0.35, 0.34),
            acceptedGestures = setOf(GestureGuiGesture.PRIMARY),
            hoverText = GestureGuiHoverText(
                text = text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_DESCRIPTION),
                x = 0.0,
                y = 0.10,
                size = 0.005,
            ),
            targetVisualId = "title",
        )
        val visuals = buildList {
            add(GestureGuiVisual.Text("title", 0.0, 0.27, text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_TITLE), size = 0.010))
            add(GestureGuiVisual.Text("description", 0.0, 0.15, text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_DESCRIPTION), size = 0.0065))
            gestures.forEachIndexed { column, gesture ->
                val centerX = startX + column * 0.30
                add(
                    GestureGuiVisual.Block(
                        "button-${gesture.name}", centerX, -0.12, elementWidth, 0.29,
                        Bukkit.createBlockData(if (column % 2 == 0) Material.LIGHT_BLUE_CONCRETE else Material.CYAN_CONCRETE),
                        layer = 4,
                    )
                )
                add(GestureGuiVisual.Item("icon-${gesture.name}", centerX, -0.09, ItemStack(icon(gesture)), scale = 0.14))
                add(GestureGuiVisual.Text("label-${gesture.name}", centerX, -0.23, label(owner, gesture), size = 0.0045, lineWidth = 70))
            }
            add(GestureGuiVisual.Text("exit", 0.0, -0.33, text(owner, GestureGuiKeys.GESTURE_GUI_EXIT_GUIDANCE), size = 0.005))
        }
        return GestureGuiView(
            GestureGuiScreenDefinition("demo-controls", elements, access = GestureGuiAccess.PUBLIC),
            visuals,
        ) { context ->
            notifyAction(context.actorId, context.gesture)
            if (context.elementId == "title-text" && context.gesture == GestureGuiGesture.PRIMARY) {
                api.getGestureGuiService().openChild(
                    context.ownerId,
                    dialog(owner),
                    GestureGuiChildOptions(context.screenId, allowParentInteraction = false),
                )
            }
        }
    }

    private fun statusView(owner: Player): GestureGuiView {
        val rows = listOf(
            StatusRow("health", 0.16, Material.RED_CONCRETE, GestureGuiKeys.GESTURE_GUI_DEMO_STATUS_HEALTH),
            StatusRow("energy", 0.00, Material.LIME_CONCRETE, GestureGuiKeys.GESTURE_GUI_DEMO_STATUS_ENERGY),
            StatusRow("ready", -0.16, Material.LIGHT_BLUE_CONCRETE, GestureGuiKeys.GESTURE_GUI_DEMO_STATUS_READY),
        )
        val elements = rows.map { row ->
            GestureGuiElement(
                elementId = "status-${row.id}",
                bounds = GestureGuiBounds(0.00, row.y - 0.06, 0.82, row.y + 0.06),
                acceptedGestures = setOf(GestureGuiGesture.PRIMARY),
                hoverText = GestureGuiHoverText(text(owner, row.label), 0.41, row.y + 0.10, size = 0.005),
                targetVisualId = "status-label-${row.id}",
            )
        }
        val visuals = buildList {
            add(GestureGuiVisual.Text("status-title", 0.0, 0.37, text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_STATUS_TITLE), size = 0.009))
            add(GestureGuiVisual.Text("status-description", 0.0, 0.27, text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_STATUS_DESCRIPTION), size = 0.0055))
            add(
                GestureGuiVisual.Block(
                    "status-portrait-background", -0.51, -0.02, 0.58, 0.48,
                    Bukkit.createBlockData(Material.BLUE_TERRACOTTA), layer = 4,
                )
            )
            add(GestureGuiVisual.Item("status-portrait", -0.51, 0.02, ItemStack(Material.TOTEM_OF_UNDYING), scale = 0.25))
            rows.forEach { row ->
                add(
                    GestureGuiVisual.Block(
                        "status-row-${row.id}", 0.41, row.y, 0.82, 0.12,
                        Bukkit.createBlockData(row.material), layer = 4,
                    )
                )
                add(GestureGuiVisual.Text("status-label-${row.id}", 0.41, row.y - 0.02, text(owner, row.label), size = 0.0055))
            }
            add(GestureGuiVisual.Text("status-exit", 0.0, -0.39, text(owner, GestureGuiKeys.GESTURE_GUI_EXIT_GUIDANCE), size = 0.005))
        }
        return GestureGuiView(
            GestureGuiScreenDefinition("demo-status", elements, access = GestureGuiAccess.PUBLIC),
            visuals,
        ) { context -> notifyAction(context.actorId, context.gesture) }
    }

    private fun choiceView(owner: Player): GestureGuiView {
        val choices = listOf(
            ChoiceCard("builder", -0.45, 0.10, Material.BRICKS, Material.ORANGE_TERRACOTTA, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_BUILDER),
            ChoiceCard("explorer", 0.45, 0.10, Material.COMPASS, Material.GREEN_TERRACOTTA, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_EXPLORER),
            ChoiceCard("trader", -0.45, -0.20, Material.EMERALD, Material.LIME_TERRACOTTA, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_TRADER),
            ChoiceCard("guardian", 0.45, -0.20, Material.SHIELD, Material.BLUE_TERRACOTTA, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_GUARDIAN),
        )
        val elements = choices.map { choice ->
            GestureGuiElement(
                elementId = "choice-${choice.id}",
                bounds = GestureGuiBounds(choice.x - 0.36, choice.y - 0.12, choice.x + 0.36, choice.y + 0.12),
                acceptedGestures = setOf(GestureGuiGesture.PRIMARY),
                hoverText = GestureGuiHoverText(text(owner, choice.label), choice.x, choice.y + 0.17, size = 0.005),
                targetVisualId = "choice-icon-${choice.id}",
            )
        }
        val visuals = buildList {
            add(GestureGuiVisual.Text("choice-title", 0.0, 0.40, text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_TITLE), size = 0.009))
            add(GestureGuiVisual.Text("choice-description", 0.0, 0.31, text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_DESCRIPTION), size = 0.0055))
            choices.forEach { choice ->
                add(
                    GestureGuiVisual.Block(
                        "choice-card-${choice.id}", choice.x, choice.y, 0.72, 0.24,
                        Bukkit.createBlockData(choice.background), layer = 4,
                    )
                )
                add(GestureGuiVisual.Item("choice-icon-${choice.id}", choice.x - 0.20, choice.y + 0.01, ItemStack(choice.icon), scale = 0.13))
                add(GestureGuiVisual.Text("choice-label-${choice.id}", choice.x + 0.10, choice.y - 0.02, text(owner, choice.label), size = 0.0055, lineWidth = 80))
            }
            add(GestureGuiVisual.Text("choice-exit", 0.0, -0.42, text(owner, GestureGuiKeys.GESTURE_GUI_EXIT_GUIDANCE), size = 0.005))
        }
        return GestureGuiView(
            GestureGuiScreenDefinition("demo-choice", elements, access = GestureGuiAccess.PUBLIC),
            visuals,
        ) { context -> notifyAction(context.actorId, context.gesture) }
    }

    private fun dialog(owner: Player): GestureGuiView = GestureGuiView(
        GestureGuiScreenDefinition(
            "demo-dialog",
            listOf(
                GestureGuiElement(
                    "close-dialog",
                    GestureGuiBounds(-0.32, -0.10, 0.32, 0.10),
                    setOf(GestureGuiGesture.PRIMARY),
                    targetVisualId = "dialog-close",
                )
            ),
            access = GestureGuiAccess.PUBLIC,
        ),
        listOf(
            GestureGuiVisual.Text(
                "dialog-title", 0.0, 0.13,
                text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_DESCRIPTION), size = 0.006, lineWidth = 120,
            ),
            GestureGuiVisual.Text(
                "dialog-close", 0.0, 0.0,
                text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_DIALOG_CLOSE), size = 0.006,
            ),
        ),
        GestureGuiPanel(width = 1.1, height = 0.55, backgroundMaterial = Material.GRAY_CONCRETE),
    ) { context ->
        api.getGestureGuiService().closeChild(context.ownerId, context.screenId)
    }

    private fun label(player: Player, gesture: GestureGuiGesture): Component = when (gesture) {
        GestureGuiGesture.PRIMARY -> text(player, GestureGuiKeys.GESTURE_GUI_DEMO_PRIMARY)
        GestureGuiGesture.SECONDARY -> text(player, GestureGuiKeys.GESTURE_GUI_DEMO_SECONDARY)
        GestureGuiGesture.SHIFT_PRIMARY -> text(player, GestureGuiKeys.GESTURE_GUI_DEMO_SHIFT_PRIMARY)
        GestureGuiGesture.SHIFT_SECONDARY -> text(player, GestureGuiKeys.GESTURE_GUI_DEMO_SHIFT_SECONDARY)
        GestureGuiGesture.SWAP_HAND -> text(player, GestureGuiKeys.GESTURE_GUI_DEMO_SWAP_HAND)
    }

    private fun icon(gesture: GestureGuiGesture): Material = when (gesture) {
        GestureGuiGesture.PRIMARY -> Material.IRON_SWORD
        GestureGuiGesture.SECONDARY -> Material.LEVER
        GestureGuiGesture.SHIFT_PRIMARY -> Material.DIAMOND_PICKAXE
        GestureGuiGesture.SHIFT_SECONDARY -> Material.REPEATER
        GestureGuiGesture.SWAP_HAND -> Material.SHIELD
    }

    private fun notifyAction(actorId: java.util.UUID, gesture: GestureGuiGesture) {
        val actor = Bukkit.getPlayer(actorId) ?: return
        actor.sendMessage(
            api.getLocalized(
                actor,
                GestureGuiKeys.GESTURE_GUI_DEMO_ACTION,
                mapOf("gesture" to gesture.name),
            )
        )
    }

    private data class StatusRow(
        val id: String,
        val y: Double,
        val material: Material,
        val label: com.awabi2048.ccsystem.api.localization.LocalizationKey<String>,
    )

    private data class ChoiceCard(
        val id: String,
        val x: Double,
        val y: Double,
        val icon: Material,
        val background: Material,
        val label: com.awabi2048.ccsystem.api.localization.LocalizationKey<String>,
    )

    private fun text(player: Player, key: com.awabi2048.ccsystem.api.localization.LocalizationKey<String>): Component =
        Component.text(api.getLocalized(player, key))
}
