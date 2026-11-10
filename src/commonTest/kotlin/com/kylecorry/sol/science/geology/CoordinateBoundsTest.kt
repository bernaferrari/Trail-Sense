package com.kylecorry.sol.science.geology

import kotlin.test.Test
import com.kylecorry.sol.tests.arg

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import com.kylecorry.sol.tests.isCloseTo
import com.kylecorry.sol.units.Coordinate

internal class CoordinateBoundsTest {

    @Test
    fun contains() {
        provideContains().forEach { args -> contains(args[0].arg<CoordinateBounds>(), args[1].arg<Coordinate>(), args[2].arg<Boolean>()) }
    }

    private fun contains(
        bounds: CoordinateBounds,
        coordinate: Coordinate,
        expected: Boolean
    ) {
        assertThat(bounds.contains(coordinate)).isEqualTo(expected)
    }

    @Test
    fun center() {
        provideCenter().forEach { args -> center(args[0].arg<CoordinateBounds>(), args[1].arg<Coordinate>()) }
    }

    private fun center(
        bounds: CoordinateBounds,
        expected: Coordinate
    ) {
        assertThat(bounds.center).isCloseTo(expected, 1f)
    }

    @Test
    fun intersects() {
        provideIntersects().forEach { args -> intersects(args[0].arg<CoordinateBounds>(), args[1].arg<CoordinateBounds>(), args[2].arg<Boolean>()) }
    }

    private fun intersects(
        bounds1: CoordinateBounds,
        bounds2: CoordinateBounds,
        expected: Boolean
    ) {
        assertThat(bounds1.intersects(bounds2), name = "intersects(b1=$bounds1, b2=$bounds2)").isEqualTo(expected)
        assertThat(bounds2.intersects(bounds1), name = "intersects(b1=$bounds2, b2=$bounds1)").isEqualTo(expected)
    }

    @Test
    fun from() {
        provideFrom().forEach { args -> from(args[0].arg<List<Coordinate>>(), args[1].arg<Boolean>(), args[2].arg<CoordinateBounds>()) }
    }

    private fun from(
        points: List<Coordinate>,
        checkForFullWorld: Boolean,
        expected: CoordinateBounds
    ) {
        val bounds = CoordinateBounds.from(points, checkForFullWorld)
        assertThat(bounds.north, name = "north").isCloseTo(expected.north, 0.00001)
        assertThat(bounds.south, name = "south").isCloseTo(expected.south, 0.00001)
        assertThat(bounds.east, name = "east").isCloseTo(expected.east, 0.00001)
        assertThat(bounds.west, name = "west").isCloseTo(expected.west, 0.00001)
    }

    @Test
    fun grow() {
        provideGrow().forEach { args -> grow(args[0].arg<CoordinateBounds>(), args[1].arg<Float>(), args[2].arg<CoordinateBounds>()) }
    }

    private fun grow(bounds: CoordinateBounds, percent: Float, expected: CoordinateBounds) {
        val grown = bounds.grow(percent)
        assertThat(grown.north, name = "north").isCloseTo(expected.north, 0.00001)
        assertThat(grown.south, name = "south").isCloseTo(expected.south, 0.00001)
        // Longitudes may wrap; compare width and center longitude for robustness
        assertThat(grown.widthDegrees(), name = "widthDegrees").isCloseTo(expected.widthDegrees(), 0.00001)
        assertThat(grown.center.longitude, name = "centerLongitude").isCloseTo(expected.center.longitude, 0.00001)
    }

    companion object {
        fun provideCenter(): List<List<Any?>> {
            return listOf(
                listOf(CoordinateBounds.empty, Coordinate.zero),
                listOf(CoordinateBounds(1.0, 10.0, -1.0, -10.0), Coordinate.zero),
                listOf(CoordinateBounds(1.0, 8.0, 0.0, -10.0), Coordinate(0.5, -1.0)),
                listOf(CoordinateBounds(1.0, -10.0, 0.0, 10.0), Coordinate(0.5, 180.0)),
                listOf(CoordinateBounds(80.0, 170.0, -20.0, -160.0), Coordinate(30.0, 5.0)),
                listOf(CoordinateBounds(80.0, -170.0, -20.0, 160.0), Coordinate(30.0, 175.0)),
            )
        }
        fun provideContains(): List<List<Any?>> {
            return listOf(
                listOf(
                    CoordinateBounds.empty, Coordinate(1.0, 2.0), false
                ),
                listOf(
                    CoordinateBounds(1.0, 10.0, -1.0, -10.0), Coordinate(1.0, 2.0), true
                ),
                listOf(
                    CoordinateBounds(1.0, 10.0, -1.0, -10.0), Coordinate(2.0, 2.0), false
                ),
                listOf(
                    CoordinateBounds(1.0, 10.0, -1.0, -10.0), Coordinate(-2.0, 2.0), false
                ),
                listOf(
                    CoordinateBounds(1.0, 10.0, -1.0, -10.0), Coordinate(1.0, 11.0), false
                ),
                listOf(
                    CoordinateBounds(1.0, 10.0, -1.0, -10.0), Coordinate(1.0, -11.0), false
                ),
                listOf(
                    CoordinateBounds(1.0, -10.0, -1.0, 10.0), Coordinate(1.0, 2.0), false
                ),
                listOf(
                    CoordinateBounds(1.0, -170.0, -1.0, 170.0), Coordinate(1.0, 180.0), true
                ),
                listOf(
                    CoordinateBounds(1.0, -170.0, -1.0, 170.0), Coordinate(1.0, -180.0), true
                )
            )
        }
        fun provideIntersects(): List<List<Any?>> {
            return listOf(
                // Basic non-wrap overlap
                listOf(
                    CoordinateBounds(10.0, 10.0, 0.0, 0.0),
                    CoordinateBounds(5.0, 5.0, -5.0, -5.0),
                    true
                ),

                // Disjoint vertically (no overlap in latitude)
                listOf(
                    CoordinateBounds(10.0, 10.0, 5.0, 0.0),
                    CoordinateBounds(4.0, 10.0, 0.0, 0.0),
                    false
                ),

                // Touching at latitude edge (should intersect)
                listOf(
                    CoordinateBounds(10.0, 10.0, 0.0, 0.0),
                    CoordinateBounds(0.0, 10.0, -5.0, 0.0),
                    true
                ),

                // Disjoint horizontally (non-wrap)
                listOf(
                    CoordinateBounds(10.0, 10.0, 0.0, 0.0),
                    CoordinateBounds(10.0, 30.0, 0.0, 20.0),
                    false
                ),

                // Touching at longitude edge (non-wrap, should intersect)
                listOf(
                    CoordinateBounds(10.0, 10.0, 0.0, 0.0),
                    CoordinateBounds(10.0, 20.0, 0.0, 10.0),
                    true
                ),

                // Self wraps (antimeridian) intersects on left segment [-180, east]
                listOf(
                    CoordinateBounds(10.0, -170.0, 0.0, 170.0),
                    CoordinateBounds(10.0, -172.0, 0.0, -178.0),
                    true
                ),

                // Self wraps intersects on right segment [west, 180]
                listOf(
                    CoordinateBounds(10.0, -170.0, 0.0, 170.0),
                    CoordinateBounds(10.0, 175.0, 0.0, 172.0),
                    true
                ),

                // Self wraps but disjoint horizontally
                listOf(
                    CoordinateBounds(10.0, -170.0, 0.0, 170.0),
                    CoordinateBounds(10.0, -150.0, 0.0, -160.0),
                    false
                ),

                // Other wraps vs non-wrap intersect (overlap on left segment)
                listOf(
                    CoordinateBounds(10.0, -165.0, 0.0, -175.0),
                    CoordinateBounds(10.0, -170.0, 0.0, 170.0),
                    true
                ),

                // Other wraps vs non-wrap disjoint
                listOf(
                    CoordinateBounds(10.0, -150.0, 0.0, -160.0),
                    CoordinateBounds(10.0, -170.0, 0.0, 170.0),
                    false
                ),

                // Both wrap with vertical overlap -> should intersect
                listOf(
                    CoordinateBounds(10.0, -170.0, 0.0, 170.0),
                    CoordinateBounds(10.0, -160.0, 0.0, 160.0),
                    true
                ),

                // Both wrap but vertically disjoint -> should not intersect
                listOf(
                    CoordinateBounds(10.0, -170.0, 5.0, 170.0),
                    CoordinateBounds(4.0, -160.0, 0.0, 160.0),
                    false
                ),

                // Intersection with world bounds (should intersect whenever lat overlaps)
                listOf(
                    CoordinateBounds.world,
                    CoordinateBounds(10.0, 10.0, 0.0, 0.0),
                    true
                ),

                // Zero-width rectangle intersecting a normal rectangle (touching by longitude)
                listOf(
                    CoordinateBounds(1.0, 0.0, 0.0, 0.0),
                    CoordinateBounds(1.0, 1.0, -1.0, -1.0),
                    true
                ),

                // Zero-height rectangle intersecting a normal rectangle (touching by latitude)
                listOf(
                    CoordinateBounds(0.0, 10.0, 0.0, -10.0),
                    CoordinateBounds(1.0, 1.0, -1.0, -1.0),
                    true
                ),

                // Zero-width vs zero-width at same longitude -> intersect
                listOf(
                    CoordinateBounds(1.0, 10.0, -1.0, 10.0),
                    CoordinateBounds(1.0, 10.0, -1.0, 10.0),
                    true
                ),

                // Zero-width vs zero-width at different longitudes -> no intersect
                listOf(
                    CoordinateBounds(1.0, 10.0, -1.0, 10.0),
                    CoordinateBounds(1.0, 20.0, -1.0, 20.0),
                    false
                ),

                // Touching exactly at antimeridian with non-wrap other reaching 180
                listOf(
                    CoordinateBounds(10.0, -170.0, 0.0, 170.0),
                    CoordinateBounds(10.0, 180.0, 0.0, 170.0),
                    true
                ),

                // Non-wrap near -180 touching wrapped on left segment
                listOf(
                    CoordinateBounds(10.0, -180.0, 0.0, -180.0),
                    CoordinateBounds(10.0, -179.0, 0.0, 179.0),
                    true
                ),

                // Basically full world
                listOf(
                    CoordinateBounds(10.0, 179.99995, 0.0, -180.0),
                    CoordinateBounds(5.0, 180.0, -5.0, 179.99997),
                    true
                )
            )
        }
        fun provideGrow(): List<List<Any?>> {
            return listOf(
                // Non-wrap rectangle, 10x10 -> grow 10% => lat +/-1, lon +/-1
                listOf(
                    CoordinateBounds(10.0, 10.0, 0.0, 0.0),
                    0.1f,
                    CoordinateBounds(11.0, 11.0, -1.0, -1.0)
                ),

                // Zero width rectangle, widthDegrees = 0 -> no horizontal change
                listOf(
                    CoordinateBounds(10.0, 0.0, 0.0, 0.0),
                    0.5f,
                    CoordinateBounds(15.0, 0.0, -5.0, 0.0)
                ),

                // Zero height rectangle, heightDegrees = 0 -> no vertical change
                listOf(
                    CoordinateBounds(0.0, 10.0, 0.0, 0.0),
                    0.5f,
                    CoordinateBounds(0.0, 15.0, 0.0, -5.0)
                ),

                // Wrap across antimeridian: west=170, east=-170, width=20 -> 50% grow => lon +/-10
                listOf(
                    CoordinateBounds(10.0, -170.0, 0.0, 170.0),
                    0.5f,
                    CoordinateBounds(15.0, -160.0, -5.0, 160.0)
                ),

                // Near poles: clamp lat to +/-90
                listOf(
                    CoordinateBounds(89.0, 10.0, 80.0, 0.0),
                    0.5f, // height=9 -> latDelta=4.5 => north would be 93.5 -> clamp to 90
                    CoordinateBounds(90.0, 15.0, 75.5, -5.0)
                ),

                // World bounds remain world after grow
                listOf(
                    CoordinateBounds.world,
                    0.25f,
                    CoordinateBounds.world
                ),

                // Percent = 0 -> no change
                listOf(
                    CoordinateBounds(10.0, 10.0, 0.0, 0.0),
                    0.0f,
                    CoordinateBounds(10.0, 10.0, 0.0, 0.0)
                ),

                // East touches antimeridian, growth crosses it
                listOf(
                    CoordinateBounds(10.0, 180.0, 0.0, 179.0),
                    0.5f, // width=1 -> lonDelta=0.5; height=10 -> latDelta=5
                    CoordinateBounds(15.0, -179.5, -5.0, 178.5)
                ),

                // West touches antimeridian, growth crosses it
                listOf(
                    CoordinateBounds(10.0, -179.0, 0.0, -180.0),
                    0.5f,
                    CoordinateBounds(15.0, -178.5, -5.0, 179.5)
                ),
            )
        }
        fun provideFrom(): List<List<Any?>> {
            return listOf(
                // Full world exact extremes with check enabled
                listOf(
                    listOf(
                        Coordinate(10.0, -180.0),
                        Coordinate(10.0, 180.0),
                        Coordinate(-10.0, -180.0),
                        Coordinate(-10.0, 180.0)
                    ),
                    true,
                    CoordinateBounds(10.0, 180.0, -10.0, -180.0)
                ),

                // Approximate extremes with check enabled: use provided min/max longitudes
                listOf(
                    listOf(
                        Coordinate(5.0, -180.0),
                        Coordinate(5.0, 179.9995),
                        Coordinate(-5.0, -180.0),
                        Coordinate(-5.0, 179.9995)
                    ),
                    true,
                    CoordinateBounds(5.0, 179.9995, -5.0, -180.0)
                ),

                // Approximate extremes with check disabled: minimal wrap across antimeridian
                listOf(
                    listOf(
                        Coordinate(5.0, -180.0),
                        Coordinate(5.0, 179.9995),
                        Coordinate(-5.0, -180.0),
                        Coordinate(-5.0, 179.9995)
                    ),
                    false,
                    CoordinateBounds(5.0, -180.0, -5.0, 179.9995)
                ),

                // Exact extremes with check disabled: zero width, center at -180
                listOf(
                    listOf(
                        Coordinate(10.0, -180.0),
                        Coordinate(10.0, 180.0),
                        Coordinate(-10.0, -180.0),
                        Coordinate(-10.0, 180.0)
                    ),
                    false,
                    CoordinateBounds(10.0, -180.0, -10.0, -180.0)
                ),

                // Non-wrap mid-longitudes: west=-20, east=30
                listOf(
                    listOf(
                        Coordinate(10.0, -20.0),
                        Coordinate(10.0, 30.0),
                        Coordinate(-10.0, -20.0),
                        Coordinate(-10.0, 30.0)
                    ),
                    false,
                    CoordinateBounds(10.0, 30.0, -10.0, -20.0)
                ),

                // Wrap across antimeridian: west=170, east=-170
                listOf(
                    listOf(
                        Coordinate(10.0, 170.0),
                        Coordinate(10.0, -170.0),
                        Coordinate(-10.0, 170.0),
                        Coordinate(-10.0, -170.0)
                    ),
                    false,
                    CoordinateBounds(10.0, -170.0, -10.0, 170.0)
                ),
            )
        }

    }
}