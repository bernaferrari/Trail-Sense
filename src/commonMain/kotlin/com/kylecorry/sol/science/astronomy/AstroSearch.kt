package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.math.Range
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

internal object AstroSearch {

    /**
     * The maximum number of iterations to perform when searching for an event
     */
    private const val MAX_ITERATIONS = 100

    /**
     * Finds the start time of an event. The range should start before the event and end during the event.
     *
     * @param range the range to search
     * @param precision the precision of the search
     * @param predicate the predicate to test
     * @return the start time or null if not found
     */
    fun findStart(range: Range<Instant>, precision: Duration, predicate: (Instant) -> Boolean): Instant? {
        var left = range.start
        var right = range.end
        var iterations = 0
        var wasFound = predicate(right)

        while ((right - left) > precision && iterations < MAX_ITERATIONS) {
            val mid = left + ((right.toEpochMilliseconds() - left.toEpochMilliseconds()) / 2).milliseconds
            if (predicate(mid)) {
                wasFound = true
                right = mid
            } else {
                left = mid
            }
            iterations++
        }

        return if (wasFound) {
            right
        } else {
            null
        }
    }

    /**
     * Finds the end time of an event. The range should start during the event and end after the event.
     *
     * @param range the range to search
     * @param precision the precision of the search
     * @param predicate the predicate to test
     * @return the end time or null if not found
     */
    fun findEnd(range: Range<Instant>, precision: Duration, predicate: (Instant) -> Boolean): Instant? {
        var left = range.start
        var right = range.end
        var iterations = 0
        var wasFound = predicate(left)

        while ((right - left) > precision && iterations < MAX_ITERATIONS) {
            val mid = left + ((right.toEpochMilliseconds() - left.toEpochMilliseconds()) / 2).milliseconds
            if (predicate(mid)) {
                wasFound = true
                left = mid
            } else {
                right = mid
            }
            iterations++
        }

        return if (wasFound) {
            left
        } else {
            null
        }
    }

    /**
     * Finds the peak of an event. The range should be during the event.
     *
     * @param range the range to search
     * @param precision the precision of the search
     * @param producer the function to produce a value for a given time
     * @return the peak time
     */
    fun findPeak(range: Range<Instant>, precision: Duration, producer: (Instant) -> Float): Instant {
        var left = range.start
        var right = range.end
        var iterations = 0
        while ((right - left) > precision && iterations < MAX_ITERATIONS) {
            val remaining = (right - left).inWholeMilliseconds
            val midLeft = left + (remaining / 3).milliseconds
            val midRight = left + (remaining * 2 / 3).milliseconds
            val valueLeft = producer(midLeft)
            val valueRight = producer(midRight)
            if (valueLeft < valueRight) {
                left = midLeft
            } else {
                right = midRight
            }
            iterations++
        }

        return if (producer(left) > producer(right)) {
            left
        } else {
            right
        }
    }

    /**
     * Searches for an event within a range. Uses bidirectional search within the range. The event time returned
     * only indicates a time that the event is occurring, not necessarily a start, peak, or end time.
     *
     * @param range the range to search
     * @param precision the precision of the search
     * @param start the start time to search from, defaults to the middle of the range
     * @param predicate the predicate to test
     * @return the time of the event or null if not found
     */
    fun findEvent(
        range: Range<Instant>,
        precision: Duration,
        start: Instant = range.start + (range.end - range.start) / 2,
        predicate: (Instant) -> Boolean
    ): Instant? {
        var left = start
        var right = start
        var iterations = 0

        // An upper bound just to be safe
        val maxIterations = 2 * ((range.end - range.start).inWholeMilliseconds / precision.inWholeMilliseconds).toInt()

        while ((left >= range.start || right <= range.end) && iterations < maxIterations) {
            if (left >= range.start && predicate(left)) {
                return left
            }
            if (right <= range.end && right != left && predicate(right)) {
                return right
            }
            left -= precision
            right += precision
            iterations++
        }

        return null
    }

}
