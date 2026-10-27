package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiOutline
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiTextAlignment
import com.awabi2048.ccsystem.api.gesturegui.theme.GestureGuiTheme
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
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

    // ─── テーマ適用部品 ────────────────────────────────────
    //
    // 見た目の意味（選択中・設定済み・無効等）から素材・色を導く部品群です。
    // 値は cross-system-ui-design.md の Gesture GUI 規則に対応し、
    // 素材・サイズの直書きは呼び出し側へ残しません。

    /**
     * 設定タブです。選択中は [theme] の `accentDeep` 背景＋幅の右伸長で示し、
     * 文言位置は動かしません（左端固定・右へ `selectedExtension` 伸長）。
     * 幅伸長は `baseWidth` が Fixed のときだけ適用します。
     * 警告は `warning` 指定時に文言を赤で示します。
     */
    fun tab(
        id: String,
        label: Component,
        actionId: String,
        theme: GestureGuiTheme,
        selected: Boolean,
        warning: Boolean = false,
        baseWidth: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.10),
        selectedExtension: Double = 0.15,
        textSize: Double = theme.textLabel,
        lineWidth: Int = 90,
        acceptedGestures: Set<GestureGuiGesture> = setOf(GestureGuiGesture.PRIMARY),
        hover: GestureGuiHover? = null,
        gestureGuard: ((Player, GestureGuiGesture) -> Boolean)? = null,
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode {
        val base = baseWidth as? GestureGuiSizeSpec.Fixed
        val extended = selected && base != null
        val width = if (extended) GestureGuiSizeSpec.Fixed(base!!.value * (1.0 + selectedExtension)) else baseWidth
        // 伸長時は基準幅ぶんの左側領域へ文言を固定し、背景だけが右へ伸びる形にします。
        val labelBoxWidth = if (extended) baseWidth else width
        return GestureGuiOverlay(
            children = listOf(
                GestureGuiBlock(
                    blockData = if (selected) theme.accentDeep else theme.surface,
                    width = GestureGuiSizeSpec.Auto,
                    height = GestureGuiSizeSpec.Auto,
                    id = "$id-bg",
                ),
                GestureGuiText(
                    text = if (warning) label.color(NamedTextColor.RED) else label,
                    size = textSize,
                    lineWidth = lineWidth,
                    alignment = GestureGuiTextAlignment.CENTER,
                    id = "$id-label",
                    width = labelBoxWidth,
                    height = GestureGuiSizeSpec.Auto,
                ),
            ),
            id = id,
            actionId = actionId,
            acceptedGestures = acceptedGestures,
            gestureGuard = gestureGuard,
            hover = hover,
            width = width,
            height = height,
            margin = margin,
            // 伸長時に基準幅のラベル箱が左端へ寄るよう、交差軸は先頭寄せです。
            crossAlignment = GestureGuiCrossAlignment.START,
            mainArrangement = GestureGuiMainArrangement.CENTER,
        )
    }

    /**
     * ページ移動行です。`◀ n/m ▶` の3区画を1行で構成し、
     * 端ページ側の矢印は無効背景＋消費面で描きます（入力を素通ししません）。
     *
     * @param status 現在位置の文言です（例 `"2/5"`）。ローカライズは呼び出し側です。
     * @param prevActionId / nextActionId ページ操作の action ID です。
     *   null の側は無効表示＋消費面になります。
     */
    fun pager(
        id: String,
        theme: GestureGuiTheme,
        status: Component,
        prevActionId: String?,
        nextActionId: String?,
        prevLabel: Component = Component.text("◀"),
        nextLabel: Component = Component.text("▶"),
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.10),
        arrowWidth: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.16),
        gap: Double = 0.02,
        statusSize: Double = theme.textCaption,
        arrowSize: Double = theme.textLabel,
        prevGestures: Set<GestureGuiGesture> = setOf(GestureGuiGesture.PRIMARY),
        nextGestures: Set<GestureGuiGesture> = prevGestures,
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = GestureGuiRow(
        children = listOf(
            pagerArrow("$id-prev", prevLabel, prevActionId, prevGestures, theme, arrowWidth, height, arrowSize),
            GestureGuiText(
                text = status,
                size = statusSize,
                alignment = GestureGuiTextAlignment.CENTER,
                id = "$id-status",
                width = GestureGuiSizeSpec.Fraction(1.0),
                height = GestureGuiSizeSpec.Auto,
            ),
            pagerArrow("$id-next", nextLabel, nextActionId, nextGestures, theme, arrowWidth, height, arrowSize),
        ),
        id = id,
        width = width,
        height = height,
        gap = gap,
        margin = margin,
        crossAlignment = GestureGuiCrossAlignment.CENTER,
    )

    private fun pagerArrow(
        id: String,
        label: Component,
        actionId: String?,
        gestures: Set<GestureGuiGesture>,
        theme: GestureGuiTheme,
        width: GestureGuiSizeSpec,
        height: GestureGuiSizeSpec,
        textSize: Double,
    ): GestureGuiNode = GestureGuiOverlay(
        children = listOf(
            GestureGuiBlock(
                blockData = if (actionId != null) theme.accent else theme.disabled,
                width = GestureGuiSizeSpec.Auto,
                height = GestureGuiSizeSpec.Auto,
                id = "$id-bg",
            ),
            GestureGuiText(
                text = label,
                size = textSize,
                alignment = GestureGuiTextAlignment.CENTER,
                id = "$id-glyph",
            ),
        ),
        id = id,
        actionId = actionId,
        acceptedGestures = if (actionId != null) gestures else emptySet(),
        consumeInput = actionId == null,
        width = width,
        height = height,
        mainArrangement = GestureGuiMainArrangement.CENTER,
        crossAlignment = GestureGuiCrossAlignment.CENTER,
    )

    /**
     * 選択・設定項目カードです。状態から背景・枠・操作面を導きます。
     *
     * 背景：`enabled` なら `disabled`、`toggleValue` 指定時は ON/OFF の値色、
     * それ以外は `surface`（値状態で通常背景を変えません）。
     * 枠：`selected` → 白縁、`configured` → 空色縁（選択中を優先）。
     * 無効時は操作面を持たず `consumeInput` で入力だけを消費します。
     * 無効かつ文言に色指定がない場合は灰色文字へ補正します。
     */
    fun choiceCard(
        id: String,
        label: Component,
        theme: GestureGuiTheme,
        actionId: String? = null,
        enabled: Boolean = true,
        selected: Boolean = false,
        configured: Boolean = false,
        toggleValue: Boolean? = null,
        acceptedGestures: Set<GestureGuiGesture> = setOf(GestureGuiGesture.PRIMARY),
        hover: GestureGuiHover? = null,
        gestureGuard: ((Player, GestureGuiGesture) -> Boolean)? = null,
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.10),
        textSize: Double = theme.textLabel,
        textAlignment: GestureGuiTextAlignment = GestureGuiTextAlignment.CENTER,
        lineWidth: Int = 160,
        outlineRatio: Double = 0.10,
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode {
        val background = when {
            !enabled -> theme.disabled
            toggleValue != null -> if (toggleValue) theme.on else theme.off
            else -> theme.surface
        }
        val outline = when {
            selected -> theme.outlineSelected
            configured -> theme.outlineSet
            else -> null
        }?.let { GestureGuiOutline(it, outlineRatio) }
        val effectiveLabel = if (!enabled && label.color() == null) {
            label.color(NamedTextColor.GRAY)
        } else {
            label
        }
        return GestureGuiOverlay(
            children = listOf(
                GestureGuiBlock(
                    blockData = background,
                    width = GestureGuiSizeSpec.Auto,
                    height = GestureGuiSizeSpec.Auto,
                    outline = outline,
                    id = "$id-bg",
                ),
                GestureGuiText(
                    text = effectiveLabel,
                    size = textSize,
                    lineWidth = lineWidth,
                    alignment = textAlignment,
                    id = "$id-label",
                ),
            ),
            id = id,
            actionId = if (enabled) actionId else null,
            acceptedGestures = if (enabled) acceptedGestures else emptySet(),
            consumeInput = !enabled,
            gestureGuard = gestureGuard,
            hover = hover,
            width = width,
            height = height,
            margin = margin,
            mainArrangement = GestureGuiMainArrangement.CENTER,
            crossAlignment = GestureGuiCrossAlignment.CENTER,
        )
    }

    /**
     * 「項目名 現在値」の結合ラベルを持つ設定カードです。
     * 未設定の値は呼び出し側が「未設定」文言として渡します（規則：値はラベル内へ直接表示）。
     */
    fun valueCard(
        id: String,
        label: Component,
        value: Component,
        theme: GestureGuiTheme,
        actionId: String? = null,
        enabled: Boolean = true,
        configured: Boolean = false,
        selected: Boolean = false,
        acceptedGestures: Set<GestureGuiGesture> = setOf(GestureGuiGesture.PRIMARY),
        hover: GestureGuiHover? = null,
        gestureGuard: ((Player, GestureGuiGesture) -> Boolean)? = null,
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.10),
        textSize: Double = theme.textLabel,
        lineWidth: Int = 160,
        outlineRatio: Double = 0.10,
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = choiceCard(
        id = id,
        label = label.append(Component.text(" ")).append(value),
        theme = theme,
        actionId = actionId,
        enabled = enabled,
        selected = selected,
        configured = configured,
        acceptedGestures = acceptedGestures,
        hover = hover,
        gestureGuard = gestureGuard,
        width = width,
        height = height,
        textSize = textSize,
        lineWidth = lineWidth,
        outlineRatio = outlineRatio,
        margin = margin,
    )

    /**
     * 一覧行です。主部品（カード等）と端操作（ボタン等）を1行へ並べます。
     * 主部品は残余幅を占めるよう `Fraction` または `Percent` 幅を推奨します。
     */
    fun listRow(
        id: String,
        content: GestureGuiNode,
        actions: List<GestureGuiNode> = emptyList(),
        gap: Double = 0.02,
        height: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = GestureGuiRow(
        children = listOf(content) + actions,
        id = id,
        width = GestureGuiSizeSpec.Percent(1.0),
        height = height,
        gap = gap,
        margin = margin,
        crossAlignment = GestureGuiCrossAlignment.CENTER,
    )

    /** グループ境界線の細帯です。表示専用で、操作面・ホバーを持ちません。 */
    fun divider(
        id: String,
        theme: GestureGuiTheme,
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
        thickness: GestureGuiSizeSpec = GestureGuiSizeSpec.Fixed(0.012),
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = GestureGuiBlock(
        blockData = theme.divider,
        width = width,
        height = thickness,
        id = id,
        margin = margin,
    )

    /**
     * 常設説明領域です。本文＋任意の詳細行を補足サイズで描きます。
     * ホバー置換の対象にする場合は呼び出し側が同じ `id` を参照します。
     */
    fun description(
        id: String,
        body: Component,
        theme: GestureGuiTheme,
        detail: Component? = null,
        width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
        alignment: GestureGuiTextAlignment = GestureGuiTextAlignment.CENTER,
        lineWidth: Int = 280,
        gap: Double = 0.005,
        margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    ): GestureGuiNode = GestureGuiColumn(
        children = listOfNotNull(
            GestureGuiText(
                text = body,
                size = theme.textCaption,
                lineWidth = lineWidth,
                alignment = alignment,
                id = id,
                width = GestureGuiSizeSpec.Percent(1.0),
                height = GestureGuiSizeSpec.Auto,
            ),
            detail?.let {
                GestureGuiText(
                    text = it,
                    size = theme.textCaption,
                    lineWidth = lineWidth,
                    alignment = alignment,
                    id = "$id-detail",
                    width = GestureGuiSizeSpec.Percent(1.0),
                    height = GestureGuiSizeSpec.Auto,
                )
            },
        ),
        id = "$id-area",
        width = width,
        height = GestureGuiSizeSpec.Auto,
        gap = gap,
        margin = margin,
    )
}
