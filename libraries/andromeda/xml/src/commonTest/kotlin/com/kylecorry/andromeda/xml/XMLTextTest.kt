package com.kylecorry.andromeda.xml

import kotlin.test.Test
import kotlin.test.assertEquals

class XMLTextTest {
    @Test fun combinesEntityAndCdataText() {
        val parsed = XMLConvert.parse("<value>a&amp;b<![CDATA[c]]> d</value>")
        assertEquals("a&bc d", parsed.text)
        assertEquals(parsed, XmlSerializer().deserialize(XmlSerializer().serialize(parsed)))
    }
}
