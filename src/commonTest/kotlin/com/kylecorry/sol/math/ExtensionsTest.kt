package com.kylecorry.sol.math

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

class ExtensionsTest {

    @Test
    fun sumOfFloat() {
        provideFloatSums().forEach { args -> sumOfFloat(args[0].arg<List<Float>>(), args[1].arg<Float>()) }
    }

    private fun sumOfFloat(values: List<Float>, sum: Float) {
        assertEquals(sum, values.sumOfFloat { it })
    }

    @Test
    fun batch() {
        provideBatch().forEach { args -> batch(args[0].arg<List<Int>>(), args[1].arg<Int>(), args[2].arg<List<List<Int>>>()) }
    }

    private fun batch(values: List<Int>, n: Int, expected: List<List<Int>>) {
        val batched = values.batch(n)
        assertEquals(expected, batched)
    }

    @Test
    fun split() {
        provideSplit().forEach { args -> split(args[0].arg<List<Int>>(), args[1].arg<Float>(), args[2].arg<Pair<List<Int>, List<Int>>>()) }
    }

    private fun split(values: List<Int>, percent: Float, expected: Pair<List<Int>, List<Int>>) {
        val split = values.split(percent)
        assertEquals(expected, split)
    }

    @Test
    fun floatFloorToInt() {
        floatFloorToInt(0.0f, 0)
        floatFloorToInt(1.0f, 1)
        floatFloorToInt(1.2f, 1)
        floatFloorToInt(1.999f, 1)
        floatFloorToInt(-0.0f, 0)
        floatFloorToInt(-1.0f, -1)
        floatFloorToInt(-1.2f, -2)
        floatFloorToInt(-1.001f, -2)
    }

    private fun floatFloorToInt(value: Float, expected: Int) {
        val actual = value.floorToInt()
        assertEquals(expected, actual)
    }

    @Test
    fun floatCeilToInt() {
        floatCeilToInt(0.0f, 0)
        floatCeilToInt(1.0f, 1)
        floatCeilToInt(1.2f, 2)
        floatCeilToInt(1.001f, 2)
        floatCeilToInt(-0.0f, 0)
        floatCeilToInt(-1.0f, -1)
        floatCeilToInt(-1.2f, -1)
        floatCeilToInt(-1.999f, -1)
    }

    private fun floatCeilToInt(value: Float, expected: Int) {
        val actual = value.ceilToInt()
        assertEquals(expected, actual)
    }

    @Test
    fun doubleFloorToInt() {
        doubleFloorToInt(0.0, 0)
        doubleFloorToInt(1.0, 1)
        doubleFloorToInt(1.2, 1)
        doubleFloorToInt(1.999, 1)
        doubleFloorToInt(-0.0, 0)
        doubleFloorToInt(-1.0, -1)
        doubleFloorToInt(-1.2, -2)
        doubleFloorToInt(-1.001, -2)
    }

    private fun doubleFloorToInt(value: Double, expected: Int) {
        val actual = value.floorToInt()
        assertEquals(expected, actual)
    }

    @Test
    fun doubleCeilToInt() {
        doubleCeilToInt(0.0, 0)
        doubleCeilToInt(1.0, 1)
        doubleCeilToInt(1.2, 2)
        doubleCeilToInt(1.001, 2)
        doubleCeilToInt(-0.0, 0)
        doubleCeilToInt(-1.0, -1)
        doubleCeilToInt(-1.2, -1)
        doubleCeilToInt(-1.999, -1)
    }

    private fun doubleCeilToInt(value: Double, expected: Int) {
        val actual = value.ceilToInt()
        assertEquals(expected, actual)
    }

    companion object {
        fun provideFloatSums(): List<List<Any?>> {
            return listOf(
                listOf(listOf(1f, 2f, 3f), 6f),
                listOf(listOf(1f, 2f, 3f, 4f), 10f),
                listOf(listOf(1f, 2f, 3f, 4f, 5f), 15f),
                listOf(emptyList<Float>(), 0f),
                listOf(listOf(1f), 1f),
            )
        }
        fun provideBatch(): List<List<Any?>> {
            return listOf(
                listOf(listOf<Int>(), 2, listOf<List<Int>>()),
                listOf(listOf(1), 2, listOf(listOf(1))),
                listOf(listOf(1, 2), 2, listOf(listOf(1, 2))),
                listOf(listOf(1, 2, 3), 2, listOf(listOf(1, 2), listOf(3))),
                listOf(listOf(1, 2, 3, 4), 2, listOf(listOf(1, 2), listOf(3, 4))),
                listOf(listOf(1, 2, 3, 4), 3, listOf(listOf(1, 2, 3), listOf(4))),
            )
        }
        fun provideSplit(): List<List<Any?>> {
            return listOf(
                listOf(listOf<Int>(), 0.5f, listOf<Int>() to listOf<Int>()),
                listOf(listOf(1), 0.5f, listOf(1) to listOf<Int>()),
                listOf(listOf(1, 2), 0.5f, listOf(1) to listOf(2)),
                listOf(listOf(1, 2, 3), 0.5f, listOf(1, 2) to listOf(3)),
                listOf(listOf(1, 2, 3), 0.333f, listOf(1) to listOf(2, 3)),
                listOf(listOf(1, 2, 3), 0.666f, listOf(1, 2) to listOf(3)),
                listOf(listOf(1, 2, 3), 0.75f, listOf(1, 2, 3) to listOf<Int>()),
                listOf(listOf(1, 2, 3), 1f, listOf(1, 2, 3) to listOf<Int>()),
            )
        }

    }

}