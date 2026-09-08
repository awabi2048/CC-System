package com.awabi2048.ccsystem.features.misc.gesturegui

import com.awabi2048.ccsystem.api.CCSystemAPI
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiAccess
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiActionContext
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiChildOptions
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiView
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiVisual
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiAbsoluteOffsets
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiBlock
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiBox
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiDocument
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHover
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHoverPosition
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHoverTarget
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiItem
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutFacade
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiNode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiOverlay
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiSizeSpec
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiText
import com.awabi2048.ccsystem.api.localization.generated.GestureGuiKeys
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 汎用APIの全描画・入力経路を実サーバーで確認するための管理者向け画面です。
 *
 * 画面記述は宣言ノードで示し、低レベル View の直書きは行いません。
 * 解決・変換は公開入口の [GestureGuiLayoutFacade] を用います。
 */
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

    /** パネル内容域の寸法です（枠を除く）。絶対配置の基準にします。 */
    private fun contentSize(panel: GestureGuiPanel): Pair<Double, Double> =
        (panel.width - panel.frameWidth * 2.0) to (panel.height - panel.frameWidth * 2.0)

    /**
     * 画面中央原点の矩形を、パネル内容域基準の絶対オフセットへ変換します。
     * 中心座標は従来の直書き配置と同一であり、宣言移行後も見た目を維持します。
     */
    private fun at(panel: GestureGuiPanel, cx: Double, cy: Double, w: Double, h: Double): GestureGuiAbsoluteOffsets {
        val (contentWidth, contentHeight) = contentSize(panel)
        return within(contentWidth, contentHeight, cx, cy, w, h)
    }

    /** 親矩形（幅・高さ・中央原点）内の矩形を、親内容域基準の絶対オフセットへ変換します。 */
    private fun within(
        parentWidth: Double,
        parentHeight: Double,
        cx: Double,
        cy: Double,
        w: Double,
        h: Double,
    ): GestureGuiAbsoluteOffsets = GestureGuiAbsoluteOffsets(
        left = cx - w / 2.0 + parentWidth / 2.0,
        top = parentHeight / 2.0 - cy - h / 2.0,
    )

    /** 宣言ノードを解決・変換し、低レベル View を得ます。診断は警告として残します。 */
    private fun compile(
        root: GestureGuiNode,
        panel: GestureGuiPanel,
        screenId: String,
        handlers: Map<String, (GestureGuiActionContext) -> Unit>,
    ): GestureGuiView {
        val resolved = GestureGuiLayoutFacade.layout(GestureGuiDocument(root, panel))
        resolved.diagnostics.forEach { diagnostic ->
            Bukkit.getLogger().warning("[GestureGuiDemo] layout diagnostic: $diagnostic")
        }
        val compiled = GestureGuiLayoutFacade.compile(
            resolved,
            screenId,
            actionHandlers = handlers,
            access = GestureGuiAccess.PUBLIC,
        )
        compiled.diagnostics.forEach { diagnostic ->
            Bukkit.getLogger().warning("[GestureGuiDemo] compile diagnostic: $diagnostic")
        }
        return compiled.view
    }

    /** 全て絶対配置の画面ルートです。ルートはパネル内容域へ広げます。 */
    private fun screenRoot(children: List<GestureGuiNode>): GestureGuiNode = GestureGuiBox(
        children = children,
        width = GestureGuiSizeSpec.Percent(1.0),
        height = GestureGuiSizeSpec.Percent(1.0),
    )

    private fun staticText(
        panel: GestureGuiPanel,
        id: String,
        cx: Double,
        cy: Double,
        w: Double,
        h: Double,
        text: Component,
        size: Double,
        lineWidth: Int = 160,
    ): GestureGuiNode = GestureGuiText(
        text = text,
        size = size,
        lineWidth = lineWidth,
        id = id,
        width = GestureGuiSizeSpec.Fixed(w),
        height = GestureGuiSizeSpec.Fixed(h),
        absolute = at(panel, cx, cy, w, h),
    )

    private fun controlsView(owner: Player): GestureGuiView {
        val panel = GestureGuiPanel()
        val gestures = GestureGuiGesture.entries
        val elementWidth = 0.24
        val buttonHeight = 0.29
        val buttonCenterY = -0.12
        val startX = -0.60
        val nodes = mutableListOf<GestureGuiNode>()
        nodes += staticText(panel, "title", 0.0, 0.27, 0.7, 0.04, text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_TITLE), 0.010)
        nodes += staticText(
            panel, "description", 0.0, 0.15, 1.4, 0.03,
            text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_DESCRIPTION), 0.0065,
        )
        gestures.forEachIndexed { column, gesture ->
            val centerX = startX + column * 0.30
            val buttonId = "button-${gesture.name.lowercase()}"
            val iconId = "icon-${gesture.name}"
            val labelId = "$buttonId-label"
            nodes += GestureGuiOverlay(
                children = listOf(
                    GestureGuiBlock(
                        blockData = Bukkit.createBlockData(
                            if (column % 2 == 0) Material.LIGHT_BLUE_CONCRETE else Material.CYAN_CONCRETE,
                        ),
                        width = GestureGuiSizeSpec.Auto,
                        height = GestureGuiSizeSpec.Auto,
                        id = "$buttonId-bg",
                    ),
                    GestureGuiItem(
                        item = ItemStack(icon(gesture)),
                        scale = 0.14,
                        width = GestureGuiSizeSpec.Fixed(0.14),
                        height = GestureGuiSizeSpec.Fixed(0.14),
                        id = iconId,
                        // 図柄は操作面の中央から少し上に寄せます（従来配置と同じ）。
                        absolute = within(elementWidth, buttonHeight, 0.0, 0.03, 0.14, 0.14),
                    ),
                    GestureGuiText(
                        text = label(owner, gesture),
                        size = 0.0045,
                        lineWidth = 70,
                        id = labelId,
                        width = GestureGuiSizeSpec.Fixed(0.22),
                        height = GestureGuiSizeSpec.Fixed(0.03),
                        // 文言は操作面の下部に寄せます（従来配置と同じ）。
                        absolute = within(elementWidth, buttonHeight, 0.0, -0.11, 0.22, 0.03),
                    ),
                ),
                id = buttonId,
                actionId = "demo-action",
                acceptedGestures = setOf(gesture),
                width = GestureGuiSizeSpec.Fixed(elementWidth),
                height = GestureGuiSizeSpec.Fixed(buttonHeight),
                absolute = at(panel, centerX, buttonCenterY, elementWidth, buttonHeight),
                hover = GestureGuiHover(
                    text = label(owner, gesture),
                    size = 0.006,
                    lineWidth = 100,
                    position = GestureGuiHoverPosition.Fixed(centerX, 0.06),
                    replacement = GestureGuiHoverTarget.Node(iconId),
                ),
            )
        }
        // 見出し自体も操作面であり、子画面を開きます。表示の置換対象は見出し文言です。
        nodes += GestureGuiBox(
            children = emptyList(),
            id = "title-text",
            actionId = "demo-title",
            width = GestureGuiSizeSpec.Fixed(0.7),
            height = GestureGuiSizeSpec.Fixed(0.13),
            absolute = at(panel, 0.0, 0.275, 0.7, 0.13),
            hover = GestureGuiHover(
                text = text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_DESCRIPTION),
                size = 0.005,
                position = GestureGuiHoverPosition.Fixed(0.0, 0.10),
                replacement = GestureGuiHoverTarget.Node("title"),
            ),
        )
        nodes += staticText(
            panel, "exit", 0.0, -0.33, 1.0, 0.03,
            text(owner, GestureGuiKeys.GESTURE_GUI_EXIT_GUIDANCE), 0.005,
        )
        return compile(
            screenRoot(nodes),
            panel,
            "demo-controls",
            mapOf(
                "demo-action" to { context -> notifyAction(context.actorId, context.gesture) },
                "demo-title" to { context ->
                    notifyAction(context.actorId, context.gesture)
                    if (context.gesture == GestureGuiGesture.PRIMARY) {
                        api.getGestureGuiService().openChild(
                            context.ownerId,
                            dialog(owner),
                            GestureGuiChildOptions(context.screenId, allowParentInteraction = false),
                        )
                    }
                },
            ),
        )
    }

    private fun statusView(owner: Player): GestureGuiView {
        val panel = GestureGuiPanel()
        val rows = listOf(
            StatusRow("health", 0.16, Material.RED_CONCRETE, GestureGuiKeys.GESTURE_GUI_DEMO_STATUS_HEALTH),
            StatusRow("energy", 0.00, Material.LIME_CONCRETE, GestureGuiKeys.GESTURE_GUI_DEMO_STATUS_ENERGY),
            StatusRow("ready", -0.16, Material.LIGHT_BLUE_CONCRETE, GestureGuiKeys.GESTURE_GUI_DEMO_STATUS_READY),
        )
        val rowWidth = 0.82
        val rowHeight = 0.12
        val rowCenterX = 0.41
        val nodes = mutableListOf<GestureGuiNode>()
        nodes += staticText(
            panel, "status-title", 0.0, 0.37, 1.2, 0.04,
            text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_STATUS_TITLE), 0.009,
        )
        nodes += staticText(
            panel, "status-description", 0.0, 0.27, 1.4, 0.03,
            text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_STATUS_DESCRIPTION), 0.0055,
        )
        nodes += GestureGuiBlock(
            blockData = Bukkit.createBlockData(Material.BLUE_TERRACOTTA),
            width = GestureGuiSizeSpec.Fixed(0.58),
            height = GestureGuiSizeSpec.Fixed(0.48),
            id = "status-portrait-background",
            absolute = at(panel, -0.51, -0.02, 0.58, 0.48),
        )
        nodes += GestureGuiItem(
            item = ItemStack(Material.TOTEM_OF_UNDYING),
            scale = 0.25,
            width = GestureGuiSizeSpec.Fixed(0.25),
            height = GestureGuiSizeSpec.Fixed(0.25),
            id = "status-portrait",
            absolute = at(panel, -0.51, 0.02, 0.25, 0.25),
        )
        rows.forEach { row ->
            val labelId = "status-label-${row.id}"
            nodes += GestureGuiOverlay(
                children = listOf(
                    GestureGuiBlock(
                        blockData = Bukkit.createBlockData(row.material),
                        width = GestureGuiSizeSpec.Auto,
                        height = GestureGuiSizeSpec.Auto,
                        id = "status-row-${row.id}",
                    ),
                    GestureGuiText(
                        text = text(owner, row.label),
                        size = 0.0055,
                        id = labelId,
                        width = GestureGuiSizeSpec.Fixed(0.78),
                        height = GestureGuiSizeSpec.Fixed(0.04),
                        absolute = within(rowWidth, rowHeight, 0.0, -0.02, 0.78, 0.04),
                    ),
                ),
                id = "status-${row.id}",
                actionId = "demo-action",
                width = GestureGuiSizeSpec.Fixed(rowWidth),
                height = GestureGuiSizeSpec.Fixed(rowHeight),
                absolute = at(panel, rowCenterX, row.y, rowWidth, rowHeight),
                hover = GestureGuiHover(
                    text = text(owner, row.label),
                    size = 0.005,
                    position = GestureGuiHoverPosition.Fixed(rowCenterX, row.y + 0.10),
                    replacement = GestureGuiHoverTarget.Node(labelId),
                ),
            )
        }
        nodes += staticText(
            panel, "status-exit", 0.0, -0.39, 1.0, 0.03,
            text(owner, GestureGuiKeys.GESTURE_GUI_EXIT_GUIDANCE), 0.005,
        )
        return compile(
            screenRoot(nodes),
            panel,
            "demo-status",
            mapOf("demo-action" to { context -> notifyAction(context.actorId, context.gesture) }),
        )
    }

    private fun choiceView(owner: Player): GestureGuiView {
        val panel = GestureGuiPanel()
        val choices = listOf(
            ChoiceCard("builder", -0.45, 0.10, Material.BRICKS, Material.ORANGE_TERRACOTTA, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_BUILDER),
            ChoiceCard("explorer", 0.45, 0.10, Material.COMPASS, Material.GREEN_TERRACOTTA, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_EXPLORER),
            ChoiceCard("trader", -0.45, -0.20, Material.EMERALD, Material.LIME_TERRACOTTA, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_TRADER),
            ChoiceCard("guardian", 0.45, -0.20, Material.SHIELD, Material.BLUE_TERRACOTTA, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_GUARDIAN),
        )
        val cardWidth = 0.72
        val cardHeight = 0.24
        val nodes = mutableListOf<GestureGuiNode>()
        nodes += staticText(
            panel, "choice-title", 0.0, 0.40, 1.2, 0.04,
            text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_TITLE), 0.009,
        )
        nodes += staticText(
            panel, "choice-description", 0.0, 0.31, 1.4, 0.03,
            text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_CHOICE_DESCRIPTION), 0.0055,
        )
        choices.forEach { choice ->
            val iconId = "choice-icon-${choice.id}"
            nodes += GestureGuiOverlay(
                children = listOf(
                    GestureGuiBlock(
                        blockData = Bukkit.createBlockData(choice.background),
                        width = GestureGuiSizeSpec.Auto,
                        height = GestureGuiSizeSpec.Auto,
                        id = "choice-card-${choice.id}",
                    ),
                    GestureGuiItem(
                        item = ItemStack(choice.icon),
                        scale = 0.13,
                        width = GestureGuiSizeSpec.Fixed(0.13),
                        height = GestureGuiSizeSpec.Fixed(0.13),
                        id = iconId,
                        // 図柄は左寄り・中央より少し上に置きます（従来配置と同じ）。
                        absolute = within(cardWidth, cardHeight, -0.20, 0.01, 0.13, 0.13),
                    ),
                    GestureGuiText(
                        text = text(owner, choice.label),
                        size = 0.0055,
                        lineWidth = 80,
                        id = "choice-label-${choice.id}",
                        width = GestureGuiSizeSpec.Fixed(0.40),
                        height = GestureGuiSizeSpec.Fixed(0.05),
                        // 文言は右寄り・中央より少し下に置きます（従来配置と同じ）。
                        absolute = within(cardWidth, cardHeight, 0.10, -0.02, 0.40, 0.05),
                    ),
                ),
                id = "choice-${choice.id}",
                actionId = "demo-action",
                width = GestureGuiSizeSpec.Fixed(cardWidth),
                height = GestureGuiSizeSpec.Fixed(cardHeight),
                absolute = at(panel, choice.x, choice.y, cardWidth, cardHeight),
                hover = GestureGuiHover(
                    text = text(owner, choice.label),
                    size = 0.005,
                    position = GestureGuiHoverPosition.Fixed(choice.x, choice.y + 0.17),
                    replacement = GestureGuiHoverTarget.Node(iconId),
                ),
            )
        }
        nodes += staticText(
            panel, "choice-exit", 0.0, -0.42, 1.0, 0.03,
            text(owner, GestureGuiKeys.GESTURE_GUI_EXIT_GUIDANCE), 0.005,
        )
        return compile(
            screenRoot(nodes),
            panel,
            "demo-choice",
            mapOf("demo-action" to { context -> notifyAction(context.actorId, context.gesture) }),
        )
    }

    private fun dialog(owner: Player): GestureGuiView {
        val panel = GestureGuiPanel(width = 1.1, height = 0.55, backgroundMaterial = Material.GRAY_CONCRETE)
        // 閉じる文言自体が操作面であり、枠いっぱいに受け付けます（従来配置と同じ）。
        val nodes = listOf(
            staticText(
                panel, "dialog-title", 0.0, 0.13, 0.9, 0.06,
                text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_DESCRIPTION), 0.006, 120,
            ),
            GestureGuiText(
                text = text(owner, GestureGuiKeys.GESTURE_GUI_DEMO_DIALOG_CLOSE),
                size = 0.006,
                id = "dialog-close",
                actionId = "dialog-close",
                width = GestureGuiSizeSpec.Fixed(0.64),
                height = GestureGuiSizeSpec.Fixed(0.20),
                absolute = at(panel, 0.0, 0.0, 0.64, 0.20),
            ),
        )
        return compile(
            screenRoot(nodes),
            panel,
            "demo-dialog",
            mapOf(
                "dialog-close" to { context ->
                    api.getGestureGuiService().closeChild(context.ownerId, context.screenId)
                },
            ),
        )
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
