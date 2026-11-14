package com.kylecorry.sol.math.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3

internal class GeometryTest {

    @Test
    fun containsCircle() {
        val circle1 = Circle(Vector2(0f, 0f), 1f)

        assertTrue(Geometry.contains(circle1, Vector2(0f, 0f)))
        assertTrue(Geometry.contains(circle1, Vector2(1f, 0f)))
        assertTrue(Geometry.contains(circle1, Vector2(0f, 1f)))
        assertTrue(Geometry.contains(circle1, Vector2(0.5f, 0.5f)))
    }

    @Test
    fun containsPolygonVector() {
        val pentagon = Polygon(
            listOf(
                Vector2(0f, 0f),
                Vector2(1f, 0f),
                Vector2(1.5f, 1f),
                Vector2(0.5f, 1.5f),
                Vector2(-0.5f, 1f)
            )
        )

        // Centered
        assertTrue(Geometry.contains(pentagon, Vector2(0.5f, 0.5f)))

        // Inside, towards top
        assertTrue(Geometry.contains(pentagon, Vector2(0.5f, 0.75f)))

        // On edge
        assertTrue(Geometry.contains(pentagon, Vector2(0.5f, 1f)))

        // On vertex
        assertTrue(Geometry.contains(pentagon, Vector2(0.5f, 1.5f)))

        // Outside
        assertFalse(Geometry.contains(pentagon, Vector2(0.5f, 1.6f)))
    }

    @Test
    fun getIntersectionLineCircle() {
        val line1 = Line(Vector2(0f, 0f), Vector2(3f, 0f))
        val line2 = Line(Vector2(-5f, -6f), Vector2(4f, 3f))
        val line3 = Line(Vector2(0.5f, 1f), Vector2(0.5f, -1f))
        val line4 = Line(Vector2(1f, 0f), Vector2(2f, 2f))
        val line5 = Line(Vector2(4f, 0f), Vector2(4f, 6f))

        // TODO: Add test where line is outside circle, but points to circle

        val circle1 = Circle(Vector2(0f, 0f), 1f)
        val circle2 = Circle(Vector2(4f, 3f), 2f)

        val intersection1 = Geometry.getIntersection(line1, circle1)
        val intersection2 = Geometry.getIntersection(line2, circle1)
        val intersection3 = Geometry.getIntersection(line2, circle2)
        val intersection4 = Geometry.getIntersection(line1, circle2)
        val intersection5 = Geometry.getIntersection(line3, circle1)
        val intersection6 = Geometry.getIntersection(line4, circle1)
        val intersection7 = Geometry.getIntersection(line5, circle2)

        assertEquals(Line(Vector2(0f, 0f), Vector2(1f, 0f)), intersection1!!, 0.00001f)
        assertEquals(Line(Vector2(0f, -1f), Vector2(1f, 0f)), intersection2!!, 0.00001f)
        assertEquals(Line(Vector2(2.58579f, 1.58579f), Vector2(4f, 3f)), intersection3!!, 0.00001f)
        assertEquals(
            Line(Vector2(0.5f, 0.866025f), Vector2(0.5f, -0.866025f)),
            intersection5!!,
            0.00001f
        )
        assertEquals(Line(Vector2(1f, 0f), Vector2(1f, 0f)), intersection6!!, 0.00001f)
        assertEquals(Line(Vector2(4f, 5f), Vector2(4f, 1f)), intersection7!!, 0.00001f)
        assertNull(intersection4)
    }

    @Test
    fun getConnectedLinesHandlesClosedLoops() {
        val segments = listOf(
            Vector2(0f, 0f) to Vector2(1f, 0f),
            Vector2(1f, 0f) to Vector2(1f, 1f),
            Vector2(1f, 1f) to Vector2(0f, 1f),
            Vector2(0f, 1f) to Vector2(0f, 0f)
        )

        val lines = Geometry.getConnectedLines(segments)

        assertEquals(1, lines.size)
        assertEquals(5, lines.first().size)
        assertEquals(lines.first().first(), lines.first().last())
    }

    @Test
    fun getIntersectionLineLine() {
        provideLineLineIntersections().forEach { args -> getIntersectionLineLine(args[0].arg<Line>(), args[1].arg<Line>(), args[2].arg<Vector2?>()) }
    }

    private fun getIntersectionLineLine(line1: Line, line2: Line, expected: Vector2?) {
        val actual = Geometry.getIntersection(line1, line2)

        if (expected == null) {
            assertNull(actual)
            assertFalse(Geometry.intersects(line1, line2))
        } else {
            assertEquals(expected, actual!!, 0.00001f)
            assertTrue(Geometry.intersects(line1, line2))
        }
    }

    @Test
    fun snapToLine2DTo2D() {
        snapToLine2DTo2D(0f, 0f, 0f, 0f)
        snapToLine2DTo2D(3f, 1f, 3f, 1f)
        // Before line
        snapToLine2DTo2D(-1f, -1f, 0f, 0f)
        // After line
        snapToLine2DTo2D(4f, 2f, 3f, 1f)
        // On line
        snapToLine2DTo2D(1f, 0.333333f, 1f, 0.333333f)
        // Above line
        snapToLine2DTo2D(1f, 1f, 1.2f, 0.4f)
        // Below line
        snapToLine2DTo2D(1f, -1f, 0.6f, 0.2f)
    }

    private fun snapToLine2DTo2D(x: Float, y: Float, expectedX: Float, expectedY: Float) {
        val x1 = 0f
        val y1 = 0f
        val x2 = 3f
        val y2 = 1f

        // Raw
        assertEquals(Vector2(expectedX, expectedY), Geometry.snapToLine(x, y, x1, y1, x2, y2), 0.00001f)

        // Objects
        assertEquals(
            Vector2(expectedX, expectedY),
            Geometry.snapToLine(Vector2(x, y), Line(Vector2(x1, y1), Vector2(x2, y2))),
            0.00001f
        )
    }

    @Test
    fun snapToLine2DTo2DZeroLengthLine() {
        assertEquals(Vector2(2f, 3f), Geometry.snapToLine(5f, -4f, 2f, 3f, 2f, 3f), 0.00001f)
    }

    @Test
    fun snapToLine2DTo3D() {
        snapToLine2DTo3D(0f, 0f, 0f, 0f, 0f)
        snapToLine2DTo3D(3f, 1f, 3f, 1f, 2f)
        // Before line
        snapToLine2DTo3D(-1f, -1f, 0f, 0f, 0f)
        // After line
        snapToLine2DTo3D(4f, 2f, 3f, 1f, 2f)
        // On line
        snapToLine2DTo3D(1f, 0.333333f, 1f, 0.333333f, 0.666667f)
        // Above line
        snapToLine2DTo3D(1f, 1f, 1.2f, 0.4f, 0.8f)
        // Below line
        snapToLine2DTo3D(1f, -1f, 0.6f, 0.2f, 0.4f)
    }

    private fun snapToLine2DTo3D(x: Float, y: Float, expectedX: Float, expectedY: Float, expectedZ: Float) {
        val x1 = 0f
        val y1 = 0f
        val z1 = 0f
        val x2 = 3f
        val y2 = 1f
        val z2 = 2f

        // Raw
        assertEquals(
            Vector3(expectedX, expectedY, expectedZ),
            Geometry.snapToLine(Vector2(x, y), x1, y1, z1, x2, y2, z2),
            0.00001f
        )

        // Objects
        assertEquals(
            Vector3(expectedX, expectedY, expectedZ),
            Geometry.snapToLine(Vector2(x, y), x1, y1, z1, x2, y2, z2),
            0.00001f
        )
    }

    @Test
    fun snapToLine3DTo3D() {
        snapToLine3DTo3D(0f, 0f, 0f, 0f, 0f, 0f)
        snapToLine3DTo3D(3f, 1f, 3f, 3f, 1f, 2f)
        // Before line
        snapToLine3DTo3D(-1f, -1f, -1f, 0f, 0f, 0f)
        // After line
        snapToLine3DTo3D(4f, 2f, 3f, 3f, 1f, 2f)
        // On line
        snapToLine3DTo3D(1f, 0.333333f, 0.666667f, 1f, 0.333333f, 0.666667f)
        // Above line
        snapToLine3DTo3D(1f, 1f, 1f, 1.2857f, 0.4286f, 0.8571f)
        // Below line
        snapToLine3DTo3D(1f, -1f, 0.8f, 0.7714f, 0.2571f, 0.5143f)
    }

    private fun snapToLine3DTo3D(x: Float, y: Float, z: Float, expectedX: Float, expectedY: Float, expectedZ: Float) {
        val x1 = 0f
        val y1 = 0f
        val z1 = 0f
        val x2 = 3f
        val y2 = 1f
        val z2 = 2f

        // Raw
        assertEquals(
            Vector3(expectedX, expectedY, expectedZ),
            Geometry.snapToLine(Vector3(x, y, z), x1, y1, z1, x2, y2, z2),
            0.0001f
        )

        // Objects
        assertEquals(
            Vector3(expectedX, expectedY, expectedZ),
            Geometry.snapToLine(Vector3(x, y, z), x1, y1, z1, x2, y2, z2),
            0.0001f
        )
    }

    @Test
    fun snapToLine2DTo3DZeroLengthLine() {
        assertEquals(
            Vector3(2f, 3f, 4f),
            Geometry.snapToLine(5f, -4f, 2f, 3f, 4f, 2f, 3f, 4f),
            0.00001f
        )
    }

    @Test
    fun snapToLine3DTo3DZeroLengthLine() {
        assertEquals(
            Vector3(2f, 3f, 4f),
            Geometry.snapToLine(Vector3(5f, -4f, 1f), 2f, 3f, 4f, 2f, 3f, 4f),
            0.00001f
        )
    }

    @Test
    fun getInteriorAngle() {
        getInteriorAngle(0f, 1f, 0f, 0f, 1f, 0f, 90f)
        getInteriorAngle(-1f, 0f, 0f, 0f, 1f, 0f, 180f)
        getInteriorAngle(1f, 0f, 0f, 0f, 0.5f, 0.8660254f, 60f)
    }

    private fun getInteriorAngle(
        previousX: Float,
        previousY: Float,
        currentX: Float,
        currentY: Float,
        nextX: Float,
        nextY: Float,
        expected: Float
    ) {
        assertEquals(
            expected,
            Geometry.getInteriorAngle(
                Vector2(previousX, previousY),
                Vector2(currentX, currentY),
                Vector2(nextX, nextY)
            ),
            0.0001f
        )
    }

    @Test
    fun getInteriorAngleReturnsZeroForDegenerateSegments() {
        assertEquals(
            0f,
            Geometry.getInteriorAngle(Vector2(0f, 0f), Vector2(0f, 0f), Vector2(1f, 0f)),
            0.0001f
        )
        assertEquals(
            0f,
            Geometry.getInteriorAngle(Vector2(1f, 0f), Vector2(0f, 0f), Vector2(0f, 0f)),
            0.0001f
        )
    }

    private fun assertEquals(expected: Line, actual: Line, delta: Float = 0f) {
        assertEquals(expected.start, actual.start, delta)
        assertEquals(expected.end, actual.end, delta)
    }

    private fun assertEquals(expected: Vector2, actual: Vector2, delta: Float = 0f) {
        assertEquals(expected.x, actual.x, delta)
        assertEquals(expected.y, actual.y, delta)
    }

    private fun assertEquals(expected: Vector3, actual: Vector3, delta: Float = 0f) {
        assertEquals(expected.x, actual.x, delta)
        assertEquals(expected.y, actual.y, delta)
        assertEquals(expected.z, actual.z, delta)
    }

    @Test
    fun scaleToFit() {
        provideScaleToFit().forEach { args -> scaleToFit(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Float>(), args[3].arg<Float>(), args[4].arg<Float>()) }
    }

    private fun scaleToFit(
        width: Float,
        height: Float,
        maxWidth: Float,
        maxHeight: Float,
        expected: Float
    ) {
        val actual = Geometry.scaleToFit(width, height, maxWidth, maxHeight)
        assertEquals(expected, actual, 0.00001f)
    }

    companion object {
        fun provideLineLineIntersections(): List<List<Any?>> {
            return listOf(
                // Diagonal segments crossing at midpoint
                listOf(
                    Line(Vector2(0f, 0f), Vector2(4f, 4f)),
                    Line(Vector2(0f, 4f), Vector2(4f, 0f)),
                    Vector2(2f, 2f)
                ),
                // Vertical segment crossing horizontal segment
                listOf(
                    Line(Vector2(0f, 0f), Vector2(4f, 0f)),
                    Line(Vector2(2f, -2f), Vector2(2f, 2f)),
                    Vector2(2f, 0f)
                ),
                // Segments touching at a shared endpoint
                listOf(
                    Line(Vector2(0f, 0f), Vector2(2f, 2f)),
                    Line(Vector2(2f, 2f), Vector2(4f, 0f)),
                    Vector2(2f, 2f)
                ),
                // Infinite lines cross, but the segments do not reach the crossing point
                listOf(
                    Line(Vector2(0f, 0f), Vector2(1f, 1f)),
                    Line(Vector2(2f, 0f), Vector2(2f, 3f)),
                    null
                ),
                // Parallel horizontal segments with vertical separation
                listOf(
                    Line(Vector2(0f, 0f), Vector2(4f, 0f)),
                    Line(Vector2(0f, 1f), Vector2(4f, 1f)),
                    null
                ),
                // Collinear horizontal segments with a gap between them
                listOf(
                    Line(Vector2(0f, 0f), Vector2(1f, 0f)),
                    Line(Vector2(2f, 0f), Vector2(3f, 0f)),
                    null
                )
            )
        }
        fun provideScaleToFit(): List<List<Any?>> {
            return listOf(
                // Square destination
                listOf(2.0f, 2.0f, 2.0f, 2.0f, 1.0f),
                listOf(1.0f, 1.0f, 2.0f, 2.0f, 2.0f),
                listOf(4.0f, 4.0f, 2.0f, 2.0f, 0.5f),
                listOf(4.0f, 2.0f, 2.0f, 2.0f, 0.5f),
                listOf(2.0f, 4.0f, 2.0f, 2.0f, 0.5f),
                listOf(1.0f, 4.0f, 2.0f, 2.0f, 0.5f),
                listOf(4.0f, 1.0f, 2.0f, 2.0f, 0.5f),
                listOf(2.0f, 1.0f, 2.0f, 2.0f, 1.0f),
                listOf(1.0f, 2.0f, 2.0f, 2.0f, 1.0f),
                listOf(1.0f, 0.5f, 2.0f, 2.0f, 2.0f),
                listOf(0.5f, 1.0f, 2.0f, 2.0f, 2.0f),

                // Long destination
                listOf(2.0f, 2.0f, 4.0f, 2.0f, 1.0f),
                listOf(1.0f, 1.0f, 4.0f, 2.0f, 2.0f),
                listOf(4.0f, 4.0f, 4.0f, 2.0f, 0.5f),
                listOf(4.0f, 2.0f, 4.0f, 2.0f, 1.0f),
                listOf(2.0f, 4.0f, 4.0f, 2.0f, 0.5f),
                listOf(1.0f, 4.0f, 4.0f, 2.0f, 0.5f),
                listOf(4.0f, 1.0f, 4.0f, 2.0f, 1.0f),
                listOf(2.0f, 1.0f, 4.0f, 2.0f, 2.0f),
                listOf(1.0f, 2.0f, 4.0f, 2.0f, 1.0f),
                listOf(1.0f, 0.5f, 4.0f, 2.0f, 4.0f),
                listOf(0.5f, 1.0f, 4.0f, 2.0f, 2.0f),

                // Tall destination
                listOf(2.0f, 2.0f, 2.0f, 4.0f, 1.0f),
                listOf(1.0f, 1.0f, 2.0f, 4.0f, 2.0f),
                listOf(4.0f, 4.0f, 2.0f, 4.0f, 0.5f),
                listOf(4.0f, 2.0f, 2.0f, 4.0f, 0.5f),
                listOf(2.0f, 4.0f, 2.0f, 4.0f, 1.0f),
                listOf(1.0f, 4.0f, 2.0f, 4.0f, 1.0f),
                listOf(4.0f, 1.0f, 2.0f, 4.0f, 0.5f),
                listOf(2.0f, 1.0f, 2.0f, 4.0f, 1.0f),
                listOf(1.0f, 2.0f, 2.0f, 4.0f, 2.0f),
                listOf(1.0f, 0.5f, 2.0f, 4.0f, 2.0f),
                listOf(0.5f, 1.0f, 2.0f, 4.0f, 4.0f),
            )
        }
    }

    @Test
    fun manhattanDistance() {
        assertEquals(7f, Geometry.manhattanDistance(listOf(1f, 2f), listOf(4f, -2f)), 0.0001f)
        assertEquals(0f, Geometry.manhattanDistance(emptyList(), emptyList()), 0.0001f)
    }

    @Test
    fun euclideanDistance() {
        assertEquals(5f, Geometry.euclideanDistance(listOf(1f, 2f), listOf(4f, -2f)), 0.0001f)
        assertEquals(0f, Geometry.euclideanDistance(emptyList(), emptyList()), 0.0001f)
    }

    @Test
    fun distanceIgnoresExtraDimensions() {
        assertEquals(3f, Geometry.manhattanDistance(listOf(1f, 5f), listOf(4f)), 0.0001f)
        assertEquals(3f, Geometry.euclideanDistance(listOf(4f), listOf(1f, 5f)), 0.0001f)
    }
}
