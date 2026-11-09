package com.kylecorry.andromeda.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.style.TextIndent
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

/** How markdown renders into an [AnnotatedString]. */
data class MarkdownTextStyles(
    val strong: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold),
    val emphasis: SpanStyle = SpanStyle(fontStyle = FontStyle.Italic),
    val strikethrough: SpanStyle = SpanStyle(textDecoration = TextDecoration.LineThrough),
    val code: SpanStyle = SpanStyle(fontFamily = FontFamily.Monospace),
    val link: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline),
    /** Relative sizes of headings 1 to 6, which are also bold. */
    val headingScales: List<Float> = listOf(2f, 1.5f, 1.17f, 1f, 0.83f, 0.67f),
    val bullet: String = "•",
    /** How far each list level is indented; wrapped lines of an item line up after its marker. */
    val listIndent: TextUnit = 1.5.em
)

/**
 * A custom inline delimiter pair, such as `%%hidden%%` or `+centered+`. A pair opens before a non-space character
 * that does not follow a letter or digit and closes after a non-space character that is not followed by one.
 */
data class MarkdownDelimiter(val delimiter: String, val effect: Effect) {
    init {
        require(delimiter.isNotEmpty()) { "The delimiter must not be empty" }
    }

    sealed interface Effect {
        /** Drops the delimited text. */
        data object Hide : Effect

        /** Styles the delimited text. */
        data class Style(val style: SpanStyle) : Effect

        /** Styles the paragraph that holds the delimited text, such as centering it. */
        data class Paragraph(val style: ParagraphStyle) : Effect
    }
}

/** Text that becomes a link without markdown link syntax. */
enum class MarkdownAutoLink {
    /** Bare web addresses, such as `www.example.com` or `https://example.com`. */
    Web,

    /** Bare email addresses, linked with `mailto:`. */
    Email,

    /** Phone numbers found by [PhoneNumberLinkifier], linked with `tel:`. */
    Phone
}

/** A piece of rendered markdown: a run of styled text or an image between them. */
sealed interface MarkdownBlock {
    data class Text(val text: AnnotatedString) : MarkdownBlock

    /** An image to load from [source] (a URI such as `file:///android_asset/a.webp`), described by [description]. */
    data class Image(val source: String, val description: String, val title: String? = null) : MarkdownBlock
}

/**
 * Renders GitHub-flavored [markdown] as styled text: emphasis, headings, code, strikethrough, lists, block quotes,
 * tables and links (inline, reference, and the [autoLinks] found in plain text) as [LinkAnnotation.Url]s. Images
 * and raw HTML are left out; use [toBlocks] to keep the images. Blocks are separated by a blank line and soft line
 * breaks become spaces. List items are hanging paragraphs ([ParagraphStyle]s with a [TextIndent]), which break
 * lines themselves, so no newline separates them in the text.
 */
fun Markdown.toAnnotatedString(
    markdown: String,
    styles: MarkdownTextStyles = MarkdownTextStyles(),
    delimiters: List<MarkdownDelimiter> = emptyList(),
    autoLinks: Set<MarkdownAutoLink> = MarkdownAutoLink.entries.toSet()
): AnnotatedString {
    val text = normalizeLineBreaks(markdown)
    return AnnotatedMarkdownRenderer(text, styles, delimiters, autoLinks, keepImages = false).render(parse(text))
}

/**
 * Renders [markdown] like [toAnnotatedString], split around its images: text runs become [MarkdownBlock.Text] and
 * each image a [MarkdownBlock.Image], in reading order, so images can be shown at full width between the text.
 */
fun Markdown.toBlocks(
    markdown: String,
    styles: MarkdownTextStyles = MarkdownTextStyles(),
    delimiters: List<MarkdownDelimiter> = emptyList(),
    autoLinks: Set<MarkdownAutoLink> = MarkdownAutoLink.entries.toSet()
): List<MarkdownBlock> {
    val text = normalizeLineBreaks(markdown)
    val renderer = AnnotatedMarkdownRenderer(text, styles, delimiters, autoLinks, keepImages = true)
    val rendered = renderer.render(parse(text))
    val blocks = mutableListOf<MarkdownBlock>()
    fun addText(start: Int, end: Int) {
        val run = rendered.subSequence(start, end).trim()
        if (run.isNotEmpty()) blocks += MarkdownBlock.Text(run)
    }
    var position = 0
    rendered.getStringAnnotations(IMAGE_TAG, 0, rendered.length).forEach { placeholder ->
        addText(position, placeholder.start)
        blocks += renderer.images[placeholder.item.toInt()]
        position = placeholder.end
    }
    addText(position, rendered.length)
    return blocks
}

private fun normalizeLineBreaks(markdown: String) = markdown.replace("\r\n", "\n").replace("\r", "\n")

private fun parse(text: String): ASTNode {
    return MarkdownParser(GFMFlavourDescriptor(), cancellationToken = CancellationToken.NonCancellable)
        .buildMarkdownTreeFromString(text)
}

private const val IMAGE_TAG = "com.kylecorry.andromeda.markdown.image"

/** Stands in for an image in the rendered text until [toBlocks] splits the text around it. */
private const val IMAGE_PLACEHOLDER = "\uFFFC"

private class AnnotatedMarkdownRenderer(
    private val source: String,
    private val styles: MarkdownTextStyles,
    private val delimiters: List<MarkdownDelimiter>,
    private val autoLinks: Set<MarkdownAutoLink>,
    private val keepImages: Boolean
) {
    private val definitions = mutableMapOf<String, String>()

    /** The images met while rendering with [keepImages], indexed by their placeholders' annotations. */
    val images = mutableListOf<MarkdownBlock.Image>()

    fun render(root: ASTNode): AnnotatedString {
        collectDefinitions(root)
        return joinBlocks(blocksOf(root, depth = 0), BLOCK_SEPARATOR)
    }

    private fun collectDefinitions(node: ASTNode) {
        if (node.type == MarkdownElementTypes.LINK_DEFINITION) {
            val label = node.child(MarkdownElementTypes.LINK_LABEL)?.let(::labelOf)
            val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)?.let(::destinationOf)
            if (label != null && destination != null) {
                if (label !in definitions) definitions[label] = destination
            }
            return
        }
        node.children.forEach(::collectDefinitions)
    }

    // Blocks

    private fun blocksOf(container: ASTNode, depth: Int): List<AnnotatedString> {
        return container.children.mapNotNull { block(it, depth) }.filter { it.isNotEmpty() }
    }

    private fun block(node: ASTNode, depth: Int): AnnotatedString? {
        return when (node.type) {
            MarkdownElementTypes.PARAGRAPH -> paragraph(node.children)
            MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
            MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 ->
                heading(node, ATX_LEVELS.getValue(node.type))

            MarkdownElementTypes.SETEXT_1 -> heading(node, 1)
            MarkdownElementTypes.SETEXT_2 -> heading(node, 2)
            MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> list(node, depth)
            MarkdownElementTypes.BLOCK_QUOTE -> joinBlocks(blocksOf(node, depth), BLOCK_SEPARATOR)
            MarkdownElementTypes.CODE_FENCE -> codeFence(node)
            MarkdownElementTypes.CODE_BLOCK -> codeBlock(node)
            GFMElementTypes.TABLE -> table(node)
            MarkdownTokenTypes.HORIZONTAL_RULE -> AnnotatedString(HORIZONTAL_RULE)
            MarkdownElementTypes.LINK_DEFINITION, MarkdownElementTypes.HTML_BLOCK -> null
            else -> if (node.children.isEmpty()) null else paragraph(node.children)
        }
    }

    private fun heading(node: ASTNode, level: Int): AnnotatedString? {
        val content = node.child(MarkdownTokenTypes.ATX_CONTENT) ?: node.child(MarkdownTokenTypes.SETEXT_CONTENT)
            ?: return null
        val inline = paragraph(if (content.children.isEmpty()) listOf(content) else content.children) ?: return null
        val scale = styles.headingScales.getOrElse(level - 1) { 1f }
        return buildAnnotatedString {
            withStyle(styles.strong.merge(SpanStyle(fontSize = scale.em))) { append(inline) }
        }
    }

    private fun list(node: ASTNode, depth: Int): AnnotatedString {
        val ordered = node.type == MarkdownElementTypes.ORDERED_LIST
        val items = node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }.map { item ->
            val marker = if (ordered) {
                item.child(MarkdownTokenTypes.LIST_NUMBER)?.getTextInNode(source)?.trim() ?: "1."
            } else {
                styles.bullet
            }
            listItem(item, "$marker ", depth)
        }
        return joinBlocks(items, LINE_SEPARATOR)
    }

    /**
     * A list item as hanging paragraphs: the marker indented for its level, and the item's wrapped lines and further
     * paragraphs lined up after the marker. Nested lists indent themselves.
     */
    private fun listItem(item: ASTNode, marker: String, depth: Int): AnnotatedString {
        val indent = styles.listIndent.value * depth
        val textStart = (indent + markerWidth(marker)).em
        val markerParagraph = TextIndent(indent.em, textStart)
        val parts = item.children.mapNotNull { child ->
            block(child, depth + 1)?.takeIf { it.isNotEmpty() }?.let { child.isList() to it }
        }
        val lines = parts.mapIndexed { index, (isList, text) ->
            when {
                isList -> text
                index == 0 -> hanging(buildAnnotatedString { append(marker); append(text) }, markerParagraph)
                else -> hanging(text, TextIndent(textStart, textStart))
            }
        }
        if (parts.firstOrNull()?.first == false) return joinBlocks(lines, LINE_SEPARATOR)
        // An empty item, or one that starts with a nested list, still shows its marker on its own line
        return joinBlocks(listOf(hanging(AnnotatedString(marker.trimEnd()), markerParagraph)) + lines, LINE_SEPARATOR)
    }

    private fun ASTNode.isList() =
        type == MarkdownElementTypes.UNORDERED_LIST || type == MarkdownElementTypes.ORDERED_LIST

    /** [text] as one paragraph indented by [indent], unless it already sets its own paragraph style. */
    private fun hanging(text: AnnotatedString, indent: TextIndent): AnnotatedString {
        if (text.paragraphStyles.isNotEmpty()) return text
        return buildAnnotatedString { withStyle(ParagraphStyle(textIndent = indent)) { append(text) } }
    }

    /** Roughly how wide a list marker and its space are, in ems of the body text. */
    private fun markerWidth(marker: String): Float = marker.sumOf { char ->
        when {
            char.isDigit() -> 0.56
            char == ' ' -> 0.28
            char == '.' || char == ')' -> 0.28
            else -> 0.6
        }
    }.toFloat()

    private fun codeFence(node: ASTNode): AnnotatedString {
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        var started = false
        node.children.forEach { child ->
            when (child.type) {
                MarkdownTokenTypes.CODE_FENCE_CONTENT -> current.append(child.getTextInNode(source))
                MarkdownTokenTypes.EOL -> {
                    if (started) {
                        lines += current.toString()
                        current = StringBuilder()
                    }
                    started = true
                }
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return code(lines.joinToString("\n").trimEnd('\n'))
    }

    private fun codeBlock(node: ASTNode): AnnotatedString {
        val text = node.children.joinToString("") { child ->
            when (child.type) {
                MarkdownTokenTypes.CODE_LINE -> child.getTextInNode(source).toString().removePrefix(CODE_INDENT)
                MarkdownTokenTypes.EOL -> "\n"
                else -> ""
            }
        }
        return code(text.trimEnd('\n'))
    }

    private fun code(text: String) = buildAnnotatedString { withStyle(styles.code) { append(text) } }

    private fun table(node: ASTNode): AnnotatedString {
        val rows = node.children.filter { it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW }.map { row ->
            val cells = row.children.filter { it.type == GFMTokenTypes.CELL }
                .map { inline(it.children).trim() }
            val line = joinBlocks(cells, AnnotatedString(" | "))
            if (row.type == GFMElementTypes.HEADER) {
                buildAnnotatedString { withStyle(styles.strong) { append(line) } }
            } else {
                line
            }
        }
        return joinBlocks(rows, LINE_SEPARATOR)
    }

    // Paragraphs and inline content

    private fun paragraph(children: List<ASTNode>): AnnotatedString? {
        val text = inline(children).trim()
        if (text.isEmpty()) return null
        return applyDelimiters(text)
    }

    private fun inline(children: List<ASTNode>): AnnotatedString {
        return buildAnnotatedString {
            val writer = InlineWriter(this)
            writer.write(children)
            writer.flush()
        }
    }

    private inner class InlineWriter(private val out: AnnotatedString.Builder) {
        private var afterLineBreak = false
        private var afterHardBreak = false

        /** How many links the writer is inside; text in a link is not auto-linked again. */
        private var linkDepth = 0

        /** Plain text waiting to be written; the lexer splits entities such as `&amp;` over several tokens. */
        private val pending = StringBuilder()

        fun write(nodes: List<ASTNode>) = nodes.forEach(::write)

        fun flush() {
            if (pending.isEmpty()) return
            val text = decodeText(pending)
            pending.clear()
            if (linkDepth > 0) {
                out.append(text)
                return
            }
            var position = 0
            autoLinksIn(text).forEach { (range, url) ->
                out.append(text, position, range.first)
                linkText(url, text.substring(range))
                position = range.last + 1
            }
            out.append(text, position, text.length)
        }

        private fun write(node: ASTNode) {
            val type = node.type
            if (type == MarkdownTokenTypes.WHITE_SPACE && afterLineBreak) return
            if (type == MarkdownTokenTypes.EOL && afterHardBreak) {
                afterHardBreak = false
                return
            }
            afterLineBreak = false
            afterHardBreak = false
            if (node.children.isEmpty() && type !in SPECIAL_LEAVES) {
                pending.append(node.getTextInNode(source))
                return
            }
            flush()
            when (type) {
                MarkdownTokenTypes.EOL -> {
                    out.append(' ')
                    afterLineBreak = true
                }

                MarkdownTokenTypes.HARD_LINE_BREAK -> {
                    out.append('\n')
                    afterLineBreak = true
                    afterHardBreak = true
                }

                MarkdownElementTypes.STRONG -> styled(styles.strong, node.children.trimDelimiters(MarkdownTokenTypes.EMPH))
                MarkdownElementTypes.EMPH -> styled(styles.emphasis, node.children.trimDelimiters(MarkdownTokenTypes.EMPH))
                GFMElementTypes.STRIKETHROUGH -> styled(styles.strikethrough, node.children.trimDelimiters(GFMTokenTypes.TILDE))
                MarkdownElementTypes.CODE_SPAN -> out.withStyle(styles.code) { append(codeSpanText(node)) }
                MarkdownElementTypes.INLINE_LINK -> {
                    val label = node.child(MarkdownElementTypes.LINK_TEXT)
                    val url = node.child(MarkdownElementTypes.LINK_DESTINATION)?.let(::destinationOf)
                    link(url, label?.children?.trimDelimiters(MarkdownTokenTypes.LBRACKET, MarkdownTokenTypes.RBRACKET).orEmpty())
                }

                MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                    val label = node.child(MarkdownElementTypes.LINK_TEXT)
                    val url = node.child(MarkdownElementTypes.LINK_LABEL)?.let(::labelOf)?.let(definitions::get)
                    referenceLink(node, url, label)
                }

                MarkdownElementTypes.SHORT_REFERENCE_LINK -> {
                    val label = node.child(MarkdownElementTypes.LINK_LABEL)
                    val url = label?.let(::labelOf)?.let(definitions::get)
                    referenceLink(node, url, label)
                }

                MarkdownElementTypes.AUTOLINK -> {
                    val target = node.children.firstOrNull {
                        it.type == MarkdownTokenTypes.AUTOLINK || it.type == MarkdownTokenTypes.EMAIL_AUTOLINK
                    }
                    if (target == null) {
                        write(node.children)
                    } else {
                        val text = target.getTextInNode(source).toString()
                        val url = if (target.type == MarkdownTokenTypes.EMAIL_AUTOLINK) "mailto:$text" else text
                        linkText(url, text)
                    }
                }

                GFMTokenTypes.GFM_AUTOLINK -> {
                    val text = node.getTextInNode(source).toString()
                    val url = bareUrl(text)
                    val kind = if (url.startsWith("mailto:")) MarkdownAutoLink.Email else MarkdownAutoLink.Web
                    if (kind in autoLinks && linkDepth == 0) {
                        linkText(url, text)
                    } else {
                        pending.append(text)
                    }
                }

                GFMTokenTypes.CHECK_BOX -> out.append(if (node.getTextInNode(source).contains('x', ignoreCase = true)) "☑ " else "☐ ")
                // Quote markers that continue a quoted paragraph on its next lines aren't part of the text.
                MarkdownTokenTypes.BLOCK_QUOTE -> afterLineBreak = true
                MarkdownElementTypes.IMAGE -> if (keepImages) image(node)
                MarkdownTokenTypes.HTML_TAG, MarkdownElementTypes.LINK_DEFINITION -> Unit
                else -> write(node.children)
            }
        }

        private fun styled(style: SpanStyle, children: List<ASTNode>) {
            val start = out.length
            write(children)
            flush()
            out.addStyle(style, start, out.length)
        }

        private fun link(url: String?, label: List<ASTNode>) {
            if (url.isNullOrBlank()) {
                write(label)
                return
            }
            val start = out.length
            linkDepth++
            write(label)
            flush()
            linkDepth--
            out.addLink(LinkAnnotation.Url(url, TextLinkStyles(styles.link)), start, out.length)
        }

        private fun image(node: ASTNode) {
            val link = node.children.firstOrNull { it.type != MarkdownTokenTypes.EXCLAMATION_MARK } ?: return
            val text = link.child(MarkdownElementTypes.LINK_TEXT)
            val url = when (link.type) {
                MarkdownElementTypes.INLINE_LINK -> link.child(MarkdownElementTypes.LINK_DESTINATION)?.let(::destinationOf)
                MarkdownElementTypes.FULL_REFERENCE_LINK, MarkdownElementTypes.SHORT_REFERENCE_LINK ->
                    link.child(MarkdownElementTypes.LINK_LABEL)?.let(::labelOf)?.let(definitions::get)
                else -> null
            }
            if (url.isNullOrBlank()) return
            val description = (text ?: link.child(MarkdownElementTypes.LINK_LABEL))
                ?.children?.trimDelimiters(MarkdownTokenTypes.LBRACKET, MarkdownTokenTypes.RBRACKET)
                ?.let(::plainText).orEmpty()
            val title = link.child(MarkdownElementTypes.LINK_TITLE)?.getTextInNode(source)?.toString()
                ?.let { decodeText(it.substring(1, it.length - 1)) }
            val start = out.length
            out.append(IMAGE_PLACEHOLDER)
            out.addStringAnnotation(IMAGE_TAG, images.size.toString(), start, out.length)
            images += MarkdownBlock.Image(url, description, title)
        }

        private fun linkText(url: String, text: String) {
            val start = out.length
            out.append(text)
            out.addLink(LinkAnnotation.Url(url, TextLinkStyles(styles.link)), start, out.length)
        }

        private fun referenceLink(node: ASTNode, url: String?, label: ASTNode?) {
            if (url == null || label == null) {
                out.append(decodeText(node.getTextInNode(source)))
                return
            }
            val inner = label.children.trimDelimiters(MarkdownTokenTypes.LBRACKET, MarkdownTokenTypes.RBRACKET)
            link(url, inner)
        }
    }

    /**
     * The bare emails and phone numbers in plain [text] that [autoLinks] asks for, in order and not overlapping.
     * Bare web addresses are found by the GFM parser.
     */
    private fun autoLinksIn(text: String): List<Pair<IntRange, String>> {
        val found = mutableListOf<Pair<IntRange, String>>()
        if (MarkdownAutoLink.Email in autoLinks) {
            EMAIL.findAll(text).forEach { found += it.range to "mailto:${it.value}" }
        }
        if (MarkdownAutoLink.Phone in autoLinks) {
            PhoneNumberLinkifier.find(text).forEach { found += it.range to it.url }
        }
        var end = -1
        return found.sortedBy { it.first.first }.filter { (range, _) ->
            (range.first > end).also { if (it) end = range.last }
        }
    }

    /** The text of inline content without its styles, links or images, such as an image's description. */
    private fun plainText(nodes: List<ASTNode>): String {
        return AnnotatedMarkdownRenderer(source, styles, emptyList(), emptySet(), keepImages = false)
            .inline(nodes).text.trim()
    }

    private fun codeSpanText(node: ASTNode): String {
        val inner = node.children.trimDelimiters(MarkdownTokenTypes.BACKTICK, MarkdownTokenTypes.ESCAPED_BACKTICKS)
        val text = inner.joinToString("") { child ->
            if (child.type == MarkdownTokenTypes.EOL) " " else child.getTextInNode(source).toString()
        }
        // CommonMark strips one space on each side when both are present and the span is not only spaces.
        return if (text.length >= 2 && text.first() == ' ' && text.last() == ' ' && text.isNotBlank()) {
            text.substring(1, text.length - 1)
        } else {
            text
        }
    }

    private fun destinationOf(node: ASTNode): String {
        return decodeText(node.getTextInNode(source).toString().trim().removeSurrounding("<", ">"))
    }

    private fun labelOf(node: ASTNode): String {
        return node.getTextInNode(source).toString().removeSurrounding("[", "]").trim().lowercase()
            .replace(WHITESPACE, " ")
    }

    private fun bareUrl(text: String): String = when {
        text.contains("://") || text.startsWith("mailto:") -> text
        text.startsWith("www.", ignoreCase = true) -> "http://$text"
        text.contains('@') -> "mailto:$text"
        else -> text
    }

    // Custom delimiters

    private fun applyDelimiters(text: AnnotatedString): AnnotatedString {
        if (delimiters.isEmpty()) return text
        val raw = text.text
        val cuts = mutableListOf<IntRange>()
        val styled = mutableListOf<Pair<IntRange, SpanStyle>>()
        var paragraphStyle: ParagraphStyle? = null
        var index = 0
        while (index < raw.length) {
            val match = delimiters.firstNotNullOfOrNull { delimiter -> findPair(raw, index, delimiter)?.let { delimiter to it } }
            if (match == null) {
                index++
                continue
            }
            val (delimiter, close) = match
            val length = delimiter.delimiter.length
            val inner = (index + length) until close
            when (val effect = delimiter.effect) {
                MarkdownDelimiter.Effect.Hide -> cuts += index until close + length
                is MarkdownDelimiter.Effect.Style -> {
                    cuts += index until index + length
                    cuts += close until close + length
                    styled += inner to effect.style
                }

                is MarkdownDelimiter.Effect.Paragraph -> {
                    cuts += index until index + length
                    cuts += close until close + length
                    paragraphStyle = paragraphStyle?.merge(effect.style) ?: effect.style
                }
            }
            index = close + length
        }
        if (cuts.isEmpty()) return text

        fun map(position: Int): Int = position - cuts.sumOf { cut ->
            when {
                cut.last < position -> cut.last - cut.first + 1
                cut.first < position -> position - cut.first
                else -> 0
            }
        }

        val result = buildAnnotatedString {
            var position = 0
            cuts.sortedBy { it.first }.forEach { cut ->
                if (cut.first > position) append(text.subSequence(position, cut.first))
                position = maxOf(position, cut.last + 1)
            }
            if (position < raw.length) append(text.subSequence(position, raw.length))
            styled.forEach { (range, style) -> addStyle(style, map(range.first), map(range.last + 1)) }
        }
        val trimmed = result.trim()
        val style = paragraphStyle ?: return trimmed
        return buildAnnotatedString { withStyle(style) { append(trimmed) } }
    }

    /** The index of the delimiter closing a pair that opens at [start], or null when no pair opens there. */
    private fun findPair(text: String, start: Int, delimiter: MarkdownDelimiter): Int? {
        val d = delimiter.delimiter
        if (!text.startsWith(d, start)) return null
        val afterOpen = start + d.length
        if (start > 0 && text[start - 1].isLetterOrDigit()) return null
        if (afterOpen >= text.length || text[afterOpen].isWhitespace() || text.startsWith(d, afterOpen)) return null
        var close = text.indexOf(d, afterOpen)
        while (close >= 0) {
            val afterClose = close + d.length
            val closes = !text[close - 1].isWhitespace() && (afterClose >= text.length || !text[afterClose].isLetterOrDigit())
            if (closes) return close
            close = text.indexOf(d, close + 1)
        }
        return null
    }

    /**
     * Joins [blocks] with [separator]. A paragraph style already breaks the line where it starts or ends, so one
     * newline of the separator is dropped there; otherwise it would show as an extra empty line. Between two styled
     * paragraphs a newline on its own would be a paragraph of two empty lines, so the one that is left ends the
     * paragraph before it instead.
     */
    private fun joinBlocks(blocks: List<AnnotatedString>, separator: AnnotatedString): AnnotatedString {
        val parts = blocks.toMutableList()
        val separators = MutableList(maxOf(blocks.size - 1, 0)) { separator }
        for (index in separators.indices) {
            val endsParagraph = parts[index].endsParagraph()
            val startsParagraph = parts[index + 1].startsParagraph()
            if (!endsParagraph && !startsParagraph) continue
            val remaining = separator.subSequence(1, separator.length)
            if (endsParagraph && startsParagraph && remaining.isNotEmpty()) {
                parts[index] = parts[index].withNewlineInLastParagraph()
                separators[index] = remaining.subSequence(1, remaining.length)
            } else {
                separators[index] = remaining
            }
        }
        return buildAnnotatedString {
            parts.forEachIndexed { index, block ->
                if (index > 0) append(separators[index - 1])
                append(block)
            }
        }
    }

    /** This text and a newline, which the paragraph styles that ended the text also cover. */
    private fun AnnotatedString.withNewlineInLastParagraph(): AnnotatedString {
        val original = this
        return buildAnnotatedString {
            append(original.text)
            append('\n')
            original.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            original.paragraphStyles.forEach {
                addStyle(it.item, it.start, if (it.end == original.length) it.end + 1 else it.end)
            }
            original.getStringAnnotations(0, original.length).forEach {
                addStringAnnotation(it.tag, it.item, it.start, it.end)
            }
            original.getLinkAnnotations(0, original.length).forEach {
                when (val link = it.item) {
                    is LinkAnnotation.Url -> addLink(link, it.start, it.end)
                    is LinkAnnotation.Clickable -> addLink(link, it.start, it.end)
                }
            }
        }
    }

    private fun AnnotatedString.startsParagraph() = paragraphStyles.any { it.start == 0 }

    private fun AnnotatedString.endsParagraph() = paragraphStyles.any { it.end == length }

    private companion object {
        val BLOCK_SEPARATOR = AnnotatedString("\n\n")
        val LINE_SEPARATOR = AnnotatedString("\n")
        const val HORIZONTAL_RULE = "———"
        const val CODE_INDENT = "    "
        val WHITESPACE = Regex("\\s+")
        val EMAIL = Regex("""(?<![\p{L}\p{N}._%+-])[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}(?![\p{L}\p{N}_-])""")
        /** Leaf tokens that are not plain text. */
        val SPECIAL_LEAVES = setOf(
            MarkdownTokenTypes.EOL,
            MarkdownTokenTypes.HARD_LINE_BREAK,
            MarkdownTokenTypes.BLOCK_QUOTE,
            MarkdownTokenTypes.HTML_TAG,
            GFMTokenTypes.GFM_AUTOLINK,
            GFMTokenTypes.CHECK_BOX
        )
        val ATX_LEVELS = mapOf(
            MarkdownElementTypes.ATX_1 to 1,
            MarkdownElementTypes.ATX_2 to 2,
            MarkdownElementTypes.ATX_3 to 3,
            MarkdownElementTypes.ATX_4 to 4,
            MarkdownElementTypes.ATX_5 to 5,
            MarkdownElementTypes.ATX_6 to 6
        )
    }
}

private fun ASTNode.child(type: IElementType): ASTNode? = children.firstOrNull { it.type == type }

/** Drops the delimiter tokens (such as `**` or `[` and `]`) from both ends of an element's children. */
private fun List<ASTNode>.trimDelimiters(vararg types: IElementType): List<ASTNode> {
    return dropWhile { it.type in types }.dropLastWhile { it.type in types }
}

private fun AnnotatedString.trim(): AnnotatedString {
    val start = text.indexOfFirst { !it.isWhitespace() }
    if (start < 0) return AnnotatedString("")
    val end = text.indexOfLast { !it.isWhitespace() } + 1
    return if (start == 0 && end == length) this else subSequence(start, end)
}

private val ESCAPE_OR_ENTITY = Regex("""\\([!-/:-@\[-`{-~])|&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z][a-zA-Z0-9]{1,31});""")

private val NAMED_ENTITIES = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00A0",
    "copy" to "©", "reg" to "®", "trade" to "™", "deg" to "°", "plusmn" to "±", "times" to "×", "divide" to "÷",
    "middot" to "·", "bull" to "•", "hellip" to "…", "ndash" to "–", "mdash" to "—", "lsquo" to "‘", "rsquo" to "’",
    "ldquo" to "“", "rdquo" to "”", "laquo" to "«", "raquo" to "»", "frac12" to "½", "frac14" to "¼", "frac34" to "¾",
    "micro" to "µ", "para" to "¶", "sect" to "§", "euro" to "€", "pound" to "£", "yen" to "¥", "cent" to "¢",
    "larr" to "←", "rarr" to "→", "uarr" to "↑", "darr" to "↓", "harr" to "↔", "le" to "≤", "ge" to "≥", "ne" to "≠",
    "asymp" to "≈", "infin" to "∞", "minus" to "−", "prime" to "′", "Prime" to "″"
)

/** Resolves backslash escapes and HTML entities (named and numeric) the way CommonMark does in text. */
private fun decodeText(text: CharSequence): String {
    return ESCAPE_OR_ENTITY.replace(text) { match ->
        match.groups[1]?.let { return@replace it.value }
        val entity = match.groupValues[2]
        when {
            entity.startsWith("#x") || entity.startsWith("#X") -> codePoint(entity.substring(2).toIntOrNull(16))
            entity.startsWith("#") -> codePoint(entity.substring(1).toIntOrNull())
            else -> NAMED_ENTITIES[entity]
        } ?: match.value
    }
}

private fun codePoint(value: Int?): String? {
    if (value == null || value !in 1..0x10FFFF || value in 0xD800..0xDFFF) return if (value == 0) "\uFFFD" else null
    return if (value < 0x10000) {
        value.toChar().toString()
    } else {
        val offset = value - 0x10000
        charArrayOf((0xD800 + (offset shr 10)).toChar(), (0xDC00 + (offset and 0x3FF)).toChar()).concatToString()
    }
}
