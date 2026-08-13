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

    private fun view(owner: Player, index: Int): GestureGuiView {
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
            GestureGuiScreenDefinition("demo-$index", elements, access = GestureGuiAccess.PUBLIC),
            visuals,
        ) { context ->
            val actor = Bukkit.getPlayer(context.actorId)
            actor?.sendMessage(
                api.getLocalized(
                    actor,
                    GestureGuiKeys.GESTURE_GUI_DEMO_ACTION,
                    mapOf("gesture" to context.gesture.name),
                )
            )
            if (context.elementId == "title-text" && context.gesture == GestureGuiGesture.PRIMARY) {
                api.getGestureGuiService().openChild(
                    context.ownerId,
                    dialog(owner),
                    GestureGuiChildOptions(context.screenId, allowParentInteraction = false),
                )
            }
        }
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

    private fun text(player: Player, key: com.awabi2048.ccsystem.api.localization.LocalizationKey<String>): Component =
        Component.text(api.getLocalized(player, key))
}
