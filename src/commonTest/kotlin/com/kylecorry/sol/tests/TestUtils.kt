package com.kylecorry.sol.tests

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.prop
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.reflect.typeOf
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.TimeSource

fun assertk.Assert<Coordinate>.isCloseTo(other: Coordinate, meters: Float) {
    prop("distance") {
        it.distanceTo(other)
    }.isLessThan(meters)
}

fun assertk.Assert<Distance>.isCloseTo(other: Distance, tolerance: Float) {
    prop("units", Distance::units).isEqualTo(other.units)
    prop("value", Distance::value).isCloseTo(other.value, tolerance)
}

fun <T> parametrized(cases: Collection<T>, test: (case: T) -> Unit) {
    for (case in cases) {
        test(case)
    }
}

/**
 * Converts a parameterized test argument to the declared parameter type, widening numbers the
 * same way JUnit's implicit argument conversion did (e.g. an Int literal for a Double parameter).
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> Any?.arg(): T {
    if (this == null) return null as T
    val classifier = typeOf<T>().classifier
    if (this is Number) {
        return when (classifier) {
            Double::class -> toDouble()
            Float::class -> toFloat()
            Long::class -> toLong()
            Int::class -> toInt()
            Short::class -> toShort()
            Byte::class -> toByte()
            else -> this
        } as T
    }
    return this as T
}

fun assertDate(
    expected: Instant?,
    actual: Instant?,
    maxDifference: Duration
) {
    if (expected == null) {
        assertNull(actual)
    } else {
        assertNotNull(actual, "Expected $expected, but was null")
        val diff = (expected - actual).absoluteValue
        assertTrue(diff <= maxDifference, "Expected $expected, found $actual")
    }
}

/**
 * Compares local date times by interpreting both in UTC, which keeps the difference independent
 * of daylight saving transitions.
 */
fun assertDate(
    expected: LocalDateTime?,
    actual: LocalDateTime?,
    maxDifference: Duration
) {
    assertDate(expected?.toInstant(TimeZone.UTC), actual?.toInstant(TimeZone.UTC), maxDifference)
}

fun assertDuration(
    expected: Duration?,
    actual: Duration?,
    maxDifference: Duration
) {
    if (expected == null) {
        assertNull(actual)
    } else {
        assertNotNull(actual, "Expected $expected, but was null")
        val diff = (expected - actual).absoluteValue
        assertTrue(diff <= maxDifference, "Expected $expected, found $actual")
    }
}

fun assertVector(
    expected: Vector2,
    actual: Vector2,
    tolerance: Float
) {
    assertThat(actual.x).isCloseTo(expected.x, tolerance)
    assertThat(actual.y).isCloseTo(expected.y, tolerance)
}

inline fun performanceTest(repetitions: Int, crossinline test: () -> Unit) {
    val mark = TimeSource.Monotonic.markNow()
    repeat(repetitions) { test() }
    val total = mark.elapsedNow()
    println("Performance test took $total, ${total / repetitions} per test")
}

fun assertContentEquals(expected: FloatArray, actual: FloatArray, tolerance: Float) {
    kotlin.test.assertEquals(expected.size, actual.size, "Array sizes differ")
    expected.indices.forEach { i ->
        kotlin.test.assertEquals(expected[i], actual[i], tolerance, "Arrays differ at index $i")
    }
}
