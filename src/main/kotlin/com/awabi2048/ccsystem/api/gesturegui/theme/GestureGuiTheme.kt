package com.awabi2048.ccsystem.api.gesturegui.theme

import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack

/**
 * テーマ語彙です。マークアップ記述で用いる CSS 値・class・属性値の名前を定義します。
 *
 * - ブロック：CSS `background`・`mc-block material` の値（例 `background: surface`）
 * - 縁：`class` 値で枠素材を選択します（例 `class="card selected"` ＋ `border: 0.08`）。
 *   `border` の数値は太さ比率だけを表し、素材はこの語彙が決めます。
 * - 文字：`class` 値で論理文字サイズを選択します（例 `class="caption"`）
 * - アイテム：`mc-item src` の値（例 `src="nav-prev"`）
 *
 * ブロック名は `accent-deep` のようにダッシュ区切りでも `accent_deep` でも解決されます。
 * 語彙外の名前は生素材名（`light_gray_concrete` 等）として解釈されるため、
 * テーマ未導入の記述をそのまま併用できます。
 */
object GestureGuiThemeTokens {
    // ─── ブロック（CSS `background` / `material` の値）──────────
    /** 通常背景です。未設定・中立の項目もこの色を維持します。 */
    const val SURFACE = "surface"
    /** 主操作・強調の背景です。 */
    const val ACCENT = "accent"
    /** 選択中タブ・安全側の確定操作・パネル枠と揃える濃いアクセントです。 */
    const val ACCENT_DEEP = "accent-deep"
    /** 削除等の破壊的操作の背景です。 */
    const val DANGER = "danger"
    /** 戻る操作の背景です。 */
    const val BACK = "back"
    /** Boolean の ON を直接示す背景です。 */
    const val ON = "on"
    /** Boolean の OFF を直接示す背景です。 */
    const val OFF = "off"

    // ─── 縁（class → `border` の枠素材）───────────────────────
    /** 選択中の枠です（白縁）。 */
    const val SELECTED = "selected"
    /** 設定済みの枠です（空色縁）。 */
    const val SET = "set"

    // ─── 文字階調（class → 論理文字サイズ）─────────────────────
    /** 見出しです。 */
    const val TITLE = "title"
    /** ラベル・操作文言です。 */
    const val LABEL = "label"
    /** 本文です。 */
    const val BODY = "body"
    /** 補足説明です。 */
    const val CAPTION = "caption"

    // ─── アイテム（`mc-item src` の値）──────────────────────────
    /** 前ページ移動の図柄です。 */
    const val NAV_PREV = "nav-prev"
    /** 次ページ移動の図柄です。 */
    const val NAV_NEXT = "nav-next"
}

/**
 * Gesture GUI の見た目規則です。
 *
 * `.docs/specs/ui/cross-system-ui-design.md` の Gesture GUI 規則を名前付き
 * トークンとして保持し、素材・文字サイズの直書きを画面側から排除します。
 * 各値は [GestureGuiThemeTokens] の名前から解決します。
 *
 * 語彙外の見た目が必要な場合は `extraBlocks` / `extraTextSizes` / `items` で
 * 呼び出し側の名前を追加します。標準値の変更は `copy` または
 * [GestureGuiThemes.standard] の派生で行い、画面ごとの独自値は作りません。
 */
data class GestureGuiTheme(
    val surface: BlockData,
    val accent: BlockData,
    val accentDeep: BlockData,
    val danger: BlockData,
    val back: BlockData,
    val on: BlockData,
    val off: BlockData,
    val outlineSelected: BlockData,
    val outlineSet: BlockData,
    val textTitle: Double = 0.006,
    val textLabel: Double = 0.005,
    val textBody: Double = 0.0055,
    val textCaption: Double = 0.0045,
    /** 語彙外のブロック名→素材の追加表です。キーは大小・`-`/`_` を区別しません。 */
    val extraBlocks: Map<String, BlockData> = emptyMap(),
    /** 語彙外の役割名→論理文字サイズの追加表です。キーは大小・`-`/`_` を区別しません。 */
    val extraTextSizes: Map<String, Double> = emptyMap(),
    /** アイテム名→表示品の表です。キーは大小・`-`/`_` を区別しません。 */
    val items: Map<String, ItemStack> = emptyMap(),
) {
    private val normalizedExtraBlocks = extraBlocks.mapKeys { (key, _) -> normalize(key) }
    private val normalizedExtraTextSizes = extraTextSizes.mapKeys { (key, _) -> normalize(key) }
    private val normalizedItems = items.mapKeys { (key, _) -> normalize(key) }

    /** CSS 値のブロック名を素材へ解決します。未知名は null です。 */
    fun block(name: String): BlockData? = when (normalize(name)) {
        normalize(GestureGuiThemeTokens.SURFACE) -> surface
        normalize(GestureGuiThemeTokens.ACCENT) -> accent
        normalize(GestureGuiThemeTokens.ACCENT_DEEP) -> accentDeep
        normalize(GestureGuiThemeTokens.DANGER) -> danger
        normalize(GestureGuiThemeTokens.BACK) -> back
        normalize(GestureGuiThemeTokens.ON) -> on
        normalize(GestureGuiThemeTokens.OFF) -> off
        else -> normalizedExtraBlocks[normalize(name)]
    }

    /**
     * 枠の意図クラスから枠素材を選びます。`border` 属性の数値が太さを決め、
     * ここで素材が決まります。複数指定時は `selected` を優先します。
     */
    fun outline(classes: Set<String>): BlockData? = when {
        GestureGuiThemeTokens.SELECTED in classes -> outlineSelected
        GestureGuiThemeTokens.SET in classes -> outlineSet
        else -> null
    }

    /**
     * 文字役割クラスから論理文字サイズを選びます。
     * 1要素に役割クラスは1つを想定し、複数指定時はこの順序の先を採用します。
     */
    fun textSize(classes: Set<String>): Double? = when {
        GestureGuiThemeTokens.TITLE in classes -> textTitle
        GestureGuiThemeTokens.LABEL in classes -> textLabel
        GestureGuiThemeTokens.BODY in classes -> textBody
        GestureGuiThemeTokens.CAPTION in classes -> textCaption
        else -> classes.firstNotNullOfOrNull { normalizedExtraTextSizes[normalize(it)] }
    }

    /** `mc-item src` の名前を表示品へ解決します。未知名は null です。 */
    fun item(reference: String): ItemStack? = normalizedItems[normalize(reference)]

    private fun normalize(name: String): String =
        name.trim().lowercase().replace("-", "_").replace(" ", "_")
}

/**
 * 標準テーマの生成口です。
 *
 * 素材の実体化に Bukkit が必要なため、サーバー稼働中に呼び出します。
 * 値は `.docs/specs/ui/cross-system-ui-design.md` の Gesture GUI 規則に
 * 対応します（通常背景＝薄灰、選択＝白縁、設定済み＝空色縁、ON＝LIME／OFF＝GRAY）。
 */
object GestureGuiThemes {
    fun standard(): GestureGuiTheme = GestureGuiTheme(
        surface = Material.LIGHT_GRAY_CONCRETE.createBlockData(),
        accent = Material.CYAN_CONCRETE.createBlockData(),
        accentDeep = Material.CYAN_TERRACOTTA.createBlockData(),
        danger = Material.RED_CONCRETE.createBlockData(),
        back = Material.BROWN_CONCRETE.createBlockData(),
        on = Material.LIME_CONCRETE.createBlockData(),
        off = Material.GRAY_CONCRETE.createBlockData(),
        outlineSelected = Material.WHITE_CONCRETE.createBlockData(),
        outlineSet = Material.LIGHT_BLUE_CONCRETE.createBlockData(),
        items = mapOf(
            GestureGuiThemeTokens.NAV_PREV to ItemStack(Material.ARROW),
            GestureGuiThemeTokens.NAV_NEXT to ItemStack(Material.ARROW),
        ),
    )
}
