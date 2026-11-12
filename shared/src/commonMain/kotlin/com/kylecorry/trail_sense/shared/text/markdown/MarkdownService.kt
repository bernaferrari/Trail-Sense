package com.kylecorry.trail_sense.shared.text.markdown

import androidx.compose.ui.text.AnnotatedString
import com.kylecorry.andromeda.markdown.Markdown
import com.kylecorry.andromeda.markdown.MarkdownBlock
import com.kylecorry.andromeda.markdown.MarkdownDelimiter
import com.kylecorry.andromeda.markdown.toAnnotatedString
import com.kylecorry.andromeda.markdown.toBlocks

/**
 * Turns the app's markdown (strings, guides, plugin and layer text) into Compose text. Links, including bare web
 * addresses, emails and phone numbers, are [androidx.compose.ui.text.LinkAnnotation]s; screens color them with
 * [com.kylecorry.trail_sense.shared.views.compose.toAnnotatedString]. [toMarkdown] leaves images out; [toBlocks]
 * keeps them for [com.kylecorry.trail_sense.shared.views.compose.TrailSenseMarkdown].
 */
class MarkdownService(private val delimiters: List<MarkdownDelimiter> = emptyList()) {

    fun toMarkdown(markdown: String): AnnotatedString {
        return Markdown.toAnnotatedString(markdown, delimiters = delimiters)
    }

    /** The markdown as text runs and the images between them. */
    fun toBlocks(markdown: String): List<MarkdownBlock> {
        return Markdown.toBlocks(markdown, delimiters = delimiters)
    }
}
