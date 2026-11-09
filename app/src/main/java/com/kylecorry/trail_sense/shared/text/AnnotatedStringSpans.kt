package com.kylecorry.trail_sense.shared.text

import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AlignmentSpan
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
import androidx.compose.ui.unit.TextUnitType

/**
 * Android spans for text drawn outside Compose (such as a PDF laid out with StaticLayout): weight, italics, relative
 * sizes, monospace, underline and strike-through, links and centered paragraphs.
 */
fun AnnotatedString.toSpanned(): Spanned {
    val spannable = SpannableString(text)
    fun span(what: Any, start: Int, end: Int) {
        if (end > start) spannable.setSpan(what, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
    }
    getLinkAnnotations(0, length).forEach { range ->
        (range.item as? LinkAnnotation.Url)?.let { span(URLSpan(it.url), range.start, range.end) }
    }
    return spannable
}
