package com.kylecorry.sol.shared

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertContentEquals

import com.kylecorry.sol.shared.ArrayUtils.swap

class ArrayUtilsTest {

    @Test
    fun swap() {
        val arr = arrayOf(1, 2, 3)
        arr.swap(0, 2)
        assertContentEquals(arrayOf(3, 2, 1), arr)

        arr.swap(1, 1)
        assertContentEquals(arrayOf(3, 2, 1), arr)

        assertFailsWith<IndexOutOfBoundsException> {
            arr.swap(0, 3)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            arr.swap(3, 0)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            arr.swap(-1, 0)
        }
    }
}