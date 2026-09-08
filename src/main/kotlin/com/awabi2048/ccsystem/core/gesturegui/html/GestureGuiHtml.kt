package com.awabi2048.ccsystem.core.gesturegui.html

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiOutline
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiPanel
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiTextAlignment
import com.awabi2048.ccsystem.api.gesturegui.html.GestureGuiHtmlEnvironment
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiAbsoluteOffsets
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiBlock
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiBox
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiColumn
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiComponents
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCrossAlignment
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiCustom
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiDocument
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiEdgeInsets
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiGrid
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiItem
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutDiagnostic
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiLayoutErrorCode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiMainArrangement
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiNode
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiOverflow
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiOverlay
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiRow
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiSizeSpec
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiText
import com.awabi2048.ccsystem.api.gesturegui.layout.GestureGuiViewport
import net.kyori.adventure.text.Component

/**
 * 制限付き HTML/CSS frontend です（自前実装・Profile 1）。
 *
 * HTML/CSS 自体は内部モデルにせず、この frontend が [GestureGuiDocument] へ変換します。
 * 対応範囲は issue #25 の Profile 1 に限定し、範囲外は診断します。
 * 対応タグ: div / section / header / footer / nav / span / p / button /
 * mc-item / mc-block / gesture-viewport / custom / style。
 */
object GestureGuiHtml {
    /** frontend の出力である文書と診断の一覧です。 */
    data class HtmlResult(
        val document: GestureGuiDocument,
        val diagnostics: List<GestureGuiLayoutDiagnostic>,
    )

    /**
     * HTML 文字列を文書へ変換します。
     *
     * @param documentName 診断用の発生源名です（例 "editor.html"）。
     */
    fun parse(
        html: String,
        environment: GestureGuiHtmlEnvironment,
        documentName: String? = null,
        panel: GestureGuiPanel = GestureGuiPanel(),
    ): HtmlResult {
        val ctx = Context(environment, documentName)
        val roots = DomParser(html, ctx).parse()
        val styleSheet = StyleSheet(ctx)
        roots.filterIsInstance<DomElement>().filter { it.tag == "style" }.forEach { styleSheet.addRules(it.textContent(), it.line) }
        val bodies = roots.filter { !(it is DomElement && it.tag == "style") }
        val nodes = bodies.flatMap { buildTopLevel(it, styleSheet, ctx) }
        val root = when {
            nodes.isEmpty() -> {
                ctx.add(GestureGuiLayoutErrorCode.HTML_PARSE_ERROR, null, null, "empty document")
                GestureGuiColumn(children = emptyList(), id = "empty")
            }
            nodes.size == 1 -> nodes.single()
            else -> GestureGuiColumn(children = nodes, id = "root", width = GestureGuiSizeSpec.Percent(1.0))
        }
        return HtmlResult(GestureGuiDocument(root, panel), ctx.diagnostics.toList())
    }

    // ─── 診断収集 ────────────────────────────────────────────

    private class Context(
        val environment: GestureGuiHtmlEnvironment,
        val documentName: String?,
    ) {
        val diagnostics = mutableListOf<GestureGuiLayoutDiagnostic>()

        fun add(code: GestureGuiLayoutErrorCode, nodeId: String?, parentId: String?, message: String) {
            diagnostics += GestureGuiLayoutDiagnostic(code, nodeId, parentId, message, documentName)
        }
    }

    // ─── DOM ─────────────────────────────────────────────────

    private sealed interface DomNode {
        val line: Int
    }

    private data class DomText(val text: String, override val line: Int) : DomNode

    private data class DomElement(
        val tag: String,
        val attributes: Map<String, String>,
        val children: MutableList<DomNode> = mutableListOf(),
        override val line: Int,
    ) : DomNode {
        fun textContent(): String = children.joinToString("") {
            when (it) {
                is DomText -> it.text
                is DomElement -> it.textContent()
            }
        }

        val id: String? get() = attributes["id"]?.takeIf(String::isNotBlank)
        val classes: Set<String> get() = attributes["class"]?.split(Regex("\\s+"))?.filter(String::isNotBlank)?.toSet().orEmpty()
    }

    private class DomParser(private val input: String, private val ctx: Context) {
        private var position = 0
        private var line = 1

        fun parse(): List<DomNode> {
            val roots = mutableListOf<DomNode>()
            val stack = mutableListOf<DomElement>()
            fun currentChildren(): MutableList<DomNode> = stack.lastOrNull()?.children ?: roots
            while (position < input.length) {
                if (input[position] == '<') {
                    when {
                        input.startsWith("<!--", position) -> skipTo("-->")
                        input.startsWith("<!", position) -> skipTo(">")
                        input.startsWith("</", position) -> {
                            val tagLine = line
                            position += 2
                            val name = readName()
                            skipTo(">")
                            val index = stack.indexOfLast { it.tag == name }
                            if (index < 0) {
                                ctx.add(
                                    GestureGuiLayoutErrorCode.HTML_PARSE_ERROR,
                                    null,
                                    null,
                                    "unmatched closing tag at line $tagLine: $name",
                                )
                            } else {
                                repeat(stack.size - index - 1) { unclosed ->
                                    val dropped = stack.removeLast()
                                    ctx.add(
                                        GestureGuiLayoutErrorCode.HTML_PARSE_ERROR,
                                        dropped.id,
                                        null,
                                        "unclosed tag auto-closed at line $tagLine: ${dropped.tag}",
                                    )
                                }
                                stack.removeLast()
                            }
                        }
                        else -> {
                            val tagLine = line
                            position += 1
                            val name = readName().lowercase()
                            val attributes = readAttributes()
                            var selfClosing = false
                            if (position < input.length && input[position] == '/') {
                                selfClosing = true
                                position += 1
                            }
                            if (position < input.length && input[position] == '>') {
                                position += 1
                            } else {
                                skipTo(">")
                            }
                            if (name.isBlank()) {
                                ctx.add(
                                    GestureGuiLayoutErrorCode.HTML_PARSE_ERROR,
                                    null,
                                    null,
                                    "empty tag name at line $tagLine",
                                )
                            } else {
                                val element = DomElement(name, attributes, line = tagLine)
                                currentChildren() += element
                                if (!selfClosing && name !in VOID_TAGS) stack += element
                            }
                        }
                    }
                } else {
                    val start = position
                    val textLine = line
                    while (position < input.length && input[position] != '<') {
                        if (input[position] == '\n') line += 1
                        position += 1
                    }
                    val text = input.substring(start, position)
                    if (text.isNotBlank()) currentChildren() += DomText(text, textLine)
                }
            }
            while (stack.isNotEmpty()) {
                val dropped = stack.removeLast()
                ctx.add(
                    GestureGuiLayoutErrorCode.HTML_PARSE_ERROR,
                    dropped.id,
                    null,
                    "unclosed tag auto-closed at end of document: ${dropped.tag}",
                )
            }
            // 閉じ忘れは roots へ既に追加済みのため、そのまま返します。
            return roots
        }

        private fun skipTo(marker: String) {
            val index = input.indexOf(marker, position)
            if (index < 0) {
                line += input.substring(position).count { it == '\n' }
                position = input.length
            } else {
                line += input.substring(position, index).count { it == '\n' }
                position = index + marker.length
            }
        }

        private fun readName(): String {
            skipBlanks()
            val start = position
            while (position < input.length && (input[position].isLetterOrDigit() || input[position] == '-' || input[position] == '_')) {
                position += 1
            }
            return input.substring(start, position)
        }

        private fun readAttributes(): Map<String, String> {
            val attributes = mutableMapOf<String, String>()
            while (true) {
                skipBlanks()
                if (position >= input.length || input[position] == '>' || input[position] == '/') break
                val name = readName().lowercase()
                if (name.isBlank()) {
                    position += 1
                    continue
                }
                skipBlanks()
                var value = ""
                if (position < input.length && input[position] == '=') {
                    position += 1
                    skipBlanks()
                    if (position < input.length && (input[position] == '"' || input[position] == '\'')) {
                        val quote = input[position]
                        position += 1
                        val start = position
                        while (position < input.length && input[position] != quote) {
                            if (input[position] == '\n') line += 1
                            position += 1
                        }
                        value = input.substring(start, position)
                        if (position < input.length) position += 1
                    } else {
                        val start = position
                        while (position < input.length && !input[position].isWhitespace() && input[position] != '>') {
                            position += 1
                        }
                        value = input.substring(start, position)
                    }
                }
                attributes[name] = value
            }
            return attributes
        }

        private fun skipBlanks() {
            while (position < input.length && input[position].isWhitespace() && input[position] != '\n') {
                position += 1
            }
            while (position < input.length && input[position].isWhitespace()) {
                if (input[position] == '\n') line += 1
                position += 1
            }
        }
    }

    private val VOID_TAGS: Set<String> = setOf("mc-item", "mc-block")

    // ─── CSS ─────────────────────────────────────────────────

    private data class CssRule(
        val tag: String?,
        val clazz: String?,
        val id: String?,
        val declarations: Map<String, String>,
        val order: Int,
    ) {
        val specificity: Int get() = (if (id != null) 100 else 0) + (if (clazz != null) 10 else 0) + (if (tag != null) 1 else 0)

        fun matches(element: DomElement): Boolean {
            if (tag != null && element.tag != tag) return false
            if (clazz != null && clazz !in element.classes) return false
            if (id != null && element.id != id) return false
            return true
        }
    }

    private class StyleSheet(private val ctx: Context) {
        private val rules = mutableListOf<CssRule>()

        fun addRules(css: String, line: Int) {
            val cleaned = css.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            cleaned.split("}").forEach { block ->
                val parts = block.split("{")
                if (parts.size != 2) return@forEach
                val selectors = parts[0].split(",").map(String::trim).filter(String::isNotBlank)
                val declarations = parts[1].split(";").mapNotNull { declaration ->
                    val pair = declaration.split(":").map(String::trim)
                    if (pair.size != 2 || pair[0].isEmpty()) null else pair[0].lowercase() to pair[1]
                }.toMap()
                if (declarations.isEmpty()) return@forEach
                selectors.forEach { selector ->
                    parseSelector(selector)?.let { (tag, clazz, id) ->
                        rules += CssRule(tag, clazz, id, declarations, rules.size)
                    } ?: ctx.add(
                        GestureGuiLayoutErrorCode.UNSUPPORTED_PROPERTY,
                        null,
                        null,
                        "unsupported selector at line $line: $selector",
                    )
                }
            }
        }

        private fun parseSelector(selector: String): Triple<String?, String?, String?>? {
            val trimmed = selector.trim()
            if (trimmed.isEmpty() || trimmed.contains(Regex("\\s")) || trimmed.contains(":")) return null
            if (trimmed.startsWith("#")) {
                val id = trimmed.drop(1)
                if (id.isEmpty() || !isName(id)) return null
                return Triple(null, null, id)
            }
            if (trimmed.startsWith(".")) {
                val clazz = trimmed.drop(1)
                if (clazz.isEmpty() || !isName(clazz)) return null
                return Triple(null, clazz, null)
            }
            val dot = trimmed.indexOf('.')
            val hash = trimmed.indexOf('#')
            if (hash >= 0) return null
            if (dot < 0) {
                if (!isName(trimmed)) return null
                return Triple(trimmed.lowercase(), null, null)
            }
            val tag = trimmed.substring(0, dot)
            val clazz = trimmed.substring(dot + 1)
            if (!isName(tag) || !isName(clazz)) return null
            return Triple(tag.lowercase(), clazz, null)
        }

        private fun isName(value: String): Boolean =
            value.isNotEmpty() && value.all { it.isLetterOrDigit() || it == '-' || it == '_' }

        fun styleOf(element: DomElement): Map<String, String> {
            val applied = mutableMapOf<String, String>()
            rules.filter { it.matches(element) }
                .sortedWith(compareBy({ it.specificity }, { it.order }))
                .forEach { applied.putAll(it.declarations) }
            parseInline(element.attributes["style"].orEmpty(), element.line).forEach { (key, value) ->
                applied[key] = value
            }
            return applied
        }

        private fun parseInline(style: String, line: Int): Map<String, String> {
            if (style.isBlank()) return emptyMap()
            return style.split(";").mapNotNull { declaration ->
                val pair = declaration.split(":").map(String::trim)
                if (pair.size != 2 || pair[0].isEmpty() || pair[1].isEmpty()) {
                    if (declaration.isNotBlank()) {
                        ctx.add(
                            GestureGuiLayoutErrorCode.UNSUPPORTED_PROPERTY,
                            null,
                            null,
                            "unsupported inline declaration at line $line: $declaration",
                        )
                    }
                    null
                } else {
                    pair[0].lowercase() to pair[1]
                }
            }.toMap()
        }
    }

    // ─── 構築 ────────────────────────────────────────────────

    private fun buildTopLevel(node: DomNode, styleSheet: StyleSheet, ctx: Context): List<GestureGuiNode> =
        when (node) {
            is DomText -> {
                val text = node.text.trim()
                if (text.isEmpty()) emptyList() else listOf(
                    GestureGuiText(text = Component.text(text), id = null),
                )
            }
            is DomElement -> listOf(buildElement(node, styleSheet, null, ctx))
        }

    private fun buildElement(
        element: DomElement,
        styleSheet: StyleSheet,
        parentId: String?,
        ctx: Context,
    ): GestureGuiNode {
        val style = validatedStyle(element, styleSheet.styleOf(element), ctx)
        val id = element.id
        return when (element.tag) {
            "span", "p" -> buildText(element, style, parentId, ctx)
            "button" -> buildButton(element, style, parentId, ctx)
            "mc-item" -> buildItem(element, style, parentId, ctx)
            "mc-block" -> buildBlock(element, style, parentId, ctx)
            "gesture-viewport" -> buildViewport(element, style, styleSheet, parentId, ctx)
            "custom" -> buildCustom(element, style, parentId, ctx)
            "div", "section", "header", "footer", "nav" -> buildContainer(element, style, styleSheet, parentId, ctx)
            "style" -> GestureGuiBox(children = emptyList(), id = id)
            else -> {
                ctx.add(
                    GestureGuiLayoutErrorCode.UNSUPPORTED_TAG,
                    id,
                    parentId,
                    "unsupported tag at line ${element.line}: ${element.tag}",
                )
                // 未知要素は透過させ、子を親へ巻き上げます。
                val children = buildChildren(element, styleSheet, parentId, ctx)
                when {
                    children.isEmpty() -> GestureGuiBox(children = emptyList(), id = id)
                    children.size == 1 -> children.single()
                    else -> GestureGuiColumn(children = children, id = id)
                }
            }
        }
    }

    private fun buildChildren(
        element: DomElement,
        styleSheet: StyleSheet,
        parentId: String?,
        ctx: Context,
    ): List<GestureGuiNode> = element.children.flatMap { child ->
        when (child) {
            is DomText -> {
                val text = child.text.trim().replace(Regex("\\s+"), " ")
                if (text.isEmpty()) emptyList() else listOf(
                    GestureGuiText(
                        text = Component.text(text),
                        size = ctx.environment.textSize(element.tag, element.classes, element.id),
                        id = null,
                    ),
                )
            }
            is DomElement -> if (child.tag == "style") {
                emptyList()
            } else {
                listOf(buildElement(child, styleSheet, parentId, ctx))
            }
        }
    }

    private fun directText(element: DomElement): String =
        element.textContent().trim().replace(Regex("\\s+"), " ")

    private fun buildText(
        element: DomElement,
        style: Map<String, String>,
        parentId: String?,
        ctx: Context,
    ): GestureGuiNode {
        val absolute = absoluteOf(style, element, ctx)
        return GestureGuiText(
            text = Component.text(directText(element)),
            size = positiveNumberOr(style["font-size"], ctx.environment.textSize(element.tag, element.classes, element.id), element, ctx),
            alignment = when (style["text-align"]?.lowercase()) {
                "left" -> GestureGuiTextAlignment.LEFT
                "right" -> GestureGuiTextAlignment.RIGHT
                "center", null -> GestureGuiTextAlignment.CENTER
                else -> {
                    unsupported(element, "text-align", style["text-align"].orEmpty(), ctx)
                    GestureGuiTextAlignment.CENTER
                }
            },
            id = element.id,
            actionId = element.attributes["action"]?.takeIf(String::isNotBlank),
            width = sizeOr(style["width"], fill = true, element, ctx),
            height = sizeOr(style["height"], fill = false, element, ctx),
            margin = insetsOr(style["margin"], element, ctx),
            absolute = absolute,
        )
    }

    private fun buildButton(
        element: DomElement,
        style: Map<String, String>,
        parentId: String?,
        ctx: Context,
    ): GestureGuiNode {
        val action = element.attributes["action"]?.takeIf(String::isNotBlank)
        if (action == null) {
            ctx.add(
                GestureGuiLayoutErrorCode.UNKNOWN_ACTION,
                element.id,
                parentId,
                "button without action at line ${element.line}",
            )
        }
        val background = ctx.environment.buttonBackground(
            style["background"]?.let(::normalizeMaterial),
            element.classes,
            element.id,
        )
        val outline = style["border"]?.let { raw ->
            val ratio = raw.trim().toDoubleOrNull()
            if (ratio == null || !ratio.isFinite() || ratio <= 0.0 || ratio >= 0.5) {
                unsupported(element, "border", raw, ctx)
                null
            } else {
                ctx.environment.outlineBlock(element.classes, element.id)?.let { GestureGuiOutline(it, ratio) }
            }
        }
        return GestureGuiComponents.button(
            id = element.id ?: "button",
            label = Component.text(directText(element)),
            actionId = action ?: "missing-action",
            background = background,
            width = sizeOr(style["width"], fill = true, element, ctx),
            height = sizeOr(style["height"], fill = false, element, ctx)
                .takeUnless { it is GestureGuiSizeSpec.Auto } ?: GestureGuiSizeSpec.Fixed(0.1),
            textSize = positiveNumberOr(style["font-size"], 0.0055, element, ctx),
            outline = outline,
            margin = insetsOr(style["margin"], element, ctx),
        ).let { node ->
            val absolute = absoluteOf(style, element, ctx)
            if (absolute == null) node else (node as GestureGuiOverlay).copy(absolute = absolute)
        }
    }

    private fun buildItem(
        element: DomElement,
        style: Map<String, String>,
        parentId: String?,
        ctx: Context,
    ): GestureGuiNode {
        val reference = element.attributes["src"]?.takeIf(String::isNotBlank)
        if (reference == null) {
            ctx.add(
                GestureGuiLayoutErrorCode.HTML_PARSE_ERROR,
                element.id,
                parentId,
                "mc-item without src at line ${element.line}",
            )
            return GestureGuiBox(children = emptyList(), id = element.id)
        }
        return GestureGuiItem(
            item = ctx.environment.itemStack(reference, element.classes, element.id),
            width = sizeOr(style["width"], fill = true, element, ctx)
                .takeUnless { it is GestureGuiSizeSpec.Auto } ?: GestureGuiSizeSpec.Fixed(0.16),
            height = sizeOr(style["height"], fill = false, element, ctx)
                .takeUnless { it is GestureGuiSizeSpec.Auto } ?: GestureGuiSizeSpec.Fixed(0.16),
            id = element.id,
            actionId = element.attributes["action"]?.takeIf(String::isNotBlank),
            margin = insetsOr(style["margin"], element, ctx),
            absolute = absoluteOf(style, element, ctx),
        )
    }

    private fun buildBlock(
        element: DomElement,
        style: Map<String, String>,
        parentId: String?,
        ctx: Context,
    ): GestureGuiNode {
        val material = element.attributes["material"]?.takeIf(String::isNotBlank)?.let(::normalizeMaterial)
        if (material == null) {
            ctx.add(
                GestureGuiLayoutErrorCode.HTML_PARSE_ERROR,
                element.id,
                parentId,
                "mc-block without material at line ${element.line}",
            )
            return GestureGuiBox(children = emptyList(), id = element.id)
        }
        return GestureGuiBlock(
            blockData = ctx.environment.blockData(material, element.classes, element.id),
            width = sizeOr(style["width"], fill = true, element, ctx)
                .takeUnless { it is GestureGuiSizeSpec.Auto } ?: GestureGuiSizeSpec.Fixed(0.2),
            height = sizeOr(style["height"], fill = false, element, ctx)
                .takeUnless { it is GestureGuiSizeSpec.Auto } ?: GestureGuiSizeSpec.Fixed(0.2),
            id = element.id,
            actionId = element.attributes["action"]?.takeIf(String::isNotBlank),
            margin = insetsOr(style["margin"], element, ctx),
            absolute = absoluteOf(style, element, ctx),
        )
    }

    private fun buildViewport(
        element: DomElement,
        style: Map<String, String>,
        styleSheet: StyleSheet,
        parentId: String?,
        ctx: Context,
    ): GestureGuiNode {
        val children = buildChildren(element, styleSheet, element.id ?: parentId, ctx)
        val content = when {
            children.isEmpty() -> GestureGuiBox(children = emptyList(), id = "${element.id ?: "viewport"}-empty")
            children.size == 1 -> children.single()
            else -> GestureGuiColumn(children = children, id = "${element.id ?: "viewport"}-body")
        }
        return GestureGuiViewport(
            content = content,
            id = element.id,
            actionId = element.attributes["action"]?.takeIf(String::isNotBlank),
            width = sizeOr(style["width"], fill = true, element, ctx),
            height = sizeOr(style["height"], fill = true, element, ctx),
            margin = insetsOr(style["margin"], element, ctx),
            absolute = absoluteOf(style, element, ctx),
            padding = insetsOr(style["padding"], element, ctx),
            overflow = overflowOr(style["overflow"], element.attributes["data-overflow"], element, ctx, GestureGuiOverflow.CLIP),
        )
    }

    private fun buildCustom(
        element: DomElement,
        style: Map<String, String>,
        parentId: String?,
        ctx: Context,
    ): GestureGuiNode {
        val renderer = element.attributes["renderer"]?.takeIf(String::isNotBlank)
        if (renderer == null) {
            ctx.add(
                GestureGuiLayoutErrorCode.HTML_PARSE_ERROR,
                element.id,
                parentId,
                "custom without renderer at line ${element.line}",
            )
            return GestureGuiBox(children = emptyList(), id = element.id)
        }
        return GestureGuiCustom(
            rendererId = renderer,
            width = sizeOr(style["width"], fill = true, element, ctx),
            height = sizeOr(style["height"], fill = true, element, ctx),
            id = element.id,
            actionId = element.attributes["action"]?.takeIf(String::isNotBlank),
            margin = insetsOr(style["margin"], element, ctx),
            absolute = absoluteOf(style, element, ctx),
        )
    }

    private fun buildContainer(
        element: DomElement,
        style: Map<String, String>,
        styleSheet: StyleSheet,
        parentId: String?,
        ctx: Context,
    ): GestureGuiNode {
        val display = style["display"]?.lowercase() ?: "block"
        val direction = style["flex-direction"]?.lowercase() ?: "row"
        val gap = numberOr(style["gap"], 0.0, element, ctx) ?: 0.0
        val padding = insetsOr(style["padding"], element, ctx)
        val margin = insetsOr(style["margin"], element, ctx)
        val absolute = absoluteOf(style, element, ctx)
        val overflow = overflowOr(style["overflow"], element.attributes["data-overflow"], element, ctx)
        val main = arrangementOr(style["justify-content"], element, ctx)
        val cross = alignmentOr(style["align-items"], element, ctx)
        val width = sizeOr(style["width"], fill = true, element, ctx)
        val height = sizeOr(style["height"], fill = false, element, ctx)
        val minMaxed = applyMinMax(width, height, style, element, ctx)
        val childParentId = element.id ?: parentId
        val background = style["background"]?.takeIf(String::isNotBlank)?.let(::normalizeMaterial)
        val blockData = background?.let { ctx.environment.containerBackground(it, element.classes, element.id) }
        val decorated = blockData != null
        // ID・margin・absolute・割合寸法は外箱だけに属します。内箱へ再適用すると
        // ID重複、50%の二乗、絶対配置オフセットの二重適用が発生します。
        val innerId = if (decorated) null else element.id
        val innerAction = if (decorated) null else element.attributes["action"]?.takeIf(String::isNotBlank)
        val innerWidth = if (decorated) GestureGuiSizeSpec.Auto else minMaxed.first
        val innerHeight = if (decorated) GestureGuiSizeSpec.Auto else minMaxed.second
        val innerMargin = if (decorated) GestureGuiEdgeInsets() else margin
        val innerAbsolute = if (decorated) null else absolute
        val inner: GestureGuiNode = when {
            display == "grid" -> {
                val columns = gridColumns(style["grid-template-columns"], element.attributes["data-columns"], element, ctx)
                GestureGuiGrid(
                    children = buildChildren(element, styleSheet, childParentId, ctx),
                    columns = columns,
                    id = innerId,
                    actionId = innerAction,
                    width = innerWidth,
                    height = innerHeight,
                    margin = innerMargin,
                    absolute = innerAbsolute,
                    padding = padding,
                    gap = gap,
                    overflow = overflow,
                )
            }
            display == "flex" && direction == "column" -> GestureGuiColumn(
                children = buildChildren(element, styleSheet, childParentId, ctx),
                id = innerId,
                actionId = innerAction,
                width = innerWidth,
                height = innerHeight,
                margin = innerMargin,
                absolute = innerAbsolute,
                padding = padding,
                gap = gap,
                overflow = overflow,
                mainArrangement = main,
                crossAlignment = cross,
            )
            display == "flex" -> GestureGuiRow(
                children = buildChildren(element, styleSheet, childParentId, ctx),
                id = innerId,
                actionId = innerAction,
                width = innerWidth,
                height = innerHeight,
                margin = innerMargin,
                absolute = innerAbsolute,
                padding = padding,
                gap = gap,
                overflow = overflow,
                mainArrangement = main,
                crossAlignment = cross,
            )
            display == "block" -> GestureGuiBox(
                children = buildChildren(element, styleSheet, childParentId, ctx),
                id = innerId,
                actionId = innerAction,
                width = innerWidth,
                height = innerHeight,
                margin = innerMargin,
                absolute = innerAbsolute,
                padding = padding,
                gap = gap,
                overflow = overflow,
                crossAlignment = cross,
            )
            else -> {
                unsupported(element, "display", display, ctx)
                GestureGuiBox(
                    children = buildChildren(element, styleSheet, childParentId, ctx),
                    id = innerId,
                    width = innerWidth,
                    height = innerHeight,
                    margin = innerMargin,
                    absolute = innerAbsolute,
                    padding = padding,
                    gap = gap,
                    overflow = overflow,
                )
            }
        }
        // CSS background がある容器は背景 Block と内容の重ねに展開します。
        if (blockData == null) {
            if (style["border"] != null) unsupported(element, "border", style["border"].orEmpty(), ctx)
            return inner
        }
        val borderRatio = style["border"]?.trim()?.toDoubleOrNull()
        val outline = if (borderRatio != null) {
            if (!borderRatio.isFinite() || borderRatio <= 0.0 || borderRatio >= 0.5) {
                unsupported(element, "border", style["border"].orEmpty(), ctx)
                null
            } else {
                ctx.environment.outlineBlock(element.classes, element.id)?.let { GestureGuiOutline(it, borderRatio) }
            }
        } else {
            if (style["border"] != null) unsupported(element, "border", style["border"].orEmpty(), ctx)
            null
        }
        return GestureGuiOverlay(
            children = listOf(
                GestureGuiBlock(
                    blockData = blockData,
                    width = GestureGuiSizeSpec.Auto,
                    height = GestureGuiSizeSpec.Auto,
                    outline = outline,
                    id = null,
                ),
                inner,
            ),
            id = element.id,
            actionId = element.attributes["action"]?.takeIf(String::isNotBlank),
            width = minMaxed.first,
            height = minMaxed.second,
            margin = margin,
            absolute = absolute,
            overflow = overflow,
        )
    }

    // ─── 値の解釈 ────────────────────────────────────────────

    /**
     * 未知の宣言と、既知でもこのタグでは効かない宣言をまとめて検証します。
     * Profileの対応範囲を限定しつつ、意図しない見た目を診断なしで採用しません。
     */
    private fun validatedStyle(element: DomElement, style: Map<String, String>, ctx: Context): Map<String, String> {
        val common = setOf("width", "height", "margin", "position", "top", "right", "bottom", "left")
        val containers = setOf("div", "section", "header", "footer", "nav")
        val supported = common + when (element.tag) {
            "p", "span" -> setOf("font-size", "text-align")
            "button" -> setOf("font-size", "background", "border")
            "gesture-viewport" -> setOf("overflow", "padding")
            in containers -> setOf(
                "display", "flex-direction", "justify-content", "align-items", "gap",
                "padding", "overflow", "background", "border", "grid-template-columns",
                "min-width", "max-width", "min-height", "max-height",
            )
            else -> emptySet()
        }
        return style.filter { (property, value) ->
            val applicable = property in supported && when (property) {
                "flex-direction", "justify-content" -> style["display"]?.lowercase() == "flex"
                "align-items" -> style["display"]?.lowercase() != "grid"
                "grid-template-columns" -> style["display"]?.lowercase() == "grid"
                else -> true
            }
            if (!applicable) unsupported(element, property, value, ctx)
            applicable
        }.toMutableMap().also { valid ->
            valid["flex-direction"]?.let { direction ->
                if (direction.lowercase() !in setOf("row", "column")) {
                    unsupported(element, "flex-direction", direction, ctx)
                    valid.remove("flex-direction")
                }
            }
        }
    }

    /** font-sizeはgapと異なり0を許可しません。ノード生成前に構造化診断へ変換します。 */
    private fun positiveNumberOr(raw: String?, fallback: Double, element: DomElement, ctx: Context): Double {
        if (raw == null) return fallback
        val value = raw.trim().toDoubleOrNull()
        if (value == null || !value.isFinite() || value <= 0.0) {
            unsupported(element, "font-size", raw, ctx)
            return fallback
        }
        return value
    }

    private fun normalizeMaterial(raw: String): String =
        raw.trim().lowercase().replace("-", "_").replace(" ", "_")

    private fun unsupported(element: DomElement, property: String, value: String, ctx: Context) {
        ctx.add(
            GestureGuiLayoutErrorCode.UNSUPPORTED_PROPERTY,
            element.id,
            null,
            "unsupported $property at line ${element.line}: $value",
        )
    }

    private fun numberOr(raw: String?, fallback: Double, element: DomElement, ctx: Context): Double? {
        if (raw == null) return fallback
        val value = raw.trim().toDoubleOrNull()
        if (value == null || !value.isFinite() || value < 0.0) {
            unsupported(element, "number", raw, ctx)
            return fallback
        }
        return value
    }

    private fun sizeOr(raw: String?, fill: Boolean, element: DomElement, ctx: Context): GestureGuiSizeSpec {
        if (raw == null) return if (fill) GestureGuiSizeSpec.Percent(1.0) else GestureGuiSizeSpec.Auto
        val value = raw.trim().lowercase()
        if (value == "auto") return GestureGuiSizeSpec.Auto
        if (value.endsWith("%")) {
            val ratio = value.dropLast(1).toDoubleOrNull()
            if (ratio != null && ratio.isFinite() && ratio >= 0.0 && ratio <= 100.0) {
                return GestureGuiSizeSpec.Percent(ratio / 100.0)
            }
        } else if (value.endsWith("fr")) {
            val weight = value.dropLast(2).toDoubleOrNull()
            if (weight != null && weight.isFinite() && weight > 0.0) {
                return GestureGuiSizeSpec.Fraction(weight)
            }
        } else {
            val fixed = value.toDoubleOrNull()
            if (fixed != null && fixed.isFinite() && fixed > 0.0) {
                return GestureGuiSizeSpec.Fixed(fixed)
            }
        }
        unsupported(element, "size", raw, ctx)
        return if (fill) GestureGuiSizeSpec.Percent(1.0) else GestureGuiSizeSpec.Auto
    }

    private fun applyMinMax(
        width: GestureGuiSizeSpec,
        height: GestureGuiSizeSpec,
        style: Map<String, String>,
        element: DomElement,
        ctx: Context,
    ): Pair<GestureGuiSizeSpec, GestureGuiSizeSpec> {
        // Profile 1 では Fixed に対する min/max の clamp のみ対応します。
        fun clamp(spec: GestureGuiSizeSpec, minRaw: String?, maxRaw: String?): GestureGuiSizeSpec {
            if (spec !is GestureGuiSizeSpec.Fixed) {
                if (minRaw != null || maxRaw != null) {
                    unsupported(element, "min/max size", "${minRaw ?: ""}/${maxRaw ?: ""}", ctx)
                }
                return spec
            }
            var value = spec.value
            fun limit(raw: String?): Double? {
                if (raw == null) return null
                val number = raw.trim().toDoubleOrNull()
                if (number == null || !number.isFinite() || number <= 0.0) {
                    unsupported(element, "min/max size", raw, ctx)
                    return null
                }
                return number
            }
            limit(minRaw)?.let { value = maxOf(value, it) }
            limit(maxRaw)?.let { value = minOf(value, it) }
            return GestureGuiSizeSpec.Fixed(value)
        }
        return clamp(width, style["min-width"], style["max-width"]) to
            clamp(height, style["min-height"], style["max-height"])
    }

    private fun insetsOr(raw: String?, element: DomElement, ctx: Context): GestureGuiEdgeInsets {
        if (raw == null) return GestureGuiEdgeInsets()
        val tokens = raw.trim().split(Regex("\\s+"))
        val parts = tokens.mapNotNull { it.toDoubleOrNull() }
        if (parts.size != tokens.size || parts.size !in 1..4 || parts.any { !it.isFinite() || it < 0.0 }) {
            unsupported(element, "insets", raw, ctx)
            return GestureGuiEdgeInsets()
        }
        val (top, right, bottom, left) = when (parts.size) {
            1 -> listOf(parts[0], parts[0], parts[0], parts[0])
            2 -> listOf(parts[0], parts[1], parts[0], parts[1])
            3 -> listOf(parts[0], parts[1], parts[2], parts[1])
            else -> listOf(parts[0], parts[1], parts[2], parts[3])
        }
        // CSS 順序（時計回り: 上右下左）を内部表現へ変換します。
        return GestureGuiEdgeInsets(left = left, top = top, right = right, bottom = bottom)
    }

    private fun absoluteOf(style: Map<String, String>, element: DomElement, ctx: Context): GestureGuiAbsoluteOffsets? {
        val position = style["position"]?.lowercase()
        if (position == null || position == "relative" || position == "static") {
            if (position == "static") unsupported(element, "position", "static", ctx)
            listOf("top", "right", "bottom", "left").forEach { key ->
                if (style[key] != null) unsupported(element, key, style[key].orEmpty(), ctx)
            }
            return null
        }
        if (position != "absolute") {
            unsupported(element, "position", position, ctx)
            return null
        }
        fun offset(key: String): Double? {
            val raw = style[key] ?: return null
            val value = raw.trim().toDoubleOrNull()
            if (value == null || !value.isFinite() || value < 0.0) {
                unsupported(element, key, raw, ctx)
                return null
            }
            return value
        }
        return GestureGuiAbsoluteOffsets(
            left = offset("left"),
            top = offset("top"),
            right = offset("right"),
            bottom = offset("bottom"),
        )
    }

    private fun overflowOr(css: String?, data: String?, element: DomElement, ctx: Context, default: GestureGuiOverflow = GestureGuiOverflow.VISIBLE): GestureGuiOverflow {
        data?.lowercase()?.let {
            return when (it) {
                "reject" -> GestureGuiOverflow.REJECT
                "clip" -> GestureGuiOverflow.CLIP
                "visible" -> GestureGuiOverflow.VISIBLE
                else -> {
                    unsupported(element, "data-overflow", it, ctx)
                    GestureGuiOverflow.VISIBLE
                }
            }
        }
        return when (css?.lowercase()) {
            null -> default
            "visible" -> GestureGuiOverflow.VISIBLE
            "hidden", "clip" -> GestureGuiOverflow.CLIP
            else -> {
                unsupported(element, "overflow", css.orEmpty(), ctx)
                GestureGuiOverflow.VISIBLE
            }
        }
    }

    private fun arrangementOr(raw: String?, element: DomElement, ctx: Context): GestureGuiMainArrangement =
        when (raw?.lowercase()?.trim()) {
            null, "flex-start", "start" -> GestureGuiMainArrangement.START
            "center" -> GestureGuiMainArrangement.CENTER
            "flex-end", "end" -> GestureGuiMainArrangement.END
            "space-between" -> GestureGuiMainArrangement.SPACE_BETWEEN
            else -> {
                unsupported(element, "justify-content", raw.orEmpty(), ctx)
                GestureGuiMainArrangement.START
            }
        }

    private fun alignmentOr(raw: String?, element: DomElement, ctx: Context): GestureGuiCrossAlignment =
        when (raw?.lowercase()?.trim()) {
            null, "stretch" -> GestureGuiCrossAlignment.STRETCH
            "flex-start", "start" -> GestureGuiCrossAlignment.START
            "center" -> GestureGuiCrossAlignment.CENTER
            "flex-end", "end" -> GestureGuiCrossAlignment.END
            else -> {
                unsupported(element, "align-items", raw.orEmpty(), ctx)
                GestureGuiCrossAlignment.STRETCH
            }
        }

    private fun gridColumns(css: String?, attr: String?, element: DomElement, ctx: Context): Int {
        css?.let {
            val match = Regex("repeat\\(\\s*(\\d+)").find(it.lowercase())
            val count = match?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (count != null && count >= 1) return count
            unsupported(element, "grid-template-columns", it, ctx)
        }
        attr?.toIntOrNull()?.takeIf { it >= 1 }?.let { return it }
        if (attr != null) unsupported(element, "data-columns", attr, ctx)
        return 1
    }
}
