package com.kylecorry.luna.text

import kotlin.test.Test
import kotlin.test.assertEquals

class SlugifyTest {

    @Test
    fun testSlugify() {
        val cases = listOf(
            "Hello" to "hello",
            "Hello World" to "hello-world",
            "Hello World!" to "hello-world",
            "test123" to "test123",
            "test 123" to "test-123",
            "test 123 test" to "test-123-test",
            "test'something" to "testsomething",
            "Hello World! é" to "hello-world-e",
            "he?llo world" to "hello-world",
            "HELLO WORLD" to "hello-world",
            "  hello    world  " to "hello-world",
            "?" to "",
            "" to "",
            "áéíóů" to "aeiou",
        )
        for ((input, expected) in cases) {
            assertEquals(expected, input.slugify(), "slugify(\"$input\")")
        }
    }

}
