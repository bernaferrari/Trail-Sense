package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.math.Range
import java.time.Duration
import java.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinDuration
import kotlin.time.toKotlinInstant

internal object JvmAstroSearch {
    private fun Range<Instant>.toKotlinRange() = Range(start.toKotlinInstant(), end.toKotlinInstant())

    fun findStart(range: Range<Instant>, precision: Duration, predicate: (Instant) -> Boolean): Instant? =
        AstroSearch.findStart(range.toKotlinRange(), precision.toKotlinDuration()) {
            predicate(it.toJavaInstant())
        }?.toJavaInstant()

    fun findEnd(range: Range<Instant>, precision: Duration, predicate: (Instant) -> Boolean): Instant? =
        AstroSearch.findEnd(range.toKotlinRange(), precision.toKotlinDuration()) {
            predicate(it.toJavaInstant())
        }?.toJavaInstant()

    fun findPeak(range: Range<Instant>, precision: Duration, producer: (Instant) -> Float): Instant =
        AstroSearch.findPeak(range.toKotlinRange(), precision.toKotlinDuration()) {
            producer(it.toJavaInstant())
        }.toJavaInstant()

    fun findEvent(
        range: Range<Instant>, precision: Duration, start: Instant, predicate: (Instant) -> Boolean
    ): Instant? = AstroSearch.findEvent(range.toKotlinRange(), precision.toKotlinDuration(), start.toKotlinInstant()) {
        predicate(it.toJavaInstant())
    }?.toJavaInstant()
}
