package com.kylecorry.sol.math.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.math.Vector2

internal class IntersectionMathTest {

    @Test
    fun getIntersectionLineRect() {
        provideLineRectIntersection().forEach { args -> getIntersectionLineRect(args[0].arg<Line>(), args[1].arg<Rectangle>(), args[2].arg<List<Vector2>>()) }
    }

    private fun getIntersectionLineRect(line: Line, rect: Rectangle, expected: List<Vector2>) {
        val intersection = IntersectionMath.getIntersection(line, rect)
        assertEquals(expected.size, intersection.size)
        expected.forEach {
            assertTrue(intersection.contains(it))
        }
    }

    @Test
    fun getIntersectionPointsRect() {
        providePointsRectIntersection().forEach { args -> getIntersectionPointsRect(args[0].arg<Vector2>(), args[1].arg<Vector2>(), args[2].arg<Rectangle>(), args[3].arg<List<Vector2>>()) }
    }

    private fun getIntersectionPointsRect(
        a: Vector2,
        b: Vector2,
        rect: Rectangle,
        expected: List<Vector2>
    ) {
        val intersection = IntersectionMath.getIntersection(a, b, rect)
        assertEquals(expected.size, intersection.size)
        expected.forEach {
            assertTrue(intersection.contains(it))
        }
    }

    companion object {
        fun provideLineRectIntersection(): List<List<Any?>> {
            return listOf(
                listOf(
                    Line(Vector2(0f, 0f), Vector2(10f, 10f)),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(10f, 10f))
                ),
                listOf(
                    Line(Vector2(0f, 0f), Vector2(10f, 10f)),
                    Rectangle(0f, 5f, 5f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(5f, 5f))
                ),
                listOf(
                    Line(Vector2(0f, 0f), Vector2(10f, 10f)),
                    Rectangle(5f, 10f, 10f, 5f),
                    listOf(Vector2(5f, 5f), Vector2(10f, 10f))
                ),
                // Horizontal line
                listOf(
                    Line(Vector2(0f, 0f), Vector2(10f, 0f)),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(10f, 0f))
                ),
                // Vertical line
                listOf(
                    Line(Vector2(0f, 0f), Vector2(0f, 10f)),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(0f, 10f))
                ),
                // Diagonal, no intersection
                listOf(
                    Line(Vector2(-10f, 0f), Vector2(-1f, 10f)),
                    Rectangle(0f, 5f, 5f, 0f),
                    emptyList<Vector2>()
                ),
                // Horizontal, no intersection
                listOf(
                    Line(Vector2(0f, -1f), Vector2(10f, -1f)),
                    Rectangle(0f, 5f, 5f, 0f),
                    emptyList<Vector2>()
                ),
                // Vertical, no intersection
                listOf(
                    Line(Vector2(-1f, 0f), Vector2(-1f, 10f)),
                    Rectangle(0f, 5f, 5f, 0f),
                    emptyList<Vector2>()
                ),
                // Horizontal, same as top
                listOf(
                    Line(Vector2(0f, 10f), Vector2(10f, 10f)),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 10f), Vector2(10f, 10f))
                ),
                // Horizontal, same as bottom
                listOf(
                    Line(Vector2(0f, 0f), Vector2(10f, 0f)),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(10f, 0f))
                ),
                // Vertical, same as left
                listOf(
                    Line(Vector2(0f, 0f), Vector2(0f, 10f)),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(0f, 10f))
                ),
                // Vertical, same as right
                listOf(
                    Line(Vector2(10f, 0f), Vector2(10f, 10f)),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(10f, 0f), Vector2(10f, 10f))
                ),
            )
        }
        fun providePointsRectIntersection(): List<List<Any?>> {
            return listOf(
                listOf(
                    Vector2(0f, 0f),
                    Vector2(10f, 10f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(10f, 10f))
                ),
                listOf(
                    Vector2(0f, 0f),
                    Vector2(10f, 10f),
                    Rectangle(0f, 5f, 5f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(5f, 5f))
                ),
                listOf(
                    Vector2(0f, 0f),
                    Vector2(10f, 10f),
                    Rectangle(5f, 10f, 10f, 5f),
                    listOf(Vector2(5f, 5f), Vector2(10f, 10f))
                ),
                // Horizontal line
                listOf(
                    Vector2(0f, 0f),
                    Vector2(10f, 0f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(10f, 0f))
                ),
                // Vertical line
                listOf(
                    Vector2(0f, 0f),
                    Vector2(0f, 10f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(0f, 10f))
                ),
                // Diagonal, no intersection
                listOf(
                    Vector2(-10f, 0f),
                    Vector2(-1f, 10f),
                    Rectangle(0f, 5f, 5f, 0f),
                    emptyList<Vector2>()
                ),
                // Horizontal, no intersection
                listOf(
                    Vector2(0f, -1f),
                    Vector2(10f, -1f),
                    Rectangle(0f, 5f, 5f, 0f),
                    emptyList<Vector2>()
                ),
                // Vertical, no intersection
                listOf(
                    Vector2(-1f, 0f),
                    Vector2(-1f, 10f),
                    Rectangle(0f, 5f, 5f, 0f),
                    emptyList<Vector2>()
                ),
                // Horizontal, same as top
                listOf(
                    Vector2(0f, 10f),
                    Vector2(10f, 10f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 10f), Vector2(10f, 10f))
                ),
                // Horizontal, same as bottom
                listOf(
                    Vector2(0f, 0f),
                    Vector2(10f, 0f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(10f, 0f))
                ),
                // Vertical, same as left
                listOf(
                    Vector2(0f, 0f),
                    Vector2(0f, 10f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f), Vector2(0f, 10f))
                ),
                // Vertical, same as right
                listOf(
                    Vector2(10f, 0f),
                    Vector2(10f, 10f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(10f, 0f), Vector2(10f, 10f))
                ),
                // Ends before left of rectangle
                listOf(
                    Vector2(-10f, 0f),
                    Vector2(-5f, 0f),
                    Rectangle(0f, 10f, 10f, 0f),
                    emptyList<Vector2>()
                ),
                // Starts after right of rectangle
                listOf(
                    Vector2(15f, 0f),
                    Vector2(20f, 0f),
                    Rectangle(0f, 10f, 10f, 0f),
                    emptyList<Vector2>()
                ),
                // Starts above top of rectangle
                listOf(
                    Vector2(0f, 15f),
                    Vector2(0f, 20f),
                    Rectangle(0f, 10f, 10f, 0f),
                    emptyList<Vector2>()
                ),
                // Starts below bottom of rectangle
                listOf(
                    Vector2(0f, -10f),
                    Vector2(0f, -5f),
                    Rectangle(0f, 10f, 10f, 0f),
                    emptyList<Vector2>()
                ),
                // Starts before left of rectangle, ends inside rectangle
                listOf(
                    Vector2(-10f, 0f),
                    Vector2(5f, 0f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f))
                ),
                // Starts in rectangle, ends after right of rectangle
                listOf(
                    Vector2(5f, 0f),
                    Vector2(15f, 0f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(10f, 0f))
                ),
                // Starts above top of rectangle, ends inside rectangle
                listOf(
                    Vector2(0f, 15f),
                    Vector2(0f, 5f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 10f))
                ),
                // Starts in rectangle, ends below bottom of rectangle
                listOf(
                    Vector2(0f, 5f),
                    Vector2(0f, -5f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f))
                ),
                // Starts in rectangle, ends above top of rectangle
                listOf(
                    Vector2(0f, 5f),
                    Vector2(0f, 15f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 10f))
                ),
                // Starts in rectangle, ends inside rectangle (point)
                listOf(
                    Vector2(5f, 5f),
                    Vector2(5f, 5f),
                    Rectangle(0f, 10f, 10f, 0f),
                    emptyList<Vector2>()
                ),
                // Starts in rectangle, ends inside rectangle (line has distance)
                listOf(
                    Vector2(5f, 5f),
                    Vector2(5f, 6f),
                    Rectangle(0f, 10f, 10f, 0f),
                    emptyList<Vector2>()
                ),
                // Single point on border
                listOf(
                    Vector2(0f, 0f),
                    Vector2(0f, 0f),
                    Rectangle(0f, 10f, 10f, 0f),
                    listOf(Vector2(0f, 0f))
                ),
            )
        }
    }

}