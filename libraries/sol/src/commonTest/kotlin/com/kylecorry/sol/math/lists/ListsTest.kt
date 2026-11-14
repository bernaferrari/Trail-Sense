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

    @Test
    fun binarySearchFindsFirstMatchingValue() {
        val sorted = listOf(1.0, 2.0, 2.0, 2.0, 5.0)
        listOf(0.0 to 0, 1.0 to 0, 1.5 to 1, 2.0 to 1, 3.0 to 4, 5.0 to 4, 6.0 to 5).forEach { (value, expected) ->
            assertEquals(expected, Lists.binarySearch(sorted) { it >= value })
        }
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