package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiOutline
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiTextAlignment
import net.kyori.adventure.text.Component
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack

/**
 * 再利用可能な複合部品です。
 *
 * 新しい Display 型ではなく、primitive（Box / Text / Block / Item 等）への
 * 展開関数とします。見た目（素材・文言・寸法）は呼び出し側指定とし、
 * CC-System 側で背景・配色を補完しません（Gesture GUI 方針）。
 */
object GestureGuiComponents {
    /**
     * 背景矩形＋中央文言のボタンです。
     * 操作面は解決 bounds から自動生成され、visual との二重管理は不要です。
     */
    fun button(
        id: String,
        label: Component,
        actionId: String,
        background: BlockData,
        // 既定は fill です。交差軸での Fraction は無効扱いのため避けます。
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.12),
        textSize: Double = 0.0055,
        textAlignment: GestureGuiTextAlignment = GestureGuiTextAlignment.CENTER,
        outline: GestureGuiOutline? = null,
        glowColor: Int? = null,
        acceptedGestures: Set<GestureGuiGesture> = setOf(GestureGuiGesture.PRIMARY),
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = GestureGuiOverlay(
        children = listOf(
            GestureGuiBlock(
                blockData = background,
                width = GestureGuiSizeSpec.Auto,
                height = GestureGuiSizeSpec.Auto,
                glowColor = glowColor,
                outline = outline,
                id = "$id-bg",
            ),
            GestureGuiText(
                text = label,
                size = textSize,
                alignment = textAlignment,
                id = "$id-label",
            ),
        ),
        id = id,
        actionId = actionId,
        acceptedGestures = acceptedGestures,
        width = width,
        height = height,
        margin = margin,
        mainArrangement = GestureGuiMainArrangement.CENTER,
        crossAlignment = GestureGuiCrossAlignment.CENTER,
    )

    /** アイテム図柄付きのボタンです。 */
    fun iconButton(
        id: String,
        item: ItemStack,
        actionId: String,
        background: BlockData? = null,
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.16),
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.16),
        itemScale: Double = 0.22,
        itemWidth: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(0.6),
        itemHeight: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(0.6),
        glowColor: Int? = null,
        acceptedGestures: Set<GestureGuiGesture> = setOf(GestureGuiGesture.PRIMARY),
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode {
        val foreground = GestureGuiItem(
            item = item,
            scale = itemScale,
            width = itemWidth,
            height = itemHeight,
            glowColor = glowColor,
            id = "$id-icon",
        )
        val children = if (background != null) {
            listOf(
                GestureGuiBlock(
                    blockData = background,
                    width = GestureGuiSizeSpec.Auto,
                    height = GestureGuiSizeSpec.Auto,
                    id = "$id-bg",
                ),
                foreground,
            )
        } else {
            listOf(foreground)
        }
        return GestureGuiOverlay(
            children = children,
            id = id,
            actionId = actionId,
            acceptedGestures = acceptedGestures,
            width = width,
            height = height,
            margin = margin,
            mainArrangement = GestureGuiMainArrangement.CENTER,
            crossAlignment = GestureGuiCrossAlignment.CENTER,
        )
    }

    /** 背景付きの内容箱です。 */
    fun card(
        id: String,
        background: BlockData,
        content: List<GestureGuiNode>,
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
        padding: GestureGuiEdgeInsets = GestureGuiEdgeInsets(
            left = 0.03,
            top = 0.03,
            right = 0.03,
            bottom = 0.03,
        ),
        gap: Double = 0.02,
        outline: GestureGuiOutline? = null,
        actionId: String? = null,
        acceptedGestures: Set<GestureGuiGesture> = if (actionId != null) {
            setOf(GestureGuiGesture.PRIMARY)
        } else {
            emptySet()
        },
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = GestureGuiOverlay(
        children = listOf(
            GestureGuiBlock(
                blockData = background,
                width = GestureGuiSizeSpec.Auto,
                height = GestureGuiSizeSpec.Auto,
                outline = outline,
                id = "$id-bg",
            ),
            GestureGuiColumn(
                children = content,
                id = "$id-body",
                width = GestureGuiSizeSpec.Auto,
                height = GestureGuiSizeSpec.Auto,
                padding = padding,
                gap = gap,
            ),
        ),
        id = id,
        actionId = actionId,
        acceptedGestures = acceptedGestures,
        width = width,
        height = height,
        margin = margin,
    )

    /**
     * 設定タブ等の選択肢です。選択状態の見た目差は素材の切り替えで表します。
     * 状態は色だけでなく、文言・素材でも区別できるよう呼び出し側で指定します。
     */
    fun tab(
        id: String,
        label: Component,
        actionId: String,
        selected: Boolean,
        selectedBackground: BlockData,
        unselectedBackground: BlockData,
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.1),
        textSize: Double = 0.0055,
        acceptedGestures: Set<GestureGuiGesture> = setOf(GestureGuiGesture.PRIMARY),
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = button(
        id = id,
        label = label,
        actionId = actionId,
        background = if (selected) selectedBackground else unselectedBackground,
        width = width,
        height = height,
        textSize = textSize,
        acceptedGestures = acceptedGestures,
        margin = margin,
    )

    /** 見出し付きの区分です。 */
    fun section(
        id: String,
        title: Component,
        children: List<GestureGuiNode>,
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
        titleSize: Double = 0.006,
        gap: Double = 0.02,
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = GestureGuiColumn(
        children = listOf(
            GestureGuiText(
                text = title,
                size = titleSize,
                alignment = GestureGuiTextAlignment.LEFT,
                id = "$id-title",
            ),
        ) + children,
        id = id,
        width = width,
        height = height,
        gap = gap,
        margin = margin,
    )

    /** 項目名と現在値の対です。 */
    fun labelValue(
        id: String,
        label: Component,
        value: Component,
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
        labelWeight: Double = 1.0,
        valueWeight: Double = 1.0,
        textSize: Double = 0.006,
        gap: Double = 0.02,
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = GestureGuiRow(
        children = listOf(
            GestureGuiText(
                text = label,
                size = textSize,
                alignment = GestureGuiTextAlignment.LEFT,
                id = "$id-label",
                width = GestureGuiSizeSpec.Fraction(labelWeight),
            ),
            GestureGuiText(
                text = value,
                size = textSize,
                alignment = GestureGuiTextAlignment.RIGHT,
                id = "$id-value",
                width = GestureGuiSizeSpec.Fraction(valueWeight),
            ),
        ),
        id = id,
        width = width,
        height = GestureGuiSizeSpec.Auto,
        gap = gap,
        margin = margin,
    )

    /**
     * 対象・確定・取消を固定役割として扱う確認画面です。
     * 最初に新 layout API へ移す確認系画面の雛形とします。
     */
    fun confirmation(
        id: String,
        message: Component,
        confirmLabel: Component,
        cancelLabel: Component,
        confirmAction: String,
        cancelAction: String,
        confirmBackground: BlockData,
        cancelBackground: BlockData,
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(0.8),
        messageSize: Double = 0.006,
        buttonHeight: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.1),
        gap: Double = 0.03,
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = GestureGuiColumn(
        children = listOf(
            GestureGuiText(
                text = message,
                size = messageSize,
                id = "$id-message",
            ),
            GestureGuiRow(
                children = listOf(
                    button(
                        id = "$id-confirm",
                        label = confirmLabel,
                        actionId = confirmAction,
                        background = confirmBackground,
                        width = GestureGuiSizeSpec.Fraction(1.0),
                        height = buttonHeight,
                    ),
                    button(
                        id = "$id-cancel",
                        label = cancelLabel,
                        actionId = cancelAction,
                        background = cancelBackground,
                        width = GestureGuiSizeSpec.Fraction(1.0),
                        height = buttonHeight,
                    ),
                ),
                id = "$id-actions",
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Auto,
                gap = gap,
            ),
        ),
        id = id,
        width = width,
        height = GestureGuiSizeSpec.Auto,
        gap = gap,
        margin = margin,
    )
}
