package com.kylecorry.andromeda.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class WebDomainLinkifierTest {

    @Test
    fun findsBareDomains() {
        assertEquals(listOf("http://example.com"), urls("Visit example.com today"))
        assertEquals(listOf("http://sub.example.org/path"), urls("See sub.example.org/path."))
        assertEquals(listOf("http://example.co.uk:8080/a?b=1#c"), urls("Open example.co.uk:8080/a?b=1#c, then"))
        assertEquals(listOf("http://Example.com/x_(y)"), urls("(Example.com/x_(y))"))
        assertEquals(listOf("http://trailsense.app", "http://kylecorry.io"), urls("trailsense.app and kylecorry.io"))
    }

    @Test
    fun findsTheAddressRange() {
        val text = "Docs at example.com/docs."
        assertEquals(listOf("example.com/docs"), WebDomainLinkifier.find(text).map { text.substring(it.range) })
    }

    @Test
    fun ignoresTextThatIsNotAnAddress() {
        listOf(
            "A photo.jpg, notes.md, data.csv, index.html or archive.zip",
            "Pi is 3.14 and 1.5em wide",
            "Abbreviations e.g. and i.e. and U.S.A.",
            "The end.It starts",
            "Mail a@example.com",
            "Already https://example.com or /files/example.com",
            "example.community"
        ).forEach { assertEquals(emptyList(), urls(it), it) }
    }

    private fun urls(text: String) = WebDomainLinkifier.find(text).map { it.url }
}
