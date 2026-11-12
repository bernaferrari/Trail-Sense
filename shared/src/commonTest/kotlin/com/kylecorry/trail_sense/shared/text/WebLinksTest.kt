package com.kylecorry.trail_sense.shared.text

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebLinksTest {

    @Test
    fun linksAnAddressAtTheEndOfASentence() {
        val text = "Find maps at https://kylecorry.com/Trail-Sense/offline_maps.html."

        assertEquals(
            listOf("https://kylecorry.com/Trail-Sense/offline_maps.html" to "https://kylecorry.com/Trail-Sense/offline_maps.html"),
            links(text)
        )
    }

    @Test
    fun linksEveryAddress() {
        val text = "http://a.example/x, then www.b.example; done"

        assertEquals(
            listOf("http://a.example/x" to "http://a.example/x", "www.b.example" to "https://www.b.example"),
            links(text)
        )
    }

    @Test
    fun keepsBalancedBracketsAndDropsUnbalancedOnes() {
        val text = "(see https://en.wikipedia.org/wiki/Map_(disambiguation))"

        assertEquals(
            listOf("https://en.wikipedia.org/wiki/Map_(disambiguation)" to "https://en.wikipedia.org/wiki/Map_(disambiguation)"),
            links(text)
        )
    }

    @Test
    fun plainTextHasNoLinks() {
        assertEquals(emptyList(), links("No links here. Just example.com"))
    }

    @Test
    fun keepsExistingStyles() {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Bold") }
            append(" https://example.com")
        }

        val linked = text.withWebLinks()

        assertEquals(text.text, linked.text)
        assertTrue(linked.spanStyles.any { it.item.fontWeight == FontWeight.Bold && it.end == 4 })
        assertEquals(1, linked.getLinkAnnotations(0, linked.length).size)
    }

    private fun links(text: String): List<Pair<String, String>> {
        val linked = text.withWebLinks()
        return linked.getLinkAnnotations(0, linked.length).map {
            linked.text.substring(it.start, it.end) to (it.item as LinkAnnotation.Url).url
        }
    }
}
