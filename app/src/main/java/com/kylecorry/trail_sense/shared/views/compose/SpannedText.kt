package com.kylecorry.trail_sense.shared.views.compose

import android.graphics.Typeface
import android.text.Layout
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AlignmentSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import com.kylecorry.trail_sense.shared.text.HiddenSpan

/**
 * Styled Android text (what MarkdownService and Html produce) as Compose text: bold, italics, size changes (headings),
 * colors, underline and strike-through keep their look, URL spans become links (opening when [linksClickable]),
 * list items get their bullet or number back as text, centered paragraphs stay centered, and hidden spans drop out.
 */
fun CharSequence.toAnnotatedString(linkColor: Color, linksClickable: Boolean = true): AnnotatedString {
    if (this !is Spanned) return AnnotatedString(toString())
    val source = this
    val text = source.toString()

    val styled = buildAnnotatedString {
        append(text)
        val usedParagraphs = mutableListOf<IntRange>()
        source.getSpans(0, source.length, Any::class.java).forEach { span ->
            val start = source.getSpanStart(span)
            val end = source.getSpanEnd(span)
            if (start < 0 || end <= start) return@forEach
            when (span) {
                is URLSpan -> {
                    val style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                    if (linksClickable) {
                        addLink(LinkAnnotation.Url(span.url, TextLinkStyles(style)), start, end)
                    } else {
                        addStyle(style, start, end)
                    }
                }

                is ForegroundColorSpan -> addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
                is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                is StrikethroughSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
                is StyleSpan -> styleOf(span.style)?.let { addStyle(it, start, end) }
                is MetricAffectingSpan -> metricStyleOf(span)?.let { addStyle(it, start, end) }
                is CharacterStyle -> Unit
                is AlignmentSpan -> {
                    if (span.alignment == Layout.Alignment.ALIGN_CENTER) {
                        // Paragraph styles must cover whole paragraphs and may not overlap.
                        val paragraphStart = text.lastIndexOf('\n', start - 1).let { if (it < 0) 0 else it + 1 }
                        val paragraphEnd = text.indexOf('\n', end - 1).let { if (it < 0) text.length else it }
                        val range = paragraphStart until paragraphEnd
                        if (!range.isEmpty() && usedParagraphs.none { it.first <= range.last && range.first <= it.last }) {
                            usedParagraphs += range
                            addStyle(ParagraphStyle(textAlign = TextAlign.Center), paragraphStart, paragraphEnd)
                        }
                    }
                }
            }
        }
    }

    // Bullets and numbers that the spans drew, and text the spans hide.
    val inserts = mutableListOf<Pair<Int, String>>()
    val hidden = mutableListOf<IntRange>()
    source.getSpans(0, source.length, Any::class.java).forEach { span ->
        val start = source.getSpanStart(span)
        val end = source.getSpanEnd(span)
        if (start < 0 || end <= start) return@forEach
        when {
            span is HiddenSpan -> hidden += start until end
            span is LeadingMarginSpan -> listMarker(span)?.let { inserts += start to it }
        }
    }
    if (inserts.isEmpty() && hidden.isEmpty()) return styled

    val cuts = (inserts.map { it.first } + hidden.flatMap { listOf(it.first, it.last + 1) } + text.length)
        .distinct()
        .sorted()
    return buildAnnotatedString {
        var position = 0
        for (cut in cuts) {
            if (cut > position && hidden.none { position >= it.first && position <= it.last }) {
                append(styled.subSequence(position, cut))
            }
            if (cut >= position) {
                inserts.filter { it.first == cut }.forEach { append(it.second) }
            }
            position = maxOf(position, cut)
        }
    }
}

private fun styleOf(style: Int): SpanStyle? = when (style) {
    Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    Typeface.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
    else -> null
}

/** What a span that changes the paint (Markwon's strong, emphasis and heading spans) does to the text. */
private fun metricStyleOf(span: MetricAffectingSpan): SpanStyle? {
    val base = 100f
    val paint = TextPaint().apply { textSize = base }
    runCatching { span.updateMeasureState(paint) }.getOrNull() ?: return null
    val bold = paint.isFakeBoldText || paint.typeface?.isBold == true
    val italic = paint.textSkewX != 0f || paint.typeface?.isItalic == true
    val scale = paint.textSize / base
    if (!bold && !italic && scale == 1f) return null
    return SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        fontSize = if (scale != 1f) scale.em else androidx.compose.ui.unit.TextUnit.Unspecified
    )
}

/** Markwon draws list bullets and numbers in the margin; as plain text they lead the item instead. */
private fun listMarker(span: LeadingMarginSpan): String? {
    return when (span.javaClass.simpleName) {
        "BulletListItemSpan" -> "• "
        "OrderedListItemSpan" -> runCatching {
            val field = span.javaClass.getDeclaredField("number")
            field.isAccessible = true
            // Markwon keeps the number with its dot and a no-break space, such as "1. ".
            val number = field.get(span).toString()
            if (number.lastOrNull()?.isWhitespace() == true || number.endsWith(' ')) number else "$number "
        }.getOrNull()

        else -> null
    }
}
