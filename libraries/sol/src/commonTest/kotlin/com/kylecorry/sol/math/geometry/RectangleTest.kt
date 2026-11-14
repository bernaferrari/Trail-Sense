package com.kylecorry.sol.math.geometry

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import com.kylecorry.sol.tests.arg

internal class RectangleTest {

    @Test
    fun intersects() {
        provideIntersectingRectangles().forEach { args -> intersects(args[0].arg<Rectangle>(), args[1].arg<Rectangle>()) }
    }

    private fun intersects(rect1: Rectangle, rect2: Rectangle) {
        assertTrue(rect1.intersects(rect2))
        assertTrue(rect2.intersects(rect1))
    }

    @Test
    fun doesNotIntersect() {
        provideNonIntersectingRectangles().forEach { args -> doesNotIntersect(args[0].arg<Rectangle>(), args[1].arg<Rectangle>()) }
    }

    private fun doesNotIntersect(rect1: Rectangle, rect2: Rectangle) {
        assertFalse(rect1.intersects(rect2))
        assertFalse(rect2.intersects(rect1))
    }

    companion object {
        fun provideIntersectingRectangles(): List<List<Any?>> {
            return listOf(
                listOf(
                    Rectangle(0f, 10f, 10f, 0f),
                    Rectangle(0f, 10f, 10f, 0f)
                ),
                listOf(
                    Rectangle(0f, 10f, 10f, 0f),
                    Rectangle(2f, 8f, 8f, 2f)
                ),
                listOf(
                    Rectangle(2f, 8f, 8f, 2f),
                    Rectangle(0f, 10f, 10f, 0f)
                ),
                listOf(
                    Rectangle(0f, 6f, 10f, 4f),
                    Rectangle(4f, 10f, 6f, 0f)
                ),
                listOf(
                    Rectangle(0f, 10f, 6f, 4f),
                    Rectangle(4f, 8f, 10f, 0f)
                ),
                listOf(
                    Rectangle(0f, 10f, 5f, 5f),
                    Rectangle(5f, 10f, 10f, 5f)
                ),
                listOf(
                    Rectangle(0f, 10f, 5f, 5f),
                    Rectangle(5f, 5f, 10f, 0f)
                ),
                listOf(
                    Rectangle(0f, 10f, 10f, 5f),
                    Rectangle(2f, 5f, 8f, 0f)
                )
            )
        }
        fun provideNonIntersectingRectangles(): List<List<Any?>> {
            return listOf(
                listOf(
                    Rectangle(0f, 10f, 4f, 6f),
                    Rectangle(5f, 10f, 9f, 6f)
                ),
                listOf(
                    Rectangle(0f, 10f, 4f, 6f),
                    Rectangle(0f, 5f, 4f, 1f)
                ),
                listOf(
                    Rectangle(0f, 10f, 4f, 6f),
                    Rectangle(5f, 5f, 9f, 1f)
                )
            )
        }
    }
}
