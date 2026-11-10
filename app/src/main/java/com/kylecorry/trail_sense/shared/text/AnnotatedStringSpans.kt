package com.kylecorry.trail_sense.shared.text

import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import kotlin.math.roundToInt

/**
 * Android spans for text drawn outside Compose (such as a PDF laid out with StaticLayout): weight, italics, relative
 * sizes, monospace, underline and strike-through, links, and centered or indented paragraphs. Compose breaks lines
 * at paragraph styles by itself, so a newline is added where a paragraph starts or ends without one. [textSize] (in
 * pixels) sizes the paragraphs' em indents.
 */
fun AnnotatedString.toSpanned(textSize: Float): Spanned {
    val breaks = paragraphBreaks()
    val spannable = SpannableStringBuilder(text)
    breaks.sortedDescending().forEach { spannable.insert(it, "\n") }
    // Once the breaks are inserted, a range starts after the breaks at or before it and ends after those before it
    fun span(what: Any, start: Int, end: Int) {
        val from = start + breaks.count { it <= start }
        val to = end + breaks.count { it < end }
        if (to > from) spannable.setSpan(what, from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    spanStyles.forEach { range ->
        val style = range.item
        val bold = (style.fontWeight?.weight ?: 0) >= FontWeight.Bold.weight
        val italic = style.fontStyle == FontStyle.Italic
        when {
            bold && italic -> span(StyleSpan(Typeface.BOLD_ITALIC), range.start, range.end)
            bold -> span(StyleSpan(Typeface.BOLD), range.start, range.end)
            italic -> span(StyleSpan(Typeface.ITALIC), range.start, range.end)
        }
        if (style.fontSize.type == TextUnitType.Em) {
            span(RelativeSizeSpan(style.fontSize.value), range.start, range.end)
        }
        if (style.fontFamily == FontFamily.Monospace) {
            span(TypefaceSpan("monospace"), range.start, range.end)
        }
        style.textDecoration?.let { decoration ->
            if (decoration.contains(TextDecoration.Underline)) span(UnderlineSpan(), range.start, range.end)
            if (decoration.contains(TextDecoration.LineThrough)) span(StrikethroughSpan(), range.start, range.end)
        }
    }
    paragraphStyles.forEach { range ->
        if (range.item.textAlign == TextAlign.Center) {
            span(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), range.start, range.end)
        }
        range.item.textIndent?.let { indent ->
            val first = indent.firstLine.toPixels(textSize)
            val rest = indent.restLine.toPixels(textSize)
            if (first > 0 || rest > 0) span(LeadingMarginSpan.Standard(first, rest), range.start, range.end)
        }
    }
    getLinkAnnotations(0, length).forEach { range ->
        (range.item as? LinkAnnotation.Url)?.let { span(URLSpan(it.url), range.start, range.end) }
    }
    return spannable
}

/** The offsets where a paragraph style starts or ends without a newline next to it. */
private fun AnnotatedString.paragraphBreaks(): Set<Int> {
    val breaks = mutableSetOf<Int>()
    paragraphStyles.forEach { range ->
        if (range.start > 0 && text[range.start - 1] != '\n') breaks += range.start
        if (range.end < length && text[range.end - 1] != '\n' && text[range.end] != '\n') breaks += range.end
    }
    return breaks
}

private fun TextUnit.toPixels(textSize: Float): Int = when (type) {
    TextUnitType.Em -> (value * textSize).roundToInt()
    TextUnitType.Sp -> value.roundToInt()
    else -> 0
}
