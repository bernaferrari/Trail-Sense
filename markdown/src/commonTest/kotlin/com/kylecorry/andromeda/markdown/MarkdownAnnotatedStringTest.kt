package com.kylecorry.andromeda.markdown

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownAnnotatedStringTest {

    @Test
    fun rendersInlineStyles() {
        val text = Markdown.toAnnotatedString("Some **bold**, *italic*, ~~gone~~ and `code`.")
        assertEquals("Some bold, italic, gone and code.", text.text)
        assertTrue(text.hasStyle("bold") { it.fontWeight == FontWeight.Bold })
        assertTrue(text.hasStyle("italic") { it.fontStyle == FontStyle.Italic })
        assertTrue(text.hasStyle("gone") { it.textDecoration == TextDecoration.LineThrough })
        assertTrue(text.hasStyle("code") { it.fontFamily == FontFamily.Monospace })
    }

    @Test
    fun separatesBlocksAndJoinsSoftBreaks() {
        val text = Markdown.toAnnotatedString("# Title\n\nFirst line\nsame paragraph\n\nSecond  \nhard break")
        assertEquals("Title\n\nFirst line same paragraph\n\nSecond\nhard break", text.text)
        assertTrue(text.hasStyle("Title") { it.fontWeight == FontWeight.Bold })
    }

    @Test
    fun rendersLists() {
        val text = Markdown.toAnnotatedString("Steps:\n\n- one\n- two\n  - nested\n\n1. first\n2. second\n\n   more on second\n\nDone")
        // Copied text has one item per line; nested items and further paragraphs are indented with em spaces
        assertEquals(
            listOf("Steps:", "• one", "• two", "\u2003\u2005\u2005• nested", "1. first", "2. second", "\u2003more on second", "Done"),
            text.text.lines().filter { it.isNotBlank() }
        )
        assertTrue("• one\n• two\n\u2003\u2005\u2005• nested" in text.text)
        assertTrue("1. first\n2. second\n\u2003more on second" in text.text)
        if (indentsEachLine) {
            // Each list is one paragraph whose wrapped lines hang after the markers
            val lists = text.paragraphStyles
            assertEquals(2, lists.size)
            assertTrue(text.text.substring(lists[0].start, lists[0].end).startsWith("• one"))
            assertTrue(lists.all { it.item.textIndent!!.restLine.value > it.item.textIndent!!.firstLine.value })
        } else {
            assertEquals("Steps:\n\n• one\n• two\n\u2003\u2005\u2005• nested\n\n1. first\n2. second\n\u2003more on second\n\nDone", text.text)
            assertTrue(text.paragraphStyles.isEmpty())
        }
    }

    @Test
    fun rendersLinks() {
        val text = Markdown.toAnnotatedString(
            "See [the site](https://example.com), <https://kylecorry.com>, www.trailsense.org and [ref].\n\n[ref]: https://ref.example"
        )
        assertEquals("See the site, https://kylecorry.com, www.trailsense.org and ref.", text.text)
        assertEquals(
            listOf(
                "the site" to "https://example.com",
                "https://kylecorry.com" to "https://kylecorry.com",
                "www.trailsense.org" to "http://www.trailsense.org",
                "ref" to "https://ref.example"
            ),
            text.getLinkAnnotations(0, text.length).map {
                text.text.substring(it.start, it.end) to (it.item as LinkAnnotation.Url).url
            }
        )
    }

    @Test
    fun rendersQuotesCodeAndEntities() {
        val text = Markdown.toAnnotatedString("> quoted\n> text\n\n```\nval x = 1\n\nval y = 2\n```\n\nTom &amp; Jerry &#169; &#x1F600; a < b \\*not italic\\*")
        assertEquals("quoted text\n\nval x = 1\n\nval y = 2\n\nTom & Jerry © \uD83D\uDE00 a < b *not italic*", text.text)
    }

    @Test
    fun dropsImagesAndHtml() {
        val text = Markdown.toAnnotatedString("Before ![alt](file:///image.webp) after<!-- comment -->")
        assertEquals("Before  after", text.text)
    }

    @Test
    fun appliesCustomDelimiters() {
        val center = ParagraphStyle(textAlign = TextAlign.Center)
        val delimiters = listOf(
            MarkdownDelimiter("%%", MarkdownDelimiter.Effect.Hide),
            MarkdownDelimiter("+", MarkdownDelimiter.Effect.Paragraph(center)),
            MarkdownDelimiter("^", MarkdownDelimiter.Effect.Style(SpanStyle(fontWeight = FontWeight.Bold)))
        )
        val text = Markdown.toAnnotatedString("Shown %%secret%% text ^up^\n\n+Centered+\n\n1+1 = 2 and 50% + 3", delimiters = delimiters)
        // The centered paragraph breaks the lines around it, so one newline of each blank line is left
        assertEquals("Shown  text up\nCentered\n1+1 = 2 and 50% + 3", text.text)
        assertTrue(text.hasStyle("up") { it.fontWeight == FontWeight.Bold })
        val centered = text.paragraphStyles.single()
        assertEquals("Centered", text.text.substring(centered.start, centered.end))
        assertEquals(TextAlign.Center, centered.item.textAlign)
    }

    private fun androidx.compose.ui.text.AnnotatedString.hasStyle(word: String, predicate: (SpanStyle) -> Boolean): Boolean {
        val start = text.indexOf(word)
        val end = start + word.length
        return spanStyles.any { it.start <= start && it.end >= end && predicate(it.item) }
    }
}
