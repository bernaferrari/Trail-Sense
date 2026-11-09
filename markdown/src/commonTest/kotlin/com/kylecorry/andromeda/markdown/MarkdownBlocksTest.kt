package com.kylecorry.andromeda.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownBlocksTest {

    @Test
    fun splitsTextAroundImages() {
        val blocks = Markdown.toBlocks(
            "# Splint\n\nBefore the image.\n\n![A splint](file:///android_asset/guide/splint.webp \"Splinting\")\n\nAfter **it**."
        )
        assertEquals(3, blocks.size)
        assertEquals("Splint\n\nBefore the image.", (blocks[0] as MarkdownBlock.Text).text.text)
        assertEquals(
            MarkdownBlock.Image("file:///android_asset/guide/splint.webp", "A splint", "Splinting"),
            blocks[1]
        )
        assertEquals("After it.", (blocks[2] as MarkdownBlock.Text).text.text)
    }

    @Test
    fun keepsImagesInsideParagraphsInOrder() {
        val blocks = Markdown.toBlocks("See ![](file:///a.png) then ![*b*][b] done\n\n[b]: android.resource://app/drawable/b")
        assertEquals(
            listOf(
                MarkdownBlock.Text(AnnotatedString("See")),
                MarkdownBlock.Image("file:///a.png", ""),
                MarkdownBlock.Text(AnnotatedString("then")),
                MarkdownBlock.Image("android.resource://app/drawable/b", "b"),
                MarkdownBlock.Text(AnnotatedString("done"))
            ),
            blocks
        )
    }

    @Test
    fun textWithoutImagesIsOneBlock() {
        val markdown = "Some *text*\n\n- a\n- b"
        assertEquals(
            listOf(MarkdownBlock.Text(Markdown.toAnnotatedString(markdown))),
            Markdown.toBlocks(markdown)
        )
    }

    @Test
    fun annotatedStringStillDropsImages() {
        assertEquals("Before\n\nAfter", Markdown.toAnnotatedString("Before\n\n![x](file:///x.png)\n\nAfter").text)
    }

    @Test
    fun linksPhoneNumbersInText() {
        val text = Markdown.toAnnotatedString("Call **555-123-4567** or +44 20 7946 0018.")
        assertEquals("Call 555-123-4567 or +44 20 7946 0018.", text.text)
        assertEquals(
            listOf("555-123-4567" to "tel:5551234567", "+44 20 7946 0018" to "tel:+442079460018"),
            text.links()
        )
    }

    @Test
    fun doesNotLinkPhoneNumbersInLinksOrCode() {
        val text = Markdown.toAnnotatedString("[555-123-4567](https://example.com) and `555-765-4321`")
        assertEquals(listOf("555-123-4567" to "https://example.com"), text.links())
    }

    @Test
    fun autoLinksCanBeTurnedOff() {
        val markdown = "Mail a@example.com, visit www.example.com or example.org/maps or call 555-123-4567. " +
            "[Explicit](https://e.com)"
        assertEquals(
            listOf(
                "a@example.com" to "mailto:a@example.com",
                "www.example.com" to "http://www.example.com",
                "example.org/maps" to "http://example.org/maps",
                "555-123-4567" to "tel:5551234567",
                "Explicit" to "https://e.com"
            ),
            Markdown.toAnnotatedString(markdown).links()
        )
        val none = Markdown.toAnnotatedString(markdown, autoLinks = emptySet())
        assertEquals(listOf("Explicit" to "https://e.com"), none.links())
        assertTrue(none.text.startsWith("Mail a@example.com, visit www.example.com or example.org/maps or call"))
    }

    @Test
    fun doesNotLinkDomainsInLinksCodeOrFileNames() {
        val text = Markdown.toAnnotatedString(
            "[see example.com](https://other.org), `example.com`, photo.jpg and notes.md, but sub.example.org/a."
        )
        assertEquals(
            listOf("see example.com" to "https://other.org", "sub.example.org/a" to "http://sub.example.org/a"),
            text.links()
        )
    }

    private fun AnnotatedString.links(): List<Pair<String, String>> {
        return getLinkAnnotations(0, length).map { text.substring(it.start, it.end) to (it.item as LinkAnnotation.Url).url }
    }
}
