package com.kylecorry.sol.math.algebra

import kotlin.test.Test
import kotlin.test.assertEquals

import assertk.assertFailure
import com.kylecorry.sol.math.Vector

internal class MatrixTest {

    @Test
    fun size() {
        size(2, 3, 6)
        size(0, 3, 0)
        size(3, 0, 0)
        size(0, 0, 0)
        size(1, 3, 3)
        size(3, 1, 3)
        size(3, 3, 9)
    }

    private fun size(rows: Int, columns: Int, expected: Int) {
        val matrix = Matrix.create(rows, columns, 0f)
        assertEquals(expected, matrix.size())
    }

    @Test
    fun toVectorFromRowMatrix() {
        val matrix = Matrix.row(1f, 2f, 3f)

        val actual = matrix.toVector()

        assertVectorEquals(Vector.from(1f, 2f, 3f), actual)
    }

    @Test
    fun toVectorFromColumnMatrix() {
        val matrix = Matrix.column(1f, 2f, 3f)

        val actual = matrix.toVector()

        assertVectorEquals(Vector.from(1f, 2f, 3f), actual)
    }

    @Test
    fun toVectorRequiresRowOrColumnMatrix() {
        val matrix = Matrix.create(
            arrayOf(
                floatArrayOf(1f, 2f),
                floatArrayOf(3f, 4f)
            )
        )

        assertFailure {
            matrix.toVector()
        }
    }

    private fun assertVectorEquals(expected: Vector, actual: Vector) {
        assertEquals(expected.size, actual.size)
        for (i in 0..<expected.size) {
            assertEquals(expected[i], actual[i])
        }
    }
}
