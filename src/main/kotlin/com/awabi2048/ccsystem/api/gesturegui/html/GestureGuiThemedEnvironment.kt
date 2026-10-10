package com.awabi2048.ccsystem.api.gesturegui.html

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiHover
import com.awabi2048.ccsystem.api.gesturegui.theme.GestureGuiTheme
import com.awabi2048.ccsystem.api.gesturegui.theme.GestureGuiThemeTokens
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * テーマ語彙を解決する [GestureGuiHtmlEnvironment] の基底実装です。
 *
 * 見た目（背景・枠・文字サイズ・アイテム）は [theme] へ委譲し、画面側は
 * `background: surface` や `class="card selected"` の語彙だけを記述します。
 * 語彙外の値は生素材名（`light_gray_concrete` 等）として解釈するため、
 * テーマ未導入の記述と併用できます。
 *
 * 振る舞い（文言・ホバー・実行可否）は要素ID基準のまま呼び出し側が提供します。
 * 各 `*ById` 関数は null を返すと未指定として扱います。
 *
 * ### 見た目の解決順
 * 1. 要素ID登録（`blockById` / `outlineById` / `itemById` / `textSizeById`）
 *    …状態に応じて変わる表示向けの登録口です。
 * 2. テーマ語彙 … `surface`・`accent` 等、および `selected` 等のclass
 * 3. 生素材名 … `light_gray_concrete` 等
 * 4. 未指定 … `button` の背景は `surface`、容器背景・枠は「なし」
 *
 * 語彙にも生素材にも解決できない明示値はエラーとします（綴り違いを
 * 透明な背景で覆い隠さないため）。背景を付けない意図は `background` 未指定で表します。
 */
open class GestureGuiThemedEnvironment(
    val theme: GestureGuiTheme,
    protected val textById: (String) -> Component? = { null },
    protected val hoverById: (String) -> GestureGuiHover? = { null },
    protected val guardById: (String) -> ((Player, GestureGuiGesture) -> Boolean)? = { null },
    protected val blockById: (String) -> BlockData? = { null },
    protected val outlineById: (String) -> BlockData? = { null },
    protected val itemById: (String) -> ItemStack? = { null },
    protected val textSizeById: (String) -> Double? = { null },
) : GestureGuiHtmlEnvironment {
    override fun textComponent(text: String, tag: String, classes: Set<String>, id: String?): Component {
        val component = id?.let(textById) ?: Component.text(text)
        // 状態classの文字色は、CSS color 指定や登録Component側の色指定へ譲ります。
        val stateColor = stateTextColor(classes) ?: return component
        return if (component.color() == null) component.color(stateColor) else component
    }

    /** 状態classの文字色です。無効は灰、警告は赤です。 */
    protected open fun stateTextColor(classes: Set<String>): NamedTextColor? = when {
        GestureGuiThemeTokens.DISABLED_CLASS in classes -> NamedTextColor.GRAY
        GestureGuiThemeTokens.WARN_CLASS in classes -> NamedTextColor.RED
        else -> null
    }

    override fun hover(tag: String, classes: Set<String>, id: String?): GestureGuiHover? =
        id?.let(hoverById)

    override fun gestureGuard(
        tag: String,
        classes: Set<String>,
        id: String?,
    ): ((Player, GestureGuiGesture) -> Boolean)? = id?.let(guardById)

    override fun buttonBackground(background: String?, classes: Set<String>, id: String?): BlockData {
        id?.let(blockById)?.let { return it }
        if (background == null) {
            // 無効項目は通常背景ではなく無効色で示します（規則：赤系は使わない）。
            return if (GestureGuiThemeTokens.DISABLED_CLASS in classes) theme.disabled else theme.surface
        }
        return theme.block(background) ?: rawBlock(background)
            ?: error("未知のボタン背景指定です: $background (id=$id)")
    }

    override fun containerBackground(background: String, classes: Set<String>, id: String?): BlockData? {
        id?.let(blockById)?.let { return it }
        return theme.block(background) ?: rawBlock(background)
            ?: error("未知の容器背景指定です: $background (id=$id)")
    }

    override fun outlineBlock(classes: Set<String>, id: String?): BlockData? =
        id?.let(outlineById) ?: theme.outline(classes)

    override fun blockData(material: String, classes: Set<String>, id: String?): BlockData =
        id?.let(blockById)
            ?: theme.block(material)
            ?: rawBlock(material)
            ?: error("未知のブロック指定です: $material (id=$id)")

    override fun itemStack(reference: String, classes: Set<String>, id: String?): ItemStack =
        id?.let(itemById)
            ?: theme.item(reference)
            ?: rawItem(reference)
            ?: error("未知のアイテム指定です: $reference (id=$id)")

    override fun textSize(tag: String, classes: Set<String>, id: String?): Double =
        id?.let(textSizeById) ?: theme.textSize(classes) ?: theme.textBody

    /** 語彙外の名前を生素材名として解決します。未対応名は null です。 */
    protected open fun rawBlock(name: String): BlockData? =
        Material.matchMaterial(name.uppercase())?.createBlockData()

    /** 語彙外の名前をアイテム素材として解決します。未対応名は null です。 */
    protected open fun rawItem(reference: String): ItemStack? =
        Material.matchMaterial(reference.uppercase())?.let(::ItemStack)
}
