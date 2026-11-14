package com.kylecorry.sol.math.lists

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

internal class ListsTest {

    @Test
    fun argmax() {
        provideArgmax().forEach { args -> argmax(args[0].arg<List<Float>>(), args[1].arg<Int>()) }
    }

    private fun <T : Comparable<T>> argmax(value: List<T>, expected: Int) {
        val actual = Lists.argmax(value)
        assertEquals(expected, actual)
    }

    companion object {
        fun provideArgmax(): List<List<Any?>> {
            return listOf(
                listOf(listOf(1f, 2f, 3f), 2),
                listOf(listOf(1f, 0.5f, 0.75f), 0),
                listOf(listOf(1f, 1f, 1f), 0),
                listOf(listOf<Float>(), -1),
            )
        }
    }
}