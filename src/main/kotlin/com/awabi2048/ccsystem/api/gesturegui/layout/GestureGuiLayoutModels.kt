package com.awabi2048.ccsystem.api.gesturegui.layout

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiOutline
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiTextAlignment
import net.kyori.adventure.text.Component
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack

/**
 * 宣言的レイアウトのサイズ指定です。
 *
 * 呼び出し側が明示的に指定することを前提にし、複雑化を避けるため
 * Fixed / Percent / Fraction / Auto の4種に限定します。
 * box-sizing相当は常に border-box です。
 */
sealed interface GestureGuiSizeSpec {
    /** ブロック単位の固定値です。 */
    data class Fixed(val value: Double) : GestureGuiSizeSpec {
        init {
            require(value.isFinite() && value > 0.0) { "layout fixed size must be positive and finite" }
        }
    }

    /** containing block に対する比率です。0.0〜1.0 の範囲に制限します。 */
    data class Percent(val ratio: Double) : GestureGuiSizeSpec {
        init {
            require(ratio.isFinite() && ratio >= 0.0 && ratio <= 1.0) {
                "layout percent ratio must be finite and within 0.0..1.0"
            }
        }
    }

    /** フロー余白を重み配分します。Row / Column の主軸でのみ有効です。 */
    data class Fraction(val weight: Double) : GestureGuiSizeSpec {
        init {
            require(weight.isFinite() && weight > 0.0) { "layout fraction weight must be positive and finite" }
        }
    }

    /**
     * 内容に応じた自動解決です。
     *
     * Text は行数と言語サイズから概算し、Grid / Overlay セル内では利用可能域へ広がります。
     * Text 以外のリーフをフロー内に Auto で置いた場合は Engine が 0 扱い＋診断とするため、
     * 通常は Fixed / Percent / Fraction を明示してください（呼び出し側指定方針）。
     */
    data object Auto : GestureGuiSizeSpec
}

/** 上下左右の余白です。ブロック単位で、負値は許可しません。 */
data class GestureGuiEdgeInsets(
    val left: Double = 0.0,
    val top: Double = 0.0,
    val right: Double = 0.0,
    val bottom: Double = 0.0,
) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() && it >= 0.0 }) {
            "layout insets must be finite and non-negative"
        }
    }

    val horizontal: Double get() = left + right
    val vertical: Double get() = top + bottom
}

/**
 * containing block 基準の絶対配置オフセットです。
 *
 * null の辺は未指定です。左右両方または上下両方を指定し、かつ対応軸が Auto の場合、
 * その軸は両オフセット間に引き伸ばされます。フロー配置からは除外されます。
 */
data class GestureGuiAbsoluteOffsets(
    val left: Double? = null,
    val top: Double? = null,
    val right: Double? = null,
    val bottom: Double? = null,
) {
    init {
        require(listOfNotNull(left, top, right, bottom).all(Double::isFinite)) {
            "layout absolute offsets must be finite"
        }
    }
}

/** 子のはみ出しに対する振る舞いです。 */
enum class GestureGuiOverflow {
    /** 意図的な逸脱を許可します（tooltip / 装飾）。診断しません。 */
    VISIBLE,

    /** 描画・操作可能範囲を制限し、はみ出しを診断します。 */
    CLIP,

    /** 親領域からの逸脱を検証エラーとします。 */
    REJECT,
}

/** 主軸方向の配置です。 */
enum class GestureGuiMainArrangement {
    START,
    CENTER,
    END,
    SPACE_BETWEEN,
}

/** 交差軸方向の配置です。 */
enum class GestureGuiCrossAlignment {
    START,
    CENTER,
    END,
    STRETCH,
}

/**
 * 宣言的 UI tree のノードです。
 *
 * 通常レイアウトでは絶対座標を持ちません。座標解決は Layout Engine が行い、
 * 結果は [com.awabi2048.ccsystem.api.gesturegui.layout.ResolvedGestureGuiNode] へ出ます。
 * 見た目（素材・文言・寸法）は呼び出し側指定とし、CC-System 側で補完しません。
 */
sealed interface GestureGuiNode {
    /** 診断・lint 用の安定IDです。null 可ですが、指定時は同一文書内で一意にしてください。 */
    val id: String?

    /**
     * Kotlin 側 registry へ接続する action ID です。
     * null の場合は非操作ノードとして interaction bounds を生成しません。
     */
    val actionId: String?
    val acceptedGestures: Set<GestureGuiGesture>
    val width: GestureGuiSizeSpec
    val height: GestureGuiSizeSpec
    val margin: GestureGuiEdgeInsets

    /** null ならフロー配置、非 null なら絶対配置です。 */
    val absolute: GestureGuiAbsoluteOffsets?
}

/** 共通プロパティの検証です。各ノードの init から呼びます。 */
internal fun requireCommonNodeProps(id: String?, actionId: String?) {
    require(id == null || id.isNotBlank()) { "layout node id must not be blank" }
    require(actionId == null || actionId.isNotBlank()) { "layout action id must not be blank" }
}

/** 縦積みの汎用箱です。子は上から下へ積まれます。 */
data class GestureGuiBox(
    val children: List<GestureGuiNode>,
    override val id: String? = null,
    override val actionId: String? = null,
    override val acceptedGestures: Set<GestureGuiGesture> = if (actionId != null) {
        setOf(GestureGuiGesture.PRIMARY)
    } else {
        emptySet()
    },
    override val width: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
    override val height: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
    override val margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    override val absolute: GestureGuiAbsoluteOffsets? = null,
    val padding: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    val gap: Double = 0.0,
    val overflow: GestureGuiOverflow = GestureGuiOverflow.VISIBLE,
    val crossAlignment: GestureGuiCrossAlignment = GestureGuiCrossAlignment.STRETCH,
) : GestureGuiNode {
    init {
        requireCommonNodeProps(id, actionId)
        require(gap.isFinite() && gap >= 0.0) { "layout gap must be finite and non-negative" }
    }
}

/** 水平フローです。 */
data class GestureGuiRow(
    val children: List<GestureGuiNode>,
    override val id: String? = null,
    override val actionId: String? = null,
    override val acceptedGestures: Set<GestureGuiGesture> = if (actionId != null) {
        setOf(GestureGuiGesture.PRIMARY)
    } else {
        emptySet()
    },
    override val width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
    override val height: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
    override val margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    override val absolute: GestureGuiAbsoluteOffsets? = null,
    val padding: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    val gap: Double = 0.0,
    val overflow: GestureGuiOverflow = GestureGuiOverflow.VISIBLE,
    val mainArrangement: GestureGuiMainArrangement = GestureGuiMainArrangement.START,
    val crossAlignment: GestureGuiCrossAlignment = GestureGuiCrossAlignment.STRETCH,
) : GestureGuiNode {
    init {
        requireCommonNodeProps(id, actionId)
        require(gap.isFinite() && gap >= 0.0) { "layout gap must be finite and non-negative" }
    }
}

/** 垂直フローです。 */
data class GestureGuiColumn(
    val children: List<GestureGuiNode>,
    override val id: String? = null,
    override val actionId: String? = null,
    override val acceptedGestures: Set<GestureGuiGesture> = if (actionId != null) {
        setOf(GestureGuiGesture.PRIMARY)
    } else {
        emptySet()
    },
    override val width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
    override val height: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
    override val margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    override val absolute: GestureGuiAbsoluteOffsets? = null,
    val padding: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    val gap: Double = 0.0,
    val overflow: GestureGuiOverflow = GestureGuiOverflow.VISIBLE,
    val mainArrangement: GestureGuiMainArrangement = GestureGuiMainArrangement.START,
    val crossAlignment: GestureGuiCrossAlignment = GestureGuiCrossAlignment.STRETCH,
) : GestureGuiNode {
    init {
        requireCommonNodeProps(id, actionId)
        require(gap.isFinite() && gap >= 0.0) { "layout gap must be finite and non-negative" }
    }
}

/**
 * 格子配置です。子は行優先でセルへ順に配置されます。
 * rows が null の場合は列数から必要行数を自動算出します。
 */
data class GestureGuiGrid(
    val children: List<GestureGuiNode>,
    val columns: Int,
    val rows: Int? = null,
    override val id: String? = null,
    override val actionId: String? = null,
    override val acceptedGestures: Set<GestureGuiGesture> = if (actionId != null) {
        setOf(GestureGuiGesture.PRIMARY)
    } else {
        emptySet()
    },
    override val width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
    override val height: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
    override val margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    override val absolute: GestureGuiAbsoluteOffsets? = null,
    val padding: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    val gap: Double = 0.0,
    val crossGap: Double = gap,
    val overflow: GestureGuiOverflow = GestureGuiOverflow.VISIBLE,
) : GestureGuiNode {
    init {
        requireCommonNodeProps(id, actionId)
        require(columns >= 1) { "layout grid columns must be at least 1" }
        require(rows == null || rows >= 1) { "layout grid rows must be at least 1" }
        require(gap.isFinite() && gap >= 0.0) { "layout gap must be finite and non-negative" }
        require(crossGap.isFinite() && crossGap >= 0.0) { "layout cross gap must be finite and non-negative" }
    }
}

/** 重ね配置です。子は同一内容域へ積まれ、宣言順に前面になります。 */
data class GestureGuiOverlay(
    val children: List<GestureGuiNode>,
    override val id: String? = null,
    override val actionId: String? = null,
    override val acceptedGestures: Set<GestureGuiGesture> = if (actionId != null) {
        setOf(GestureGuiGesture.PRIMARY)
    } else {
        emptySet()
    },
    override val width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
    override val height: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
    override val margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    override val absolute: GestureGuiAbsoluteOffsets? = null,
    val padding: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    val overflow: GestureGuiOverflow = GestureGuiOverflow.VISIBLE,
    val mainArrangement: GestureGuiMainArrangement = GestureGuiMainArrangement.START,
    val crossAlignment: GestureGuiCrossAlignment = GestureGuiCrossAlignment.START,
) : GestureGuiNode {
    init {
        requireCommonNodeProps(id, actionId)
    }
}

/**
 * 切り抜き領域です。内容は単一ノードに限定し、clip の一般化点とします。
 * Graph 等の独自クリッピング置換先として使います。
 */
data class GestureGuiViewport(
    val content: GestureGuiNode,
    override val id: String? = null,
    override val actionId: String? = null,
    override val acceptedGestures: Set<GestureGuiGesture> = emptySet(),
    override val width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
    override val height: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
    override val margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    override val absolute: GestureGuiAbsoluteOffsets? = null,
    val padding: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    val overflow: GestureGuiOverflow = GestureGuiOverflow.CLIP,
) : GestureGuiNode {
    init {
        requireCommonNodeProps(id, actionId)
    }
}

/** 文字表示のリーフです。 */
data class GestureGuiText(
    val text: Component,
    /** 画面上の論理文字サイズです。renderer が TextDisplay 用倍率へ変換します。 */
    val size: Double = 0.0125,
    val lineWidth: Int = 160,
    val alignment: GestureGuiTextAlignment = GestureGuiTextAlignment.CENTER,
    val seeThrough: Boolean = false,
    /**
     * 概算に使う論理行数です。Auto 高さの解決に用います。
     * 完全な intrinsic text sizing は Profile 1 の対象外のため呼び出し側指定とします。
     */
    val lines: Int = 1,
    override val id: String? = null,
    override val actionId: String? = null,
    override val acceptedGestures: Set<GestureGuiGesture> = if (actionId != null) {
        setOf(GestureGuiGesture.PRIMARY)
    } else {
        emptySet()
    },
    override val width: GestureGuiSizeSpec = GestureGuiSizeSpec.Percent(1.0),
    override val height: GestureGuiSizeSpec = GestureGuiSizeSpec.Auto,
    override val margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    override val absolute: GestureGuiAbsoluteOffsets? = null,
) : GestureGuiNode {
    init {
        requireCommonNodeProps(id, actionId)
        require(size > 0.0 && size.isFinite()) { "layout text size must be positive and finite" }
        require(lineWidth > 0) { "layout text lineWidth must be positive" }
        require(lines >= 1) { "layout text lines must be at least 1" }
    }
}

/**
 * 矩形表示のリーフです。
 * フロー内の Auto は 0 扱い＋診断となるため、通常は明示サイズを指定してください。
 */
data class GestureGuiBlock(
    val blockData: BlockData,
    override val width: GestureGuiSizeSpec,
    override val height: GestureGuiSizeSpec,
    val glowColor: Int? = null,
    val outline: GestureGuiOutline? = null,
    override val id: String? = null,
    override val actionId: String? = null,
    override val acceptedGestures: Set<GestureGuiGesture> = if (actionId != null) {
        setOf(GestureGuiGesture.PRIMARY)
    } else {
        emptySet()
    },
    override val margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    override val absolute: GestureGuiAbsoluteOffsets? = null,
) : GestureGuiNode {
    init {
        requireCommonNodeProps(id, actionId)
    }
}

/**
 * アイテム表示のリーフです。
 * フロー内の Auto は 0 扱い＋診断となるため、通常は明示サイズを指定してください。
 */
data class GestureGuiItem(
    val item: ItemStack,
    val scale: Double = 0.22,
    override val width: GestureGuiSizeSpec,
    override val height: GestureGuiSizeSpec,
    val glowColor: Int? = null,
    override val id: String? = null,
    override val actionId: String? = null,
    override val acceptedGestures: Set<GestureGuiGesture> = if (actionId != null) {
        setOf(GestureGuiGesture.PRIMARY)
    } else {
        emptySet()
    },
    override val margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    override val absolute: GestureGuiAbsoluteOffsets? = null,
) : GestureGuiNode {
    init {
        requireCommonNodeProps(id, actionId)
        require(scale.isFinite() && scale > 0.0) { "layout item scale must be positive and finite" }
    }
}

/**
 * 特殊描画向けの escape hatch です。
 *
 * viewport / clip は CC-System が担い、意味的配置と low-level visual 生成は
 * rendererId に対応する呼び出し側 renderer が担います。
 * フロー内の Auto は 0 扱い＋診断となるため、通常は明示サイズを指定してください。
 */
data class GestureGuiCustom(
    val rendererId: String,
    override val width: GestureGuiSizeSpec,
    override val height: GestureGuiSizeSpec,
    override val id: String? = null,
    override val actionId: String? = null,
    override val acceptedGestures: Set<GestureGuiGesture> = emptySet(),
    override val margin: GestureGuiEdgeInsets = GestureGuiEdgeInsets(),
    override val absolute: GestureGuiAbsoluteOffsets? = null,
) : GestureGuiNode {
    init {
        requireCommonNodeProps(id, actionId)
        require(rendererId.isNotBlank()) { "layout custom rendererId must not be blank" }
    }
}

/**
 * レイアウト解決の入力文書です。HTML / Kotlin DSL / 直接構築のいずれも
 * 最終的にこの形へ変換されます。HTML/CSS 自体は内部モデルにしません。
 */
data class GestureGuiDocument(
    val root: GestureGuiNode,
    val panel: GestureGuiPanel = GestureGuiPanel(),
)
