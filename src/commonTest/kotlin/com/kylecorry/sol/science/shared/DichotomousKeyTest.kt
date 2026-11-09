package com.kylecorry.sol.science.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DichotomousKeyTest {

    @Test
    fun canGenerateQuestion(){
        val key = DichotomousKey.question("Q", DichotomousKey.answer("A"), DichotomousKey.answer("B"))

        assertEquals("Q", key.label)
        assertNull(key.value)
        assertEquals("A", key.yes?.label)
        assertEquals("B", key.no?.label)
    }

    @Test
    fun canGenerateAnswer(){
        val answer = DichotomousKey.answer("A")
        assertEquals("A", answer.label)
        assertEquals("A", answer.value)

        val answer2 = DichotomousKey.answer("B", 123)
        assertEquals("B", answer2.label)
        assertEquals(123, answer2.value)
    }

}