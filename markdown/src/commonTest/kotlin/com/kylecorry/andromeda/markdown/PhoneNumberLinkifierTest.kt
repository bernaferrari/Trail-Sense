package com.kylecorry.andromeda.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneNumberLinkifierTest {

    @Test
    fun findsCommonFormats() {
        assertEquals(listOf("tel:5551234567"), urls("Call 555-123-4567 now"))
        assertEquals(listOf("tel:5551234567"), urls("Call (555) 123-4567."))
        assertEquals(listOf("tel:5551234567"), urls("Call 555.123.4567"))
        assertEquals(listOf("tel:5551234567"), urls("Call 5551234567"))
        assertEquals(listOf("tel:18005551234"), urls("Call 1-800-555-1234"))
        assertEquals(listOf("tel:5551234"), urls("Call 555-1234"))
        assertEquals(listOf("tel:+442079460018"), urls("Call +44 20 7946 0018"))
        assertEquals(listOf("tel:+15551234567"), urls("Call +1 (555) 123-4567"))
        assertEquals(listOf("tel:0123456789"), urls("Appelez le 01 23 45 67 89"))
    }

    @Test
    fun findsTheNumbersRange() {
        val text = "Office: 555-123-4567, home: (555) 765-4321"
        val matches = PhoneNumberLinkifier.find(text)
        assertEquals(listOf("555-123-4567", "(555) 765-4321"), matches.map { text.substring(it.range) })
    }

    @Test
    fun ignoresNumbersThatAreNotPhoneNumbers() {
        listOf(
            "Dial 911",
            "It is 8848 m high",
            "On 2024-05-06 and 05/06/2024",
            "At 45.123456, -122.123456",
            "Pi is 3.14159265",
            "Server 192.168.0.1",
            "Steps 1 2 3 4 5 6 7",
            "Order AB5551234567",
            "Version 1234567-beta",
            "user5551234567@example.com",
            "Too long 1234567890123456789"
        ).forEach { assertEquals(emptyList(), urls(it), it) }
    }

    private fun urls(text: String) = PhoneNumberLinkifier.find(text).map { it.url }
}
